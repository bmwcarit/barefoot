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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class KStateTest {

    private static class MockElem extends StateCandidate<MockElem, StateTransition, Sample> {

        public MockElem(int id, double seqprob, double filtprob, MockElem pred) {
            super(Integer.toString(id));
            this.seqprob(seqprob);
            this.filtprob(filtprob);
            this.predecessor(pred);
        }

        public MockElem(JSONObject json, MockFactory factory) throws JSONException {
            super(json, factory);
        }

        public int numid() {
            return Integer.parseInt(id());
        }
    }

    private static class MockFactory extends Factory<MockElem, StateTransition, Sample> {

        @Override
        public MockElem candidate(JSONObject json) throws JSONException {
            return new MockElem(json, this);
        }

        @Override
        public StateTransition transition(JSONObject json) throws JSONException {
            return new StateTransition(json);
        }

        @Override
        public Sample sample(JSONObject json) throws JSONException {
            return new Sample(json);
        }
    }

    @Test
    public void TestKStateUnbound() {
        Map<Integer, MockElem> elements = new HashMap<>();
        elements.put(0, new MockElem(0, Math.log10(0.3), 0.3, null));
        elements.put(1, new MockElem(1, Math.log10(0.2), 0.2, null));
        elements.put(2, new MockElem(2, Math.log10(0.5), 0.5, null));

        KState<MockElem, StateTransition, Sample> state = new KState<>();
        {
            Set<MockElem> vector =
                    new HashSet<>(Arrays.asList(elements.get(0), elements.get(1), elements.get(2)));

            state.update(vector, new Sample(0));

            assertEquals(3, state.size());
            assertEquals(2, state.estimate().numid());
        }

        elements.put(3, new MockElem(3, Math.log10(0.3), 0.3, elements.get(1)));
        elements.put(4, new MockElem(4, Math.log10(0.2), 0.2, elements.get(1)));
        elements.put(5, new MockElem(5, Math.log10(0.4), 0.4, elements.get(2)));
        elements.put(6, new MockElem(6, Math.log10(0.1), 0.1, elements.get(2)));

        {
            Set<MockElem> vector = new HashSet<>(Arrays.asList(elements.get(3), elements.get(4),
                    elements.get(5), elements.get(6)));

            state.update(vector, new Sample(1));

            assertEquals(6, state.size());
            assertEquals(5, state.estimate().numid());

            List<Integer> sequence = new LinkedList<>(Arrays.asList(2, 5));
            for (int i = 0; i < state.sequence().size() - 1; ++i) {
                assertEquals(sequence.get(i).longValue(), state.sequence().get(i).numid());
            }
        }

        elements.put(7, new MockElem(7, Math.log10(0.3), 0.3, elements.get(5)));
        elements.put(8, new MockElem(8, Math.log10(0.2), 0.2, elements.get(5)));
        elements.put(9, new MockElem(9, Math.log10(0.4), 0.4, elements.get(6)));
        elements.put(10, new MockElem(10, Math.log10(0.1), 0.1, elements.get(6)));

        {
            Set<MockElem> vector = new HashSet<>(Arrays.asList(elements.get(7), elements.get(8),
                    elements.get(9), elements.get(10)));

            state.update(vector, new Sample(2));

            assertEquals(7, state.size());
            assertEquals(9, state.estimate().numid());

            List<Integer> sequence = new LinkedList<>(Arrays.asList(2, 6, 9));
            for (int i = 0; i < state.sequence().size() - 1; ++i) {
                assertEquals(sequence.get(i).longValue(), state.sequence().get(i).numid());
            }
        }

        elements.put(11, new MockElem(11, Math.log10(0.3), 0.3, null));
        elements.put(12, new MockElem(12, Math.log10(0.2), 0.2, null));
        elements.put(13, new MockElem(13, Math.log10(0.4), 0.4, null));
        elements.put(14, new MockElem(14, Math.log10(0.1), 0.1, null));

        {
            Set<MockElem> vector = new HashSet<>(Arrays.asList(elements.get(11), elements.get(12),
                    elements.get(13), elements.get(14)));

            state.update(vector, new Sample(3));

            assertEquals(8, state.size());
            assertEquals(13, state.estimate().numid());

            List<Integer> sequence = new LinkedList<>(Arrays.asList(2, 6, 9, 13));
            for (int i = 0; i < state.sequence().size() - 1; ++i) {
                assertEquals(sequence.get(i).longValue(), state.sequence().get(i).numid());
            }
        }
        {
            Set<MockElem> vector = new HashSet<>();

            state.update(vector, new Sample(4));

            assertEquals(8, state.size());
            assertEquals(13, state.estimate().numid());

            List<Integer> sequence = new LinkedList<>(Arrays.asList(2, 6, 9, 13));
            for (int i = 0; i < state.sequence().size() - 1; ++i) {
                assertEquals(sequence.get(i).longValue(), state.sequence().get(i).numid());
            }
        }
    }

    @Test
    public void TestBreak() {
        // Test k-state in case of HMM break 'no transition' as reported in barefoot issue #83.
        // Tests only 'no transitions', no emissions is empty vector and, hence, input to update
        // operation.

        KState<MockElem, StateTransition, Sample> state = new KState<>();
        Map<Integer, MockElem> elements = new HashMap<>();
        elements.put(0, new MockElem(0, Math.log10(0.4), 0.4, null));
        {
            Set<MockElem> vector = new HashSet<>(Arrays.asList(elements.get(0)));
            state.update(vector, new Sample(0));
        }
        elements.put(1, new MockElem(1, Math.log(0.7), 0.6, null));
        elements.put(2, new MockElem(2, Math.log(0.3), 0.4, elements.get(0)));
        {
            Set<MockElem> vector = new HashSet<>(Arrays.asList(elements.get(1), elements.get(2)));
            state.update(vector, new Sample(1));
        }
        elements.put(3, new MockElem(3, Math.log(0.5), 0.6, null));
        {
            Set<MockElem> vector = new HashSet<>(Arrays.asList(elements.get(3)));
            state.update(vector, new Sample(2));
        }
        List<MockElem> seq = state.sequence();
        assertEquals(seq.get(0).numid(), 0);
        assertEquals(seq.get(1).numid(), 1);
        assertEquals(seq.get(2).numid(), 3);
    }

    @Test
    public void TestKState() {
        Map<Integer, MockElem> elements = new HashMap<>();
        elements.put(0, new MockElem(0, Math.log10(0.3), 0.3, null));
        elements.put(1, new MockElem(1, Math.log10(0.2), 0.2, null));
        elements.put(2, new MockElem(2, Math.log10(0.5), 0.5, null));

        KState<MockElem, StateTransition, Sample> state = new KState<>(1, -1);
        {
            Set<MockElem> vector =
                    new HashSet<>(Arrays.asList(elements.get(0), elements.get(1), elements.get(2)));

            state.update(vector, new Sample(0));

            assertEquals(3, state.size());
            assertEquals(2, state.estimate().numid());
        }

        elements.put(3, new MockElem(3, Math.log10(0.3), 0.3, elements.get(1)));
        elements.put(4, new MockElem(4, Math.log10(0.2), 0.2, elements.get(1)));
        elements.put(5, new MockElem(5, Math.log10(0.4), 0.4, elements.get(2)));
        elements.put(6, new MockElem(6, Math.log10(0.1), 0.1, elements.get(2)));

        {
            Set<MockElem> vector = new HashSet<>(Arrays.asList(elements.get(3), elements.get(4),
                    elements.get(5), elements.get(6)));

            state.update(vector, new Sample(1));

            assertEquals(6, state.size());
            assertEquals(5, state.estimate().numid());

            List<Integer> sequence = new LinkedList<>(Arrays.asList(2, 5));
            for (int i = 0; i < state.sequence().size() - 1; ++i) {
                assertEquals(sequence.get(i).longValue(), state.sequence().get(i).numid());
            }
        }

        elements.put(7, new MockElem(7, Math.log10(0.3), 0.3, elements.get(5)));
        elements.put(8, new MockElem(8, Math.log10(0.2), 0.2, elements.get(5)));
        elements.put(9, new MockElem(9, Math.log10(0.4), 0.4, elements.get(6)));
        elements.put(10, new MockElem(10, Math.log10(0.1), 0.1, elements.get(6)));

        {
            Set<MockElem> vector = new HashSet<>(Arrays.asList(elements.get(7), elements.get(8),
                    elements.get(9), elements.get(10)));

            state.update(vector, new Sample(2));

            assertEquals(6, state.size());
            assertEquals(9, state.estimate().numid());

            List<Integer> sequence = new LinkedList<>(Arrays.asList(6, 9));
            for (int i = 0; i < state.sequence().size() - 1; ++i) {
                assertEquals(sequence.get(i).longValue(), state.sequence().get(i).numid());
            }
        }

        elements.put(11, new MockElem(11, Math.log10(0.3), 0.3, null));
        elements.put(12, new MockElem(12, Math.log10(0.2), 0.2, null));
        elements.put(13, new MockElem(13, Math.log10(0.4), 0.4, null));
        elements.put(14, new MockElem(14, Math.log10(0.1), 0.1, null));

        {
            Set<MockElem> vector = new HashSet<>(Arrays.asList(elements.get(11), elements.get(12),
                    elements.get(13), elements.get(14)));

            state.update(vector, new Sample(3));

            assertEquals(5, state.size());
            assertEquals(13, state.estimate().numid());

            List<Integer> sequence = new LinkedList<>(Arrays.asList(9, 13));
            for (int i = 0; i < state.sequence().size() - 1; ++i) {
                assertEquals(sequence.get(i).longValue(), state.sequence().get(i).numid());
            }
        }
        {
            Set<MockElem> vector = new HashSet<>();

            state.update(vector, new Sample(4));

            assertEquals(5, state.size());
            assertEquals(13, state.estimate().numid());

            List<Integer> sequence = new LinkedList<>(Arrays.asList(9, 13));
            for (int i = 0; i < state.sequence().size() - 1; ++i) {
                assertEquals(sequence.get(i).longValue(), state.sequence().get(i).numid());
            }
        }
    }

    @Test
    public void TestTState() {
        Map<Integer, MockElem> elements = new HashMap<>();
        elements.put(0, new MockElem(0, Math.log10(0.3), 0.3, null));
        elements.put(1, new MockElem(1, Math.log10(0.2), 0.2, null));
        elements.put(2, new MockElem(2, Math.log10(0.5), 0.5, null));

        KState<MockElem, StateTransition, Sample> state = new KState<>(-1, 1);
        {
            Set<MockElem> vector =
                    new HashSet<>(Arrays.asList(elements.get(0), elements.get(1), elements.get(2)));

            state.update(vector, new Sample(0));

            assertEquals(3, state.size());
            assertEquals(2, state.estimate().numid());
        }

        elements.put(3, new MockElem(3, Math.log10(0.3), 0.3, elements.get(1)));
        elements.put(4, new MockElem(4, Math.log10(0.2), 0.2, elements.get(1)));
        elements.put(5, new MockElem(5, Math.log10(0.4), 0.4, elements.get(2)));
        elements.put(6, new MockElem(6, Math.log10(0.1), 0.1, elements.get(2)));

        {
            Set<MockElem> vector = new HashSet<>(Arrays.asList(elements.get(3), elements.get(4),
                    elements.get(5), elements.get(6)));

            state.update(vector, new Sample(1));

            assertEquals(6, state.size());
            assertEquals(5, state.estimate().numid());

            List<Integer> sequence = new LinkedList<>(Arrays.asList(2, 5));
            for (int i = 0; i < state.sequence().size() - 1; ++i) {
                assertEquals(sequence.get(i).longValue(), state.sequence().get(i).numid());
            }
        }

        elements.put(7, new MockElem(7, Math.log10(0.3), 0.3, elements.get(5)));
        elements.put(8, new MockElem(8, Math.log10(0.2), 0.2, elements.get(5)));
        elements.put(9, new MockElem(9, Math.log10(0.4), 0.4, elements.get(6)));
        elements.put(10, new MockElem(10, Math.log10(0.1), 0.1, elements.get(6)));

        {
            Set<MockElem> vector = new HashSet<>(Arrays.asList(elements.get(7), elements.get(8),
                    elements.get(9), elements.get(10)));

            state.update(vector, new Sample(2));

            assertEquals(6, state.size());
            assertEquals(9, state.estimate().numid());

            List<Integer> sequence = new LinkedList<>(Arrays.asList(6, 9));
            for (int i = 0; i < state.sequence().size() - 1; ++i) {
                assertEquals(sequence.get(i).longValue(), state.sequence().get(i).numid());
            }
        }

        elements.put(11, new MockElem(11, Math.log10(0.3), 0.3, null));
        elements.put(12, new MockElem(12, Math.log10(0.2), 0.2, null));
        elements.put(13, new MockElem(13, Math.log10(0.4), 0.4, null));
        elements.put(14, new MockElem(14, Math.log10(0.1), 0.1, null));

        {
            Set<MockElem> vector = new HashSet<>(Arrays.asList(elements.get(11), elements.get(12),
                    elements.get(13), elements.get(14)));

            state.update(vector, new Sample(3));

            assertEquals(5, state.size());
            assertEquals(13, state.estimate().numid());

            List<Integer> sequence = new LinkedList<>(Arrays.asList(9, 13));
            for (int i = 0; i < state.sequence().size() - 1; ++i) {
                assertEquals(sequence.get(i).longValue(), state.sequence().get(i).numid());
            }
        }
        {
            Set<MockElem> vector = new HashSet<>();

            state.update(vector, new Sample(4));

            assertEquals(5, state.size());
            assertEquals(13, state.estimate().numid());

            List<Integer> sequence = new LinkedList<>(Arrays.asList(9, 13));
            for (int i = 0; i < state.sequence().size() - 1; ++i) {
                assertEquals(sequence.get(i).longValue(), state.sequence().get(i).numid());
            }
        }
    }

    @Test
    public void TestKStateJSON() throws JSONException {
        Map<Integer, MockElem> elements = new HashMap<>();

        KState<MockElem, StateTransition, Sample> state = new KState<>(1, -1);

        {
            JSONObject json = state.toJSON();
            state = new KState<>(json, new MockFactory());
        }

        elements.put(0, new MockElem(0, Math.log10(0.3), 0.3, null));
        elements.put(1, new MockElem(1, Math.log10(0.2), 0.2, null));
        elements.put(2, new MockElem(2, Math.log10(0.5), 0.5, null));

        state.update(
                new HashSet<>(Arrays.asList(elements.get(0), elements.get(1), elements.get(2))),
                new Sample(0));

        {
            JSONObject json = state.toJSON();
            state = new KState<>(json, new MockFactory());

            elements.clear();

            for (MockElem element : state.vector()) {
                elements.put(element.numid(), element);
            }
        }

        elements.put(3, new MockElem(3, Math.log10(0.3), 0.3, elements.get(1)));
        elements.put(4, new MockElem(4, Math.log10(0.2), 0.2, elements.get(1)));
        elements.put(5, new MockElem(5, Math.log10(0.4), 0.4, elements.get(2)));
        elements.put(6, new MockElem(6, Math.log10(0.1), 0.1, elements.get(2)));

        state.update(new HashSet<>(
                Arrays.asList(elements.get(3), elements.get(4), elements.get(5), elements.get(6))),
                new Sample(1));

        {
            JSONObject json = state.toJSON();
            state = new KState<>(json, new MockFactory());

            elements.clear();

            for (MockElem element : state.vector()) {
                elements.put(element.numid(), element);
            }
        }

        elements.put(7, new MockElem(7, Math.log10(0.3), 0.3, elements.get(5)));
        elements.put(8, new MockElem(8, Math.log10(0.2), 0.2, elements.get(5)));
        elements.put(9, new MockElem(9, Math.log10(0.4), 0.4, elements.get(6)));
        elements.put(10, new MockElem(10, Math.log10(0.1), 0.1, elements.get(6)));

        state.update(new HashSet<>(
                Arrays.asList(elements.get(7), elements.get(8), elements.get(9), elements.get(10))),
                new Sample(2));

        {
            JSONObject json = state.toJSON();
            state = new KState<>(json, new MockFactory());

            elements.clear();

            for (MockElem element : state.vector()) {
                elements.put(element.numid(), element);
            }
        }

        elements.put(11, new MockElem(11, Math.log10(0.3), 0.3, null));
        elements.put(12, new MockElem(12, Math.log10(0.2), 0.2, null));
        elements.put(13, new MockElem(13, Math.log10(0.4), 0.4, null));
        elements.put(14, new MockElem(14, Math.log10(0.1), 0.1, null));

        state.update(new HashSet<>(Arrays.asList(elements.get(11), elements.get(12),
                elements.get(13), elements.get(14))), new Sample(3));

        state.update(new HashSet<MockElem>(), new Sample(4));

        {
            JSONObject json = state.toJSON();
            KState<MockElem, StateTransition, Sample> state2 =
                    new KState<>(json, new MockFactory());

            assertEquals(state.size(), state2.size());
            assertEquals(5, state2.size());
            assertEquals(state.estimate().numid(), state2.estimate().numid());
            assertEquals(13, state2.estimate().numid());
            assertEquals(state.sequence().size(), state2.sequence().size());
            assertEquals(2, state2.sequence().size());

            List<Integer> sequence = new LinkedList<>(Arrays.asList(9, 13));
            for (int i = 0; i < state.sequence().size() - 1; ++i) {
                assertEquals(state.sequence().get(i).numid(), state2.sequence().get(i).numid());
                assertEquals(sequence.get(i).longValue(), state2.sequence().get(i).numid());
            }
        }
    }
}
