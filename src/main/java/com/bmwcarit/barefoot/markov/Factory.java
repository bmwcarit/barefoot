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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Factory for creation of state candidates, which is {@link StateCandidate}, transitions, which is
 * {@link StateTransition}, and samples, i.e. {@link Sample}. A {@link Factory} instance enables
 * dependency injection during creation from their respective JSON representation.
 *
 * @param <C> Candidate inherits from {@link StateCandidate}.
 * @param <T> Transition inherits from {@link StateTransition}.
 * @param <S> Sample inherits from {@link Sample}.
 */
public abstract class Factory<C extends StateCandidate<C, T, S>, T extends StateTransition, S extends Sample> {

    /**
     * Creates an {@link StateCandidate} object.
     *
     * @param json JSON representation of an {@link StateCandidate} object.
     * @return {@link StateCandidate} object.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public abstract C candidate(JSONObject json) throws JSONException;

    /**
     * Creates a {@link StateTransition} object.
     *
     * @param json JSON representation of a {@link StateTransition} object.
     * @return {@link StateTransition} object.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public abstract T transition(JSONObject json) throws JSONException;

    /**
     * Creates a {@link Sample} object.
     *
     * @param json JSON representation of a {@link Sample} object.
     * @return {@link Sample} object.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public abstract S sample(JSONObject json) throws JSONException;
}
