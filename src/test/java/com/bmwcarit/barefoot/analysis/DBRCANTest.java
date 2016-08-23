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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.bmwcarit.barefoot.analysis.DBCAN.ISearchIndex;
import com.bmwcarit.barefoot.util.Tuple;
import com.esri.core.geometry.Point;

public class DBRCANTest {

    @Test
    public void testEpsilonCompare() {
        assertTrue(DBRCAN.epsilonCompare(0.0, 0.0) == 0);
        assertTrue(DBRCAN.epsilonCompare(1.0, 1.0) == 0);
        assertTrue(DBRCAN.epsilonCompare(-1.0, -1.0) == 0);
        assertTrue(0.0 != 0.0 + 1E-11);
        assertTrue(DBRCAN.epsilonCompare(0.0, 0.0 + 1E-11) == 0);
        assertTrue(DBRCAN.epsilonCompare(0.0, 0.0 - 1E-11) == 0);
        assertTrue(0.0 != 0.0 + 1E-10);
        assertTrue(DBRCAN.epsilonCompare(0.0, 0.0 + 1E-10) != 0);
        assertTrue(DBRCAN.epsilonCompare(0.0, 0.0 - 1E-10) != 0);
        assertTrue(-1.0 != -1.0 + 1E-11);
        assertTrue(DBRCAN.epsilonCompare(-1.0, -1.0 + 1E-11) == 0);
        assertTrue(DBRCAN.epsilonCompare(-1.0, -1.0 - 1E-11) == 0);
        assertTrue(-1.0 != -1.0 + 1E-10);
        assertTrue(DBRCAN.epsilonCompare(-1.0, -1.0 + 1E-10) != 0);
        assertTrue(DBRCAN.epsilonCompare(-1.0, -1.0 - 1E-10) != 0);

        assertTrue(0.0 < 0.0 + 1E-10);
        assertTrue(DBRCAN.epsilonCompare(0.0, 0.0 + 1E-10) < 0);
        assertTrue(DBRCAN.epsilonCompare(0.0, 0.0 - 1E-10) > 0);
        assertTrue(-1.0 < -1.0 + 1E-10);
        assertTrue(DBRCAN.epsilonCompare(-1.0, -1.0 + 1E-10) < 0);
        assertTrue(DBRCAN.epsilonCompare(-1.0, -1.0 - 1E-10) > 0);

        assertTrue(DBRCAN.epsilonRound(2.0 + 1E-10 + 1E-11) == 2.0 + 1E-10);
        assertTrue(DBRCAN.epsilonRound(2.0 + 1E-10 - 1E-11) == 2.0 + 1E-10);
        assertTrue(DBRCAN.epsilonRound(2.0 + 1E-10) == 2.0 + 1E-10);
        assertTrue(DBRCAN.epsilonRound(2.0 + 1E-11) == 2.0);
        assertTrue(DBRCAN.epsilonRound(2.0 - 1E-10 + 1E-11) == 2.0 - 1E-10);
        assertTrue(DBRCAN.epsilonRound(2.0 - 1E-10 - 1E-11) == 2.0 - 1E-10);
        assertTrue(DBRCAN.epsilonRound(2.0 - 1E-10) == 2.0 - 1E-10);
        assertTrue(DBRCAN.epsilonRound(2.0 - 1E-11) == 2.0);
        assertTrue(DBRCAN.epsilonRound(-2.0 + 1E-10 + 1E-11) == -2.0 + 1E-10);
        assertTrue(DBRCAN.epsilonRound(-2.0 + 1E-10 - 1E-11) == -2.0 + 1E-10);
        assertTrue(DBRCAN.epsilonRound(-2.0 + 1E-10) == -2.0 + 1E-10);
        assertTrue(DBRCAN.epsilonRound(-2.0 + 1E-11) == -2.0);
        assertTrue(DBRCAN.epsilonRound(-2.0 - 1E-10 + 1E-11) == -2.0 - 1E-10);
        assertTrue(DBRCAN.epsilonRound(-2.0 - 1E-10 - 1E-11) == -2.0 - 1E-10);
        assertTrue(DBRCAN.epsilonRound(-2.0 - 1E-10) == -2.0 - 1E-10);
        assertTrue(DBRCAN.epsilonRound(-2.0 - 1E-11) == -2.0);
        assertTrue(DBRCAN.epsilonRound(0.4 - 1E-15) == 0.4);
    }

