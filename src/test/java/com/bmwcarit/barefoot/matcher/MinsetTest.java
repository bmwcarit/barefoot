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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import com.bmwcarit.barefoot.road.BaseRoad;
import com.bmwcarit.barefoot.road.RoadReader;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.roadmap.RoadPoint;
import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.spatial.SpatialOperator;
import com.bmwcarit.barefoot.util.Quintuple;
import com.bmwcarit.barefoot.util.SourceException;
import com.esri.core.geometry.Geometry.Type;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WktImportFlags;

public class MinsetTest {

    @Test
    public void TestMinset1() {
        RoadMap map = RoadMap.Load(new RoadReader() {
            class Entry extends Quintuple<Long, Long, Long, Boolean, String> {
                private static final long serialVersionUID = 1L;

                public Entry(Long one, Long two, Long three, Boolean four, String five) {
                    super(one, two, three, four, five);
                }
            };

            private Set<Entry> entries =
                    new HashSet<>(Arrays.asList(new Entry(0L, 0L, 2L, true, "LINESTRING(0 0, 1 1)"),
                            new Entry(1L, 1L, 2L, true, "LINESTRING(0 2, 1 1)"),
                            new Entry(2L, 2L, 3L, true, "LINESTRING(1 1, 2 1)"),
                            new Entry(3L, 3L, 4L, true, "LINESTRING(2 1, 3 2)"),
                            new Entry(4L, 3L, 5L, true, "LINESTRING(2 1, 3 1)"),
                            new Entry(5L, 3L, 6L, true, "LINESTRING(2 1, 3 0)")));

            private Iterator<BaseRoad> iterator = null;

            private ArrayList<BaseRoad> roads = new ArrayList<>();

            @Override
            public boolean isOpen() {
                return (iterator != null);
            }

            @Override
            public void open() throws SourceException {
                if (roads.isEmpty()) {
                    for (Entry entry : entries) {
                        Polyline geometry = (Polyline) GeometryEngine.geometryFromWkt(entry.five(),
                                WktImportFlags.wktImportDefaults, Type.Polyline);
                        roads.add(new BaseRoad(entry.one(), entry.two(), entry.three(), entry.one(),
                                entry.four(), (short) 0, 0f, 0f, 0f, 0f, geometry));
                    }
                }

                iterator = roads.iterator();
            }

            @Override
            public void open(Polygon polygon, HashSet<Short> exclusions) throws SourceException {
                open();
            }

            @Override
            public void close() throws SourceException {
                iterator = null;
            }

            @Override
            public BaseRoad next() throws SourceException {
                return iterator.hasNext() ? iterator.next() : null;
            }
        });

        map.construct();

        {
            Set<RoadPoint> candidates = new HashSet<>();
            candidates.add(new RoadPoint(map.get(0), 1));
            candidates.add(new RoadPoint(map.get(2), 1));
            candidates.add(new RoadPoint(map.get(4), 0.5));
            candidates.add(new RoadPoint(map.get(6), 0));
            candidates.add(new RoadPoint(map.get(8), 0));
            candidates.add(new RoadPoint(map.get(10), 0));

            Set<RoadPoint> minset = Minset.minimize(candidates);

            assertEquals(1, minset.size());
            assertEquals(4, minset.iterator().next().edge().id());
        }
        {
            Set<RoadPoint> candidates = new HashSet<>();
            candidates.add(new RoadPoint(map.get(0), 1));
            candidates.add(new RoadPoint(map.get(2), 1));
            candidates.add(new RoadPoint(map.get(4), 1));
            candidates.add(new RoadPoint(map.get(8), 0.5));
            candidates.add(new RoadPoint(map.get(10), 0.5));

            Set<RoadPoint> minset = Minset.minimize(candidates);

            HashSet<Long> refset = new HashSet<>(Arrays.asList(4L, 8L, 10L));
            HashSet<Long> set = new HashSet<>();
            for (RoadPoint element : minset) {
                assertTrue(refset.contains(element.edge().id()));
                set.add(element.edge().id());
            }

            assertTrue(set.containsAll(refset));
        }
        {
            Set<RoadPoint> candidates = new HashSet<>();
            candidates.add(new RoadPoint(map.get(4), 1));
            candidates.add(new RoadPoint(map.get(6), 0.0));
            candidates.add(new RoadPoint(map.get(8), 0.5));
            candidates.add(new RoadPoint(map.get(10), 0.5));

            Set<RoadPoint> minset = Minset.minimize(candidates);

            HashSet<Long> refset = new HashSet<>(Arrays.asList(4L, 8L, 10L));
            HashSet<Long> set = new HashSet<>();
            for (RoadPoint element : minset) {
                assertTrue(refset.contains(element.edge().id()));
                set.add(element.edge().id());
            }

            assertTrue(set.containsAll(refset));
        }
        {
            Set<RoadPoint> candidates = new HashSet<>();
            candidates.add(new RoadPoint(map.get(0), 1));
            candidates.add(new RoadPoint(map.get(2), 1));
            candidates.add(new RoadPoint(map.get(4), 1));
            candidates.add(new RoadPoint(map.get(6), 0.2));
            candidates.add(new RoadPoint(map.get(8), 0.5));
            candidates.add(new RoadPoint(map.get(10), 0.5));

            Set<RoadPoint> minset = Minset.minimize(candidates);

            HashSet<Long> refset = new HashSet<>(Arrays.asList(6L, 8L, 10L));
            HashSet<Long> set = new HashSet<>();
            for (RoadPoint element : minset) {
                assertTrue(refset.contains(element.edge().id()));
                set.add(element.edge().id());
            }

            assertTrue(set.containsAll(refset));
        }
    }

