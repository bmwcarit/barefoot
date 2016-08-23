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

import com.esri.core.geometry.Geometry.Type;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.WktExportFlags;
import com.esri.core.geometry.WktImportFlags;

/**
 * Measurement sample for Hidden Markov Model (HMM) map matching which is a position measurement,
 * e.g. measured with a GPS device.
 */
public class MatcherSample extends com.bmwcarit.barefoot.markov.Sample {
    private final String id;
    private final Point point;
    private final double azimuth;

    /**
     * Creates a {@link MatcherSample} object with measured position and time of measurement.
     *
     * @param time Time of measurement in milliseconds epoch time.
     * @param point Point of measured position.
     */
    public MatcherSample(long time, Point point) {
        this("", time, point);
    }

    /**
     * Creates a {@link MatcherSample} object with measured position, time of measurement, and
     * azimuth.
     *
     * @param time Time of measurement in milliseconds epoch time.
     * @param point Point of measured position.
     * @param azimuth Azimuth of measurement sample.
     */
    public MatcherSample(long time, Point point, double azimuth) {
        this("", time, point, azimuth);
    }

    /**
     * Creates a {@link MatcherSample} object with an identifier, measured position and time of
     * measurement.
     *
     * @param id Identifier of sample.
     * @param time Time of measurement in milliseconds epoch time.
     * @param point Point of measured position.
     */
    public MatcherSample(String id, long time, Point point) {
        this(id, time, point, Double.NaN);
    }

    /**
     * Creates a {@link MatcherSample} object with an identifier, measured position, time of
     * measurement, and azimuth.
     *
     * @param id Identifier of sample.
     * @param time Time of measurement in milliseconds epoch time.
     * @param point Point of measured position.
     * @param azimuth Azimuth of measurement sample.
     */
    public MatcherSample(String id, long time, Point point, double azimuth) {
        super(time);
        this.id = id;
        this.point = point;
        this.azimuth = norm(azimuth);
    }

    /**
     * Creates a {@link MatcherSample} object from its JSON representation.
     *
     * @param json JSON representation of {@link MatcherSample} object. JSONException thrown on JSON
     *        extraction or parsing error.
     * @throws JSONException thrown on JSON parse error.
     */
    public MatcherSample(JSONObject json) throws JSONException {
        super(json);
        id = json.getString("id");
        String wkt = json.getString("point");
        point = (Point) GeometryEngine.geometryFromWkt(wkt, WktImportFlags.wktImportDefaults,
                Type.Point);
        if (json.has("azimuth")) {
            azimuth = norm(json.getDouble("azimuth"));
        } else {
            azimuth = Double.NaN;
        }
    }

    private static double norm(double azimuth) {
        return azimuth >= 360 ? azimuth - (360 * (int) (azimuth / 360))
                : azimuth < 0 ? azimuth - (360 * ((int) (azimuth / 360) - 1)) : azimuth;
    }

    /**
     * Gets identifier of the sample.
     *
     * @return Identifier of the sample.
     */
    public String id() {
        return id;
    }

    /**
     * Gets point of position measurement.
     *
     * @return Point of measure position.
     */
    public Point point() {
        return point;
    }

    /**
     * Gets (optional) azimuth of measurement sample.
     *
     * @return Azimuth of measurement sample, or Double.NaN if not included.
     */
    public double azimuth() {
        return azimuth;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put("id", id);
        json.put("point", GeometryEngine.geometryToWkt(point, WktExportFlags.wktExportPoint));
        if (!Double.isNaN(azimuth)) {
            json.put("azimuth", azimuth);
        }
        return json;
    }
}
