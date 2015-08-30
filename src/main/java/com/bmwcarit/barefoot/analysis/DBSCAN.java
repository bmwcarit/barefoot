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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.bmwcarit.barefoot.analysis.DBCAN.ISearchIndex;
import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.spatial.SpatialOperator;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.QuadTree;
import com.esri.core.geometry.QuadTree.QuadTreeIterator;

/**
 * Algorithm for density-based spatial cluster analysis - DBSCAN (density-based spatial cluster
 * analysis of applications with noise).
 */
public class DBSCAN {

    /**
     * Search index for efficient access to point data.
     */
    protected static class SearchIndex implements ISearchIndex<Point> {
        private final static SpatialOperator spatial = new Geography();
        private final static int height = 16;
        private final QuadTree index = new QuadTree(defaultRegion(), height);
        private final Map<Integer, List<Point>> points = new HashMap<Integer, List<Point>>();

        private static Envelope2D defaultRegion() {
            Envelope2D region = new Envelope2D();
            region.setCoords(-180, -90, 180, 90);
            return region;
        }

        /**
         * Constructs search index with a list of points.
         *
         * @param elements List of point data.
         */
        public SearchIndex(List<Point> elements) {
            for (Point element : elements) {
                put(element);
            }
        }

        @Override
        public List<Point> radius(Point center, double radius) {
            List<Point> neighbors = new LinkedList<Point>();
            Envelope2D query = spatial.envelope(center, radius);
            QuadTreeIterator iterator = index.getIterator(query, 0);
            int handle = -1;
            while ((handle = iterator.next()) != -1) {
                int id = index.getElement(handle);
                List<Point> bucket = points.get(id);
                assert (bucket != null);
                Point point = bucket.get(0);
                double distance = spatial.distance(point, center);
                if (distance < radius) {
                    neighbors.add(point);
                }
            }
            return neighbors;
        }

        /**
         * Add point to search index.
         *
         * @param point Point to be added.
         * @return Returns true if point has been added, false if search index already contains a
         *         point with the same hash value.
         */
        public boolean put(Point point) {
            int hash = Arrays.hashCode(new Object[] {point.getX(), point.getY()});

            if (points.containsKey(hash)) {
                points.get(hash).add(point);
            } else {
                Envelope2D env = new Envelope2D();
                point.queryEnvelope2D(env);
                index.insert(hash, env);
                points.put(hash, new LinkedList<Point>(Arrays.asList(point)));
            }

            return true;
        }

        @Override
        public Iterator<Point> iterator() {
            return new Iterator<Point>() {
                private final Iterator<List<Point>> bucketit = points.values().iterator();
                private Iterator<Point> it = null;

                @Override
                public boolean hasNext() {
                    while ((it == null || !it.hasNext()) && bucketit.hasNext()) {
                        it = bucketit.next().iterator();
                    }

                    if (it == null || !it.hasNext()) {
                        return false;
                    } else {
                        return true;
                    }
                }

                @Override
                public Point next() {
                    if (this.hasNext()) {
                        return it.next();
                    } else {
                        throw new NoSuchElementException();
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    /**
     * Gets clusters of points by density properties defined as epsilon and minimum.
     *
     * @param elements List of point data.
     * @param epsilon Defines epsilon range (radius) to analyze density.
     * @param minimum Minimum number of points within epsilon range.
     * @return Set of clusters (lists) of elements.
     */
    public static Set<List<Point>> cluster(List<Point> elements, double epsilon, int minimum) {
        ISearchIndex<Point> index = new SearchIndex(elements);
        return DBCAN.cluster(index, epsilon, minimum);
    }
}
