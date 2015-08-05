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

package com.bmwcarit.barefoot.spatial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.spatial.QuadTreeIndex;
import com.bmwcarit.barefoot.spatial.SpatialOperator;
import com.bmwcarit.barefoot.util.Tuple;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WktImportFlags;

public class QuadTreeIndexTest {
    private static List<Polyline> geometries() {
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

        List<Polyline> geometries = new LinkedList<Polyline>();
        geometries.add((Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p1 + "," + p2
                + ")", WktImportFlags.wktImportDefaults, Geometry.Type.Polyline));
        geometries.add((Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p3 + "," + p1
                + ")", WktImportFlags.wktImportDefaults, Geometry.Type.Polyline));
        geometries.add((Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p4 + "," + p1
                + ")", WktImportFlags.wktImportDefaults, Geometry.Type.Polyline));
        geometries.add((Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p1 + "," + p5
                + ")", WktImportFlags.wktImportDefaults, Geometry.Type.Polyline));
        geometries.add((Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p2 + "," + p4
                + ")", WktImportFlags.wktImportDefaults, Geometry.Type.Polyline));
        geometries.add((Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p5 + "," + p3
                + ")", WktImportFlags.wktImportDefaults, Geometry.Type.Polyline));

        return geometries;
    }

    @Test
    public void testIndexNearest() {
        SpatialOperator spatial = new Geography();
        QuadTreeIndex index = new QuadTreeIndex();
        List<Polyline> lines = geometries();
        {
            int i = 0;
            for (Polyline line : lines) {
                index.add(i++, line);
            }
        }

        {
            Point c = new Point(11.343629, 48.083797);

            double dmin = Double.MAX_VALUE;
            int nearest = 0;
            for (int i = 0; i < lines.size(); ++i) {
                double f = spatial.intercept(lines.get(i), c);
                Point p = spatial.interpolate(lines.get(i), f);
                double d = spatial.distance(c, p);

                if (d < dmin) {
                    dmin = d;
                    nearest = i;
                }
            }

            Set<Tuple<Integer, Double>> points = index.nearest(c);

            assertEquals(1, points.size());
            assertEquals(nearest, (int) points.iterator().next().one());
        }
        {
            Point c = new Point(11.344827, 48.083752);

            double dmin = Double.MAX_VALUE;
            int nearest = 0;
            for (int i = 0; i < lines.size(); ++i) {
                double f = spatial.intercept(lines.get(i), c);
                Point p = spatial.interpolate(lines.get(i), f);
                double d = spatial.distance(c, p);

                if (d < dmin) {
                    dmin = d;
                    nearest = i;
                }
            }

            Set<Tuple<Integer, Double>> points = index.nearest(c);

            assertEquals(1, points.size());
            assertEquals(nearest, (int) points.iterator().next().one());
        }
    }

    @Test
    public void testIndexRadius() {
        SpatialOperator spatial = new Geography();
        QuadTreeIndex index = new QuadTreeIndex();
        List<Polyline> lines = geometries();
        {
            int i = 0;
            for (Polyline line : lines) {
                index.add(i++, line);
            }
        }

        {
            Point c = new Point(11.343629, 48.083797);
            double r = 50;

            Set<Integer> neighbors = new HashSet<Integer>();
            for (int i = 0; i < lines.size(); ++i) {
                double f = spatial.intercept(lines.get(i), c);
                Point p = spatial.interpolate(lines.get(i), f);
                double d = spatial.distance(c, p);

                if (d <= r) {
                    neighbors.add(i);
                }
            }
            assertEquals(4, neighbors.size());

            Set<Tuple<Integer, Double>> points = index.radius(c, r);

            assertEquals(neighbors.size(), points.size());
            for (Tuple<Integer, Double> point : points) {
                assertTrue(neighbors.contains(point.one()));
            }
        }
        {
            Point c = new Point(11.344827, 48.083752);
            double r = 10;

            Set<Integer> neighbors = new HashSet<Integer>();
            for (int i = 0; i < lines.size(); ++i) {
                double f = spatial.intercept(lines.get(i), c);
                Point p = spatial.interpolate(lines.get(i), f);
                double d = spatial.distance(c, p);

                if (d <= r) {
                    neighbors.add(i);
                }
            }
            assertEquals(1, neighbors.size());

            Set<Tuple<Integer, Double>> points = index.radius(c, r);

            assertEquals(neighbors.size(), points.size());
            for (Tuple<Integer, Double> point : points) {
                assertTrue(neighbors.contains(point.one()));
            }
        }
        {
            Point c = new Point(11.344827, 48.083752);
            double r = 5;

            Set<Integer> neighbors = new HashSet<Integer>();
            for (int i = 0; i < lines.size(); ++i) {
                double f = spatial.intercept(lines.get(i), c);
                Point p = spatial.interpolate(lines.get(i), f);
                double d = spatial.distance(c, p);

                if (d <= r) {
                    neighbors.add(i);
                }
            }
            assertEquals(0, neighbors.size());

            Set<Tuple<Integer, Double>> points = index.radius(c, r);

            assertEquals(neighbors.size(), points.size());
            for (Tuple<Integer, Double> point : points) {
                assertTrue(neighbors.contains(point.one()));
            }
        }
    }
}
