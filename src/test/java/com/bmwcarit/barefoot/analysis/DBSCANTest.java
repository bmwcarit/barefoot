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

package com.bmwcarit.barefoot.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.spatial.SpatialOperator;
import com.bmwcarit.barefoot.util.Tuple;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.Point;

public class DBSCANTest {

    @Test
    public void testRadius() {
        SpatialOperator spatial = new Geography();

        List<Point> points = new LinkedList<>();
        points.add(new Point(11.560385, 48.176874));
        points.add(new Point(11.560551, 48.177206));
        points.add(new Point(11.559370, 48.176901));
        points.add(new Point(11.557062, 48.176892));
        points.add(new Point(11.558105, 48.177123));
        points.add(new Point(11.557182, 48.177492));

        DBSCAN.SearchIndex index = new DBSCAN.SearchIndex(points);

        {
            Point search = new Point(11.560016, 48.176717);
            List<Point> result = index.radius(search, 100);
            assertTrue(result.size() > 0);
            for (Point point : points) {
                if (spatial.distance(search, point) <= 100) {
                    assertTrue(result.contains(point));
                } else {
                    assertTrue(!result.contains(point));
                }
            }
        }
        {
            Point search = new Point(11.556564, 48.17680);
            List<Point> result = index.radius(search, 100);
            assertTrue(result.size() > 0);
            for (Point point : points) {
                if (spatial.distance(search, point) <= 100) {
                    assertTrue(result.contains(point));
                } else {
                    assertTrue(!result.contains(point));
                }
            }
        }
        {
            Point search = new Point(11.556564, 48.17680);
            List<Point> result = index.radius(search, 1000);
            assertEquals(result.size(), points.size());
            for (Point point : points) {
                assertTrue(spatial.distance(search, point) <= 1000);
                assertTrue(result.contains(point));
            }
        }
    }

    @Test
    public void testCluster() {
        List<Point> points = new LinkedList<>();
        points.add(new Point(11.560385, 48.176874));
        points.add(new Point(11.560551, 48.177206));
        points.add(new Point(11.559370, 48.176901));
        points.add(new Point(11.557062, 48.176892));
        points.add(new Point(11.558105, 48.177123));
        points.add(new Point(11.557182, 48.177492));

        {
            Set<List<Point>> results = DBSCAN.cluster(points, 75, 2);
            Set<List<Point>> clusters = new HashSet<>();
            clusters.add(Arrays.asList(new Point(11.560385, 48.176874),
                    new Point(11.560551, 48.177206)));
            clusters.add(Arrays.asList(new Point(11.557062, 48.176892),
                    new Point(11.557182, 48.177492)));

            assertEquals(clusters.size(), results.size());
            for (List<Point> result : results) {
                List<Point> rcluster = null;
                Point first = result.iterator().next();
                for (List<Point> cluster : clusters) {
                    if (cluster.contains(first)) {
                        rcluster = cluster;
                        break;
                    }
                }
                assertEquals(result.size(), rcluster.size());
                for (Point element : result) {
                    assertTrue(rcluster.contains(element));
                }
            }
        }
        {
            Set<List<Point>> results = DBSCAN.cluster(points, 50, 2);
            Set<List<Point>> clusters = new HashSet<>();
            clusters.add(Arrays.asList(new Point(11.560385, 48.176874),
                    new Point(11.560551, 48.177206)));

            assertEquals(clusters.size(), results.size());
            for (List<Point> result : results) {
                List<Point> rcluster = null;
                Point first = result.iterator().next();
                for (List<Point> cluster : clusters) {
                    if (cluster.contains(first)) {
                        rcluster = cluster;
                        break;
                    }
                }
                assertEquals(result.size(), rcluster.size());
                for (Point element : result) {
                    assertTrue(rcluster.contains(element));
                }
            }
        }
        {
            Set<List<Point>> results = DBSCAN.cluster(points, 100, 2);
            Set<List<Point>> clusters = new HashSet<>();
            clusters.add(points);

            assertEquals(clusters.size(), results.size());
            for (List<Point> result : results) {
                List<Point> rcluster = null;
                Point first = result.iterator().next();
                for (List<Point> cluster : clusters) {
                    if (cluster.contains(first)) {
                        rcluster = cluster;
                        break;
                    }
                }
                assertEquals(result.size(), rcluster.size());
                for (Point element : result) {
                    assertTrue(rcluster.contains(element));
                }
            }
        }
    }

    @Test
    public void testNYCSample() throws NumberFormatException, ParseException, IOException {
        SpatialOperator spatial = new Geography();
        double radius = 100, density = 10;
        List<Point> points = new ArrayList<>();

        for (Tuple<Point, Long> element : NYCSample.sources()) {
            points.add(element.one());
        }

        for (Tuple<Point, Long> element : NYCSample.targets()) {
            points.add(element.one());
        }

        Set<List<Point>> clusters = DBSCAN.cluster(points, radius, (int) density);

        PrintWriter writer = new PrintWriter(
                DBSCANTest.class.getResource("").getPath() + "DBSCAN-clusters.json");
        writer.print("{\"type\": \"FeatureCollection\",\"features\": [");

        String[] colors = {"#259f23", "#de9a1d", "#c14f97", "#3731ff", "#0dbdce", "#ace213",
                "#e30502", "#9f2363", "#50239f"};
        int i = 0;
        for (List<Point> cluster : clusters) {
            assertTrue(cluster.size() >= density);
            for (Point point : cluster) {
                boolean distance = false;
                for (Point other : cluster) {
                    if (spatial.distance(point, other) <= radius) {
                        distance = true;
                    }
                }
                assertTrue(distance);
            }

            MultiPoint multipoint = new MultiPoint();

            for (Point point : cluster) {
                multipoint.add(point);
            }

            if (i++ > 0) {
                writer.print(", ");
            }
            writer.print("{\"type\": \"Feature\"," + "\"properties\": {\"marker-color\": \""
                    + colors[i % colors.length] + "\", \"marker-size\": \"small\"},"
                    + "\"geometry\": ");
            writer.print(GeometryEngine.geometryToGeoJson(multipoint));
            writer.print("}");
        }

        writer.print("]}");
        writer.close();
    }
}
