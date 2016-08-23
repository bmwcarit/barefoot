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

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Measurement sample as input for Hidden Markov Model (HMM) inference, e.g. with a HMM filter
 * {@link Filter}.
 */
public class Sample {
    private long time;

    /**
     * Creates {@link Sample} object with a timestamp in milliseconds epoch time.
     *
     * @param time Timestamp of position measurement in milliseconds epoch time.
     */
    public Sample(long time) {
        this.time = time;
    }

    /**
     * Creates {@link Sample} object from JSON representation.
     *
     * @param json JSON representation of a sample.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public Sample(JSONObject json) throws JSONException {
        time = json.optLong("time", Long.MIN_VALUE);
        if (time == Long.MIN_VALUE) {
            String string = json.optString("time", "");
            if (!string.isEmpty()) {
                try {
                    time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssX")
                            .parse(json.getString("time")).getTime();
                } catch (ParseException e) {
                    throw new JSONException(e);
                }
            } else {
                throw new JSONException("time key not found");
            }
        }
    }

    /**
     * Gets the timestamp of the measurement sample in milliseconds epoch time.
     *
     * @return Timestamp of the measurement in milliseconds epoch time.
     */
    public long time() {
        return time;
    }

    /**
     * Gets a JSON representation of the {@link Sample} object.
     *
     * @return JSON representation of the {@link Sample} object.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("time", time);
        return json;
    }
}
