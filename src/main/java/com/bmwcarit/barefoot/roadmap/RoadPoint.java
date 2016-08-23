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

package com.bmwcarit.barefoot.roadmap;

import org.json.JSONException;
import org.json.JSONObject;

import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.spatial.SpatialOperator;
import com.esri.core.geometry.Point;

/**
 * Point on a {@link Road} defined by a reference to the {@link Road} and a fraction <i>f</i>, with
 * <i>0 &le; f &le; 1</i>, which defines an exact position on the {@link Road}.
 */
public class RoadPoint extends com.bmwcarit.barefoot.topology.Point<Road> {
    private static final SpatialOperator spatial = new Geography();
    private final Point geometry;
    private final double azimuth;

    /**
     * Creates a {@link RoadPoint}.
     *
     * @param road {@link Road} object of the point.
     * @param fraction Exact position on the {@link Road} defined as fraction <i>f</i>, with <i>0
     *        &le; f &le; 1</i>.
     */
    public RoadPoint(Road road, double fraction) {
        super(road, fraction);
        this.geometry = spatial.interpolate(road.geometry(), fraction);
        this.azimuth = spatial.azimuth(road.geometry(), fraction);
    }

    /**
     * Gets the geometry of the point on the {@link Road}.
     *
     * @return Geometry of the point on the road.
     */
    public Point geometry() {
        return geometry;
    }

    public double azimuth() {
        return azimuth;
    }

    /**
     * Gets the JSON representation of the {@link RoadPoint}.
     *
     * @return JSON representation of the {@link RoadPoint}.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = edge().toJSON();
        json.put("frac", fraction());
        return json;
    }

    /**
     * Creates a {@link RoadPoint} object from its JSON representation.
     *
     * @param json JSON representation of the {@link RoadPoint} object.
     * @param map {@link RoadMap} as reference of the {@link RoadPoint}.
     * @return {@link RoadPoint} object.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public static RoadPoint fromJSON(JSONObject json, RoadMap map) throws JSONException {
        Road road = Road.fromJSON(json, map);
        double fraction = json.getDouble("frac");
        return new RoadPoint(road, fraction);
    }
}
