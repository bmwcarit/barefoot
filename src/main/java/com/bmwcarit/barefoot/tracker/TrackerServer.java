/*
 * Copyright (C) 2016, BMW Car IT GmbH
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
package com.bmwcarit.barefoot.tracker;

import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import com.bmwcarit.barefoot.matcher.Matcher;
import com.bmwcarit.barefoot.matcher.MatcherCandidate;
import com.bmwcarit.barefoot.matcher.MatcherKState;
import com.bmwcarit.barefoot.matcher.MatcherSample;
import com.bmwcarit.barefoot.matcher.MatcherTransition;
import com.bmwcarit.barefoot.roadmap.Road;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.roadmap.RoadPoint;
import com.bmwcarit.barefoot.roadmap.TimePriority;
import com.bmwcarit.barefoot.scheduler.StaticScheduler;
import com.bmwcarit.barefoot.scheduler.StaticScheduler.InlineScheduler;
import com.bmwcarit.barefoot.scheduler.Task;
import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.spatial.SpatialOperator;
import com.bmwcarit.barefoot.topology.Dijkstra;
import com.bmwcarit.barefoot.util.AbstractServer;
import com.bmwcarit.barefoot.util.Stopwatch;
import com.bmwcarit.barefoot.util.Tuple;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WktExportFlags;

/**
 * Tracker server (stand-alone) for Hidden Markov Model online map matching. It is a
 * {@link AbstractServer} that performs online map matching; and is configurable with a properties
 * file. It pushes updates via a ZeroMQ publisher port to subscribing listeners.
 */
public class TrackerServer extends AbstractServer {
    private final static Logger logger = LoggerFactory.getLogger(TrackerServer.class);
    private final RoadMap map;

    /**
     * Creates a {@link TrackerServer} object as stand-alone online map matching server. The
     * provided {@link Properties} object may provide the following properties:
     * <ul>
     * <li>server properties: see {@link AbstractServer#AbstractServer(Properties, ResponseFactory)}
     * </li>
     * <li>matcher.radius.max (see {@link Matcher#setMaxRadius(double)})</li>
     * <li>matcher.distance.max (see {@link Matcher#setMaxDistance(double)})</li>
     * <li>matcher.lambda (see {@link Matcher#setLambda(double)})</li>
     * <li>matcher.sigma (see {@link Matcher#setSigma(double)})</li>
     * <li>tracker.port (optional, default: 1235)</li>
     * <li>tracker.ttl (seconds, optional, default: 60, sets time to live of state information for
     * tracked objects which is infinite if set to zero)</li>
     * </ul>
     *
     * @param properties {@link Properties} object with (optional) server and matcher settings.
     * @param map {@link RoadMap} object with the map to be matched with.
     */
    public TrackerServer(Properties properties, RoadMap map) {
        super(properties, new MatcherResponseFactory(properties, map));
        this.map = map;
    }

    /**
     * Gets {@link RoadMap} object of the server.
     *
     * @return {@link RoadMap} object of the server.
     */
    public RoadMap getMap() {
        return this.map;
    }

    private static class MatcherResponseFactory extends ResponseFactory {
        private final Matcher matcher;
        private final Memory memory;
        private final Publisher publisher;

        public MatcherResponseFactory(Properties properties, RoadMap map) {
            matcher =
                    new Matcher(map, new Dijkstra<Road, RoadPoint>(), new TimePriority(),
                            new Geography());

            matcher.setMaxRadius(Double.parseDouble(properties.getProperty("matcher.radius.max",
                    Double.toString(matcher.getMaxRadius()))));
            matcher.setMaxDistance(Double.parseDouble(properties.getProperty(
                    "matcher.distance.max", Double.toString(matcher.getMaxDistance()))));
            matcher.setLambda(Double.parseDouble(properties.getProperty("matcher.lambda",
                    Double.toString(matcher.getLambda()))));
            matcher.setSigma(Double.parseDouble(properties.getProperty("matcher.sigma",
                    Double.toString(matcher.getSigma()))));
            publisher =
                    new Publisher(Integer.parseInt(properties.getProperty("tracker.port", "1235")));
            memory =
                    new Memory(Integer.parseInt(properties.getProperty("tracker.state.ttl", "60")),
                            publisher);

            int matcherThreads =
                    Integer.parseInt(properties.getProperty("matcher.threads",
                            Integer.toString(Runtime.getRuntime().availableProcessors())));

            StaticScheduler.reset(matcherThreads, (long) 1E4);

            logger.info("matcher.radius.max={}", matcher.getMaxRadius());
            logger.info("matcher.distance.max={}", matcher.getMaxDistance());
            logger.info("matcher.lambda={}", matcher.getLambda());
            logger.info("matcher.sigma={}", matcher.getSigma());
            logger.info("matcher.threads={}", matcherThreads);
            logger.info("tracker.state.ttl={}", memory.getTTL());
            logger.info("tracker.port={}", publisher.getPort());
        }

