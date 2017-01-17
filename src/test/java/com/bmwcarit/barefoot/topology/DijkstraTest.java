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

package com.bmwcarit.barefoot.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

import com.bmwcarit.barefoot.util.Tuple;

public class DijkstraTest {

    private static class Road extends AbstractEdge<Road> {
        private static final long serialVersionUID = 1L;
        private final long id;
        private final long source;
        private final long target;
        private float weight;

        public Road(long id, long source, long target, float weight) {
            this.id = id;
            this.source = source;
            this.target = target;
            this.weight = weight;
        }

        @Override
        public long id() {
            return id;
        }

        @Override
        public long source() {
            return source;
        }

        @Override
        public long target() {
            return target;
        }

        public float weight() {
            return this.weight;
        }
    }

    private static class Weight extends Cost<Road> {
        @Override
        public double cost(Road edge) {
            return edge.weight();
        }
    }

    @Test
    public void testSameRoad() {
        Graph<Road> map = new Graph<>();
        map.add(new Road(0, 0, 1, 100));
        map.add(new Road(1, 1, 0, 20));
        map.add(new Road(2, 0, 2, 100));
        map.add(new Road(3, 1, 2, 100));
        map.add(new Road(4, 1, 3, 100));
        map.construct();

        Router<Road, Point<Road>> router = new Dijkstra<>();

        {
            Set<Point<Road>> sources = new HashSet<>();
            sources.add(new Point<>(map.get(0), 0.3));
            Set<Point<Road>> targets = new HashSet<>();
            targets.add(new Point<>(map.get(0), 0.3));

            Map<Point<Road>, Tuple<Point<Road>, List<Road>>> routes =
                    router.route(sources, targets, new Weight(), null, null);

            Tuple<Point<Road>, List<Road>> route = routes.get(targets.iterator().next());
            List<Long> path = new LinkedList<>(Arrays.asList(0L));

            assertNotNull(route);
            assertEquals(path.get(0).longValue(), route.one().edge().id());
            assertEquals(path.size(), route.two().size());

            int i = 0;
            for (Road road : route.two()) {
                assertEquals((long) path.get(i++), road.id());
            }
        }
        {
            Set<Point<Road>> sources = new HashSet<>();
            sources.add(new Point<>(map.get(0), 0.3));
            Set<Point<Road>> targets = new HashSet<>();
            targets.add(new Point<>(map.get(0), 0.7));

            Map<Point<Road>, Tuple<Point<Road>, List<Road>>> routes =
                    router.route(sources, targets, new Weight(), null, null);

            Tuple<Point<Road>, List<Road>> route = routes.get(targets.iterator().next());
            List<Long> path = new LinkedList<>(Arrays.asList(0L));

            assertNotNull(route);
            assertEquals(path.get(0).longValue(), route.one().edge().id());
            assertEquals(path.size(), route.two().size());

            int i = 0;
            for (Road road : route.two()) {
                assertEquals((long) path.get(i++), road.id());
            }
        }
        {
            Set<Point<Road>> sources = new HashSet<>();
            sources.add(new Point<>(map.get(0), 0.7));
            Set<Point<Road>> targets = new HashSet<>();
            targets.add(new Point<>(map.get(0), 0.3));

            Map<Point<Road>, Tuple<Point<Road>, List<Road>>> routes =
                    router.route(sources, targets, new Weight(), null, null);

            Tuple<Point<Road>, List<Road>> route = routes.get(targets.iterator().next());
            List<Long> path = new LinkedList<>(Arrays.asList(0L, 1L, 0L));

            assertNotNull(route);
            assertEquals(path.get(0).longValue(), route.one().edge().id());
            assertEquals(path.size(), route.two().size());

            int i = 0;
            for (Road road : route.two()) {
                assertEquals((long) path.get(i++), road.id());
            }
        }
        {
            Set<Point<Road>> sources = new HashSet<>();
            sources.add(new Point<>(map.get(0), 0.8));
            sources.add(new Point<>(map.get(1), 0.2));
            Set<Point<Road>> targets = new HashSet<>();
            targets.add(new Point<>(map.get(0), 0.7));

            Map<Point<Road>, Tuple<Point<Road>, List<Road>>> routes =
                    router.route(sources, targets, new Weight(), null, null);

            Tuple<Point<Road>, List<Road>> route = routes.get(targets.iterator().next());
            List<Long> path = new LinkedList<>(Arrays.asList(1L, 0L));

            assertNotNull(route);
            assertEquals(path.get(0).longValue(), route.one().edge().id());
            assertEquals(path.size(), route.two().size());

            int i = 0;
            for (Road road : route.two()) {
                assertEquals((long) path.get(i++), road.id());
            }
        }
    }

