/*
 * Copyright 2015 BMW Car IT GmbH <sebastian.mattheis@bmw-carit.de>
 */
package com.bmwcarit.barefoot.matcher;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.bmwcarit.barefoot.roadmap.Road;
import com.bmwcarit.barefoot.util.Triple;
import com.bmwcarit.barefoot.util.Tuple;

/**
 * Collection of benchmark helper functions.
 */
public abstract class Benchmark {

    /**
     * Gets optimal alignment of two paths, e.g. map matching results, which are given as sequences
     * of edges with source id, target id and length of an edge.
     *
     * @param left One sequence of edges with source id, target id, and length in meters.
     * @param right Other sequence of edges with source id, target id, and length in meters.
     * @return Optimal alignment of two sequences of edges, which is a sequence of tuples referring
     *         to matching edges of the input sequences.
     */
    public static List<Tuple<Integer, Integer>> align(List<Triple<Long, Long, Double>> left,
            List<Triple<Long, Long, Double>> right) {
        double maxe = 0;

        for (Triple<Long, Long, Double> element : left) {
            maxe += element.three();
        }

        for (Triple<Long, Long, Double> element : right) {
            maxe += element.three();
        }

        int X = left.size(), Y = right.size();
        double[][] matrixe = new double[X][Y];
        int[][][] matrixp = new int[X][Y][3];

        double mine = maxe;
        int minx = 0, miny = 0;

        for (int y = 0; y < Y; ++y) {
            for (int x = 0; x < X; ++x) {
                matrixe[x][y] = maxe;
                matrixp[x][y][0] = -1;
                matrixp[x][y][1] = -1;

                for (int x_ = Math.max(0, x - 1); x_ <= x; ++x_) {
                    for (int y_ = Math.max(0, y - 1); y_ <= y; ++y_) {
                        if (y_ == y && x_ == x) {
                            continue;
                        }

                        if (matrixe[x_][y_] <= matrixe[x][y]) {
                            matrixe[x][y] = matrixe[x_][y_];
                            matrixp[x][y][0] = x_;
                            matrixp[x][y][1] = y_;
                        }
                    }
                }

                if (left.get(x).one().compareTo(right.get(y).one()) == 0
                        && left.get(x).two().compareTo(right.get(y).two()) == 0) {
                    matrixe[x][y] = matrixe[x][y] - (left.get(x).three() + right.get(y).three());
                    matrixp[x][y][2] = 1;
                }

                if (matrixe[x][y] <= mine) {
                    mine = matrixe[x][y];
                    minx = x;
                    miny = y;
                }
            }
        }

        LinkedList<Tuple<Integer, Integer>> matches = new LinkedList<Tuple<Integer, Integer>>();
        int x = minx, y = miny;
        do {
            if (matrixp[x][y][2] == 1) {
                matches.add(new Tuple<Integer, Integer>(x, y));
            }
            int x_ = matrixp[x][y][0];
            int y_ = matrixp[x][y][1];
            x = x_;
            y = y_;
        } while (x >= 0 && y >= 0);

        return matches;
    }

    /**
     * Converts map matching results, which is sequence of matcher candidates, to a sequence of
     * edges given as source id, target id and length in meters.
     *
     * @param candidates Map matching results, which is sequence of matcher candidates.
     * @return Sequence of edges defined by source id, target id and length in meters.
     */
    public static List<Triple<Long, Long, Double>> candidatesToSequence(
            List<MatcherCandidate> candidates) {
        LinkedList<Triple<Long, Long, Double>> sequence =
                new LinkedList<Triple<Long, Long, Double>>();

        for (MatcherCandidate candidate : candidates) {
            if (candidate.transition() == null) {
                continue;
            }

            for (Road segment : candidate.transition().route().path()) {
                if (sequence.size() > 0 && sequence.peekLast().one() == segment.source()
                        && sequence.peekLast().two() == segment.target()) {
                    continue;
                }
                sequence.add(new Triple<Long, Long, Double>(segment.source(), segment.target(),
                        (double) segment.length()));
            }
        }

        return sequence;
    }

    /**
     * Calculates error of path alignment of two sequences of edges defined by source id, target id
     * and length in meters.
     *
     * @param left One sequence of edges with source id, target id, and length in meters.
     * @param right Other sequence of edges with source id, target id, and length in meters.
     * @param alginment Alignment of two sequences of edges, which is a sequence of tuples referring
     *        to matching edges of the input sequences.
     * @return Error value that sums length of all non matched edges of input paths.
     */
    public static double error(List<Triple<Long, Long, Double>> left,
            List<Triple<Long, Long, Double>> right, List<Tuple<Integer, Integer>> alginment) {
        double error = 0, length = 0;

        for (Triple<Long, Long, Double> element : left) {
            error += element.three();
            length += element.three();
        }

        for (Triple<Long, Long, Double> element : right) {
            error += element.three();
        }

        for (Tuple<Integer, Integer> match : Benchmark.align(left, right)) {
            error -= left.get(match.one()).three();
            error -= right.get(match.two()).three();
        }

        return error / length;
    }

    /**
     * Sub-samples a sequence of {@link MatcherSample} objects.
     *
     * @param samples Sequence of {@link MatcherSample} objects.
     * @param interval Interval to be sub sampled in seconds.
     * @param offset Offset of sub sampled sequence.
     * @return Sub-sampled sequence of {@link MatcherSample} objects.
     */
    public static List<MatcherSample> subsample(List<MatcherSample> samples, int interval,
            int offset) {
        List<MatcherSample> subsamples = new ArrayList<MatcherSample>();

        subsamples.add(samples.get(0));
        long start = samples.get(0).time() / 1000, time;
        for (int i = 1; i < samples.size() - 1; ++i) {
            time = samples.get(i).time() / 1000;
            if ((time - start) % interval == 0) {
                subsamples.add(samples.get(i));
            }
        }
        subsamples.add(samples.get(samples.size() - 1));
        return subsamples;
    }
}
