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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bmwcarit.barefoot.util.Triple;

/**
 * <i>k</i>-State data structure for organizing state memory in HMM inference.
 *
 * @param <C> Candidate inherits from {@link StateCandidate}.
 * @param <T> Transition inherits from {@link StateTransition}.
 * @param <S> Sample inherits from {@link Sample}.
 */
public class KState<C extends StateCandidate<C, T, S>, T extends StateTransition, S extends Sample>
        extends StateMemory<C, T, S> {
    private final int k;
    private final long t;
    private final LinkedList<Triple<Set<C>, S, C>> sequence;
    private final Map<C, Integer> counters;

    /**
     * Creates empty {@link KState} object with default parameters, i.e. capacity is unbounded.
     */
    public KState() {
        this.k = -1;
        this.t = -1;
        this.sequence = new LinkedList<>();
        this.counters = new HashMap<>();
    }

    /**
     * Creates a {@link KState} object from a JSON representation.
     *
     * @param json JSON representation of a {@link KState} object.
     * @param factory Factory for creation of state candidates and transitions.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public KState(JSONObject json, Factory<C, T, S> factory) throws JSONException {
        this.k = json.getInt("k");
        this.t = json.getLong("t");
        this.sequence = new LinkedList<>();
        this.counters = new HashMap<>();

        Map<String, C> candidates = new HashMap<>();
        JSONArray jsoncandidates = json.getJSONArray("candidates");
        for (int i = 0; i < jsoncandidates.length(); ++i) {
            JSONObject jsoncandidate = jsoncandidates.getJSONObject(i);
            C candidate = factory.candidate(jsoncandidate.getJSONObject("candidate"));
            int count = jsoncandidate.getInt("count");

            counters.put(candidate, count);
            candidates.put(candidate.id(), candidate);
        }

        JSONArray jsonsequence = json.getJSONArray("sequence");
        for (int i = 0; i < jsonsequence.length(); ++i) {
            JSONObject jsonseqelement = jsonsequence.getJSONObject(i);
            Set<C> vector = new HashSet<>();
            JSONArray jsonvector = jsonseqelement.getJSONArray("vector");
            for (int j = 0; j < jsonvector.length(); ++j) {
                JSONObject jsonelement = jsonvector.getJSONObject(j);

                String candid = jsonelement.getString("candid");
                String predid = jsonelement.getString("predid");

                C candidate = candidates.get(candid);
                C pred = candidates.get(predid);

                if (candidate == null || (!predid.isEmpty() && pred == null)) {
                    throw new JSONException("inconsistent JSON of KState object");
                }

                candidate.predecessor(pred);
                vector.add(candidate);
            }

            S sample = factory.sample(jsonseqelement.getJSONObject("sample"));
            String kestid = jsonseqelement.getString("kestid");
            C kestimate = candidates.get(kestid);

            sequence.add(new Triple<>(vector, sample, kestimate));
        }

        Collections.sort(sequence, new Comparator<Triple<Set<C>, S, C>>() {
            @Override
            public int compare(Triple<Set<C>, S, C> left, Triple<Set<C>, S, C> right) {
                if (left.two().time() < right.two().time()) {
                    return -1;
                } else if (left.two().time() > right.two().time()) {
                    return +1;
                }
                return 0;
            }
        });
    }

    /**
     * Creates an empty {@link KState} object and sets <i>&kappa;</i> and <i>&tau;</i> parameters.
     *
     * @param k <i>&kappa;</i> parameter bounds the length of the state sequence to at most
     *        <i>&kappa;+1</i> states, if <i>&kappa; &ge; 0</i>.
     * @param t <i>&tau;</i> parameter bounds length of the state sequence to contain only states
     *        for the past <i>&tau;</i> milliseconds.
     */
    public KState(int k, long t) {
        this.k = k;
        this.t = t;
        this.sequence = new LinkedList<>();
        this.counters = new HashMap<>();
    }

    @Override
    public boolean isEmpty() {
        return counters.isEmpty();
    }

    @Override
    public int size() {
        return counters.size();
    }

    @Override
    public Long time() {
        if (sequence.isEmpty()) {
            return null;
        } else {
            return sequence.peekLast().two().time();
        }
    }

    @Override
    public S sample() {
        if (sequence.isEmpty()) {
            return null;
        } else {
            return sequence.peekLast().two();
        }
    }

    /**
     * Gets the sequence of measurements <i>z<sub>0</sub>, z<sub>1</sub>, ..., z<sub>t</sub></i>.
     *
     * @return List with the sequence of measurements.
     */
    public List<S> samples() {
        LinkedList<S> samples = new LinkedList<>();
        for (Triple<Set<C>, S, C> element : sequence) {
            samples.add(element.two());
        }
        return samples;
    }

    @Override
    public void update(Set<C> vector, S sample) {
        if (vector.isEmpty()) {
            return;
        }

        if (!sequence.isEmpty() && sequence.peekLast().two().time() > sample.time()) {
            throw new RuntimeException("out-of-order state update is prohibited");
        }

        C kestimate = null;
        for (C candidate : vector) {
            counters.put(candidate, 0);
            if (candidate.predecessor() != null) {
                if (!counters.containsKey(candidate.predecessor())
                        || !sequence.peekLast().one().contains(candidate.predecessor())) {
                    throw new RuntimeException("Inconsistent update vector.");
                }
                counters.put(candidate.predecessor(), counters.get(candidate.predecessor()) + 1);
            }
            if (kestimate == null || candidate.seqprob() > kestimate.seqprob()) {
                kestimate = candidate;
            }
        }

        if (!sequence.isEmpty()) {
            Triple<Set<C>, S, C> last = sequence.peekLast();
            Set<C> deletes = new HashSet<>();

            for (C candidate : last.one()) {
                if (counters.get(candidate) == 0) {
                    deletes.add(candidate);
                }
            }

            int size = sequence.peekLast().one().size();

            for (C candidate : deletes) {
                if (deletes.size() != size || candidate != last.three()) {
                    remove(candidate, sequence.size() - 1);
                }
            }
        }

        sequence.add(new Triple<>(vector, sample, kestimate));

        while ((t > 0 && sample.time() - sequence.peekFirst().two().time() > t)
                || (k >= 0 && sequence.size() > k + 1)) {
            Set<C> deletes = sequence.removeFirst().one();
            for (C candidate : deletes) {
                counters.remove(candidate);
            }

            for (C candidate : sequence.peekFirst().one()) {
                candidate.predecessor(null);
            }
        }

        assert (k < 0 || sequence.size() <= k + 1);
    }

    protected void remove(C candidate, int index) {
        if (sequence.get(index).three() == candidate) {
            return;
        }

        Set<C> vector = sequence.get(index).one();
        counters.remove(candidate);
        vector.remove(candidate);

        C predecessor = candidate.predecessor();
        if (predecessor != null) {
            counters.put(predecessor, counters.get(predecessor) - 1);
            if (counters.get(predecessor) == 0) {
                remove(predecessor, index - 1);
            }
        }
    }

    @Override
    public Set<C> vector() {
        if (sequence.isEmpty()) {
            return new HashSet<>();
        } else {
            return sequence.peekLast().one();
        }
    }

    @Override
    public C estimate() {
        if (sequence.isEmpty()) {
            return null;
        }

        C estimate = null;
        for (C candidate : sequence.peekLast().one()) {
            if (estimate == null || candidate.filtprob() > estimate.filtprob()) {
                estimate = candidate;
            }
        }
        return estimate;
    }

    /**
     * Gets the most likely sequence of state candidates <i>s<sub>0</sub>, s<sub>1</sub>, ...,
     * s<sub>t</sub></i>.
     *
     * @return List of the most likely sequence of state candidates.
     */
    public List<C> sequence() {
        if (sequence.isEmpty()) {
            return null;
        }

        C kestimate = sequence.peekLast().three();
        LinkedList<C> ksequence = new LinkedList<>();

        for (int i = sequence.size() - 1; i >= 0; --i) {
            if (kestimate != null) {
                ksequence.push(kestimate);
                kestimate = kestimate.predecessor();
            } else {
                ksequence.push(sequence.get(i).three());
                kestimate = sequence.get(i).three().predecessor();
            }
        }

        return ksequence;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        JSONArray jsonsequence = new JSONArray();
        for (Triple<Set<C>, S, C> element : sequence) {
            JSONObject jsonseqelement = new JSONObject();
            JSONArray jsonvector = new JSONArray();
            for (C candidate : element.one()) {
                JSONObject jsoncandidate = new JSONObject();
                jsoncandidate.put("candid", candidate.id());
                jsoncandidate.put("predid",
                        candidate.predecessor() == null ? "" : candidate.predecessor().id());
                jsonvector.put(jsoncandidate);
            }
            jsonseqelement.put("vector", jsonvector);
            jsonseqelement.put("sample", element.two().toJSON());
            jsonseqelement.put("kestid", element.three().id());
            jsonsequence.put(jsonseqelement);
        }

        JSONArray jsoncandidates = new JSONArray();
        for (Entry<C, Integer> entry : counters.entrySet()) {
            JSONObject jsoncandidate = new JSONObject();
            jsoncandidate.put("candidate", entry.getKey().toJSON());
            jsoncandidate.put("count", entry.getValue());
            jsoncandidates.put(jsoncandidate);
        }
        json.put("k", k);
        json.put("t", t);
        json.put("sequence", jsonsequence);
        json.put("candidates", jsoncandidates);

        return json;
    }
}
