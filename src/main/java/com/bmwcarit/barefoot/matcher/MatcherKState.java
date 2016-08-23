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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bmwcarit.barefoot.markov.KState;
import com.bmwcarit.barefoot.roadmap.Road;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WktExportFlags;

/**
 * <i>k</i>-State data structure wrapper of {@link KState} for organizing state memory in HMM map
 * matching.
 */
public class MatcherKState extends KState<MatcherCandidate, MatcherTransition, MatcherSample> {

    /**
     * Creates empty {@link MatcherKState} object with default parameters, which means capacity is
     * unbound.
     */
    public MatcherKState() {
        super();
    }

    /**
     * Creates a {@link MatcherKState} object from a JSON representation.
     *
     * @param json JSON representation of a {@link MatcherKState} object.
     * @param factory {@link MatcherFactory} for creation of matcher candidates and transitions.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public MatcherKState(JSONObject json, MatcherFactory factory) throws JSONException {
        super(json, factory);
    }

    /**
     * Creates an empty {@link MatcherKState} object and sets <i>&kappa;</i> and <i>&tau;</i>
     * parameters.
     *
     * @param k <i>&kappa;</i> parameter bounds the length of the state sequence to at most
     *        <i>&kappa;+1</i> states, if <i>&kappa; &ge; 0</i>.
     * @param t <i>&tau;</i> parameter bounds length of the state sequence to contain only states
     *        for the past <i>&tau;</i> milliseconds.
     */
    public MatcherKState(int k, long t) {
        super(k, t);
    }

