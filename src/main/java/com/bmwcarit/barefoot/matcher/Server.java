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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmwcarit.barefoot.markov.KState;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.scheduler.StaticScheduler;
import com.bmwcarit.barefoot.scheduler.Task;
import com.bmwcarit.barefoot.scheduler.StaticScheduler.InlineScheduler;
import com.bmwcarit.barefoot.util.AbstractServer;
import com.bmwcarit.barefoot.util.Stopwatch;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.WktExportFlags;

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
     * Default input formatter for reading a JSON array of {@link MatcherSample} objects as input for map
     * matching.
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
                Set<Long> times = new HashSet<Long>();
                JSONArray jsonrequest = new JSONArray(input);
                for (int i = 0; i < jsonrequest.length(); ++i) {
                    MatcherSample sample = new MatcherSample(jsonrequest.getJSONObject(i));
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
         * @param output {@link KState} object with the map matching of the input.
         * @return Output message with map matching result.
         */
        public String format(String request, KState<MatcherCandidate, MatcherTransition, MatcherSample> output) {
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
        public String format(String request, KState<MatcherCandidate, MatcherTransition, MatcherSample> output) {
            try {
                JSONArray jsonsequence = new JSONArray();
                if (output.sequence() != null) {
                    for (MatcherCandidate candidate : output.sequence()) {
                        JSONObject jsoncandidate = new JSONObject();
                        jsoncandidate.put("road", candidate.point().edge().id());
                        jsoncandidate.put("frac", candidate.point().fraction());
                        if (candidate.transition() != null) {
                            jsoncandidate.put(
                                    "route",
                                    GeometryEngine.geometryToWkt(candidate.transition().route()
                                            .geometry(), WktExportFlags.wktExportLineString));
                        }
                        jsonsequence.put(jsoncandidate);
                    }
                }
                return jsonsequence.toString();
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
        public String format(String request, KState<MatcherCandidate, MatcherTransition, MatcherSample> output) {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "MultiLineString");
                JSONArray jsonsequence = new JSONArray();
                if (output.sequence() != null) {
                    for (MatcherCandidate candidate : output.sequence()) {
                        if (candidate.transition() == null) {
                            continue;
                        }
                        JSONObject jsoncandidate =
                                new JSONObject(GeometryEngine.geometryToGeoJson(candidate
                                        .transition().route().geometry()));
                        jsonsequence.put(jsoncandidate.getJSONArray("coordinates"));
                    }
                }
                json.put("coordinates", jsonsequence);
                return json.toString();
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
        public String format(String request, KState<MatcherCandidate, MatcherTransition, MatcherSample> output) {
            try {
                StringBuilder response = new StringBuilder();

                JSONArray jsonsamples = new JSONArray();
                if (output.samples() != null) {
                    for (int i = 0; i < output.samples().size(); ++i) {
                        JSONObject jsonsample = new JSONObject();
                        jsonsample.put("id", output.samples().get(i).id());
                        jsonsample.put("geom", GeometryEngine.geometryToWkt(output.samples().get(i)
                                .point(), WktExportFlags.wktExportPoint));
                        jsonsample.put("time", output.samples().get(i).time() / 1000);
                        jsonsamples.put(jsonsample);
                    }
                }
                response.append(jsonsamples.toString());
                response.append("\n");

                JSONArray jsonsequence = new JSONArray();
                if (output.sequence() != null) {
                    for (int i = 0; i < output.sequence().size(); ++i) {
                        MatcherCandidate candidate = output.sequence().get(i);
                        JSONObject jsoncandidate = new JSONObject();
                        jsoncandidate.put("id", candidate.id());
                        jsoncandidate.put("time", output.samples().get(i).time() / 1000);
                        jsoncandidate.put("road", candidate.point().edge().id());
                        jsoncandidate.put("frac", candidate.point().fraction());
                        if (candidate.transition() != null) {
                            jsoncandidate.put(
                                    "geom",
                                    GeometryEngine.geometryToWkt(candidate.transition().route()
                                            .geometry(), WktExportFlags.wktExportLineString));
                            StringBuilder roads = new StringBuilder();
                            for (int j = 0; j < candidate.transition().route().size(); ++j) {
                                roads.append(candidate.transition().route().get(j).id() + " ");
                            }
                            jsoncandidate.put("roads", roads);
                        } else {
                            jsoncandidate.put("geom", GeometryEngine.geometryToWkt(candidate.point()
                                    .geometry(), WktExportFlags.wktExportPoint));
                        }
                        jsonsequence.put(jsoncandidate);
                    }
                }
                response.append(jsonsequence.toString());

                return response.toString();
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
     * @param map {@link RoadMap} object with the map to be matched with.
     * @param matcher {@link Matcher} object to be used for map matching (with respective
     *        parameterization).
     * @param input {@link InputFormatter} object for input formatting.
     * @param output {@link OutputFormatter} object for output formatting.
     */
    public Server(int portNumber, int maxRequestTime, int maxResponseTime, int maxConnectionCount,
            int numIOThreads, int matcherMinInterval, RoadMap map, Matcher matcher,
            InputFormatter input, OutputFormatter output) {
        super(portNumber, maxRequestTime, maxResponseTime, maxConnectionCount, numIOThreads,
                new MatcherResponseFactory(matcher, input, output, matcherMinInterval, 100));
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
            this.output = output;
            this.minInterval = Math.max(0, minInterval);
            this.minDistance = Math.max(0, minDistance);
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
                        final KState<MatcherCandidate, MatcherTransition, MatcherSample> state =
                                new KState<MatcherCandidate, MatcherTransition, MatcherSample>();

                        InlineScheduler scheduler = StaticScheduler.scheduler();
                        scheduler.spawn(new Task() {
                            @Override
                            public void run() {
                                MatcherSample previous = null;
                                for (MatcherSample sample : samples) {
                                    if (previous != null
                                            && (sample.time() - previous.time()) < minInterval) {
                                        continue;
                                    }

                                    Set<MatcherCandidate> vector =
                                            matcher.execute(state.vector(), state.sample(), sample);
                                    state.update(vector, sample);
                                    previous = sample;
                                }
                            }
                        });
                        if (!scheduler.sync()) {
                            return RESULT.ERROR;
                        }

                        String result = output.format(request, state);
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
