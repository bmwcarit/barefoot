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

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * State candidate in Hidden Markov Model (HMM) inference e.g. with a HMM filter {@link Filter}. A
 * state candidate refers to a possible state with some probability as part of a stochastic process.
 *
 * @param <C> Candidate inherits from {@link StateCandidate}.
 * @param <T> Transition inherits from {@link StateTransition}.
 * @param <S> Sample inherits from {@link Sample}.
 */
public class StateCandidate<C extends StateCandidate<C, T, S>, T extends StateTransition, S extends Sample> {
    private final String id;
    private C predecessor = null;
    private T transition = null;
    private double seqprob = 0d;
    private double filtprob = 0d;

    /**
     * Creates a {@link StateCandidate} object and generates a random UUID.
     */
    public StateCandidate() {
        id = UUID.randomUUID().toString();
    }

    /**
     * Creates {@link StateCandidate} object with a specific identifier.
     *
     * @param id Object identifier (should be unique in {@link StateMemory} context).
     */
    public StateCandidate(String id) {
        this.id = id;
    }

    /**
     * Creates {@link StateCandidate} object from its JSON representation.
     *
     * @param json JSON representation of an {@link StateCandidate} object.
     * @param factory {@link Factory} object for construction of state candidates and transitions.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public StateCandidate(JSONObject json, Factory<C, T, S> factory) throws JSONException {
        id = json.getString("id");
        JSONObject jsontrans = json.optJSONObject("transition");
        if (jsontrans != null) {
            transition = factory.transition(jsontrans);
        }

        // This does not handle infinite values.
        filtprob = json.getDouble("filtprob");
        seqprob = json.getDouble("seqprob");
    }

    /**
     * Gets identifier of state candidate.
     *
     * @return Identifier of state candidate.
     */
    public String id() {
        return id;
    }

    /**
     * Gets predecessor in the most likely sequence to this state candidate. If there is no such
     * sequence it's null.
     *
     * @return Predecessor in the most likely sequence to this state candidate, if it exists
     *         otherwise null.
     */
    public C predecessor() {
        return predecessor;
    }

    /**
     * Sets predecessor in the most likely sequence to this state candidate.
     *
     * @param predecessor Most likely predecessor state candidate.
     */
    public void predecessor(C predecessor) {
        this.predecessor = predecessor;
    }

    /**
     * Gets transition from predecessor, if it exists otherwise null.
     *
     * @return Transition from predecessor, if it exists otherwise null.
     */
    public T transition() {
        return transition;
    }

    /**
     * Sets transition from predecessor, if it exists otherwise null.
     *
     * @param transition Transition from most likely predecessor state candidate.
     */
    public void transition(T transition) {
        this.transition = transition;
    }

    /**
     * Gets sequence probability of the state candidate (logarithmic scaled with
     * <i>log<sub>10</sub></i>).
     *
     * @return State candidate's sequence probability.
     */
    public double seqprob() {
        return seqprob;
    }

    /**
     * Sets sequence probability of the state candidate (logarithmic scaled with
     * <i>log<sub>10</sub></i>).
     *
     * @param seqprob Sequence probability
     */
    public void seqprob(double seqprob) {
        this.seqprob = seqprob;
    }

    /**
     * Gets filter probability of the state candidate.
     *
     * @return Filter probability.
     */
    public double filtprob() {
        return filtprob;
    }

    /**
     * Sets filter probability of the state candidate.
     *
     * @param filtprob Filter probability.
     */
    public void filtprob(double filtprob) {
        this.filtprob = filtprob;
    }

    /**
     * Gets a JSON representation of the state candidate.
     *
     * @return JSON representation of the state candidate.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("filtprob", Double.isInfinite(filtprob) ? "Infinity" : filtprob);
        json.put("seqprob", Double.isInfinite(seqprob) ? "-Infinity" : seqprob);
        if (transition != null) {
            json.put("transition", transition().toJSON());
        }
        return json;
    }
}
