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

package com.bmwcarit.barefoot.matcher;

import org.json.JSONException;
import org.json.JSONObject;

import com.bmwcarit.barefoot.markov.StateTransition;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.roadmap.Route;

/**
 * State transition between matching candidates in Hidden Markov Model (HMM) map matching and
 * contains a route between respective map positions.
 */
public class MatcherTransition extends StateTransition {
    private Route route = null;

    /**
     * Creates {@link MatcherTransition} object.
     *
     * @param route {@link Route} object as state transition in map matching.
     */
    public MatcherTransition(Route route) {
        this.route = route;
    }

    /**
     * Creates {@link MatcherTransition} object from its JSON representation.
     *
     * @param json JSON representation of {@link MatcherTransition} object.
     * @param map {@link RoadMap} object
     * @throws JSONException thrown on JSON parse error.
     */
    public MatcherTransition(JSONObject json, RoadMap map) throws JSONException {
        super(json);
        route = Route.fromJSON(json.getJSONObject("route"), map);
    }

    /**
     * Gets {@link Route} object of the state transition.
     *
     * @return {@link Route} object of the state transition.
     */
    public Route route() {
        return route;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put("route", route.toJSON());
        return json;
    }
}
