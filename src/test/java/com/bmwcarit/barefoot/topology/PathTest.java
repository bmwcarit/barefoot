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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PathTest {

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

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testValid() {
        {
            Graph<Road> map = new Graph<>();
            map.add(new Road(0, 0, 0, 100));
            map.construct();

            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                Point<Road> single = new Point<>(map.get(0L), 0.2);

                Path<Road> path = new Path<>(single);
                assertTrue(path.valid());
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                Point<Road> source = new Point<>(map.get(0L), 0.2);
                Point<Road> target = new Point<>(map.get(0L), 0.3);

                Path<Road> path = new Path<>(source, target, edges);
                assertTrue(path.valid());
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(0L), 0.2);

                try {
                    new Path<>(source, target, edges);
                    fail();
                } catch (Exception e) {
                }
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                edges.add(map.get(0L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(0L), 0.2);

                Path<Road> path = new Path<>(source, target, edges);
                assertTrue(path.valid());
            }
        }
        {
            Graph<Road> map = new Graph<>();
            map.add(new Road(0, 0, 1, 100));
            map.add(new Road(1, 1, 2, 100));
            map.add(new Road(2, 2, 0, 100));
            map.construct();

            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                edges.add(map.get(0L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(0L), 0.2);

                try {
                    new Path<>(source, target, edges);
                    fail();
                } catch (Exception e) {
                }
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                edges.add(map.get(1L));
                edges.add(map.get(2L));
                edges.add(map.get(0L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(0L), 0.2);

                Path<Road> path = new Path<>(source, target, edges);
                assertTrue(path.valid());
            }
        }
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

            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(0L), 0.3);

                Path<Road> path = new Path<>(source, target, edges);
                assertTrue(path.valid());
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                Point<Road> source = new Point<>(map.get(0L), 0.4);
                Point<Road> target = new Point<>(map.get(0L), 0.3);

                try {
                    new Path<>(source, target, edges);
                    fail();
                } catch (Exception e) {
                }
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                edges.add(map.get(1L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(0L), 0.3);

                try {
                    new Path<>(source, target, edges);
                    fail();
                } catch (Exception e) {
                }
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                Point<Road> source = new Point<>(map.get(1L), 0.3);
                Point<Road> target = new Point<>(map.get(0L), 0.3);

                try {
                    new Path<>(source, target, edges);
                    fail();
                } catch (Exception e) {
                }
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(1L), 0.3);

                try {
                    new Path<>(source, target, edges);
                    fail();
                } catch (Exception e) {
                }
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                edges.add(map.get(4L));
                edges.add(map.get(10L));
                edges.add(map.get(16L));
                edges.add(map.get(15L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(15L), 0.3);

                Path<Road> path = new Path<>(source, target, edges);
                assertTrue(path.valid());
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                edges.add(map.get(4L));
                edges.add(map.get(10L));
                edges.add(map.get(17L));
                edges.add(map.get(15L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(15L), 0.3);

                try {
                    new Path<>(source, target, edges);
                    fail();
                } catch (Exception e) {
                }
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                edges.add(map.get(4L));
                edges.add(map.get(10L));
                edges.add(map.get(16L));
                edges.add(map.get(15L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(16L), 0.3);

                try {
                    new Path<>(source, target, edges);
                    fail();
                } catch (Exception e) {
                }
            }
        }
    }

    @Test
    public void testCost() {
        {
            Graph<Road> map = new Graph<>();
            map.add(new Road(0, 0, 0, 100));
            map.construct();

            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                Point<Road> single = new Point<>(map.get(0L), 0.2);

                Path<Road> path = new Path<>(single);
                assertEquals(0, path.cost(new Weight()), 1E-10);
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                Point<Road> source = new Point<>(map.get(0L), 0.2);
                Point<Road> target = new Point<>(map.get(0L), 0.2);

                Path<Road> path = new Path<>(source, target, edges);
                assertEquals(0, path.cost(new Weight()), 1E-10);
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                Point<Road> source = new Point<>(map.get(0L), 0.2);
                Point<Road> target = new Point<>(map.get(0L), 0.3);

                Path<Road> path = new Path<>(source, target, edges);
                assertEquals(10, path.cost(new Weight()), 1E-10);
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                edges.add(map.get(0L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(0L), 0.2);

                Path<Road> path = new Path<>(source, target, edges);
                assertEquals(90, path.cost(new Weight()), 1E-10);
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                edges.add(map.get(0L));
                edges.add(map.get(0L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(0L), 0.2);

                Path<Road> path = new Path<>(source, target, edges);
                assertEquals(190, path.cost(new Weight()), 1E-10);
            }
        }
        {
            Graph<Road> map = new Graph<>();
            map.add(new Road(0, 0, 1, 100));
            map.add(new Road(1, 1, 2, 100));
            map.add(new Road(2, 2, 0, 100));
            map.construct();

            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                edges.add(map.get(1L));
                edges.add(map.get(2L));
                edges.add(map.get(0L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(0L), 0.2);

                Path<Road> path = new Path<>(source, target, edges);
                assertEquals(290, path.cost(new Weight()), 1E-10);
            }
        }
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

            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                edges.add(map.get(4L));
                edges.add(map.get(10L));
                edges.add(map.get(16L));
                edges.add(map.get(15L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(15L), 0.3);

                Path<Road> path = new Path<>(source, target, edges);
                assertEquals(280, path.cost(new Weight()), 1E-10);
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                edges.add(map.get(4L));
                edges.add(map.get(8L));
                edges.add(map.get(14L));
                Point<Road> source = new Point<>(map.get(0L), 0.3);
                Point<Road> target = new Point<>(map.get(14L), 0.1);

                Path<Road> path = new Path<>(source, target, edges);
                assertEquals(240, path.cost(new Weight()), 1E-10);
            }
            {
                List<Road> edges = new LinkedList<>();
                edges.add(map.get(0L));
                edges.add(map.get(4L));
                edges.add(map.get(8L));
                edges.add(map.get(7L));
                edges.add(map.get(1L));
                edges.add(map.get(2L));
                edges.add(map.get(8L));
                edges.add(map.get(12L));
                edges.add(map.get(13L));
                Point<Road> source = new Point<>(map.get(0L), 0.0);
                Point<Road> target = new Point<>(map.get(13L), 1.0);

                Path<Road> path = new Path<>(source, target, edges);
                assertEquals(1010, path.cost(new Weight()), 1E-10);
            }
        }
    }

    @Test
    public void testAdd() {
        {
            Graph<Road> map = new Graph<>();
            map.add(new Road(0, 0, 0, 100));
            map.construct();

            {
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Path<Road> path1 = new Path<>(source1);

                Point<Road> target2 = new Point<>(map.get(0L), 0.2);
                Path<Road> path2 = new Path<>(target2);

                assertTrue(path1.add(path2));

                assertEquals(source1.edge().id(), path1.source().edge().id());
                assertEquals(source1.fraction(), path1.source().fraction(), 1E-10);
                assertEquals(target2.edge().id(), path1.target().edge().id());
                assertEquals(target2.fraction(), path1.target().fraction(), 1E-10);

                LinkedList<Long> edges = new LinkedList<>(Arrays.asList(0L));

                assertEquals(edges.size(), path1.path().size());

                int i = 0;
                for (Road road : path1.path()) {
                    assertEquals(edges.get(i++).longValue(), road.id());
                }
            }
            {
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Path<Road> path1 = new Path<>(source1);

                Point<Road> target2 = new Point<>(map.get(0L), 0.3);
                Path<Road> path2 = new Path<>(target2);

                assertFalse(path1.add(path2));
            }
            {
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Path<Road> path1 = new Path<>(source1);

                List<Road> edges2 = new LinkedList<>();
                edges2.add(map.get(0L));
                Point<Road> source2 = new Point<>(map.get(0L), 0.2);
                Point<Road> target2 = new Point<>(map.get(0L), 0.3);
                Path<Road> path2 = new Path<>(source2, target2, edges2);

                assertTrue(path1.add(path2));

                assertEquals(source1.edge().id(), path1.source().edge().id());
                assertEquals(source1.fraction(), path1.source().fraction(), 1E-10);
                assertEquals(target2.edge().id(), path1.target().edge().id());
                assertEquals(target2.fraction(), path1.target().fraction(), 1E-10);

                LinkedList<Long> edges = new LinkedList<>(Arrays.asList(0L));

                assertEquals(edges.size(), path1.path().size());

                int i = 0;
                for (Road road : path1.path()) {
                    assertEquals(edges.get(i++).longValue(), road.id());
                }
            }
            {
                List<Road> edges1 = new LinkedList<>();
                edges1.add(map.get(0L));
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Point<Road> target1 = new Point<>(map.get(0L), 0.2);
                Path<Road> path1 = new Path<>(source1, target1, edges1);

                List<Road> edges2 = new LinkedList<>();
                edges2.add(map.get(0L));
                Point<Road> source2 = new Point<>(map.get(0L), 0.2);
                Point<Road> target2 = new Point<>(map.get(0L), 0.2);
                Path<Road> path2 = new Path<>(source2, target2, edges2);

                assertTrue(path1.add(path2));

                assertEquals(source1.edge().id(), path1.source().edge().id());
                assertEquals(source1.fraction(), path1.source().fraction(), 1E-10);
                assertEquals(target2.edge().id(), path1.target().edge().id());
                assertEquals(target2.fraction(), path1.target().fraction(), 1E-10);

                LinkedList<Long> edges = new LinkedList<>(Arrays.asList(0L));

                assertEquals(edges.size(), path1.path().size());

                int i = 0;
                for (Road road : path1.path()) {
                    assertEquals(edges.get(i++).longValue(), road.id());
                }
            }
            {
                List<Road> edges1 = new LinkedList<>();
                edges1.add(map.get(0L));
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Point<Road> target1 = new Point<>(map.get(0L), 0.2);
                Path<Road> path1 = new Path<>(source1, target1, edges1);

                List<Road> edges2 = new LinkedList<>();
                edges2.add(map.get(0L));
                Point<Road> source2 = new Point<>(map.get(0L), 0.2);
                Point<Road> target2 = new Point<>(map.get(0L), 0.4);
                Path<Road> path2 = new Path<>(source2, target2, edges2);

                assertTrue(path1.add(path2));

                assertEquals(source1.edge().id(), path1.source().edge().id());
                assertEquals(source1.fraction(), path1.source().fraction(), 1E-10);
                assertEquals(target2.edge().id(), path1.target().edge().id());
                assertEquals(target2.fraction(), path1.target().fraction(), 1E-10);

                LinkedList<Long> edges = new LinkedList<>(Arrays.asList(0L));

                assertEquals(edges.size(), path1.path().size());

                int i = 0;
                for (Road road : path1.path()) {
                    assertEquals(edges.get(i++).longValue(), road.id());
                }
            }
            {
                List<Road> edges1 = new LinkedList<>();
                edges1.add(map.get(0L));
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Point<Road> target1 = new Point<>(map.get(0L), 0.3);
                Path<Road> path1 = new Path<>(source1, target1, edges1);

                List<Road> edges2 = new LinkedList<>();
                edges2.add(map.get(0L));
                Point<Road> source2 = new Point<>(map.get(0L), 0.2);
                Point<Road> target2 = new Point<>(map.get(0L), 0.4);
                Path<Road> path2 = new Path<>(source2, target2, edges2);

                assertFalse(path1.add(path2));
            }
        }
        {
            Graph<Road> map = new Graph<>();
            map.add(new Road(0, 0, 1, 100));
            map.add(new Road(1, 1, 2, 100));
            map.add(new Road(2, 2, 0, 100));
            map.construct();

            {
                List<Road> edges1 = new LinkedList<>();
                edges1.add(map.get(0L));
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Point<Road> target1 = new Point<>(map.get(0L), 1.0);
                Path<Road> path1 = new Path<>(source1, target1, edges1);

                List<Road> edges2 = new LinkedList<>();
                edges2.add(map.get(1L));
                Point<Road> source2 = new Point<>(map.get(1L), 0.0);
                Point<Road> target2 = new Point<>(map.get(1L), 0.4);
                Path<Road> path2 = new Path<>(source2, target2, edges2);

                assertTrue(path1.add(path2));

                assertEquals(source1.edge().id(), path1.source().edge().id());
                assertEquals(source1.fraction(), path1.source().fraction(), 1E-10);
                assertEquals(target2.edge().id(), path1.target().edge().id());
                assertEquals(target2.fraction(), path1.target().fraction(), 1E-10);

                LinkedList<Long> edges = new LinkedList<>(Arrays.asList(0L, 1L));

                assertEquals(edges.size(), path1.path().size());

                int i = 0;
                for (Road road : path1.path()) {
                    assertEquals(edges.get(i++).longValue(), road.id());
                }
            }
            {
                List<Road> edges1 = new LinkedList<>();
                edges1.add(map.get(0L));
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Point<Road> target1 = new Point<>(map.get(0L), 1.0);
                Path<Road> path1 = new Path<>(source1, target1, edges1);

                List<Road> edges2 = new LinkedList<>();
                edges2.add(map.get(1L));
                Point<Road> source2 = new Point<>(map.get(1L), 0.1);
                Point<Road> target2 = new Point<>(map.get(1L), 0.4);
                Path<Road> path2 = new Path<>(source2, target2, edges2);

                assertFalse(path1.add(path2));
            }
            {
                List<Road> edges1 = new LinkedList<>();
                edges1.add(map.get(0L));
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Point<Road> target1 = new Point<>(map.get(0L), 0.9);
                Path<Road> path1 = new Path<>(source1, target1, edges1);

                List<Road> edges2 = new LinkedList<>();
                edges2.add(map.get(1L));
                Point<Road> source2 = new Point<>(map.get(1L), 0.0);
                Point<Road> target2 = new Point<>(map.get(1L), 0.4);
                Path<Road> path2 = new Path<>(source2, target2, edges2);

                assertFalse(path1.add(path2));
            }
            {
                List<Road> edges1 = new LinkedList<>();
                edges1.add(map.get(0L));
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Point<Road> target1 = new Point<>(map.get(0L), 1.0);
                Path<Road> path1 = new Path<>(source1, target1, edges1);

                List<Road> edges2 = new LinkedList<>();
                edges2.add(map.get(2L));
                Point<Road> source2 = new Point<>(map.get(2L), 0.0);
                Point<Road> target2 = new Point<>(map.get(2L), 0.4);
                Path<Road> path2 = new Path<>(source2, target2, edges2);

                assertFalse(path1.add(path2));
            }
            {
                List<Road> edges1 = new LinkedList<>();
                edges1.add(map.get(0L));
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Point<Road> target1 = new Point<>(map.get(0L), 1.0);
                Path<Road> path1 = new Path<>(source1, target1, edges1);

                List<Road> edges2 = new LinkedList<>();
                edges2.add(map.get(1L));
                edges2.add(map.get(2L));
                Point<Road> source2 = new Point<>(map.get(1L), 0.0);
                Point<Road> target2 = new Point<>(map.get(2L), 0.4);
                Path<Road> path2 = new Path<>(source2, target2, edges2);

                assertTrue(path1.add(path2));

                assertEquals(source1.edge().id(), path1.source().edge().id());
                assertEquals(source1.fraction(), path1.source().fraction(), 1E-10);
                assertEquals(target2.edge().id(), path1.target().edge().id());
                assertEquals(target2.fraction(), path1.target().fraction(), 1E-10);

                LinkedList<Long> edges = new LinkedList<>(Arrays.asList(0L, 1L, 2L));

                assertEquals(edges.size(), path1.path().size());

                int i = 0;
                for (Road road : path1.path()) {
                    assertEquals(edges.get(i++).longValue(), road.id());
                }
            }
            {
                List<Road> edges1 = new LinkedList<>();
                edges1.add(map.get(0L));
                edges1.add(map.get(1L));
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Point<Road> target1 = new Point<>(map.get(1L), 1.0);
                Path<Road> path1 = new Path<>(source1, target1, edges1);

                List<Road> edges2 = new LinkedList<>();
                edges2.add(map.get(1L));
                edges2.add(map.get(2L));
                Point<Road> source2 = new Point<>(map.get(1L), 0.0);
                Point<Road> target2 = new Point<>(map.get(2L), 0.4);
                Path<Road> path2 = new Path<>(source2, target2, edges2);

                assertFalse(path1.add(path2));
            }
            {
                List<Road> edges1 = new LinkedList<>();
                edges1.add(map.get(0L));
                edges1.add(map.get(1L));
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Point<Road> target1 = new Point<>(map.get(1L), 0.5);
                Path<Road> path1 = new Path<>(source1, target1, edges1);

                List<Road> edges2 = new LinkedList<>();
                edges2.add(map.get(1L));
                edges2.add(map.get(2L));
                Point<Road> source2 = new Point<>(map.get(1L), 0.5);
                Point<Road> target2 = new Point<>(map.get(2L), 0.4);
                Path<Road> path2 = new Path<>(source2, target2, edges2);

                assertTrue(path1.add(path2));

                assertEquals(source1.edge().id(), path1.source().edge().id());
                assertEquals(source1.fraction(), path1.source().fraction(), 1E-10);
                assertEquals(target2.edge().id(), path1.target().edge().id());
                assertEquals(target2.fraction(), path1.target().fraction(), 1E-10);

                LinkedList<Long> edges = new LinkedList<>(Arrays.asList(0L, 1L, 2L));

                assertEquals(edges.size(), path1.path().size());

                int i = 0;
                for (Road road : path1.path()) {
                    assertEquals(edges.get(i++).longValue(), road.id());
                }
            }
            {
                List<Road> edges1 = new LinkedList<>();
                edges1.add(map.get(0L));
                edges1.add(map.get(1L));
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Point<Road> target1 = new Point<>(map.get(1L), 0.5);
                Path<Road> path1 = new Path<>(source1, target1, edges1);

                List<Road> edges2 = new LinkedList<>();
                edges2.add(map.get(1L));
                edges2.add(map.get(2L));
                Point<Road> source2 = new Point<>(map.get(1L), 0.55);
                Point<Road> target2 = new Point<>(map.get(2L), 0.4);
                Path<Road> path2 = new Path<>(source2, target2, edges2);

                assertFalse(path1.add(path2));
            }
        }
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

            {
                List<Road> edges1 = new LinkedList<>();
                edges1.add(map.get(0L));
                edges1.add(map.get(4L));
                edges1.add(map.get(8L));
                edges1.add(map.get(7L));
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Point<Road> target1 = new Point<>(map.get(7L), 0.5);
                Path<Road> path1 = new Path<>(source1, target1, edges1);

                List<Road> edges2 = new LinkedList<>();
                edges2.add(map.get(7L));
                edges2.add(map.get(1L));
                edges2.add(map.get(2L));
                edges2.add(map.get(8L));
                edges2.add(map.get(12L));
                edges2.add(map.get(13L));
                Point<Road> source2 = new Point<>(map.get(7L), 0.5);
                Point<Road> target2 = new Point<>(map.get(13L), 0.4);
                Path<Road> path2 = new Path<>(source2, target2, edges2);

                assertTrue(path1.add(path2));

                assertEquals(source1.edge().id(), path1.source().edge().id());
                assertEquals(source1.fraction(), path1.source().fraction(), 1E-10);
                assertEquals(target2.edge().id(), path1.target().edge().id());
                assertEquals(target2.fraction(), path1.target().fraction(), 1E-10);

                LinkedList<Long> edges =
                        new LinkedList<>(Arrays.asList(0L, 4L, 8L, 7L, 1L, 2L, 8L, 12L, 13L));

                assertEquals(edges.size(), path1.path().size());

                int i = 0;
                for (Road road : path1.path()) {
                    assertEquals(edges.get(i++).longValue(), road.id());
                }
            }
            {
                List<Road> edges1 = new LinkedList<>();
                edges1.add(map.get(0L));
                edges1.add(map.get(4L));
                edges1.add(map.get(8L));
                edges1.add(map.get(7L));
                Point<Road> source1 = new Point<>(map.get(0L), 0.2);
                Point<Road> target1 = new Point<>(map.get(7L), 1.0);
                Path<Road> path1 = new Path<>(source1, target1, edges1);

                List<Road> edges2 = new LinkedList<>();
                edges2.add(map.get(1L));
                edges2.add(map.get(2L));
                edges2.add(map.get(8L));
                edges2.add(map.get(12L));
                edges2.add(map.get(13L));
                Point<Road> source2 = new Point<>(map.get(1L), 0.0);
                Point<Road> target2 = new Point<>(map.get(13L), 0.4);
                Path<Road> path2 = new Path<>(source2, target2, edges2);

                assertTrue(path1.add(path2));

                assertEquals(source1.edge().id(), path1.source().edge().id());
                assertEquals(source1.fraction(), path1.source().fraction(), 1E-10);
                assertEquals(target2.edge().id(), path1.target().edge().id());
                assertEquals(target2.fraction(), path1.target().fraction(), 1E-10);

                LinkedList<Long> edges =
                        new LinkedList<>(Arrays.asList(0L, 4L, 8L, 7L, 1L, 2L, 8L, 12L, 13L));

                assertEquals(edges.size(), path1.path().size());

                int i = 0;
                for (Road road : path1.path()) {
                    assertEquals(edges.get(i++).longValue(), road.id());
                }
            }
        }
    }
}
