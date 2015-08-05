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

import com.bmwcarit.barefoot.markov.Factory;
import com.bmwcarit.barefoot.roadmap.RoadMap;

/**
 * Matcher factory for creation of matching candidates, transitions, and samples.
 */
public class MatcherFactory extends Factory<MatcherCandidate, MatcherTransition, MatcherSample> {
    private final RoadMap map;

    /**
     * Creates {@link MatcherFactory} object.
     *
     * @param map {@link RoadMap} object used for creation of matching candidates, transitions and
     *        samples.
     */
    public MatcherFactory(RoadMap map) {
        this.map = map;
    }

    @Override
    public MatcherCandidate candidate(JSONObject json) throws JSONException {
        return new MatcherCandidate(json, this, map);
    }

    @Override
    public MatcherTransition transition(JSONObject json) throws JSONException {
        return new MatcherTransition(json, map);
    }

    @Override
    public MatcherSample sample(JSONObject json) throws JSONException {
        return new MatcherSample(json);
    }
}