    @Test
    public void testSelfLoop() {
        Graph<Road> map = new Graph<>();
        map.add(new Road(0, 0, 0, 100));
        map.add(new Road(1, 0, 0, 100));
        map.construct();

        Router<Road, Point<Road>> router = new Dijkstra<>();

        {
            Set<Point<Road>> sources = new HashSet<>();
            sources.add(new Point<>(map.get(0), 0.3));
            Set<Point<Road>> targets = new HashSet<>();
            targets.add(new Point<>(map.get(0), 0.7));

            Map<Point<Road>, Tuple<Point<Road>, List<Road>>> routes =
                    router.route(sources, targets, new Weight(), null, null);

            Tuple<Point<Road>, List<Road>> route = routes.get(targets.iterator().next());
            List<Long> path = new LinkedList<>(Arrays.asList(0L));

            assertNotNull(route);
            assertEquals(path.get(0).longValue(), route.one().edge().id());
            assertEquals(path.size(), route.two().size());

            int i = 0;
            for (Road road : route.two()) {
                assertEquals((long) path.get(i++), road.id());
            }
        }
        {
            Set<Point<Road>> sources = new HashSet<>();
            sources.add(new Point<>(map.get(0), 0.7));
            Set<Point<Road>> targets = new HashSet<>();
            targets.add(new Point<>(map.get(0), 0.3));

            Map<Point<Road>, Tuple<Point<Road>, List<Road>>> routes =
                    router.route(sources, targets, new Weight(), null, null);

            Tuple<Point<Road>, List<Road>> route = routes.get(targets.iterator().next());
            List<Long> path = new LinkedList<>(Arrays.asList(0L, 0L));

            assertNotNull(route);
            assertEquals(path.get(0).longValue(), route.one().edge().id());
            assertEquals(path.size(), route.two().size());

            int i = 0;
            for (Road road : route.two()) {
                assertEquals((long) path.get(i++), road.id());
            }
        }
        {
            Set<Point<Road>> sources = new HashSet<>();
            sources.add(new Point<>(map.get(0), 0.8));
            sources.add(new Point<>(map.get(1), 0.2));
            Set<Point<Road>> targets = new HashSet<>();
            targets.add(new Point<>(map.get(0), 0.2));

            Map<Point<Road>, Tuple<Point<Road>, List<Road>>> routes =
                    router.route(sources, targets, new Weight(), null, null);

            Tuple<Point<Road>, List<Road>> route = routes.get(targets.iterator().next());
            List<Long> path = new LinkedList<>(Arrays.asList(0L, 0L));

            assertNotNull(route);
            assertEquals(path.get(0).longValue(), route.one().edge().id());
            assertEquals(path.size(), route.two().size());

            int i = 0;
            for (Road road : route.two()) {
                assertEquals((long) path.get(i++), road.id());
            }
        }
        {
            Set<Point<Road>> sources = new HashSet<>();
            sources.add(new Point<>(map.get(0), 0.4));
            sources.add(new Point<>(map.get(1), 0.6));
            Set<Point<Road>> targets = new HashSet<>();
            targets.add(new Point<>(map.get(0), 0.3));

            Map<Point<Road>, Tuple<Point<Road>, List<Road>>> routes =
                    router.route(sources, targets, new Weight(), null, null);

            Tuple<Point<Road>, List<Road>> route = routes.get(targets.iterator().next());
            List<Long> path = new LinkedList<>(Arrays.asList(1L, 0L));

            assertNotNull(route);
            assertEquals(path.get(0).longValue(), route.one().edge().id());
            assertEquals(path.size(), route.two().size());

            int i = 0;
            for (Road road : route.two()) {
                assertEquals((long) path.get(i++), road.id());
            }
        }
    }