        @Override
        protected ResponseHandler response(String request) {
            return new ResponseHandler(request) {
                @Override
                protected RESULT response(String request, StringBuilder response) {
                    try {
                        JSONObject json = new JSONObject(request);

                        if (!json.optString("id").isEmpty()) {
                            if (!json.optString("time").isEmpty()
                                    && !json.optString("point").isEmpty()) {

                                final MatcherSample sample = new MatcherSample(json);
                                final Memory.State state = memory.getLocked(sample.id());

                                if (state.sample() != null) {
                                    if (sample.time() < state.sample().time()) {
                                        state.updateFailedAndUnlock();
                                        logger.warn("received out of order sample");
                                        return RESULT.ERROR;
                                    }
                                }

                                final AtomicReference<Set<MatcherCandidate>> vector =
                                        new AtomicReference<Set<MatcherCandidate>>();
                                InlineScheduler scheduler = StaticScheduler.scheduler();
                                scheduler.spawn(new Task() {
                                    @Override
                                    public void run() {
                                        Stopwatch sw = new Stopwatch();
                                        sw.start();
                                        vector.set(matcher.execute(state.vector(), state.sample(),
                                                sample));
                                        sw.stop();
                                        logger.info("state update of object {} processed in {} ms",
                                                sample.id(), sw.ms());
                                    }
                                });

                                if (!scheduler.sync()) {
                                    state.updateFailedAndUnlock();
                                    throw new RuntimeException("matcher execution error");
                                } else {
                                    state.updateAndUnlock(vector.get(), sample);
                                    return RESULT.SUCCESS;
                                }
                            } else {
                                String id = json.getString("id");
                                logger.info("received state request for object {}", id);

                                return RESULT.SUCCESS;
                            }
                        } else if (json.optJSONArray("roads") != null) {
                            logger.debug("received road data request");

                            return RESULT.SUCCESS;
                        } else {
                            throw new RuntimeException("JSON request faulty or incomplete: "
                                    + request);
                        }
                    } catch (Exception e) {
                        logger.error("{}", e.getMessage());
                        e.printStackTrace();
                        return RESULT.ERROR;
                    }
                }
            };
        }
    }

    private static class Memory {
        private final Map<String, State> states = new HashMap<String, State>();
        private final Queue<Tuple<Long, State>> queue =
                new PriorityBlockingQueue<Tuple<Long, State>>(1,
                        new Comparator<Tuple<Long, State>>() {
                            @Override
                            public int compare(Tuple<Long, State> left, Tuple<Long, State> right) {
                                return (int) (left.one() - right.one());
                            }
                        });
        private final int TTL;
        private final Publisher publisher;
        private final Thread cleaner;
        private final SpatialOperator spatial = new Geography();

        private class State extends MatcherKState {
            private final Lock lock = new ReentrantLock();
            private String id;
            private long death = 0;

