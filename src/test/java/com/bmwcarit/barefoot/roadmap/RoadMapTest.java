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

package com.bmwcarit.barefoot.roadmap;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.bmwcarit.barefoot.road.BaseRoad;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WktImportFlags;

public class RoadMapTest {
    private static List<BaseRoad> osmroads() {
        /*
         * (p2) (p3) ----- (e1) : (p1) -> (p2) ----------------------------------------------------
         * - \ / --------- (e2) : (p3) -> (p1) ----------------------------------------------------
         * | (p1) | ------ (e3) : (p4) -> (p1) ----------------------------------------------------
         * - / \ --------- (e4) : (p1) -> (p5) ----------------------------------------------------
         * (p4) (p5) ----- (e5) : (p2) -> (p4) ----------------------------------------------------
         * --------------- (e6) : (p5) -> (p3) ----------------------------------------------------
         */
        String p1 = "11.3441505 48.0839963";
        String p2 = "11.3421209 48.0850624";
        String p3 = "11.3460348 48.0850108";
        String p4 = "11.3427522 48.0832129";
        String p5 = "11.3469701 48.0825356";

        List<BaseRoad> osm = new LinkedList<>();
        osm.add(new BaseRoad(1L, 1L, 2L, 1L, true, (short) 1, 1F, 60F, 60F, 100F,
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p1 + "," + p2 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline)));
        osm.add(new BaseRoad(2L, 3L, 1L, 2L, false, (short) 1, 1F, 60F, 60F, 100F,
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p3 + "," + p1 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline)));
        osm.add(new BaseRoad(3L, 4L, 1L, 3L, true, (short) 1, 1F, 60F, 60F, 100F,
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p4 + "," + p1 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline)));
        osm.add(new BaseRoad(4L, 1L, 5L, 4L, false, (short) 1, 1F, 60F, 60F, 100F,
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p1 + "," + p5 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline)));
        osm.add(new BaseRoad(5L, 2L, 4L, 5L, false, (short) 1, 1F, 60F, 60F, 100F,
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p2 + "," + p4 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline)));
        osm.add(new BaseRoad(6L, 5L, 3L, 6L, false, (short) 1, 1F, 60F, 60F, 100F,
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p5 + "," + p3 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline)));

        return osm;
    }

    @Test
    public void testSplit() {
        for (BaseRoad osmroad : osmroads()) {
            Collection<Road> roads = RoadMap.split(osmroad);
            int forward = 0, backward = 0;
            for (Road road : roads) {
                if (road.id() % 2 == 0) {
                    assertEquals(osmroad.id() * 2, road.id());
                    assertEquals(osmroad.source(), road.source());
                    assertEquals(osmroad.target(), road.target());
                    assertEquals(osmroad.refid(), road.base().refid());
                    forward += 1;
                } else {
                    assertEquals(osmroad.id() * 2 + 1, road.id());
                    assertEquals(osmroad.source(), road.target());
                    assertEquals(osmroad.target(), road.source());
                    assertEquals(osmroad.refid(), road.base().refid());
                    backward += 1;
                }
            }

            if (osmroad.oneway()) {
                assertEquals(1, forward);
                assertEquals(0, backward);
            } else {
                assertEquals(1, forward);
                assertEquals(1, backward);
            }
        }
    }

    @Test
    public void testSpatialNearest() {
        RoadMap map = new RoadMap();
        for (BaseRoad osmroad : osmroads()) {
            for (Road road : RoadMap.split(osmroad)) {
                map.add(road);
            }
        }
        map.construct();

        {
            Set<RoadPoint> points = map.spatial().nearest(new Point(11.343629, 48.083797));
            Set<Long> neighbors = new HashSet<>(Arrays.asList(6L));

            assertEquals(neighbors.size(), points.size());
            for (RoadPoint point : points) {
                neighbors.contains(point.edge().id());
            }
        }
        {
            Set<RoadPoint> points = map.spatial().nearest(new Point(11.344827, 48.083752));
            Set<Long> neighbors = new HashSet<>(Arrays.asList(8L, 9L));

            assertEquals(neighbors.size(), points.size());
            for (RoadPoint point : points) {
                neighbors.contains(point.edge().id());
            }
        }
    }

    @Test
    public void testSpatialRadius() {
        RoadMap map = new RoadMap();
        for (BaseRoad osmroad : osmroads()) {
            for (Road road : RoadMap.split(osmroad)) {
                map.add(road);
            }
        }
        map.construct();

        {
            Set<RoadPoint> points = map.spatial().radius(new Point(11.343629, 48.083797), 10.0);
            Set<Long> neighbors = new HashSet<>(Arrays.asList(6L));

            assertEquals(neighbors.size(), points.size());
            for (RoadPoint point : points) {
                neighbors.contains(point.edge().id());
            }
        }
        {
            Set<RoadPoint> points = map.spatial().radius(new Point(11.344827, 48.083752), 10.0);
            Set<Long> neighbors = new HashSet<>(Arrays.asList(8L, 9L));

            assertEquals(neighbors.size(), points.size());
            for (RoadPoint point : points) {
                neighbors.contains(point.edge().id());
            }
        }
        {
            Set<RoadPoint> points = map.spatial().radius(new Point(11.344166, 48.084077), 30.0);
            Set<Long> neighbors = new HashSet<>(Arrays.asList(2L, 4L, 5L, 6L, 8L, 9L));

            assertEquals(neighbors.size(), points.size());
            for (RoadPoint point : points) {
                neighbors.contains(point.edge().id());
            }
        }
        {
            Set<RoadPoint> points = map.spatial().radius(new Point(11.344099, 48.084972), 10.0);
            Set<Long> neighbors = new HashSet<>();

            assertEquals(neighbors.size(), points.size());
            for (RoadPoint point : points) {
                neighbors.contains(point.edge().id());
            }
        }
    }
}
