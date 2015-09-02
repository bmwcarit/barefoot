/*
 * Copyright (C) 2015, BMW Car IT GmbH
 *
 * Author: Sebastian Mattheis <sebastian.mattheis@bmw-carit.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.bmwcarit.barefoot.matcher;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmwcarit.barefoot.markov.KState;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.scheduler.StaticScheduler;
import com.bmwcarit.barefoot.scheduler.StaticScheduler.InlineScheduler;
import com.bmwcarit.barefoot.scheduler.Task;
import com.bmwcarit.barefoot.util.AbstractServer;
import com.bmwcarit.barefoot.util.Stopwatch;

/**
 * Server (stand-alone) for Hidden Markov Model offline map matching. It is a {@link AbstractServer}
 * that uses map matching components matcher, state, router, and map; and is configurable via
 * properties files. It can be customized to use arbitrary input and output formats by inheriting
 * and customizing {@link InputFormatter} and {@link OutputFormatter} classes.
 */
public class Server extends AbstractServer {
    private final static Logger logger = LoggerFactory.getLogger(Server.class);
    public final RoadMap map;
    public final Matcher matcher;

    /**
     * Default input formatter for reading a JSON array of {@link MatcherSample} objects as input
     * for map matching.
     */
    public static class InputFormatter {
        /**
         * Converts a request message into a list of {@link MatcherSample} objects as input for map
         * matching.
         *
         * @param input JSON input format of sample data.
         * @return List of {@link MatcherSample} objects.
         */
        public List<MatcherSample> format(String input) {
            List<MatcherSample> samples = new LinkedList<MatcherSample>();

            try {
                Object jsoninput = new JSONTokener(input).nextValue();
                JSONArray jsonsamples = null;

                if (jsoninput instanceof JSONObject) {
                    jsonsamples = ((JSONObject) jsoninput).getJSONArray("request");
                } else {
                    jsonsamples = ((JSONArray) jsoninput);
                }

                Set<Long> times = new HashSet<Long>();
                for (int i = 0; i < jsonsamples.length(); ++i) {
                    MatcherSample sample = new MatcherSample(jsonsamples.getJSONObject(i));
                    samples.add(sample);
                    if (times.contains(sample.time())) {
                        throw new RuntimeException("multiple samples for same time");
                    } else {
                        times.add(sample.time());
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                throw new RuntimeException("parsing JSON request: " + e.getMessage());
            }

            return samples;
        }
    }

    /**
     * Default output formatter for writing the JSON representation of a {@link KState} object with
     * map matching of the input into a response message.
     */
    public static class OutputFormatter {
        /**
         * Converts map matching output from a {@link KState} object into a response message.
         *
         * @param request String message of input data.
         * @param output {@link MatcherKState} object with the map matching of the input.
         * @return Response message.
         */
        public String format(String request, MatcherKState output) {
            try {
                return output.toJSON().toString();
            } catch (JSONException e) {
                throw new RuntimeException("creating JSON response: " + e.getMessage());
            }
        }
    }

    /**
     * Output formatter for writing map matched positions, represented be road id and fraction, and
     * the geometry of the routes into a JSON response message.
     */
    public static class SlimJSONOutputFormatter extends OutputFormatter {
        @Override
        public String format(String request, MatcherKState output) {
            try {
                return output.toSlimJSON().toString();
            } catch (JSONException e) {
                throw new RuntimeException("creating JSON response");
            }
        }
    }

    /**
     * Output formatter for writing the geometries of a map matched paths into GeoJSON response
     * message.
     */
    public static class GeoJSONOutputFormatter extends OutputFormatter {
        @Override
        public String format(String request, MatcherKState output) {
            try {
                return output.toGeoJSON().toString();
            } catch (JSONException e) {
                throw new RuntimeException("creating JSON response");
            }
        }
    }

    /**
     * Output formatter for writing extensive format of input and output of map matching in a JSON
     * response message.
     */
    public static class DebugJSONOutputFormatter extends OutputFormatter {
        @Override
        public String format(String request, MatcherKState output) {
            try {
                return output.toDebugJSON();
            } catch (JSONException e) {
                throw new RuntimeException("creating JSON response: " + e.getMessage());
            }
        }
    }

    private static class AdaptiveOutputFormatter extends OutputFormatter {
        private final OutputFormatter defaultFormatter;

        public AdaptiveOutputFormatter(OutputFormatter defaultFormatter) {
            this.defaultFormatter = defaultFormatter;
        }

        @Override
        public String format(String request, MatcherKState output) {
            try {
                Object jsonrequest = new JSONTokener(request).nextValue();

                if (jsonrequest instanceof JSONObject) {
                    String jsonformat = ((JSONObject) jsonrequest).optString("format");
                    if (jsonformat != null) {
                        switch (jsonformat) {
                            case "json":
                                return new OutputFormatter().format(request, output);
                            case "slimjson":
                                return new SlimJSONOutputFormatter().format(request, output);
                            case "geojson":
                                return new GeoJSONOutputFormatter().format(request, output);
                            case "debug":
                                return new DebugJSONOutputFormatter().format(request, output);
                            default:
                                break;
                        }
                    }
                }

                return defaultFormatter.format(request, output);
            } catch (JSONException e) {
                throw new RuntimeException("creating JSON response: " + e.getMessage());
            }
        }
    }

    /**
     * Creates a {@link Server} object as stand-alone offline map matching server.
     *
     * @param portNumber Port number of the server.
     * @param maxRequestTime Maximum request waiting time until a timeout is returned.
     * @param maxResponseTime Maximum response processing time until a timeout is returned.
     * @param maxConnectionCount Maximum number of connections being accepted by the server.
     * @param numIOThreads Number of threads for connections (I/O).
     * @param matcherMinInterval Minimum time interval between samples for being accepted as map
     *        matching input.
     * @param matcherMinDistance Minimum distance between samples for being accepted as map matching
     *        input.
     * @param map {@link RoadMap} object with the map to be matched with.
     * @param matcher {@link Matcher} object to be used for map matching (with respective
     *        parameterization).
     * @param input {@link InputFormatter} object for input formatting.
     * @param output {@link OutputFormatter} object for output formatting.
     */
    public Server(int portNumber, int maxRequestTime, int maxResponseTime, int maxConnectionCount,
            int numIOThreads, int matcherMinInterval, double matcherMinDistance, RoadMap map,
            Matcher matcher, InputFormatter input, OutputFormatter output) {
        super(portNumber, maxRequestTime, maxResponseTime, maxConnectionCount, numIOThreads,
                new MatcherResponseFactory(matcher, input, output, matcherMinInterval,
                        matcherMinDistance));
        this.map = map;
        this.matcher = matcher;
    }

    private static class MatcherResponseFactory extends ResponseFactory {
        private final Matcher matcher;
        private final InputFormatter input;
        private final OutputFormatter output;
        private final int minInterval;
        private final double minDistance;

        public MatcherResponseFactory(Matcher matcher, InputFormatter input,
                OutputFormatter output, int minInterval, double minDistance) {
            this.matcher = matcher;
            this.input = input;
            this.output = new AdaptiveOutputFormatter(output);
            this.minInterval = minInterval;
            this.minDistance = minDistance;
        }

        @Override
        protected ResponseHandler response(String request) {
            return new ResponseHandler(request) {
                @Override
                protected RESULT response(String request, StringBuilder response) {
                    try {
                        Stopwatch sw = new Stopwatch();
                        sw.start();

                        final List<MatcherSample> samples = input.format(request);
                        final AtomicReference<MatcherKState> state =
                                new AtomicReference<MatcherKState>();

                        InlineScheduler scheduler = StaticScheduler.scheduler();
                        scheduler.spawn(new Task() {
                            @Override
                            public void run() {
                                state.set(matcher.mmatch(samples, minDistance, minInterval));
                            }
                        });
                        if (!scheduler.sync()) {
                            return RESULT.ERROR;
                        }

                        String result = output.format(request, state.get());
                        response.append(result);

                        sw.stop();
                        logger.info("response processed in {} ms", sw.ms());

                        return RESULT.SUCCESS;
                    } catch (RuntimeException e) {
                        logger.error("{}", e.getMessage());
                        e.printStackTrace();
                        return RESULT.ERROR;
                    }
                }
            };
        }
    }
}