            public void updateAndUnlock(Set<MatcherCandidate> vector, MatcherSample sample) {
                super.update(vector, sample);

                if (logger.isTraceEnabled()) {
                    try {
                        logger.trace(
                                "filter: ({}, {}) with prob {} ({} m distance, {} m route) for sample {}",
                                super.estimate().point().edge().id(),
                                super.estimate().point().fraction(),
                                super.estimate().filtprob(),
                                spatial.distance(sample.point(), super.estimate().point()
                                        .geometry()), super.estimate().transition() != null ? super
                                        .estimate().transition().route().length() : 0,
                                sample.toJSON());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                id = sample.id();
                death = Math.max(death + 1, Calendar.getInstance().getTimeInMillis() + TTL * 1000);
                queue.add(new Tuple<Long, State>(death, this));
                publisher.send(sample.id(), this);
                lock.unlock();
            }

            public void updateFailedAndUnlock() {
                lock.unlock();
            }
        }

        public Memory(final int TTL, Publisher publisher) {
            this.TTL = TTL;
            this.publisher = publisher;

            cleaner = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (true) {
                        Tuple<Long, State> entry = queue.poll();
                        if (entry == null) {
                            try {
                                Thread.sleep(TTL * 1000 + 1);
                                continue;
                            } catch (InterruptedException e) {
                                //
                            }
                        }

                        while (entry.one() > Calendar.getInstance().getTimeInMillis()) {
                            try {
                                Thread.sleep(entry.one() - Calendar.getInstance().getTimeInMillis()
                                        + 1);
                            } catch (InterruptedException e) {
                                //
                            }
                        }

                        removeIfDead(entry.two());
                    }
                }
            });

            if (TTL > 0) {
                cleaner.start();
            }
        }

        public int getTTL() {
            return this.TTL;
        }

        public synchronized State getLocked(String id) {
            State state = states.get(id);
            if (state == null) {
                state = new State();
                states.put(id, state);
            }
            state.lock.lock();
            return state;
        }

        private synchronized void removeIfDead(State state) {
            state.lock.lock();
            if (state.death <= Calendar.getInstance().getTimeInMillis()) {
                states.remove(state.id);
                publisher.delete(state.id, state.death);
                logger.info("state of object {} expired and deleted", state.id);
            }
            state.lock.unlock();
        }
    }

    private static class Publisher {
        ZMQ.Socket publisher = null;
        private final int port;

        public Publisher(int port) {
            this.port = port;
            ZMQ.Context context = ZMQ.context(1);
            publisher = context.socket(ZMQ.PUB);
            publisher.bind("tcp://*:" + port);
        }

        public int getPort() {
            return this.port;
        }

        public Polyline getRoute(MatcherCandidate candidate) {
            Polyline routes = new Polyline();
            MatcherCandidate predecessor = candidate;
            while (predecessor != null) {
                MatcherTransition transition = predecessor.transition();
                if (transition != null) {
                    Polyline route = transition.route().geometry();
                    routes.startPath(route.getPoint(0));
                    for (int i = 1; i < route.getPointCount(); ++i) {
                        routes.lineTo(route.getPoint(i));
                    }
                }
                predecessor = predecessor.predecessor();
            }
            return routes;
        }

        public void send(String id, MatcherKState state) {
            try {
                JSONObject json = new JSONObject();
                json.put("id", id);
                json.put("time", state.sample().time());
                json.put("point", GeometryEngine.geometryToWkt(state.estimate().point().geometry(),
                        WktExportFlags.wktExportPoint));
                Polyline routes = getRoute(state.estimate());
                if (routes.getPathCount() > 0) {
                    json.put("route", GeometryEngine.geometryToWkt(routes,
                            WktExportFlags.wktExportMultiLineString));
                }

                JSONArray candidates = new JSONArray();
                for (MatcherCandidate candidate : state.vector()) {
                    JSONObject jsoncandidate = new JSONObject();
                    jsoncandidate.put("point", GeometryEngine.geometryToWkt(candidate.point()
                            .geometry(), WktExportFlags.wktExportPoint));
                    jsoncandidate.put("prob", Double.isInfinite(candidate.filtprob()) ? "Infinity"
                            : candidate.filtprob());

                    routes = getRoute(candidate);
                    if (routes.getPathCount() > 0) {
                        jsoncandidate.put("route", GeometryEngine.geometryToWkt(routes,
                                WktExportFlags.wktExportMultiLineString));
                    }
                    candidates.put(jsoncandidate);
                }
                json.put("candidates", candidates);
                publisher.send(json.toString());
            } catch (JSONException e) {
                logger.error("update failed: {}", e.getMessage());
                e.printStackTrace();
            }
        }

        public void delete(String id, long time) {
            try {
                JSONObject json = new JSONObject();
                json.put("id", id);
                json.put("time", time);
                publisher.send(json.toString());
            } catch (JSONException e) {
                logger.error("delete failed: {}", e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