    @Test
    public void testModulo() {
        assertTrue(DBRCAN.epsilonCompare(DBRCAN.modulo(5, 3), 2.0) == 0);
        assertTrue(DBRCAN.epsilonCompare(DBRCAN.modulo(5, -3), 2.0) == 0);
        assertTrue(DBRCAN.epsilonCompare(DBRCAN.modulo(5, -3), 2.0) == 0);
        assertTrue(DBRCAN.epsilonCompare(DBRCAN.modulo(1.3, 0.5), 0.3) == 0);
        assertTrue(DBRCAN.epsilonCompare(DBRCAN.modulo(1.3, -0.5), 0.3) == 0);
        assertTrue(DBRCAN.epsilonCompare(DBRCAN.modulo(-5, 3), 1.0) == 0);
        assertTrue(DBRCAN.epsilonCompare(DBRCAN.modulo(-5, -3), 1.0) == 0);
        assertTrue(DBRCAN.epsilonCompare(DBRCAN.modulo(-1.3, 0.5), 0.2) == 0);
        assertTrue(DBRCAN.epsilonCompare(DBRCAN.modulo(-1.3, -0.5), 0.2) == 0);
        assertTrue(DBRCAN.modulo(0.75, 0.6) != 0.15);
        assertTrue(DBRCAN.epsilonCompare(DBRCAN.modulo(0.75, 0.6), 0.15) == 0);
    }

    @Test
    public void testSearch() {
        {
            List<Double> list = Arrays.asList(0.0);
            DBRCAN.SearchIndex index = new DBRCAN.SearchIndex(1.0, list);

            assertEquals(index.search(-1.0), 0);
            assertEquals(index.search(0.0), 0);
            assertEquals(index.search(1.0), 0);
        }
        {
            List<Double> list = Arrays.asList(0.0, 0.0);
            DBRCAN.SearchIndex index = new DBRCAN.SearchIndex(1.0, list);

            assertEquals(index.search(-1.0), 0);
            assertEquals(index.search(0.0), 0);
            assertEquals(index.search(1.0), 0);
        }
        {
            List<Double> list = Arrays.asList(0.0, 1.0);
            DBRCAN.SearchIndex index = new DBRCAN.SearchIndex(2.0, list);

            assertEquals(index.search(-1.0), 1);
            assertEquals(index.search(0.0), 0);
            assertEquals(index.search(0.5), 0);
            assertEquals(index.search(1.0), 1);
            assertEquals(index.search(2.0), 0);
        }
        {
            List<Double> list = Arrays.asList(0.0, 0.0, 1.0, 1.0);
            DBRCAN.SearchIndex index = new DBRCAN.SearchIndex(2.0, list);

            assertEquals(index.search(-1.0), 1);
            assertEquals(index.search(0.0), 0);
            assertEquals(index.search(0.5), 0);
            assertEquals(index.search(1.0), 1);
            assertEquals(index.search(2.0), 0);
        }
        {
            List<Double> list = Arrays.asList(0.0, 1.0, 2.0);
            DBRCAN.SearchIndex index = new DBRCAN.SearchIndex(2.0, list);

            assertEquals(index.search(-1.0), 1);
            assertEquals(index.search(0.0), 0);
            assertEquals(index.search(0.5), 0);
            assertEquals(index.search(1.0), 1);
            assertEquals(index.search(2.0), 0);
            assertEquals(index.search(3.0), 1);
        }
        {
            List<Double> list = Arrays.asList(-1.0, 1.0, 2.0, 3.0, 4.0, 6.0);
            DBRCAN.SearchIndex index = new DBRCAN.SearchIndex(5.0, list);

            assertEquals(index.search(-1.0), 3);
            assertEquals(index.search(0.0), 3);
            assertEquals(index.search(2.5d), 1);
            assertEquals(index.search(3.0), 2);
            assertEquals(index.search(5.0), 3);
            assertEquals(index.search(6.0), 0);
        }
        {
            List<Double> list = Arrays.asList(-1.0, -6.0, 1.0, 2.0, 3.0, 3.0, 4.0, 6.0, 7.0, 8.0);
            DBRCAN.SearchIndex index = new DBRCAN.SearchIndex(5.0, list);

            assertEquals(index.search(-1.0), 3);
            assertEquals(index.search(0.0), 3);
            assertEquals(index.search(2.5d), 1);
            assertEquals(index.search(3.0), 2);
            assertEquals(index.search(5.0), 3);
            assertEquals(index.search(6.0), 0);
        }
    }