    @Test
    public void TestMinset2() {
        final SpatialOperator spatial = new Geography();
        RoadMap map = RoadMap.Load(new RoadReader() {
            class Entry extends Quintuple<Long, Long, Long, Boolean, String> {
                private static final long serialVersionUID = 1L;

                public Entry(Long one, Long two, Long three, Boolean four, String five) {
                    super(one, two, three, four, five);
                }
            };

            private Set<Entry> entries = new HashSet<>(Arrays.asList(
                    new Entry(0L, 0L, 1L, false, "LINESTRING(11.000 48.000, 11.010 48.000)"),
                    new Entry(1L, 1L, 2L, false, "LINESTRING(11.010 48.000, 11.020 48.000)"),
                    new Entry(2L, 2L, 3L, false, "LINESTRING(11.020 48.000, 11.030 48.000)"),
                    new Entry(3L, 1L, 4L, true, "LINESTRING(11.010 48.000, 11.011 47.999)"),
                    new Entry(4L, 4L, 5L, true, "LINESTRING(11.011 47.999, 11.021 47.999)"),
                    new Entry(5L, 5L, 6L, true, "LINESTRING(11.021 47.999, 11.021 48.010)")));

            private Iterator<BaseRoad> iterator = null;

            private ArrayList<BaseRoad> roads = new ArrayList<>();

            @Override
            public boolean isOpen() {
                return (iterator != null);
            }

            @Override
            public void open() throws SourceException {
                if (roads.isEmpty()) {
                    for (Entry entry : entries) {
                        Polyline geometry = (Polyline) GeometryEngine.geometryFromWkt(entry.five(),
                                WktImportFlags.wktImportDefaults, Type.Polyline);
                        roads.add(new BaseRoad(entry.one(), entry.two(), entry.three(), entry.one(),
                                entry.four(), (short) 0, 1.0f, 100.0f, 100.0f,
                                (float) spatial.length(geometry), geometry));
                    }
                }

                iterator = roads.iterator();
            }

            @Override
            public void open(Polygon polygon, HashSet<Short> exclusions) throws SourceException {
                open();
            }

            @Override
            public void close() throws SourceException {
                iterator = null;
            }

            @Override
            public BaseRoad next() throws SourceException {
                return iterator.hasNext() ? iterator.next() : null;
            }
        });

        map.construct();

        {
            Set<RoadPoint> candidates = new HashSet<>();
            candidates.add(new RoadPoint(map.get(0), 1));
            candidates.add(new RoadPoint(map.get(1), 0));
            candidates.add(new RoadPoint(map.get(2), 0));
            candidates.add(new RoadPoint(map.get(3), 1));
            candidates.add(new RoadPoint(map.get(6), 0));
            candidates.add(new RoadPoint(map.get(8), 0));

            Set<RoadPoint> minset = Minset.minimize(candidates);

            HashSet<Long> refset = new HashSet<>(Arrays.asList(0L, 3L));
            HashSet<Long> set = new HashSet<>();
            for (RoadPoint element : minset) {
                assertTrue(refset.contains(element.edge().id()));
                set.add(element.edge().id());
            }

            assertTrue(set.containsAll(refset));
        }
        {
            Set<RoadPoint> candidates = new HashSet<>();
            candidates.add(new RoadPoint(map.get(0), 1));
            candidates.add(new RoadPoint(map.get(1), 0));
            candidates.add(new RoadPoint(map.get(2), 0.1));
            candidates.add(new RoadPoint(map.get(3), 0.9));
            candidates.add(new RoadPoint(map.get(6), 0));
            candidates.add(new RoadPoint(map.get(8), 0));

            Set<RoadPoint> minset = Minset.minimize(candidates);

            HashSet<Long> refset = new HashSet<>(Arrays.asList(0L, 2L, 3L));
            HashSet<Long> set = new HashSet<>();
            for (RoadPoint element : minset) {
                assertTrue(refset.contains(element.edge().id()));
                set.add(element.edge().id());
            }

            assertTrue(set.containsAll(refset));
        }
        {
            Set<RoadPoint> candidates = new HashSet<>();
            candidates.add(new RoadPoint(map.get(0), 1));
            candidates.add(new RoadPoint(map.get(1), 0));
            candidates.add(new RoadPoint(map.get(2), 0.1));
            candidates.add(new RoadPoint(map.get(3), 0.9));
            candidates.add(new RoadPoint(map.get(6), 0));
            candidates.add(new RoadPoint(map.get(8), 0.1));

            Set<RoadPoint> minset = Minset.minimize(candidates);

            HashSet<Long> refset = new HashSet<>(Arrays.asList(0L, 2L, 3L, 8L));
            HashSet<Long> set = new HashSet<>();
            for (RoadPoint element : minset) {
                assertTrue(refset.contains(element.edge().id()));
                set.add(element.edge().id());
            }

            assertTrue(set.containsAll(refset));
        }
    }

