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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.bmwcarit.barefoot.analysis.DBCAN.ISearchIndex;
import com.bmwcarit.barefoot.util.Tuple;

/**
 * Algorithm for density-based cluster analysis of numeric data in residue classes (modulo) - DBRCAN
 * (density-based cluster analysis in residue classes of applications with noise). This enables
 * cluster analysis of temporal data where a time value is an integer (e.g. unix epoch time)
 * <i>t</i> &#8712; N and is modulo equivalent to some time <i>&tau;</i>, i.e. <i>t</i> &#8801;
 * <i>&tau;</i> (modulo <i>m</i>), within a specified time frame <i> 0 &le; &tau; &lt; m </i>. To
 * give an example, analysis of day time requires <i>m = 86400 (24h &#215; 60min &#215; 60s)</i>
 * such that unix epoch time of <i>1PM</i> of any day is mapped to the same day time <i>3600s</i>
 * unix epoch time. The main feature of DBRCAN is that it finds clusters that span values across
 * zero border, e.g. it finds clusters across midnight as with daytimes <i>23:57</i> and
 * <i>00:03</i>.
 * <p>
 * <i>Note:</i> This class distinguishes two different <i>"epsilon"</i> parameters, i.e. maximum
 * rounding error of floating point values and a range parameter in density based cluster analysis,
 * which are used because of their respective reference in standard literature. For convenience, we
 * denote <i>&epsilon;</i> as the maximum rounding error of floating point values, and
 * <i>epsilon</i> as the range parameter in density-based cluster analysis.
 */
public class DBRCAN {
    /**
     * Floating point epsilon (&epsilon;) as upper bound for rounding errors of double values.
     */
    public final static double epsilon = 1E-10;

    /**
     * Returns result of modulo operation with <i>dividend</i> and <i>divisor</i>.
     * <p>
     * <i>Note:</i> The modulo (<i>mod</i>) operation is defined for positive and negative integer
     * and floating point values:
     * <ul>
     * <li>5 <i>mod</i> 3 = 2</li>
     * <li>5 <i>mod</i> -3 = 2</li>
     * <li>1.3 <i>mod</i> 0.5 = 0.3</li>
     * <li>1.3 <i>mod</i> -0.5 = 0.3</li>
     * <li>-5 <i>mod</i> 3 = 1</li>
     * <li>-5 <i>mod</i> -3 = 1</li>
     * <li>-1.3 <i>mod</i> 0.5 = 0.2</li>
     * <li>-1.3 <i>mod</i> -0.5 = 0.2</li>
     * </ul>
     *
     * @param dividend Divident of modulo operation.
     * @param divisor Divisor of modulo operation.
     * @return Modulo result.
     */
    protected static double modulo(double dividend, double divisor) {
        double result = dividend % divisor;
        return (result < 0) ? result + Math.abs(divisor) : result;
    }

    /**
     * Gets epsilon (&epsilon;) conform rounded value.
     *
     * @param value Value to be rounded.
     * @return Epsilon conform rounded value.
     */
    protected static double epsilonRound(double value) {
        long precision = (long) Math.pow(10, Math.abs(Math.log10(epsilon)));
        assert (precision < Double.MAX_VALUE);

        return Math.floor(value)
                + (double) Math.round((value - Math.floor(value)) * precision) / precision;
    }

    /**
     * Epsilon (&epsilon;) conform comparison of floating point values.
     *
     * @param left Left value of comparison.
     * @param right Right value of comparison.
     * @return Positive value if left is smaller, negative if left is bigger, zero otherwise.
     */
    protected static int epsilonCompare(double left, double right) {
        if (Math.abs(left - right) < epsilon) {
            return 0;
        } else if (left - right >= epsilon) {
            return 1;
        } else {
            return -1;
        }
    }

