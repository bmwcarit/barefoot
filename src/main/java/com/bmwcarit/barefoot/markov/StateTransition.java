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
 * Transition between state candidates, i.e. {@link StateCandidate} objects.
 */
public class StateTransition {

    /**
     * Creates {@link StateTransition} object.
     */
    public StateTransition() {
        return;
    }

    /**
     * Creates {@link StateTransition} object from JSON representation.
     *
     * @param json JSON representation of a transition.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public StateTransition(JSONObject json) throws JSONException {
        return;
    }

    /**
     * Gets a JSON representation of the {@link StateTransition} object.
     *
     * @return JSON representation of the {@link StateTransition} object.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public JSONObject toJSON() throws JSONException {
        return new JSONObject();
    }
}
