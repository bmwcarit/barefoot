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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.bmwcarit.barefoot.analysis.DBCAN.ISearchIndex;

public class DBCANTest {

    @Test
    public void testSearch() {
        {
            DBCAN.SearchIndex index = new DBCAN.SearchIndex(Arrays.asList(0.0));

            assertEquals(index.searchLeft(-1.0), 0);
            assertEquals(index.searchLeft(0.0), 0);
            assertEquals(index.searchLeft(1.0), 0);

            assertEquals(index.searchRight(-1.0), 0);
            assertEquals(index.searchRight(0.0), 0);
            assertEquals(index.searchRight(1.0), 0);
        }
        {
            DBCAN.SearchIndex index = new DBCAN.SearchIndex(Arrays.asList(0.0, 0.0));

            assertEquals(index.searchLeft(-1.0), 0);
            assertEquals(index.searchLeft(0.0), 0);
            assertEquals(index.searchLeft(1.0), 1);

            assertEquals(index.searchRight(-1.0), 0);
            assertEquals(index.searchRight(0.0), 1);
            assertEquals(index.searchRight(1.0), 1);
        }
        {
            DBCAN.SearchIndex index = new DBCAN.SearchIndex(Arrays.asList(0.0, 0.0, 0.0, 0.0));

            assertEquals(index.searchLeft(-1.0), 0);
            assertEquals(index.searchLeft(0.0), 0);
            assertEquals(index.searchLeft(1.0), 3);

            assertEquals(index.searchRight(-1.0), 0);
            assertEquals(index.searchRight(0.0), 3);
            assertEquals(index.searchRight(1.0), 3);
        }
        {
            DBCAN.SearchIndex index = new DBCAN.SearchIndex(Arrays.asList(0.0, 1.0));

            assertEquals(index.searchLeft(-1.0), 0);
            assertEquals(index.searchLeft(0.0), 0);
            assertEquals(index.searchLeft(0.5), 0);
            assertEquals(index.searchLeft(1.0), 1);
            assertEquals(index.searchLeft(2.0), 1);

            assertEquals(index.searchRight(-1.0), 0);
            assertEquals(index.searchRight(0.0), 0);
            assertEquals(index.searchRight(0.5), 0);
            assertEquals(index.searchRight(1.0), 1);
            assertEquals(index.searchRight(2.0), 1);
        }
        {
            DBCAN.SearchIndex index = new DBCAN.SearchIndex(Arrays.asList(0.0, 0.0, 1.0));

            assertEquals(index.searchLeft(-1.0), 0);
            assertEquals(index.searchLeft(0.0), 0);
            assertEquals(index.searchLeft(0.5), 1);
            assertEquals(index.searchLeft(1.0), 2);
            assertEquals(index.searchLeft(2.0), 2);

            assertEquals(index.searchRight(-1.0), 0);
            assertEquals(index.searchRight(0.0), 1);
            assertEquals(index.searchRight(0.5), 1);
            assertEquals(index.searchRight(1.0), 2);
            assertEquals(index.searchRight(2.0), 2);
        }
        {
            DBCAN.SearchIndex index = new DBCAN.SearchIndex(Arrays.asList(0.0, 1.0, 1.0));

            assertEquals(index.searchLeft(-1.0), 0);
            assertEquals(index.searchLeft(0.0), 0);
            assertEquals(index.searchLeft(0.5), 0);
            assertEquals(index.searchLeft(1.0), 1);
            assertEquals(index.searchLeft(2.0), 2);

            assertEquals(index.searchRight(-1.0), 0);
            assertEquals(index.searchRight(0.0), 0);
            assertEquals(index.searchRight(0.5), 0);
            assertEquals(index.searchRight(1.0), 2);
            assertEquals(index.searchRight(2.0), 2);
        }
        {
            DBCAN.SearchIndex index = new DBCAN.SearchIndex(Arrays.asList(0.0, 0.0, 1.0, 1.0));

            assertEquals(index.searchLeft(-1.0), 0);
            assertEquals(index.searchLeft(0.0), 0);
            assertEquals(index.searchLeft(0.5), 1);
            assertEquals(index.searchLeft(1.0), 2);
            assertEquals(index.searchLeft(2.0), 3);

            assertEquals(index.searchRight(-1.0), 0);
            assertEquals(index.searchRight(0.0), 1);
            assertEquals(index.searchRight(0.5), 1);
            assertEquals(index.searchRight(1.0), 3);
            assertEquals(index.searchRight(2.0), 3);
        }
        {
            DBCAN.SearchIndex index = new DBCAN.SearchIndex(Arrays.asList(0.0, 1.0, 2.0));

            assertEquals(index.searchLeft(-1.0), 0);
            assertEquals(index.searchLeft(0.0), 0);
            assertEquals(index.searchLeft(0.5), 0);
            assertEquals(index.searchLeft(1.0), 1);
            assertEquals(index.searchLeft(2.0), 2);
            assertEquals(index.searchLeft(3.0), 2);

            assertEquals(index.searchRight(-1.0), 0);
            assertEquals(index.searchRight(0.0), 0);
            assertEquals(index.searchRight(0.5), 0);
            assertEquals(index.searchRight(1.0), 1);
            assertEquals(index.searchRight(2.0), 2);
            assertEquals(index.searchRight(3.0), 2);
        }
        {
            DBCAN.SearchIndex index =
                    new DBCAN.SearchIndex(Arrays.asList(0.0, 1.0, 2.0, 3.0, 4.0, 5.0));

            assertEquals(index.searchLeft(-1.0), 0);
            assertEquals(index.searchLeft(0.0), 0);
            assertEquals(index.searchLeft(0.5), 0);
            assertEquals(index.searchLeft(2.5), 2);
            assertEquals(index.searchLeft(3.0), 3);
            assertEquals(index.searchLeft(5.0), 5);
            assertEquals(index.searchLeft(6.0), 5);

            assertEquals(index.searchRight(-1.0), 0);
            assertEquals(index.searchRight(0.0), 0);
            assertEquals(index.searchRight(0.5), 0);
            assertEquals(index.searchRight(2.5), 2);
            assertEquals(index.searchRight(3.0), 3);
            assertEquals(index.searchRight(5.0), 5);
            assertEquals(index.searchRight(6.0), 5);
        }
        {
            DBCAN.SearchIndex index = new DBCAN.SearchIndex(
                    Arrays.asList(0.0, 0.0, 1.0, 2.1, 2.1, 2.1, 3.0, 4.0, 5.0, 5.0, 5.0, 5.0));

            assertEquals(index.searchLeft(-1.0), 0);
            assertEquals(index.searchLeft(0.0), 0);
            assertEquals(index.searchLeft(0.5), 1);
            assertEquals(index.searchLeft(2.0), 2);
            assertEquals(index.searchLeft(2.1), 3);
            assertEquals(index.searchLeft(2.5), 5);
            assertEquals(index.searchLeft(4.5), 7);
            assertEquals(index.searchLeft(5.0), 8);
            assertEquals(index.searchLeft(5.5), 11);

            assertEquals(index.searchRight(-1.0), 0);
            assertEquals(index.searchRight(0.0), 1);
            assertEquals(index.searchRight(0.5), 1);
            assertEquals(index.searchRight(2.0), 2);
            assertEquals(index.searchRight(2.1), 5);
            assertEquals(index.searchRight(2.5), 5);
            assertEquals(index.searchRight(4.5), 7);
            assertEquals(index.searchRight(5.0), 11);
            assertEquals(index.searchRight(5.5), 11);
        }
    }