    // @Test
    // public void TestMinsetTestmap() {
    // Point sample =
    // (Point) GeometryEngine.geometryFromWkt(
    // "POINT (11.430306434631348 47.907142639160156)",
    // WktImportFlags.wktImportDefaults, Type.Point);
    //
    // Set<RoadPoint> set = RoadMapTest.Load().spatial().radius(sample, 100);
    // HashMap<Long, RoadPoint> minset = new HashMap<Long, RoadPoint>();
    // HashMap<Long, RoadPoint> previous = new HashMap<Long, RoadPoint>();
    //
    // for (int i = 0; i < 1000; ++i) {
    // ArrayList<RoadPoint> seq = new ArrayList<RoadPoint>(set);
    // Set<RoadPoint> perm = new HashSet<RoadPoint>();
    //
    // while (!seq.isEmpty()) {
    // int k = (int) (Math.random() * seq.size());
    // RoadPoint element = seq.remove(k);
    // perm.add(new RoadPoint(element.edge(), element.fraction()));
    // }
    //
    // for (RoadPoint candidate : Minset.minimize(perm)) {
    // minset.put(candidate.edge().id(), candidate);
    // }
    //
    // assertTrue(!minset.isEmpty());
    //
    // if (!previous.isEmpty()) {
    // for (Entry<Long, RoadPoint> candidate : minset.entrySet()) {
    // assertTrue(previous.containsKey(candidate.getKey()));
    // assertEquals(previous.get(candidate.getKey()).fraction(), candidate.getValue()
    // .fraction(), 10E-6);
    // }
    //
    // minset.keySet().containsAll(previous.keySet());
    // }
    // previous = minset;
    // }
    // }
}
