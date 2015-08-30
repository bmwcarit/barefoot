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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A general-purpose algorithm for density-based cluster analysis - DBCAN (density-based cluster
 * analysis of applications with noise).
 */
public class DBCAN {
    /**
     * Interface of search index for efficient access to data elements.
     *
     * @param <T> Type of data elements.
     */
    protected interface ISearchIndex<T> extends Iterable<T> {
        List<T> radius(T center, double radius);
    }

    /**
     * Gets clusters of elements by density properties defined as epsilon and minimum.
     *
     * @param index Search index of data elements.
     * @param epsilon Defines epsilon range (radius) to analyze density.
     * @param minimum Minimum number of elements within epsilon range.
     *
     * @param <T> Type of data elements.
     * @return Set of clusters (lists) of elements.
     */
    protected static <T> Set<List<T>> cluster(ISearchIndex<T> index, double epsilon, int minimum) {
        Set<List<T>> clusters = new HashSet<List<T>>();
        Set<T> visited = new HashSet<T>();
        Set<T> noise = new HashSet<T>();

        for (T element : index) {
            if (visited.contains(element)) {
                continue;
            }
            visited.add(element);

            List<T> neighbors = index.radius(element, epsilon);

            if (neighbors.size() < minimum) {
                noise.add(element);
            } else {
                List<T> cluster = new LinkedList<T>();

                LinkedList<T> seeds = new LinkedList<T>();
                for (T neighbor : neighbors) {
                    seeds.push(neighbor);
                    visited.add(neighbor);
                    cluster.add(neighbor);
                    noise.remove(neighbor);
                }

                while (!seeds.isEmpty()) {
                    T seed = seeds.pollLast();
                    List<T> exteriors = index.radius(seed, epsilon);

                    if (exteriors.size() >= minimum) {
                        Set<T> subvisited = new HashSet<T>();
                        for (T exterior : exteriors) {
                            if (!visited.contains(exterior)) {
                                seeds.push(exterior);
                                subvisited.add(exterior);
                                cluster.add(exterior);
                            }
                            if (noise.contains(exterior)) {
                                noise.remove(exterior);
                                cluster.add(exterior);
                            }
                        }
                        visited.addAll(subvisited);
                    }
                }

                clusters.add(cluster);
            }
        }

        return clusters;
    }

    /**
     * Search index for efficient access to a list of floating point values.
     */
    protected static class SearchIndex implements ISearchIndex<Double> {
        protected final List<Double> sequence;

        /**
         * Constructs search index with a list of floating point values.
         *
         * @param elements List of floating point values.
         */
        public SearchIndex(List<Double> elements) {
            sequence = new ArrayList<Double>(elements);
            Collections.sort(sequence);
        }

        @Override
        public List<Double> radius(Double center, double radius) {
            List<Double> interval = new LinkedList<Double>();
            int n = sequence.size();
            double min = center - radius;
            double max = center + radius;

            if (n == 0 || max < sequence.get(0) || min > sequence.get(n - 1)) {
                return interval;
            }

            int left = searchLeft(min);
            int right = searchRight(max);

            if (sequence.get(left) >= min) {
                interval.add(sequence.get(left));
            }

            for (int i = left + 1; i <= right; ++i) {
                interval.add(sequence.get(i));
            }

            return interval;
        }

        /**
         * Gets index of the element, in the ordered list of floating point values, that is greater
         * or equal than the given floating point value.
         *
         * @param value Value to search for in the list of floating point values.
         * @return Index of the element that is greater or equal than the given floating point
         *         value.
         */
        protected int search(double value) {
            int n = sequence.size();
            int left = 0, right = n - 1, index = 0;
            while (left != right) {
                index = (right - left) / 2 + left;
                if (value >= sequence.get(index == left ? index + 1 : index)) {
                    left = index == left ? index + 1 : index;
                } else {
                    right = index;
                }
            }
            while (left > 0 && value == sequence.get(left - 1)) {
                left -= 1;
            }
            return left;
        }

        /**
         * Gets index of the left-most element, in the ordered list of floating point values, that
         * is greater or equal than the given floating point value.
         *
         * @param value Value to search for in the list of floating point values.
         * @return Index of the left-most element that is greater or equal than the given floating
         *         point value.
         */
        protected int searchLeft(double value) {
            int i = search(value);
            while (i > 0 && value == sequence.get(i - 1)) {
                i -= 1;
            }
            return i;
        }

        /**
         * Gets index of the right-most element, in the ordered list of floating point values, that
         * is greater or equal than the given floating point value.
         *
         * @param value Value to search for in the list of floating point values.
         * @return Index of the right-most element that is greater or equal than the given floating
         *         point value.
         */
        protected int searchRight(double value) {
            int i = search(value);
            while (i < sequence.size() - 1 && value == sequence.get(i + 1)) {
                i += 1;
            }
            return i;
        }

        @Override
        public Iterator<Double> iterator() {
            return sequence.iterator();
        }
    }

    /**
     * Gets clusters of elements by density properties defined as epsilon and minimum.
     *
     * @param elements List of floating-point values.
     * @param epsilon Defines epsilon range (radius) to analyze density.
     * @param minimum Minimum number of elements within epsilon range.
     * @return Set of clusters (lists) of elements.
     */
    public static Set<List<Double>> cluster(List<Double> elements, double epsilon, int minimum) {
        ISearchIndex<Double> index = new SearchIndex(elements);
        return cluster(index, epsilon, minimum);
    }
}