    @Test
    public void testRadius() {
        {
            ISearchIndex<Double> index = new DBCAN.SearchIndex(Arrays.asList(0.0));
            List<Double> result = index.radius(-1.0, 1.0);
            List<Double> interval = Arrays.asList(0.0);
            assertEquals(result.size(), interval.size());
            for (Double value : result) {
                assertTrue(interval.contains(value));
            }
        }
        {
            ISearchIndex<Double> index = new DBCAN.SearchIndex(Arrays.asList(0.0, 0.0));
            List<Double> result = index.radius(-1.0, 1.0);
            List<Double> interval = Arrays.asList(0.0, 0.0);
            assertEquals(result.size(), interval.size());
            for (Double value : result) {
                assertTrue(interval.contains(value));
            }
        }
        {
            ISearchIndex<Double> index = new DBCAN.SearchIndex(Arrays.asList(0.0, 0.0, 0.0));
            List<Double> result = index.radius(-1.0, 1.0);
            List<Double> interval = Arrays.asList(0.0, 0.0, 0.0);
            assertEquals(result.size(), interval.size());
            for (Double value : result) {
                assertTrue(interval.contains(value));
            }
        }
        {
            ISearchIndex<Double> index = new DBCAN.SearchIndex(
                    Arrays.asList(0.0, 0.0, 1.0, 2.0, 3.0, 3.0, 4.0, 5.0, 5.0, 5.0));
            {
                List<Double> result = index.radius(-1.0, 1.0);
                List<Double> interval = Arrays.asList(0.0, 0.0);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
            {
                List<Double> result = index.radius(-2.0, 1.0);
                List<Double> interval = new LinkedList<>();
                assertEquals(result.size(), interval.size());
            }
            {
                List<Double> result = index.radius(6.0, 0.5);
                List<Double> interval = new LinkedList<>();
                assertEquals(result.size(), interval.size());
            }
            {
                List<Double> result = index.radius(6.0, 3.0);
                List<Double> interval = Arrays.asList(3.0, 3.0, 4.0, 5.0, 5.0, 5.0);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
            {
                List<Double> result = index.radius(4.0, 3.0);
                List<Double> interval = Arrays.asList(1.0, 2.0, 3.0, 3.0, 4.0, 5.0, 5.0, 5.0);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
            {
                List<Double> result = index.radius(3.0, 1.5);
                List<Double> interval = Arrays.asList(2.0, 3.0, 3.0, 4.0);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
            {
                List<Double> result = index.radius(4.0, 4.0);
                List<Double> interval =
                        Arrays.asList(0.0, 0.0, 1.0, 2.0, 3.0, 3.0, 4.0, 5.0, 5.0, 5.0);
                assertEquals(result.size(), interval.size());
                for (Double value : result) {
                    assertTrue(interval.contains(value));
                }
            }
        }
    }

    @Test
    public void testCluster() {
        {
            List<Double> list = Arrays.asList(0.0, 3.0, 6.0, 6.5, 8.0, 11.0, 13.5, 15.5, 18.0, 20.5,
                    23.0, 24.0, 24.5, 25.0, 28.0);
            {
                Set<List<Double>> results = DBCAN.cluster(list, 2, 2);
                Set<List<Double>> clusters = new HashSet<>();
                clusters.add(Arrays.asList(23.0, 24.0, 24.5, 25.0));
                clusters.add(Arrays.asList(6.0, 6.5, 8.0));
                clusters.add(Arrays.asList(13.5, 15.5));

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
                Set<List<Double>> results = DBCAN.cluster(list, 3, 2);
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
            {
                Set<List<Double>> results = DBCAN.cluster(list, 1, 2);
                Set<List<Double>> clusters = new HashSet<>();
                clusters.add(Arrays.asList(23.0, 24.0, 24.5, 25.0));
                clusters.add(Arrays.asList(6.0, 6.5));

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
                Set<List<Double>> results = DBCAN.cluster(list, 0.5, 2);
                Set<List<Double>> clusters = new HashSet<>();
                clusters.add(Arrays.asList(24.0, 24.5, 25.0));
                clusters.add(Arrays.asList(6.0, 6.5));

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
                Set<List<Double>> results = DBCAN.cluster(list, 0.25, 2);
                Set<List<Double>> clusters = new HashSet<>();

                assertEquals(clusters.size(), results.size());
            }
        }
        {
            List<Double> list = Arrays.asList(0.0, 3.0, 3.0, 3.0, 6.0, 6.5, 6.5, 8.0, 11.0, 13.5,
                    13.5, 15.5, 18.0, 20.5, 23.0, 24.0, 24.5, 25.0, 28.0, 28.0, 28.0);
            {
                Set<List<Double>> results = DBCAN.cluster(list, 2, 2);
                Set<List<Double>> clusters = new HashSet<>();
                clusters.add(Arrays.asList(23.0, 24.0, 24.5, 25.0));
                clusters.add(Arrays.asList(6.0, 6.5, 6.5, 8.0));
                clusters.add(Arrays.asList(13.5, 13.5, 15.5));
                clusters.add(Arrays.asList(3.0, 3.0, 3.0));
                clusters.add(Arrays.asList(28.0, 28.0, 28.0));

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
                Set<List<Double>> results = DBCAN.cluster(list, 3, 2);
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
            {
                Set<List<Double>> results = DBCAN.cluster(list, 1, 2);
                Set<List<Double>> clusters = new HashSet<>();
                clusters.add(Arrays.asList(23.0, 24.0, 24.5, 25.0));
                clusters.add(Arrays.asList(6.0, 6.5, 6.5));
                clusters.add(Arrays.asList(3.0, 3.0, 3.0));
                clusters.add(Arrays.asList(13.5, 13.5));
                clusters.add(Arrays.asList(28.0, 28.0, 28.0));

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
                Set<List<Double>> results = DBCAN.cluster(list, 0.5, 2);
                Set<List<Double>> clusters = new HashSet<>();
                clusters.add(Arrays.asList(24.0, 24.5, 25.0));
                clusters.add(Arrays.asList(6.0, 6.5, 6.5));
                clusters.add(Arrays.asList(3.0, 3.0, 3.0));
                clusters.add(Arrays.asList(28.0, 28.0, 28.0));
                clusters.add(Arrays.asList(13.5, 13.5));

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
                Set<List<Double>> results = DBCAN.cluster(list, 0.25, 2);
                Set<List<Double>> clusters = new HashSet<>();
                clusters.add(Arrays.asList(6.5, 6.5));
                clusters.add(Arrays.asList(3.0, 3.0, 3.0));
                clusters.add(Arrays.asList(28.0, 28.0, 28.0));
                clusters.add(Arrays.asList(13.5, 13.5));

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
}