    /**
     * Gets {@link JSONObject} with GeoJSON format of {@link MatcherKState} matched geometries.
     *
     * @return {@link JSONObject} with GeoJSON format of {@link MatcherKState} matched geometries.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public JSONObject toGeoJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "MultiLineString");
        JSONArray jsonsequence = new JSONArray();
        if (this.sequence() != null) {
            for (MatcherCandidate candidate : this.sequence()) {
                if (candidate.transition() == null) {
                    continue;
                }
                JSONObject jsoncandidate = new JSONObject(GeometryEngine
                        .geometryToGeoJson(candidate.transition().route().geometry()));
                jsonsequence.put(jsoncandidate.getJSONArray("coordinates"));
            }
        }
        json.put("coordinates", jsonsequence);
        return json;
    }

    /**
     * Gets JSON format String of {@link MatcherKState}, includes {@link JSONArray} String of
     * samples and {@link JSONArray} String of of matching results.
     *
     * @return JSON format String of {@link MatcherKState}, includes {@link JSONArray} String of
     *         samples and {@link JSONArray} String of of matching results.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public String toDebugJSON() throws JSONException {
        StringBuilder output = new StringBuilder();

        JSONArray jsonsamples = new JSONArray();
        if (this.samples() != null) {
            for (int i = 0; i < this.samples().size(); ++i) {
                JSONObject jsonsample = new JSONObject();
                jsonsample.put("id", this.samples().get(i).id());
                jsonsample.put("geom", GeometryEngine.geometryToWkt(this.samples().get(i).point(),
                        WktExportFlags.wktExportPoint));
                jsonsample.put("time", this.samples().get(i).time() / 1000);
                jsonsamples.put(jsonsample);
            }
        }
        output.append(jsonsamples.toString());
        output.append("\n");

        JSONArray jsonsequence = new JSONArray();
        if (this.sequence() != null) {
            for (int i = 0; i < this.sequence().size(); ++i) {
                MatcherCandidate candidate = this.sequence().get(i);
                JSONObject jsoncandidate = candidate.toJSON();
                jsoncandidate.put("time", this.samples().get(i).time() / 1000);
                if (candidate.transition() != null) {
                    jsoncandidate.put("geom",
                            GeometryEngine.geometryToWkt(candidate.transition().route().geometry(),
                                    WktExportFlags.wktExportLineString));
                } else {
                    jsoncandidate.put("geom", GeometryEngine.geometryToWkt(
                            candidate.point().geometry(), WktExportFlags.wktExportPoint));
                }
                jsonsequence.put(jsoncandidate);
            }
        }
        output.append(jsonsequence.toString());

        return output.toString();
    }

    /**
     * Gets {@link JSONArray} of {@link MatcherKState} with map matched positions, represented by
     * road id and fraction, and the geometry of the routes.
     *
     * @return {@link JSONArray} of {@link MatcherKState} with map matched positions, represented by
     *         road id and fraction, and the geometry of the routes.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public JSONArray toSlimJSON() throws JSONException {
        JSONArray json = new JSONArray();
        if (this.sequence() != null) {
            for (MatcherCandidate candidate : this.sequence()) {
                JSONObject jsoncandidate = candidate.point().toJSON();
                if (candidate.transition() != null) {
                    jsoncandidate.put("route",
                            GeometryEngine.geometryToWkt(candidate.transition().route().geometry(),
                                    WktExportFlags.wktExportLineString));
                }
                json.put(jsoncandidate);
            }
        }
        return json;
    }

    private Polyline monitorRoute(MatcherCandidate candidate) {
        Polyline routes = new Polyline();
        MatcherCandidate predecessor = candidate;
        while (predecessor != null) {
            MatcherTransition transition = predecessor.transition();
            if (transition != null) {
                Polyline route = transition.route().geometry();
                routes.startPath(route.getPoint(0));
                for (int i = 1; i < route.getPointCount(); ++i) {
                    routes.lineTo(route.getPoint(i));
                }
            }
            predecessor = predecessor.predecessor();
        }
        return routes;
    }

    public JSONObject toMonitorJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("time", sample().time());
        json.put("point", GeometryEngine.geometryToWkt(estimate().point().geometry(),
                WktExportFlags.wktExportPoint));
        Polyline routes = monitorRoute(estimate());
        if (routes.getPathCount() > 0) {
            json.put("route",
                    GeometryEngine.geometryToWkt(routes, WktExportFlags.wktExportMultiLineString));
        }

        JSONArray candidates = new JSONArray();
        for (MatcherCandidate candidate : vector()) {
            JSONObject jsoncandidate = new JSONObject();
            jsoncandidate.put("point", GeometryEngine.geometryToWkt(candidate.point().geometry(),
                    WktExportFlags.wktExportPoint));
            jsoncandidate.put("prob",
                    Double.isInfinite(candidate.filtprob()) ? "Infinity" : candidate.filtprob());

            routes = monitorRoute(candidate);
            if (routes.getPathCount() > 0) {
                jsoncandidate.put("route", GeometryEngine.geometryToWkt(routes,
                        WktExportFlags.wktExportMultiLineString));
            }
            candidates.put(jsoncandidate);
        }
        json.put("candidates", candidates);
        return json;
    }

    private static String getOSMRoad(Road road) {
        return road.base().refid() + ":" + road.source() + ":" + road.target();
    }

    public JSONObject toOSMJSON(RoadMap map) throws JSONException {
        JSONObject json = this.toJSON();

        if (json.has("candidates")) {
            JSONArray candidates = json.getJSONArray("candidates");

            for (int i = 0; i < candidates.length(); ++i) {
                {
                    JSONObject point = candidates.getJSONObject(i).getJSONObject("candidate")
                            .getJSONObject("point");
                    Road road = map.get(point.getLong("road"));
                    if (road == null) {
                        throw new JSONException("road not found in map");
                    }
                    point.put("road", getOSMRoad(road));
                }
                if (candidates.getJSONObject(i).getJSONObject("candidate").has("transition")) {
                    JSONObject route = candidates.getJSONObject(i).getJSONObject("candidate")
                            .getJSONObject("transition").getJSONObject("route");

                    JSONArray roads = route.getJSONArray("roads");
                    JSONArray osmroads = new JSONArray();
                    for (int j = 0; j < roads.length(); ++j) {
                        Road road = map.get(roads.getLong(j));
                        osmroads.put(getOSMRoad(road));
                    }
                    route.put("roads", osmroads);

                    {
                        JSONObject source = route.getJSONObject("source");
                        Road road = map.get(source.getLong("road"));
                        source.put("road", getOSMRoad(road));
                    }
                    {
                        JSONObject target = route.getJSONObject("target");
                        Road road = map.get(target.getLong("road"));
                        target.put("road", getOSMRoad(road));
                    }
                }
            }
        }
        return json;
    }
}
