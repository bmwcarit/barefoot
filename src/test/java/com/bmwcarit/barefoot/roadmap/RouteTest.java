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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import com.bmwcarit.barefoot.road.BaseRoad;
import com.bmwcarit.barefoot.road.Heading;
import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.spatial.SpatialOperator;
import com.bmwcarit.barefoot.topology.Graph;
import com.bmwcarit.barefoot.util.SourceException;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WktExportFlags;
import com.esri.core.geometry.WktImportFlags;

public class RouteTest {

    @Test
    public void testGeometry() {
        SpatialOperator spatial = new Geography();

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

        {
            Polyline geometry1 = (Polyline) GeometryEngine.geometryFromWkt(
                    "LINESTRING(" + p1 + "," + p2 + "," + p4 + ")",
                    WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);
            Polyline geometry2 =
                    (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p4 + "," + p1 + ")",
                            WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);

            List<BaseRoad> osm = new LinkedList<>();
            osm.add(new BaseRoad(1L, 1L, 4L, 1L, false, (short) 1, 1F, 60F, 60F,
                    (float) spatial.length(geometry1), geometry1));
            osm.add(new BaseRoad(2L, 4L, 1L, 2L, false, (short) 1, 1F, 60F, 60F,
                    (float) spatial.length(geometry2), geometry2));

            Graph<Road> map = new Graph<>();
            for (BaseRoad road : osm) {
                map.add(new Road(road, Heading.forward));
                map.add(new Road(road, Heading.backward));
            }
            map.construct();

            {
                RoadPoint source = new RoadPoint(map.get(2), 0.3);
                RoadPoint target = new RoadPoint(map.get(4), 0.7);
                List<Road> path = new LinkedList<>(Arrays.asList(map.get(2), map.get(4)));
                Route route = new Route(source, target, path);

                List<String> sequence = new LinkedList<>(Arrays.asList(p2, p4));

                int count = route.geometry().getPointCount();
                assertEquals(source.edge().id(), route.source().edge().id());
                assertEquals(source.fraction(), route.source().fraction(), 1E-6);
                assertEquals(source.geometry().getX(), route.geometry().getPoint(0).getX(), 1E-6);
                assertEquals(source.geometry().getY(), route.geometry().getPoint(0).getY(), 1E-6);
                assertEquals(target.edge().id(), route.target().edge().id());
                assertEquals(target.fraction(), route.target().fraction(), 1E-6);
                assertEquals(target.geometry().getX(), route.geometry().getPoint(count - 1).getX(),
                        1E-6);
                assertEquals(target.geometry().getY(), route.geometry().getPoint(count - 1).getY(),
                        1E-6);

                int i = 1;
                for (String wkt : sequence) {
                    Point point1 = (Point) GeometryEngine.geometryFromWkt("POINT(" + wkt + ")",
                            WktImportFlags.wktImportDefaults, Geometry.Type.Point);
                    Point point2 = route.geometry().getPoint(i++);
                    assertEquals(point1.getX(), point2.getX(), 1E-6);
                    assertEquals(point1.getY(), point2.getY(), 1E-6);
                }
            }
            {
                RoadPoint source = new RoadPoint(map.get(2), 0.0);
                RoadPoint target = new RoadPoint(map.get(4), 1.0);
                List<Road> path = new LinkedList<>(Arrays.asList(map.get(2), map.get(4)));
                Route route = new Route(source, target, path);

                List<String> sequence = new LinkedList<>(Arrays.asList(p1, p2, p4, p1));

                assertEquals(sequence.size(), route.geometry().getPointCount());

                int count = route.geometry().getPointCount();
                assertEquals(source.edge().id(), route.source().edge().id());
                assertEquals(source.fraction(), route.source().fraction(), 1E-6);
                assertEquals(source.geometry().getX(), route.geometry().getPoint(0).getX(), 1E-6);
                assertEquals(source.geometry().getY(), route.geometry().getPoint(0).getY(), 1E-6);
                assertEquals(target.edge().id(), route.target().edge().id());
                assertEquals(target.fraction(), route.target().fraction(), 1E-6);
                assertEquals(target.geometry().getX(), route.geometry().getPoint(count - 1).getX(),
                        1E-6);
                assertEquals(target.geometry().getY(), route.geometry().getPoint(count - 1).getY(),
                        1E-6);

                int i = 0;
                for (String wkt : sequence) {
                    Point point1 = (Point) GeometryEngine.geometryFromWkt("POINT(" + wkt + ")",
                            WktImportFlags.wktImportDefaults, Geometry.Type.Point);
                    Point point2 = route.geometry().getPoint(i++);
                    assertEquals(point1.getX(), point2.getX(), 1E-6);
                    assertEquals(point1.getY(), point2.getY(), 1E-6);
                }
            }
        }
        {
            Polyline geometry1 = (Polyline) GeometryEngine.geometryFromWkt(
                    "LINESTRING(" + p1 + "," + p2 + "," + p4 + ")",
                    WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);
            Polyline geometry2 = (Polyline) GeometryEngine.geometryFromWkt(
                    "LINESTRING(" + p4 + "," + p1 + "," + p3 + ")",
                    WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);
            Polyline geometry3 = (Polyline) GeometryEngine.geometryFromWkt(
                    "LINESTRING(" + p3 + "," + p5 + "," + p1 + ")",
                    WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);

            List<BaseRoad> osm = new LinkedList<>();
            osm.add(new BaseRoad(1L, 1L, 2L, 1L, false, (short) 1, 1F, 60F, 60F,
                    (float) spatial.length(geometry1), geometry1));
            osm.add(new BaseRoad(2L, 2L, 3L, 2L, false, (short) 1, 1F, 60F, 60F,
                    (float) spatial.length(geometry2), geometry2));
            osm.add(new BaseRoad(3L, 3L, 4L, 3L, false, (short) 1, 1F, 60F, 60F,
                    (float) spatial.length(geometry3), geometry3));

            Graph<Road> map = new Graph<>();
            for (BaseRoad road : osm) {
                map.add(new Road(road, Heading.forward));
                map.add(new Road(road, Heading.backward));
            }
            map.construct();

            {
                RoadPoint source = new RoadPoint(map.get(7), 0.3);
                RoadPoint target = new RoadPoint(map.get(3), 0.7);
                List<Road> path =
                        new LinkedList<>(Arrays.asList(map.get(7), map.get(5), map.get(3)));
                Route route = new Route(source, target, path);

                List<String> sequence = new LinkedList<>(Arrays.asList(p5, p3, p1, p4, p2));

                int count = route.geometry().getPointCount();
                assertEquals(sequence.size(), count - 2);
                Point sourcep = spatial.interpolate(source.edge().geometry(), source.fraction());
                assertEquals(sourcep.getX(), route.geometry().getPoint(0).getX(), 1E-6);
                assertEquals(sourcep.getY(), route.geometry().getPoint(0).getY(), 1E-6);
                Point targetp = spatial.interpolate(target.edge().geometry(), target.fraction());
                assertEquals(targetp.getX(), route.geometry().getPoint(count - 1).getX(), 1E-6);
                assertEquals(targetp.getY(), route.geometry().getPoint(count - 1).getY(), 1E-6);

                int i = 1;
                for (String wkt : sequence) {
                    Point point1 = (Point) GeometryEngine.geometryFromWkt("POINT(" + wkt + ")",
                            WktImportFlags.wktImportDefaults, Geometry.Type.Point);
                    Point point2 = route.geometry().getPoint(i++);
                    assertEquals(point1.getX(), point2.getX(), 1E-6);
                    assertEquals(point1.getY(), point2.getY(), 1E-6);
                }
            }
            {
                RoadPoint source = new RoadPoint(map.get(7), 0.3);
                RoadPoint target = new RoadPoint(map.get(7), 0.8);
                List<Road> path = new LinkedList<>(Arrays.asList(map.get(7)));
                Route route = new Route(source, target, path);

                List<String> sequence = new LinkedList<>(Arrays.asList(p5));

                int count = route.geometry().getPointCount();
                assertEquals(sequence.size(), count - 2);
                Point sourcep = spatial.interpolate(source.edge().geometry(), source.fraction());
                assertEquals(sourcep.getX(), route.geometry().getPoint(0).getX(), 1E-6);
                assertEquals(sourcep.getY(), route.geometry().getPoint(0).getY(), 1E-6);
                Point targetp = spatial.interpolate(target.edge().geometry(), target.fraction());
                assertEquals(targetp.getX(), route.geometry().getPoint(count - 1).getX(), 1E-6);
                assertEquals(targetp.getY(), route.geometry().getPoint(count - 1).getY(), 1E-6);

                int i = 1;
                for (String wkt : sequence) {
                    Point point1 = (Point) GeometryEngine.geometryFromWkt("POINT(" + wkt + ")",
                            WktImportFlags.wktImportDefaults, Geometry.Type.Point);
                    Point point2 = route.geometry().getPoint(i++);
                    assertEquals(point1.getX(), point2.getX(), 1E-6);
                    assertEquals(point1.getY(), point2.getY(), 1E-6);
                }
            }
            {
                RoadPoint source = new RoadPoint(map.get(7), 0.8);
                RoadPoint target = new RoadPoint(map.get(7), 0.9);
                List<Road> path = new LinkedList<>(Arrays.asList(map.get(7)));
                Route route = new Route(source, target, path);

                List<String> sequence = new LinkedList<>();

                int count = route.geometry().getPointCount();
                assertEquals(sequence.size(), count - 2);
                Point sourcep = spatial.interpolate(source.edge().geometry(), source.fraction());
                assertEquals(sourcep.getX(), route.geometry().getPoint(0).getX(), 1E-6);
                assertEquals(sourcep.getY(), route.geometry().getPoint(0).getY(), 1E-6);
                Point targetp = spatial.interpolate(target.edge().geometry(), target.fraction());
                assertEquals(targetp.getX(), route.geometry().getPoint(count - 1).getX(), 1E-6);
                assertEquals(targetp.getY(), route.geometry().getPoint(count - 1).getY(), 1E-6);

                int i = 1;
                for (String wkt : sequence) {
                    Point point1 = (Point) GeometryEngine.geometryFromWkt("POINT(" + wkt + ")",
                            WktImportFlags.wktImportDefaults, Geometry.Type.Point);
                    Point point2 = route.geometry().getPoint(i++);
                    assertEquals(point1.getX(), point2.getX(), 1E-6);
                    assertEquals(point1.getY(), point2.getY(), 1E-6);
                }
            }
        }
    }

