/*
 * Copyright (C) 2016, BMW Car IT GmbH
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;

import com.bmwcarit.barefoot.roadmap.Loader;
import com.bmwcarit.barefoot.roadmap.Road;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.roadmap.RoadPoint;
import com.bmwcarit.barefoot.roadmap.TimePriority;
import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.topology.Dijkstra;

public class MatcherExample {

    public List<MatcherSample> readSamples(String path) throws IOException, JSONException {
        String json = new String(Files.readAllBytes(Paths.get(path)), Charset.defaultCharset());
        JSONArray jsonsamples = new JSONArray(json);
        List<MatcherSample> samples = new LinkedList<>();
        for (int i = 0; i < jsonsamples.length(); ++i) {
            samples.add(new MatcherSample(jsonsamples.getJSONObject(i)));
        }
        return samples;
    }

    @Ignore
    @Test
    public void offline() throws IOException, JSONException {
        // Load and construct road map
        RoadMap map = Loader.roadmap("config/oberbayern.properties", true).construct();

        // Instantiate matcher and state data structure
        Matcher matcher = new Matcher(map, new Dijkstra<Road, RoadPoint>(), new TimePriority(),
                new Geography());

        // Input as sample batch (offline) or sample stream (online)
        List<MatcherSample> samples =
                readSamples(ServerTest.class.getResource("x0001-001.json").getPath());

        // Match full sequence of samples
        MatcherKState state = matcher.mmatch(samples, 1, 500);

        // Access map matching result (sequence for all samples)
        for (MatcherCandidate cand : state.sequence()) {
            System.out.println(cand.point().edge().base().refid()); // OSM id
            cand.point().edge().base().id(); // road id
            cand.point().edge().heading(); // heading
            cand.point().geometry(); // GPS position (on the road)
            if (cand.transition() != null)
                cand.transition().route().geometry(); // path geometry from last matching candidate
        }
    }

    @Ignore
    @Test
    public void online() throws IOException, JSONException {
        // Load and construct road map
        RoadMap map = Loader.roadmap("config/oberbayern.properties", true).construct();

        // Instantiate matcher and state data structure
        Matcher matcher = new Matcher(map, new Dijkstra<Road, RoadPoint>(), new TimePriority(),
                new Geography());

        // Input as sample batch (offline) or sample stream (online)
        List<MatcherSample> samples =
                readSamples(ServerTest.class.getResource("x0001-001.json").getPath());

        // Create initial (empty) state memory
        MatcherKState state = new MatcherKState();

        for (MatcherSample sample : samples) {
            // Execute matcher with single sample and update state memory
            state.update(matcher.execute(state.vector(), state.sample(), sample), sample);

            // Access map matching result (estimate for most recent sample)
            MatcherCandidate estimate = state.estimate();
            System.out.println(estimate.point().edge().base().refid()); // OSM id
            estimate.point().edge().base().id(); // road id
            estimate.point().edge().heading(); // heading
            estimate.point().geometry(); // GPS position (on the road)
            if (estimate.transition() != null)
                estimate.transition().route().geometry(); // path geometry from last matching
                                                          // candidate
        }
    }
}