    /**
     * Checks if a value is between left and right border of residue class interval.
     *
     * @param left Left border of residue class interval.
     * @param right Right border of residue class interval.
     * @param value Value to be checked.
     * @param modulo Module value that defines residue class.
     * @return True if value is in the interval, false otherwise.
     */
    protected static boolean moduloBetween(double left, double right, double value, double modulo) {
        double mleft = modulo(left, modulo);
        double mright = modulo(right, modulo);
        double mvalue = modulo(value, modulo);

        if (epsilonCompare(mleft, mright) == 0) {
            if (epsilonCompare(mleft, mvalue) == 0) {
                return true;
            } else {
                return false;
            }
        } else if (epsilonCompare(mleft, mright) < 0) {
            if ((epsilonCompare(mvalue, mleft) == 0 || epsilonCompare(mvalue, mleft) > 0)
                    && epsilonCompare(mvalue, mright) < 0) {
                return true;
            } else {
                return false;
            }
        } else {
            if (epsilonCompare(mvalue, mleft) == 0 || epsilonCompare(mvalue, mleft) > 0
                    || epsilonCompare(mvalue, mright) < 0) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Search index of values defined as residue class.
     */
    protected static class SearchIndex implements ISearchIndex<Double> {
        private final List<List<Double>> sequence = new ArrayList<>();
        private final double modulo;

        /**
         * Constructs search index with values of residue class defined with some modulo value.
         *
         * @param modulo Modulo value that defines residue class.
         * @param elements Elements to be added to the search index.
         */
        public SearchIndex(final double modulo, List<Double> elements) {
            this.modulo = modulo;

            SortedMap<Double, List<Double>> map = new TreeMap<>();
            for (Double element : elements) {
                double key = epsilonRound(modulo(element, modulo));
                if (!map.containsKey(key)) {
                    map.put(key, new LinkedList<Double>());
                }
                map.get(key).add(element);
            }

            sequence.addAll(map.values());

            for (int i = 1; i < sequence.size(); ++i) {
                assert (epsilonCompare(modulo(sequence.get(i - 1).get(0), modulo),
                        modulo(sequence.get(i).get(0), modulo)) < 0);
            }
        }

        /**
         * Gets subset of list of elements that is within the interval <i>[center - radius, center +
         * radius]</i>, where center value of radius search must be <i>0 &le; center &le; m / 2</i>.
         *
         * @param center Center of the radius (interval) search.
         * @param radius Radius (half interval) of the search.
         * @return List of elements that is within the given radius (interval).
         */
        @Override
        public List<Double> radius(Double center, double radius) {
            List<Double> neighbors = new LinkedList<>();
            int n = sequence.size();
            double min = center - Math.min(radius, modulo / 2);
            double max = center + Math.min(radius, modulo / 2 - epsilon);

            if (n == 0) {
                return neighbors;
            }

            int left = search(min);
            int right = search(max);

            double first = sequence.get(left).get(0);
            if (moduloBetween(min, max, first, modulo)) {
                neighbors.addAll(sequence.get(left));
            }

            if (epsilonCompare(min, max) == 0) {
                return neighbors;
            }

            if (left != right) {
                for (int i = (left + 1) % n; i != right; i = (i + 1) % n) {
                    neighbors.addAll(sequence.get(i));
                }
                neighbors.addAll(sequence.get(right));
            }

            return neighbors;
        }

        @Override
        public Iterator<Double> iterator() {
            return new Iterator<Double>() {
                private final Iterator<List<Double>> seqit = sequence.iterator();
                private Iterator<Double> it = null;

                @Override
                public boolean hasNext() {
                    while ((it == null || !it.hasNext()) && seqit.hasNext()) {
                        it = seqit.next().iterator();
                    }

                    if (it == null || !it.hasNext()) {
                        return false;
                    } else {
                        return true;
                    }
                }

                @Override
                public Double next() {
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

        /**
         * Gets index of the element, in the ordered list of values, that is greater or equal than
         * the given search value value.
         *
         * @param value Value to search for in the list of values.
         * @return Index of the element that is greater or equal than the given value.
         */
        protected int search(double value) {
            double modvalue = modulo(value, modulo);
            int n = sequence.size();
            int left = 0, right = n - 1, index = 0;
            while (left != right) {
                index = (right - left) / 2 + left;
                double element =
                        modulo(sequence.get(index == left ? index + 1 : index).get(0), modulo);
                if (epsilonCompare(modvalue, element) == 0
                        || epsilonCompare(modvalue, element) > 0) {
                    left = index == left ? index + 1 : index;
                } else {
                    right = index;
                }
            }

            if (left == 0
                    && epsilonCompare(modvalue, modulo(sequence.get(left).get(0), modulo)) < 0) {
                left = n - 1;
            }

            return left;
        }
    }

    /**
     * Gets clusters of elements by density properties defined as <i>epsilon</i> and minimum. It
     * considers residue class as ring of values where value <i>0</i> is right next to the value
     * <i>m - &epsilon;</i>.
     *
     * @param elements List of floating-point values.
     * @param modulo Modulo value that defines the residue class.
     * @param epsilon Defines <i>epsilon</i> range (radius) to analyze density.
     * @param minimum Minimum number of elements within epsilon range.
     * @return Set of clusters (lists) of elements.
     */
    public static Set<List<Double>> cluster(List<Double> elements, double modulo, double epsilon,
            int minimum) {
        ISearchIndex<Double> index = new SearchIndex(modulo, elements);
        return DBCAN.cluster(index, epsilon, minimum);
    }

    private static double distance(double left, double right, double modulo) {
        return (right >= left) ? right - left : modulo - left + right;
    }

    /**
     * Gets left and right border (bounds) of a density cluster in a residue class of values. It
     * considers residue class of values where value <i>0</i> is right next to the value <i>m -
     * &epsilon;</i>.
     * <p>
     * <i>Note:</i> A density cluster given as a list of values in a residue class is ambiguous in
     * what is the internal of the cluster, i.e., minimum and maximum are not the respective left
     * and right borders of the cluster. However, given the epsilon density parameter of the cluster
     * it is unambiguous and can be determined with this function.
     *
     * @param cluster List of floating-point values.
     * @param modulo Modulo value that defines the residue class.
     * @param epsilon Defines <i>epsilon</i> range (radius) to analyze density.
     * @param buffer Optional buffer to be added to interval borders, zero should be used as
     *        default.
     * @return Left and right border (bounds) of density cluster.
     */
    public static Tuple<Double, Double> bounds(List<Double> cluster, double modulo, double epsilon,
            double buffer) {
        Set<Double> set = new HashSet<>();
        for (Double element : cluster) {
            set.add(modulo(element, modulo));
        }

        if (set.size() == 1) {
            Double element = set.iterator().next();
            return new Tuple<>(epsilonRound(modulo(element - buffer, modulo)),
                    epsilonRound(modulo(element + buffer, modulo)));
        }

        List<Double> sequence = new LinkedList<>(set);
        Collections.sort(sequence);
        Double left = null, right = null, maximum = null;

        for (int i = 0; i < sequence.size(); ++i) {
            double element = sequence.get(i);
            double previous = sequence.get((i == 0) ? sequence.size() - 1 : i - 1);
            double distance = distance(previous, element, modulo);

            if ((maximum == null || epsilonCompare(maximum, distance) < 0)
                    && epsilonCompare(distance, epsilon) > 0) {
                maximum = distance;
                left = previous;
                right = element;
            }
        }

        if (left != null && right != null) {
            return new Tuple<>(epsilonRound(modulo(right - buffer, modulo)),
                    epsilonRound(modulo(left + buffer, modulo)));
        }

        return null;
    }

    /**
     * Gets hierarchy of element clusters by density properties defined as <i>epsilon</i> and
     * minimum. Minimum is scaled logarithmic giving a logarithmic scaled hierarchy of density
     * clusters. It considers residue class of values where value <i>0</i> is right next to the
     * value <i>m - &epsilon;</i>.
     *
     * @param elements List of floating-point values.
     * @param modulo Modulo value that defines the residue class.
     * @param epsilon Defines <i>epsilon</i> range (radius) to analyze density.
     * @return Set of clusters (lists) of elements with density parameter (minimum).
     */
    private static Set<Tuple<List<Double>, Integer>> cluster(List<Double> elements, double modulo,
            double epsilon) {
        Set<Tuple<List<Double>, Integer>> clusters = new HashSet<>();
        int size = 1, minimum = 1;
        while (size > 0) {
            Set<List<Double>> results = DBRCAN.cluster(elements, modulo, epsilon, minimum);
            for (List<Double> result : results) {
                clusters.add(new Tuple<>(result, minimum));
            }
            size = results.size();
            minimum *= 2;
        }
        return clusters;
    }

    /**
     * Gets hierarchy of element clusters as by density properties defined as <i>epsilon</i> and
     * minimum as a density function. Minimum is scaled logarithmic giving a logarithmic scaled
     * hierarchy of density clusters. It considers residue class of values where value <i>0</i> is
     * right next to the value <i>m - &epsilon;</i>.
     *
     * @param elements List of floating-point values.
     * @param modulo Modulo value that defines the residue class.
     * @param epsilon Defines <i>epsilon</i> range (radius) to analyze density.
     * @param buffer Optional buffer to be added to interval borders, zero should be used as
     *        default.
     * @return Logarithmic scaled density function as step function with <i>x</i> value (intervals
     *         of the residue class) and <i>y</i> values (minimum as density parameter).
     */
    public static List<Tuple<Double, Integer>> function(List<Double> elements, double modulo,
            double epsilon, double buffer) {
        assert (DBRCAN.epsilonCompare(epsilon, 0.0) > 0);
        assert (DBRCAN.epsilonCompare(buffer, epsilon / 2) <= 0);

        Set<Tuple<List<Double>, Integer>> clusters = cluster(elements, modulo, epsilon);
        LinkedList<Tuple<Double, Integer>> function = new LinkedList<>();

        /*
         * The following only works because of the following constraints: (1) Given two clusters A
         * and B where A has higher density than B and A's left bound is between B's left and right
         * bound (considering algebraic ring structure). It follows that A's right bound is smaller
         * or equal to B's right bound. In simple terms, a cluster of higher density is either
         * outside or inside a cluster of lower density, but never partially inside. (2) The density
         * function drops at each cluster's right bound at least by half of its density, because of
         * logarithmic scaled clustering. (3) At a cluster's exact right bound there is no left
         * bound of a subsequent cluster, given an epsilon of greater than zero.
         */

        Map<Double, Integer> starts = new HashMap<>();
        Map<Double, Integer> ends = new HashMap<>();
        int minimum = 0;

        for (Tuple<List<Double>, Integer> cluster : clusters) {
            Tuple<Double, Double> bounds = DBRCAN.bounds(cluster.one(), modulo, epsilon, buffer);

            if (bounds == null) {
                minimum = cluster.two();
            } else {
                double start = bounds.one(), end = bounds.two();
                if (!starts.containsKey(start)) {
                    starts.put(start, cluster.two());
                } else {
                    starts.put(start, Math.max(starts.get(start), cluster.two()));
                }
                if (!ends.containsKey(end)) {
                    ends.put(end, cluster.two() / 2);
                } else {
                    ends.put(end, Math.min(ends.get(end), cluster.two() / 2));
                }
            }
        }

        for (Entry<Double, Integer> entry : starts.entrySet()) {
            function.add(new Tuple<>(entry.getKey(), Math.max(minimum, entry.getValue())));
        }

        for (Entry<Double, Integer> entry : ends.entrySet()) {
            if (buffer == 0 || !starts.containsKey(entry.getKey())) {
                function.add(new Tuple<>(entry.getKey(), Math.max(minimum, entry.getValue())));
            }
        }

        Collections.sort(function, new Comparator<Tuple<Double, Integer>>() {
            @Override
            public int compare(Tuple<Double, Integer> left, Tuple<Double, Integer> right) {
                return (DBRCAN.epsilonCompare(left.one(), right.one()) < 0) ? -1
                        : (DBRCAN.epsilonCompare(left.one(), right.one()) > 0) ? 1
                                : (left.two() < right.two()) ? 1 : -1;
            }
        });

        if (function.size() == 0) {
            function.push(new Tuple<>(0.0, minimum));
        } else if (DBRCAN.epsilonCompare(function.get(0).one(), 0.0) != 0) {
            function.push(new Tuple<>(0.0, Math.max(minimum, function.getLast().two())));
        }

        return function;
    }
}