    @Test
    public void testShortestPath() {
        {
            Graph<Road> map = new Graph<>();
            map.add(new Road(0, 0, 1, 100));
            map.add(new Road(1, 1, 0, 100));
            map.add(new Road(2, 0, 2, 160));
            map.add(new Road(3, 2, 0, 160));
            map.add(new Road(4, 1, 2, 50));
            map.add(new Road(5, 2, 1, 50));
            map.add(new Road(6, 1, 3, 200));
            map.add(new Road(7, 3, 1, 200));
            map.add(new Road(8, 2, 3, 100));
            map.add(new Road(9, 3, 2, 100));
            map.add(new Road(10, 2, 4, 40));
            map.add(new Road(11, 4, 2, 40));
            map.add(new Road(12, 3, 4, 100));
            map.add(new Road(13, 4, 3, 100));
            map.add(new Road(14, 3, 5, 200));
            map.add(new Road(15, 5, 3, 200));
            map.add(new Road(16, 4, 5, 60));
            map.add(new Road(17, 5, 4, 60));
            map.construct();

            Router<Road, Point<Road>> router = new Dijkstra<>();
            {
                // (0.7, 100) + 50 + 40 + 60 + (0.3, 200) = 280

                Set<Point<Road>> sources = new HashSet<>();
                sources.add(new Point<>(map.get(0), 0.3));
                sources.add(new Point<>(map.get(1), 0.7));

                Set<Point<Road>> targets = new HashSet<>();
                targets.add(new Point<>(map.get(14), 0.3));
                targets.add(new Point<>(map.get(15), 0.7));

                Map<Point<Road>, Tuple<Point<Road>, List<Road>>> routes =
                        router.route(sources, targets, new Weight(), null, null);

                Map<Long, List<Long>> paths = new HashMap<>();
                paths.put(14L, new LinkedList<>(Arrays.asList(0L, 4L, 8L, 14L)));
                paths.put(15L, new LinkedList<>(Arrays.asList(0L, 4L, 10L, 16L, 15L)));

                assertEquals(paths.size(), routes.size());

                for (Entry<Point<Road>, Tuple<Point<Road>, List<Road>>> pair : routes.entrySet()) {
                    List<Road> route = pair.getValue().two();
                    assertNotNull(paths.get(pair.getKey().edge().id()));
                    List<Long> path = paths.get(pair.getKey().edge().id());

                    assertNotNull(route);
                    assertEquals(path.get(0).longValue(), pair.getValue().one().edge().id());
                    assertEquals(path.size(), route.size());

                    int i = 0;
                    for (Road road : route) {
                        assertEquals((long) path.get(i++), road.id());
                    }
                }
            }
            {
                // (0.7, 100) + 50 + 100 + (0.1, 200) = 240

                Set<Point<Road>> sources = new HashSet<>();
                sources.add(new Point<>(map.get(0), 0.3));
                sources.add(new Point<>(map.get(1), 0.7));

                Set<Point<Road>> targets = new HashSet<>();
                targets.add(new Point<>(map.get(14), 0.1));
                targets.add(new Point<>(map.get(15), 0.9));

                Map<Point<Road>, Tuple<Point<Road>, List<Road>>> routes =
                        router.route(sources, targets, new Weight(), null, null);

                Map<Long, List<Long>> paths = new HashMap<>();
                paths.put(14L, new LinkedList<>(Arrays.asList(0L, 4L, 8L, 14L)));
                paths.put(15L, new LinkedList<>(Arrays.asList(0L, 4L, 10L, 16L, 15L)));

                assertEquals(paths.size(), routes.size());

                for (Entry<Point<Road>, Tuple<Point<Road>, List<Road>>> pair : routes.entrySet()) {
                    List<Road> route = pair.getValue().two();
                    List<Long> path = paths.get(pair.getKey().edge().id());

                    assertNotNull(route);
                    assertEquals(path.get(0).longValue(), pair.getValue().one().edge().id());
                    assertEquals(path.size(), route.size());

                    int i = 0;
                    for (Road road : route) {
                        assertEquals((long) path.get(i++), road.id());
                    }
                }
            }
            {
                // (0.7, 100) + 50 + 100 + (0.1, 200) = 240

                Point<Road> source = new Point<>(map.get(0), 0.3);
                Point<Road> target = new Point<>(map.get(14), 0.1);

                List<Road> route = router.route(source, target, new Weight(), new Weight(), 200d);

                assertNull(route);
            }
            {
                // (0.7, 100) + 50 + 100 + (0.1, 200) = 240
                // (0.7, 100) + 50 + 100 + (0.8, 200) = 380

                Set<Point<Road>> sources = new HashSet<>();
                sources.add(new Point<>(map.get(0), 0.3));
                sources.add(new Point<>(map.get(1), 0.7));

                Set<Point<Road>> targets = new HashSet<>();
                targets.add(new Point<>(map.get(14), 0.1));
                targets.add(new Point<>(map.get(14), 0.8));

                Map<Point<Road>, Tuple<Point<Road>, List<Road>>> routes =
                        router.route(sources, targets, new Weight(), null, null);

                Map<Long, List<Long>> paths = new HashMap<>();
                paths.put(14L, new LinkedList<>(Arrays.asList(0L, 4L, 8L, 14L)));

                assertEquals(2, routes.size());

                for (Entry<Point<Road>, Tuple<Point<Road>, List<Road>>> pair : routes.entrySet()) {
                    List<Road> route = pair.getValue().two();
                    List<Long> path = paths.get(pair.getKey().edge().id());

                    assertNotNull(route);
                    assertEquals(path.get(0).longValue(), pair.getValue().one().edge().id());
                    assertEquals(path.size(), route.size());

                    int i = 0;
                    for (Road road : route) {
                        assertEquals((long) path.get(i++), road.id());
                    }
                }
            }
        }
    }
}
