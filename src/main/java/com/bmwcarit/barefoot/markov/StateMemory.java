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

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * State memory in Hidden Markov Model (HMM) inference and organizes state vectors
 * <i>S<sub>t</sub></i>, i.e. a set of state candidates representing possible states for some time
 * <i>t</i> with a probability distribution, over time.
 *
 * @param <C> Candidate inherits from {@link StateCandidate}.
 * @param <T> Transition inherits from {@link StateTransition}.
 * @param <S> Sample inherits from {@link Sample}.
 */
public class StateMemory<C extends StateCandidate<C, T, S>, T extends StateTransition, S extends Sample> {
    private Set<C> candidates = new HashSet<>();
    private S sample = null;

    /**
     * Creates a {@link StateMemory} object.
     */
    public StateMemory() {
        return;
    }

    /**
     * Creates a {@link StateMemory} object from a JSON representation.
     *
     * @param json JSON representation of a {@link StateMemory} object.
     * @param factory Factory for creation of s, transitions, and samples.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public StateMemory(JSONObject json, Factory<C, T, S> factory) throws JSONException {
        JSONArray jsoncandidates = json.optJSONArray("candidates");
        for (int i = 0; i < jsoncandidates.length(); ++i) {
            C candidate = factory.candidate(jsoncandidates.getJSONObject(i));
            candidates.add(candidate);
        }

        JSONObject jsonsample = json.optJSONObject("sample");
        if (jsonsample != null) {
            sample = factory.sample(jsonsample);
        }
    }

    /**
     * Indicates if the state is empty.
     *
     * @return Boolean indicating if the state is empty.
     */
    public boolean isEmpty() {
        return candidates.isEmpty();
    }

    /**
     * Gets the size of the state, which is the number of state candidates organized in the data
     * structure.
     *
     * @return Size of the state, which is the number of state candidates.
     */
    public int size() {
        return candidates.size();
    }

    /**
     * Time of the last state update in milliseconds epoch time.
     *
     * @return Time of last state update in milliseconds epoch time, or null if there hasn't been
     *         any update yet.
     */
    public Long time() {
        if (sample == null) {
            return null;
        } else {
            return sample.time();
        }
    }

    /**
     * {@link Sample} object of the most recent update.
     *
     * @return {@link Sample} object of the most recent update or null if there hasn't been any
     *         update yet.
     */
    public S sample() {
        return sample;
    }

    /**
     * Updates the state with a state vector which is a set of {@link StateCandidate} objects with
     * its respective measurement, which is a {@link Sample} object.
     *
     * @param vector State vector for update of the state.
     * @param sample Sample measurement of the state vector.
     */
    public void update(Set<C> vector, S sample) {
        if (vector.isEmpty()) {
            return;
        }

        if (this.time() != null && this.time() > sample.time()) {
            throw new RuntimeException("out-of-order state update is prohibited");
        }

        this.candidates = vector;
        this.sample = sample;
    }

    /**
     * Gets state vector of the last update.
     *
     * @return State vector of the last update, or an empty set if there hasn't been any update yet.
     */
    public Set<C> vector() {
        return candidates;
    }

    /**
     * Gets a state estimate which is the most likely state candidate of the last update, with
     * respect to state candidate's filter probability (see {@link StateCandidate#filtprob()}).
     *
     * @return State estimate, which is most likely state candidate.
     */
    public C estimate() {
        if (candidates.isEmpty()) {
            return null;
        }

        C estimate = null;
        for (C candidate : candidates) {
            if (estimate == null || candidate.filtprob() > estimate.filtprob()) {
                estimate = candidate;
            }
        }
        return estimate;
    }

    /**
     * Gets a JSON representation of the {@link StateMemory} object.
     *
     * @return JSON representation of the {@link StateMemory} object.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        JSONArray jsoncanddiates = new JSONArray();
        for (C candidate : candidates) {
            jsoncanddiates.put(candidate.toJSON());
        }
        json.put("candidates", jsoncanddiates);

        if (sample != null) {
            json.put("sample", sample.toJSON());
        }
        return json;
    }
}
