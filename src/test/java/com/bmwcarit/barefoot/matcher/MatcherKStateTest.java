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
package com.bmwcarit.barefoot.matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import com.bmwcarit.barefoot.road.BaseRoad;
import com.bmwcarit.barefoot.road.RoadReader;
import com.bmwcarit.barefoot.roadmap.Road;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.roadmap.RoadPoint;
import com.bmwcarit.barefoot.roadmap.Time;
import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.spatial.SpatialOperator;
import com.bmwcarit.barefoot.topology.Cost;
import com.bmwcarit.barefoot.topology.Dijkstra;
import com.bmwcarit.barefoot.topology.Router;
import com.bmwcarit.barefoot.util.Quintuple;
import com.bmwcarit.barefoot.util.SourceException;
import com.esri.core.geometry.Geometry.Type;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WktImportFlags;

public class MatcherKStateTest {
    private final SpatialOperator spatial = new Geography();
    private final Router<Road, RoadPoint> router = new Dijkstra<>();
    private final Cost<Road> cost = new Time();
    private final RoadMap map = RoadMap.Load(new RoadReader() {
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

    public MatcherKStateTest() {
        map.construct();
    }

    @Test
    public void testJSON() throws JSONException {
        Matcher filter = new Matcher(map, router, cost, spatial);
        filter.setMaxRadius(200);
        MatcherKState state = new MatcherKState();
        MatcherFactory factory = new MatcherFactory(map);

        MatcherSample sample1 = new MatcherSample(0, new Point(11.001, 48.001));
        state.update(filter.execute(state.vector(), null, sample1), sample1);

        JSONObject json = state.toJSON();
        MatcherKState other = new MatcherKState(new JSONObject(json.toString()), factory);

        assertEquals(state.sample().id(), other.sample().id());
        assertEquals(state.sample().time(), other.sample().time());
        assertEquals(state.sample().point(), other.sample().point());
        assertEquals(state.vector().size(), other.vector().size());
        assertEquals(state.sequence().size(), other.sequence().size());
        assertEquals(state.samples().size(), other.samples().size());

        Set<String> ids = new HashSet<>();

        for (MatcherCandidate cand : state.vector()) {
            ids.add(cand.id());
        }

        for (MatcherCandidate cand : other.vector()) {
            assertTrue(ids.contains(cand.id()));
        }

        MatcherSample sample2 = new MatcherSample(42, new Point(11.010, 48.000));
        state.update(filter.execute(state.vector(), sample1, sample2), sample2);

        json = state.toJSON();
        other = new MatcherKState(new JSONObject(json.toString()), factory);

        assertEquals(state.sample().id(), other.sample().id());
        assertEquals(state.sample().time(), other.sample().time());
        assertEquals(state.sample().point(), other.sample().point());
        assertEquals(state.vector().size(), other.vector().size());
        assertEquals(state.sequence().size(), other.sequence().size());
        assertEquals(state.samples().size(), other.samples().size());

        for (int i = 0; i < state.sequence().size(); ++i) {
            assertEquals(state.sequence().get(i).id(), other.sequence().get(i).id());
            assertEquals(state.sequence().get(i).filtprob(), other.sequence().get(i).filtprob(),
                    1E-10);
            assertEquals(state.sequence().get(i).seqprob(), other.sequence().get(i).seqprob(),
                    1E-10);
        }
    }
}
