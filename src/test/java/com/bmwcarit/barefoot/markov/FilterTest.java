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

package com.bmwcarit.barefoot.markov;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.bmwcarit.barefoot.util.Tuple;

public class FilterTest {

    private class MockElement extends StateCandidate<MockElement, StateTransition, Sample> {
        public MockElement(Integer id) {
            super(id.toString());
        }

        public MockElement(Integer id, double filtprob, double seqprob) {
            super(id.toString());
            this.filtprob(filtprob);
            this.seqprob(seqprob);
        }

        public int numid() {
            return Integer.parseInt(id());
        }
    }

    private class MockStates {
        private final double[][] matrix;
        private final double[] seqprob;
        private final double[] filtprob;
        private final int[] pred;

        public MockStates(double[][] matrix) {
            this.matrix = matrix;
            this.seqprob = new double[numCandidates()];
            this.filtprob = new double[numCandidates()];
            this.pred = new int[numCandidates()];

            Arrays.fill(seqprob, Double.NEGATIVE_INFINITY);
            Arrays.fill(filtprob, 0);

            calculate();
        }

        private void calculate() {
            double normsum = 0;
            for (int c = 0; c < numCandidates(); ++c) {
                boolean transition = false;
                for (int p = 0; p < numPredecessors(); ++p) {
                    Tuple<Double, Double> pred = predecessor(p);
                    if (transition(p, c) == 0) {
                        continue;
                    }
                    transition = true;
                    this.filtprob[c] += pred.one() * transition(p, c);
                    double seqprob =
                            pred.two() + Math.log10(transition(p, c)) + Math.log10(emission(c));

                    if (seqprob > this.seqprob[c]) {
                        this.pred[c] = p;
                        this.seqprob[c] = seqprob;
                    }
                }

                if (transition == false) {
                    this.filtprob[c] = emission(c);
                    this.seqprob[c] = Math.log10(emission(c));
                    this.pred[c] = -1;
                } else {
                    this.filtprob[c] *= emission(c);
                }

                normsum += this.filtprob[c];
            }
            for (int c = 0; c < numCandidates(); ++c) {
                this.filtprob[c] /= normsum;
            }
        }

        public int numCandidates() {
            return matrix[0].length - 2;
        }

        public int numPredecessors() {
            return matrix.length - 1;
        }

        public double emission(int candidate) {
            return matrix[0][candidate + 2];
        }

        public double transition(int predecessor, int candidate) {
            return matrix[predecessor + 1][candidate + 2];
        }

        public Tuple<Double, Double> predecessor(int predecessor) {
            return new Tuple<>(matrix[predecessor + 1][0], Math.log10(matrix[predecessor + 1][1]));
        }

        public double seqprob(int candidate) {
            return seqprob[candidate];
        }

        public double filtprob(int candidate) {
            return filtprob[candidate];
        }

        public int pred(int candidate) {
            return pred[candidate];
        }
    };

    private class MockFilter extends Filter<MockElement, StateTransition, Sample> {
        private final MockStates states;

        public MockFilter(MockStates states) {
            this.states = states;
        }

        @Override
        protected Set<Tuple<MockElement, Double>> candidates(Set<MockElement> predecessors,
                Sample sample) {
            Set<Tuple<MockElement, Double>> candidates = new HashSet<>();
            for (int c = 0; c < states.numCandidates(); ++c) {
                candidates.add(new Tuple<>(new MockElement(c), states.emission(c)));
            }
            return candidates;
        }


        @Override
        protected Tuple<StateTransition, Double> transition(Tuple<Sample, MockElement> predecessor,
                Tuple<Sample, MockElement> candidate) {
            return new Tuple<>(new StateTransition(),
                    states.transition(predecessor.two().numid(), candidate.two().numid()));
        }

        public Set<MockElement> execute() {
            Set<MockElement> predecessors = new HashSet<>();
            for (int p = 0; p < states.numPredecessors(); ++p) {
                Tuple<Double, Double> pred = states.predecessor(p);
                predecessors.add(new MockElement(p, pred.one(), pred.two()));
            }
            return execute(predecessors, new Sample(0), new Sample(1));
        }
    }

    @Test
    public void FilterTestInitial() {
        MockStates states = new MockStates(new double[][] {{0, 0, 0.6, 1.0, 0.4}});
        MockFilter filter = new MockFilter(states);

        Set<MockElement> result = filter.execute();

        assertEquals(states.numCandidates(), result.size());

        for (MockElement element : result) {
            assertEquals(states.filtprob(element.numid()), element.filtprob(), 10E-6);
            assertEquals(states.seqprob(element.numid()), element.seqprob(), 10E-6);
            if (states.pred(element.numid()) == -1) {
                assertEquals(null, element.predecessor());
                assertEquals(null, element.transition());
            } else {
                assertEquals(states.pred(element.numid()), element.predecessor().numid());
                assertNotEquals(null, element.transition());
            }
        }
    }

    @Test
    public void FilterTestSubsequent() {
        MockStates states = new MockStates(new double[][] {{0, 0, 0.6, 1.0, 0.4},
                {0.2, 0.3, 0.01, 0.02, 0.3}, {0.3, 0.4, 0.2, 0.05, 0.02}});
        MockFilter filter = new MockFilter(states);

        Set<MockElement> result = filter.execute();

        assertEquals(states.numCandidates(), result.size());

        for (MockElement element : result) {
            assertEquals(states.filtprob(element.numid()), element.filtprob(), 10E-6);
            assertEquals(states.seqprob(element.numid()), element.seqprob(), 10E-6);
            if (states.pred(element.numid()) == -1) {
                assertEquals(null, element.predecessor());
                assertEquals(null, element.transition());
            } else {
                assertEquals(states.pred(element.numid()), element.predecessor().numid());
                assertNotEquals(null, element.transition());
            }
        }
    }

    @Test
    public void FilterTestBreakTransition() {
        MockStates states = new MockStates(
                new double[][] {{0, 0, 0.6, 1.0, 0.4}, {0.2, 0.3, 0, 0, 0}, {0.3, 0.4, 0, 0, 0}});
        MockFilter filter = new MockFilter(states);

        Set<MockElement> result = filter.execute();

        assertEquals(states.numCandidates(), result.size());

        for (MockElement element : result) {
            assertEquals(states.filtprob(element.numid()), element.filtprob(), 10E-6);
            assertEquals(states.seqprob(element.numid()), element.seqprob(), 10E-6);
            if (states.pred(element.numid()) == -1) {
                assertEquals(null, element.predecessor());
                assertEquals(null, element.transition());
            } else {
                assertEquals(states.pred(element.numid()), element.predecessor().numid());
                assertNotEquals(null, element.transition());
            }
        }
    }

    @Test
    public void FilterTestBreakCandidates() {
        MockStates states = new MockStates(new double[][] {{0, 0}, {0.2, 0.3}, {0.3, 0.4}});
        MockFilter filter = new MockFilter(states);

        Set<MockElement> result = filter.execute();

        assertEquals(states.numCandidates(), result.size());

        for (MockElement element : result) {
            assertEquals(states.filtprob(element.numid()), element.filtprob(), 10E-6);
            assertEquals(states.seqprob(element.numid()), element.seqprob(), 10E-6);
            if (states.pred(element.numid()) == -1) {
                assertEquals(null, element.predecessor());
                assertEquals(null, element.transition());
            } else {
                assertEquals(states.pred(element.numid()), element.predecessor().numid());
                assertNotEquals(null, element.transition());
            }
        }
    }
}