    @Test
    public void testAdd() {
        SpatialOperator spatial = new Geography();

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

        Polyline geometry1 =
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p1 + "," + p2 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);
        Polyline geometry2 =
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p3 + "," + p1 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);
        Polyline geometry3 =
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p4 + "," + p1 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);
        Polyline geometry4 =
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p1 + "," + p5 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);
        Polyline geometry5 =
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p2 + "," + p4 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);
        Polyline geometry6 =
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p5 + "," + p3 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);

        List<BaseRoad> osm = new LinkedList<>();
        osm.add(new BaseRoad(1L, 1L, 2L, 1L, false, (short) 1, 1F, 60F, 60F,
                (float) spatial.length(geometry1), geometry1));
        osm.add(new BaseRoad(2L, 3L, 1L, 2L, false, (short) 1, 1F, 60F, 60F,
                (float) spatial.length(geometry2), geometry2));
        osm.add(new BaseRoad(3L, 4L, 1L, 3L, false, (short) 1, 1F, 60F, 60F,
                (float) spatial.length(geometry3), geometry3));
        osm.add(new BaseRoad(4L, 1L, 5L, 4L, false, (short) 1, 1F, 60F, 60F,
                (float) spatial.length(geometry4), geometry4));
        osm.add(new BaseRoad(5L, 2L, 4L, 5L, false, (short) 1, 1F, 60F, 60F,
                (float) spatial.length(geometry5), geometry5));
        osm.add(new BaseRoad(6L, 5L, 3L, 6L, false, (short) 1, 1F, 60F, 60F,
                (float) spatial.length(geometry6), geometry6));


        Graph<Road> map = new Graph<>();
        for (BaseRoad road : osm) {
            map.add(new Road(road, Heading.forward));
            map.add(new Road(road, Heading.backward));
        }
        map.construct();

        {
            RoadPoint point11 = new RoadPoint(map.get(2), 0.3);
            RoadPoint point12 = new RoadPoint(map.get(2), 0.7);
            List<Road> path1 = new LinkedList<>(Arrays.asList(map.get(2)));
            Route route1 = new Route(point11, point12, path1);

            RoadPoint point21 = new RoadPoint(map.get(2), 0.7);
            RoadPoint point22 = new RoadPoint(map.get(10), 0.3);
            List<Road> path2 = new LinkedList<>(Arrays.asList(map.get(2), map.get(10)));
            Route route2 = new Route(point21, point22, path2);

            assertTrue(route1.add(route2));

            int count = route1.geometry().getPointCount();
            assertEquals(point11.edge().id(), route1.source().edge().id());
            assertEquals(point11.fraction(), route1.source().fraction(), 1E-6);
            assertEquals(point11.geometry().getX(), route1.geometry().getPoint(0).getX(), 1E-6);
            assertEquals(point11.geometry().getY(), route1.geometry().getPoint(0).getY(), 1E-6);
            assertEquals(point22.edge().id(), route1.target().edge().id());
            assertEquals(point22.fraction(), route1.target().fraction(), 1E-6);
            assertEquals(point22.geometry().getX(), route1.geometry().getPoint(count - 1).getX(),
                    1E-6);
            assertEquals(point22.geometry().getY(), route1.geometry().getPoint(count - 1).getY(),
                    1E-6);

            List<String> sequence = new LinkedList<>(Arrays.asList(p2));

            assertEquals(sequence.size(), route1.geometry().getPointCount() - 2);

            int i = 1;
            for (String wkt : sequence) {
                Point point1 = (Point) GeometryEngine.geometryFromWkt("POINT(" + wkt + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Point);
                Point point2 = route1.geometry().getPoint(i++);
                assertEquals(point1.getX(), point2.getX(), 1E-6);
                assertEquals(point1.getY(), point2.getY(), 1E-6);
            }
        }
        {
            RoadPoint point11 = new RoadPoint(map.get(2), 0.3);
            RoadPoint point12 = new RoadPoint(map.get(10), 0.7);
            List<Road> path1 = new LinkedList<>(Arrays.asList(map.get(2), map.get(10)));
            Route route1 = new Route(point11, point12, path1);

            List<String> sequence = new LinkedList<>(Arrays.asList(p2));

            assertEquals(sequence.size(), route1.geometry().getPointCount() - 2);

            int i = 1;
            for (String wkt : sequence) {
                Point point1 = (Point) GeometryEngine.geometryFromWkt("POINT(" + wkt + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Point);
                Point point2 = route1.geometry().getPoint(i++);
                assertEquals(point1.getX(), point2.getX(), 1E-6);
                assertEquals(point1.getY(), point2.getY(), 1E-6);
            }

            RoadPoint point21 = new RoadPoint(map.get(10), 0.7);
            RoadPoint point22 = new RoadPoint(map.get(11), 0.3);
            List<Road> path2 = new LinkedList<>(Arrays.asList(map.get(10), map.get(11)));
            Route route2 = new Route(point21, point22, path2);

            assertTrue(route1.add(route2));

            int count = route1.geometry().getPointCount();
            assertEquals(point11.edge().id(), route1.source().edge().id());
            assertEquals(point11.fraction(), route1.source().fraction(), 1E-6);
            assertEquals(point11.geometry().getX(), route1.geometry().getPoint(0).getX(), 1E-6);
            assertEquals(point11.geometry().getY(), route1.geometry().getPoint(0).getY(), 1E-6);
            assertEquals(point22.edge().id(), route1.target().edge().id());
            assertEquals(point22.fraction(), route1.target().fraction(), 1E-6);
            assertEquals(point22.geometry().getX(), route1.geometry().getPoint(count - 1).getX(),
                    1E-6);
            assertEquals(point22.geometry().getY(), route1.geometry().getPoint(count - 1).getY(),
                    1E-6);

            sequence = new LinkedList<>(Arrays.asList(p2, p4));

            assertEquals(sequence.size(), route1.geometry().getPointCount() - 2);

            i = 1;
            for (String wkt : sequence) {
                Point point1 = (Point) GeometryEngine.geometryFromWkt("POINT(" + wkt + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Point);
                Point point2 = route1.geometry().getPoint(i++);
                assertEquals(point1.getX(), point2.getX(), 1E-6);
                assertEquals(point1.getY(), point2.getY(), 1E-6);
            }
        }
        {
            RoadPoint point11 = new RoadPoint(map.get(2), 0.3);
            RoadPoint point12 = new RoadPoint(map.get(10), 1.0);
            List<Road> path1 = new LinkedList<>(Arrays.asList(map.get(2), map.get(10)));
            Route route1 = new Route(point11, point12, path1);

            List<String> sequence = new LinkedList<>(Arrays.asList(p2, p4));

            assertEquals(sequence.size(), route1.geometry().getPointCount() - 1);

            int i = 1;
            for (String wkt : sequence) {
                Point point1 = (Point) GeometryEngine.geometryFromWkt("POINT(" + wkt + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Point);
                Point point2 = route1.geometry().getPoint(i++);
                assertEquals(point1.getX(), point2.getX(), 1E-6);
                assertEquals(point1.getY(), point2.getY(), 1E-6);
            }

            RoadPoint point21 = new RoadPoint(map.get(11), 0.0);
            RoadPoint point22 = new RoadPoint(map.get(11), 0.3);
            List<Road> path2 = new LinkedList<>(Arrays.asList(map.get(11)));
            Route route2 = new Route(point21, point22, path2);

            assertTrue(route1.add(route2));

            int count = route1.geometry().getPointCount();
            assertEquals(point11.edge().id(), route1.source().edge().id());
            assertEquals(point11.fraction(), route1.source().fraction(), 1E-6);
            assertEquals(point11.geometry().getX(), route1.geometry().getPoint(0).getX(), 1E-6);
            assertEquals(point11.geometry().getY(), route1.geometry().getPoint(0).getY(), 1E-6);
            assertEquals(point22.edge().id(), route1.target().edge().id());
            assertEquals(point22.fraction(), route1.target().fraction(), 1E-6);
            assertEquals(point22.geometry().getX(), route1.geometry().getPoint(count - 1).getX(),
                    1E-6);
            assertEquals(point22.geometry().getY(), route1.geometry().getPoint(count - 1).getY(),
                    1E-6);
            sequence = new LinkedList<>(Arrays.asList(p2, p4));

            assertEquals(sequence.size(), route1.geometry().getPointCount() - 2);

            i = 1;
            for (String wkt : sequence) {
                Point point1 = (Point) GeometryEngine.geometryFromWkt("POINT(" + wkt + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Point);
                Point point2 = route1.geometry().getPoint(i++);
                assertEquals(point1.getX(), point2.getX(), 1E-6);
                assertEquals(point1.getY(), point2.getY(), 1E-6);
            }
        }
        {
            RoadPoint point11 = new RoadPoint(map.get(2), 0.0);
            RoadPoint point12 = new RoadPoint(map.get(10), 1.0);
            List<Road> path1 = new LinkedList<>(Arrays.asList(map.get(2), map.get(10)));
            Route route1 = new Route(point11, point12, path1);

            List<String> sequence = new LinkedList<>(Arrays.asList(p1, p2, p4));

            assertEquals(sequence.size(), route1.geometry().getPointCount());

            int i = 0;
            for (String wkt : sequence) {
                Point point1 = (Point) GeometryEngine.geometryFromWkt("POINT(" + wkt + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Point);
                Point point2 = route1.geometry().getPoint(i++);
                assertEquals(point1.getX(), point2.getX(), 1E-6);
                assertEquals(point1.getY(), point2.getY(), 1E-6);
            }

            RoadPoint point21 = new RoadPoint(map.get(11), 0.0);
            RoadPoint point22 = new RoadPoint(map.get(11), 0.3);
            List<Road> path2 = new LinkedList<>(Arrays.asList(map.get(11)));
            Route route2 = new Route(point21, point22, path2);

            assertTrue(route1.add(route2));

            int count = route1.geometry().getPointCount();
            assertEquals(point11.edge().id(), route1.source().edge().id());
            assertEquals(point11.fraction(), route1.source().fraction(), 1E-6);
            assertEquals(point11.geometry().getX(), route1.geometry().getPoint(0).getX(), 1E-6);
            assertEquals(point11.geometry().getY(), route1.geometry().getPoint(0).getY(), 1E-6);
            assertEquals(point22.edge().id(), route1.target().edge().id());
            assertEquals(point22.fraction(), route1.target().fraction(), 1E-6);
            assertEquals(point22.geometry().getX(), route1.geometry().getPoint(count - 1).getX(),
                    1E-6);
            assertEquals(point22.geometry().getY(), route1.geometry().getPoint(count - 1).getY(),
                    1E-6);

            sequence = new LinkedList<>(Arrays.asList(p1, p2, p4));

            assertEquals(sequence.size(), route1.geometry().getPointCount() - 1);

            i = 0;
            for (String wkt : sequence) {
                Point point1 = (Point) GeometryEngine.geometryFromWkt("POINT(" + wkt + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Point);
                Point point2 = route1.geometry().getPoint(i++);
                assertEquals(point1.getX(), point2.getX(), 1E-6);
                assertEquals(point1.getY(), point2.getY(), 1E-6);
            }
        }
    }

    @Test
    public void testJSON() throws JSONException {
        SpatialOperator spatial = new Geography();

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

        Polyline geometry1 =
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p1 + "," + p2 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);
        Polyline geometry2 =
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p3 + "," + p1 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);
        Polyline geometry3 =
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p4 + "," + p1 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);
        Polyline geometry4 =
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p1 + "," + p5 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);
        Polyline geometry5 =
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p2 + "," + p4 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);
        Polyline geometry6 =
                (Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p5 + "," + p3 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);

        List<BaseRoad> osm = new LinkedList<>();
        osm.add(new BaseRoad(1L, 1L, 2L, 1L, false, (short) 1, 1F, 60F, 60F,
                (float) spatial.length(geometry1), geometry1));
        osm.add(new BaseRoad(2L, 3L, 1L, 2L, false, (short) 1, 1F, 60F, 60F,
                (float) spatial.length(geometry2), geometry2));
        osm.add(new BaseRoad(3L, 4L, 1L, 3L, false, (short) 1, 1F, 60F, 60F,
                (float) spatial.length(geometry3), geometry3));
        osm.add(new BaseRoad(4L, 1L, 5L, 4L, false, (short) 1, 1F, 60F, 60F,
                (float) spatial.length(geometry4), geometry4));
        osm.add(new BaseRoad(5L, 2L, 4L, 5L, false, (short) 1, 1F, 60F, 60F,
                (float) spatial.length(geometry5), geometry5));
        osm.add(new BaseRoad(6L, 5L, 3L, 6L, false, (short) 1, 1F, 60F, 60F,
                (float) spatial.length(geometry6), geometry6));


        RoadMap map = new RoadMap();
        for (BaseRoad road : osm) {
            map.add(new Road(road, Heading.forward));
            map.add(new Road(road, Heading.backward));
        }
        map.construct();

        RoadPoint point1 = new RoadPoint(map.get(2), 0.7);
        RoadPoint point2 = new RoadPoint(map.get(10), 0.3);
        List<Road> path = new LinkedList<>(Arrays.asList(map.get(2), map.get(10)));
        Route route = new Route(point1, point2, path);

        String json = route.toJSON().toString();
        Route route2 = Route.fromJSON(new JSONObject(json), map);

        assertEquals(route.size(), route2.size());

        for (int i = 0; i < route.size(); ++i) {
            assertEquals(route.get(i), route2.get(i));
        }
    }

    // @Test
    public void testGeometryBug() throws SourceException, JSONException, IOException {
        RoadMap map = Testmap.instance();

        String json = "{\"roads\":[2675296, 3766758, 3209202, 2051292, 2815288, 2051286, 3209220, "
                + "83452, 7820, 5272, 2815282, 3209196, 3209200, 3209192, 3455442, 3766816],"
                + "\"source\":{\"frac\":0.7122465965298992,\"road\":2675296},"
                + "\"target\":{\"frac\":0.9495659741632408,\"road\":3766816}}";

        Route route = Route.fromJSON(new JSONObject(json), map);

        System.out.println(
                GeometryEngine.geometryToWkt(route.geometry(), WktExportFlags.wktExportLineString));
    }
}