    @Test
    public void testRadius() {
        {
            List<Double> list = Arrays.asList(0.0);
            ISearchIndex<Double> index = new DBRCAN.SearchIndex(1.0, list);

            List<Double> interval = Arrays.asList(0.0);
            List<Double> result = index.radius(0.0, 1.0);
            assertEquals(result.size(), interval.size());
            for (Double value : result) {
                assertTrue(interval.contains(value));
            }
        }
        {
            List<Double> list = Arrays.asList(0.0, 0.0);
            ISearchIndex<Double> index = new DBRCAN.SearchIndex(1.0, list);

            List<Double> interval = Arrays.asList(0.0, 0.0);
            List<Double> result = index.radius(0.0, 1.0);
            assertEquals(result.size(), interval.size());
            for (Double value : result) {
                assertTrue(interval.contains(value));
            }
        }
        {
            List<Double> list = Arrays.asList(0.0, 1.0, 2.0, 3.0, 4.0, 5.0);
            ISearchIndex<Double> index = new DBRCAN.SearchIndex(6.0, list);

            {
                List<Double> interval = Arrays.asList(4.0, 5.0, 0.0);
                List<Double> result = index.radius(5.0, 1.0);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
            {
                List<Double> interval = Arrays.asList(3.0, 4.0, 5.0);
                List<Double> result = index.radius(4.0, 1.0);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
            {
                List<Double> interval = Arrays.asList(0.0);
                List<Double> result = index.radius(0.0, 0.0);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
            {
                List<Double> interval = Arrays.asList(0.0, 1.0, 2.0, 3.0, 4.0, 5.0);
                List<Double> result = index.radius(0.0, 3.0);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
            {
                List<Double> interval = Arrays.asList(0.0, 1.0, 2.0, 3.0, 4.0, 5.0);
                List<Double> result = index.radius(4.0, 3.0);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
            {
                List<Double> interval = Arrays.asList(2.0, 3.0, 4.0);
                List<Double> result = index.radius(3.0, 1.0);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
            {
                List<Double> interval = Arrays.asList(0.0, 1.0, 2.0, 3.0, 4.0, 5.0);
                List<Double> result = index.radius(4.0, 4.0);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
        }
        {
            List<Double> list = Arrays.asList(0.0, 1.0, 1.0, 2.0, 3.0, 4.0, 4.0, 5.0);
            ISearchIndex<Double> index = new DBRCAN.SearchIndex(6.0, list);

            {
                List<Double> interval = Arrays.asList(4.0, 4.0, 5.0, 0.0);
                List<Double> result = index.radius(5.0, 1.0);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
        }
        {
            List<Double> list = Arrays.asList(0.5);
            ISearchIndex<Double> index = new DBRCAN.SearchIndex(1.0, list);

            List<Double> interval = new LinkedList<>();
            List<Double> result = index.radius(0.2, 0.2);
            assertEquals(result.size(), interval.size());
        }
        {
            List<Double> list = Arrays.asList(0.0, 0.25, 0.5, 0.75);
            ISearchIndex<Double> index = new DBRCAN.SearchIndex(1.0, list);

            List<Double> interval = Arrays.asList(0.0, 0.25, 0.5, 0.75);
            List<Double> result = index.radius(0.5, 0.5);
            assertEquals(result.size(), interval.size());
            for (Double value : result) {
                assertTrue(interval.contains(value));
            }
        }
        {
            List<Double> list =
                    Arrays.asList(-0.95, -0.3, -0.1, 0.0, 0.25, 0.5, 0.75, 1.2, 1.45, 1.5);
            ISearchIndex<Double> index = new DBRCAN.SearchIndex(0.6, list);

            {
                List<Double> interval = Arrays.asList(-0.95, -0.3, 0.25, 0.75, 1.45, 1.5);
                List<Double> result = index.radius(0.25, 0.1);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
            {
                List<Double> interval = Arrays.asList(-0.95, 0.25, 0.75, 1.45);
                List<Double> result = index.radius(0.2, 0.05);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
            {
                List<Double> interval = Arrays.asList(-0.1, 0.0, 0.5, 1.2);
                List<Double> result = index.radius(0.55, 0.05);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
            {
                List<Double> interval = Arrays.asList(-0.1, 0.0, 0.5, 0.75, 1.2);
                List<Double> result = index.radius(0.0, 0.15);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
            {
                List<Double> interval = new LinkedList<>();
                List<Double> result = index.radius(0.2, 0.049);
                assertEquals(result.size(), interval.size());
            }
        }
        {
            List<Double> list = Arrays.asList(-0.95, -0.3, -0.1, 0.25, 0.5, 0.75, 1.45, 1.5);
            ISearchIndex<Double> index = new DBRCAN.SearchIndex(0.6, list);

            {
                List<Double> interval = new LinkedList<>();
                List<Double> result = index.radius(0.58, 0.05);
                assertEquals(result.size(), interval.size());
            }
        }
    }

    @Test
    public void testCluster() {
        {
            List<Double> list =
                    Arrays.asList(-0.95, -0.3, -0.1, 0.0, 0.25, 0.5, 0.75, 1.2, 1.45, 1.5);
            {
                Set<List<Double>> results = DBRCAN.cluster(list, 0.6, 0.075, 2);
                Set<List<Double>> clusters = new HashSet<>();
                clusters.add(Arrays.asList(-0.95, -0.3, 0.25, 1.45, 1.5));
                clusters.add(Arrays.asList(0.0, 1.2));
                clusters.add(Arrays.asList(-0.1, 0.5));

                assertEquals(clusters.size(), results.size());
                for (List<Double> result : results) {
                    List<Double> rcluster = null;
                    Double first = result.iterator().next();
                    for (List<Double> cluster : clusters) {
                        if (cluster.contains(first)) {
                            rcluster = cluster;
                            break;
                        }
                    }
                    assertEquals(result.size(), rcluster.size());
                    for (Double element : result) {
                        assertTrue(rcluster.contains(element));
                    }
                }
            }
            {
                Set<List<Double>> results = DBRCAN.cluster(list, 0.6, 0.1, 2);
                Set<List<Double>> clusters = new HashSet<>();
                clusters.add(Arrays.asList(-0.95, -0.3, 0.25, 0.75, 1.45, 1.5));
                clusters.add(Arrays.asList(-0.1, 0.0, 0.5, 1.2));

                assertEquals(clusters.size(), results.size());
                for (List<Double> result : results) {
                    List<Double> rcluster = null;
                    Double first = result.iterator().next();
                    for (List<Double> cluster : clusters) {
                        if (cluster.contains(first)) {
                            rcluster = cluster;
                            break;
                        }
                    }
                    assertEquals(result.size(), rcluster.size());
                    for (Double element : result) {
                        assertTrue(rcluster.contains(element));
                    }
                }
            }
        }
        {
            List<Double> list = Arrays.asList(-1.9, -1.7, 0.7, 0.9, 1.0, 1.1, 1.2, 3.1, 3.15, 3.2,
                    5.4, 5.5, 5.8);
            {
                Set<List<Double>> results = DBRCAN.cluster(list, 2.4, 0.1, 4);
                Set<List<Double>> clusters = new HashSet<>();
                clusters.add(list);

                assertEquals(clusters.size(), results.size());
                for (List<Double> result : results) {
                    List<Double> rcluster = null;
                    Double first = result.iterator().next();
                    for (List<Double> cluster : clusters) {
                        if (cluster.contains(first)) {
                            rcluster = cluster;
                            break;
                        }
                    }
                    assertEquals(result.size(), rcluster.size());
                    for (Double element : result) {
                        assertTrue(rcluster.contains(element));
                    }
                }
            }
        }
    }

    @Test
    public void testBounds() {
        {
            List<Double> cluster = Arrays.asList(-0.95, -0.3, 0.25, 1.45, 1.5);
            Tuple<Double, Double> bounds = DBRCAN.bounds(cluster, 0.6, 0.075, 0.0);

            assertTrue(bounds != null);
            assertTrue(DBRCAN.epsilonCompare(bounds.one(), 0.25) == 0);
            assertTrue(DBRCAN.epsilonCompare(bounds.two(), 0.3) == 0);
        }
        {
            List<Double> cluster = Arrays.asList(0.0, 1.2);
            Tuple<Double, Double> bounds = DBRCAN.bounds(cluster, 0.6, 0.075, 0.0);

            assertTrue(bounds != null);
            assertTrue(DBRCAN.epsilonCompare(bounds.one(), 0.0) == 0);
            assertTrue(DBRCAN.epsilonCompare(bounds.two(), 0.0) == 0);
        }
        {
            List<Double> cluster = Arrays.asList(0.0, 1.2, 1.85);
            Tuple<Double, Double> bounds = DBRCAN.bounds(cluster, 0.6, 0.075, 0.0);

            assertTrue(bounds != null);
            assertTrue(DBRCAN.epsilonCompare(bounds.one(), 0.0) == 0);
            assertTrue(DBRCAN.epsilonCompare(bounds.two(), 0.05) == 0);
        }
        {
            List<Double> cluster = Arrays.asList(-0.1, 0.0, 0.5, 1.2, 1.9);
            Tuple<Double, Double> bounds = DBRCAN.bounds(cluster, 0.6, 0.1, 0.0);

            assertTrue(bounds != null);
            assertTrue(DBRCAN.epsilonCompare(bounds.one(), 0.5) == 0);
            assertTrue(DBRCAN.epsilonCompare(bounds.two(), 0.1) == 0);
        }
        {
            List<Double> cluster = Arrays.asList(-0.1, 0.0, 0.5, 1.2, 1.9);
            Tuple<Double, Double> bounds = DBRCAN.bounds(cluster, 0.3, 0.1, 0.0);

            assertTrue(bounds == null);
        }

        {
            List<Double> cluster = Arrays.asList(-0.95, -0.3, 0.25, 1.45, 1.5);
            Tuple<Double, Double> bounds = DBRCAN.bounds(cluster, 0.6, 0.075, 0.1);

            assertTrue(bounds != null);
            assertTrue(DBRCAN.epsilonCompare(bounds.one(), 0.15) == 0);
            assertTrue(DBRCAN.epsilonCompare(bounds.two(), 0.4) == 0);
        }
        {
            List<Double> cluster = Arrays.asList(0.0, 1.2);
            Tuple<Double, Double> bounds = DBRCAN.bounds(cluster, 0.6, 0.075, 0.1);

            assertTrue(bounds != null);
            assertTrue(DBRCAN.epsilonCompare(bounds.one(), 0.5) == 0);
            assertTrue(DBRCAN.epsilonCompare(bounds.two(), 0.1) == 0);
        }
        {
            List<Double> cluster = Arrays.asList(0.0, 1.2, 1.85);
            Tuple<Double, Double> bounds = DBRCAN.bounds(cluster, 0.6, 0.075, 0.1);

            assertTrue(bounds != null);
            assertTrue(DBRCAN.epsilonCompare(bounds.one(), 0.5) == 0);
            assertTrue(DBRCAN.epsilonCompare(bounds.two(), 0.15) == 0);
        }
        {
            List<Double> cluster = Arrays.asList(-0.1, 0.0, 0.5, 1.2, 1.9);
            Tuple<Double, Double> bounds = DBRCAN.bounds(cluster, 0.6, 0.1, 0.1);

            assertTrue(bounds != null);
            assertTrue(DBRCAN.epsilonCompare(bounds.one(), 0.4) == 0);
            assertTrue(DBRCAN.epsilonCompare(bounds.two(), 0.2) == 0);
        }
        {
            List<Double> cluster = Arrays.asList(-0.1, 0.0, 0.5, 1.2, 1.9);
            Tuple<Double, Double> bounds = DBRCAN.bounds(cluster, 0.3, 0.1, 0.1);

            assertTrue(bounds == null);
        }
    }

    @Test
    public void testFunction() {
        {
            List<Double> list = new LinkedList<>();

            List<Tuple<Double, Integer>> result = DBRCAN.function(list, 2.4, 0.1, 0.0);
            List<Tuple<Double, Integer>> function = new LinkedList<>();
            function.add(new Tuple<>(0.0, 0));

            assertEquals(result.size(), function.size());

            for (int i = 0; i < result.size(); ++i) {
                assertTrue(DBRCAN.epsilonCompare(result.get(i).one(), function.get(i).one()) == 0);
                assertEquals(result.get(i).two(), function.get(i).two());
            }
        }
        {
            List<Double> list =
                    Arrays.asList(-1.9, -1.75, -1.7, 0.7, 0.9, 2.8, 3.1, 3.15, 3.2, 5.4, 5.5, 5.8);

            List<Tuple<Double, Integer>> result = DBRCAN.function(list, 2.4, 0.1, 0.0);
            List<Tuple<Double, Integer>> function = new LinkedList<>();
            function.add(new Tuple<>(0.0, 0));
            function.add(new Tuple<>(0.4, 2));
            function.add(new Tuple<>(0.5, 4));
            function.add(new Tuple<>(0.6, 8));
            function.add(new Tuple<>(0.8, 4));
            function.add(new Tuple<>(0.9, 2));
            function.add(new Tuple<>(1.0, 0));

            assertEquals(result.size(), function.size());

            for (int i = 0; i < result.size(); ++i) {
                assertTrue(DBRCAN.epsilonCompare(result.get(i).one(), function.get(i).one()) == 0);
                assertEquals(result.get(i).two(), function.get(i).two());
            }
        }
        {
            List<Double> list = Arrays.asList(-1.9, -1.7, -1.4, -1.25, -1.2, 0.05, 0.7, 0.75, 0.9,
                    1.2, 1.3, 1.7, 2.0, 2.3, 2.35, 2.8, 3.1, 3.15, 3.2, 3.5, 3.55, 3.6, 5.4, 5.5,
                    6.0);

            List<Tuple<Double, Integer>> result = DBRCAN.function(list, 2.4, 0.1, 0.0);
            List<Tuple<Double, Integer>> function = new LinkedList<>();
            function.add(new Tuple<>(0.0, 2));
            function.add(new Tuple<>(0.05, 0));
            function.add(new Tuple<>(0.4, 2));
            function.add(new Tuple<>(0.5, 4));
            function.add(new Tuple<>(0.6, 8));
            function.add(new Tuple<>(0.9, 2));
            function.add(new Tuple<>(1.0, 8));
            function.add(new Tuple<>(1.3, 0));
            function.add(new Tuple<>(1.7, 1));
            function.add(new Tuple<>(1.7, 0));
            function.add(new Tuple<>(2.0, 1));
            function.add(new Tuple<>(2.0, 0));
            function.add(new Tuple<>(2.3, 2));

            assertEquals(result.size(), function.size());

            for (int i = 0; i < result.size(); ++i) {
                assertTrue(DBRCAN.epsilonCompare(result.get(i).one(), function.get(i).one()) == 0);
                assertEquals(result.get(i).two(), function.get(i).two());
            }
        }
        {
            List<Double> list = Arrays.asList(-1.9, -1.7, -1.4, -1.25, -1.2, 0.05, 0.7, 0.75, 0.9,
                    1.2, 1.3, 1.7, 2.0, 2.3, 2.35, 2.8, 3.1, 3.15, 3.2, 3.5, 3.55, 3.6, 5.4, 5.5,
                    6.0);

            List<Tuple<Double, Integer>> result = DBRCAN.function(list, 2.4, 0.1, 0.05);
            List<Tuple<Double, Integer>> function = new LinkedList<>();
            function.add(new Tuple<>(0.0, 2));
            function.add(new Tuple<>(0.1, 0));
            function.add(new Tuple<>(0.35, 2));
            function.add(new Tuple<>(0.45, 4));
            function.add(new Tuple<>(0.55, 8));
            function.add(new Tuple<>(0.95, 8));
            function.add(new Tuple<>(1.35, 0));
            function.add(new Tuple<>(1.65, 1));
            function.add(new Tuple<>(1.75, 0));
            function.add(new Tuple<>(1.95, 1));
            function.add(new Tuple<>(2.05, 0));
            function.add(new Tuple<>(2.25, 2));

            assertEquals(result.size(), function.size());

            for (int i = 0; i < result.size(); ++i) {
                assertTrue(DBRCAN.epsilonCompare(result.get(i).one(), function.get(i).one()) == 0);
                assertEquals(result.get(i).two(), function.get(i).two());
            }
        }
    }

    @Test
    public void testNYCSample() throws NumberFormatException, IOException, ParseException {
        List<Point> points = new LinkedList<>();
        Map<Point, List<Long>> times = new HashMap<>();

        for (Tuple<Point, Long> source : NYCSample.sources()) {
            points.add(source.one());
            if (times.containsKey(source.one())) {
                times.get(source.one()).add(source.two());
            } else {
                times.put(source.one(), new LinkedList<>(Arrays.asList(source.two())));
            }
        }

        for (Tuple<Point, Long> target : NYCSample.targets()) {
            points.add(target.one());
            if (times.containsKey(target.one())) {
                times.get(target.one()).add(target.two());
            } else {
                times.put(target.one(), new LinkedList<>(Arrays.asList(target.two())));
            }
        }

        Set<List<Point>> clusters = DBSCAN.cluster(points, 100, 10);

        int clusterId = 0;
        for (List<Point> cluster : clusters) {
            List<Double> timestamps = new LinkedList<>();

            for (Point point : cluster) {
                if (points.contains(point)) {
                    for (Long time : times.get(point)) {
                        timestamps.add((double) time);
                    }
                }
            }

            List<Tuple<Double, Integer>> function =
                    DBRCAN.function(timestamps, 24 * 60 * 60, 30 * 60, 0);

            DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            PrintWriter writer = new PrintWriter(
                    DBSCANTest.class.getResource("").getPath() + clusterId + ".dat");
            Integer density = null;
            for (Tuple<Double, Integer> element : function) {
                if (density != null) {
                    writer.println((long) (double) element.one() + " " + density + " "
                            + formatter.format(new Date((long) (double) element.one() * 1000)));
                }
                writer.println((long) (double) element.one() + " " + element.two() + " "
                        + formatter.format(new Date((long) (double) element.one() * 1000)));
                density = element.two();
            }
            writer.println(
                    (24 * 60 * 60) + " " + density + " " + formatter.format(24 * 60 * 60 * 1000));
            writer.close();

            clusterId += 1;
        }
    }
}
