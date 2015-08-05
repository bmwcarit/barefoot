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

package com.bmwcarit.barefoot.road;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bmwcarit.barefoot.util.Tuple;

/**
 * Reader for road type configurations which provides a mapping of road class identifiers to
 * priority factor and default maximum speed.
 */
public class Configuration {

    /**
     * Reads road type configuration from file.
     *
     * @param path Path of the road type configuration file.
     * @return Mapping of road class identifiers to priority factor and default maximum speed.
     * @throws JSONException thrown on JSON extraction or parsing error.
     * @throws IOException thrown on file reading error.
     */
    public static Map<Short, Tuple<Double, Integer>> read(String path) throws JSONException,
            IOException {
        BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        String line = null, json = new String();
        while ((line = file.readLine()) != null) {
            json += line;
        }
        file.close();

        return read(new JSONObject(json));
    }

    /**
     * Reads road type configuration from JSON representation.
     *
     * @param jsonconfig JSON representation of the road type configuration.
     * @return Mapping of road class identifiers to priority factor and default maximum speed.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public static Map<Short, Tuple<Double, Integer>> read(JSONObject jsonconfig)
            throws JSONException {

        Map<Short, Tuple<Double, Integer>> config = new HashMap<Short, Tuple<Double, Integer>>();

        JSONArray jsontags = jsonconfig.getJSONArray("tags");
        for (int i = 0; i < jsontags.length(); ++i) {
            JSONObject jsontag = jsontags.getJSONObject(i);
            JSONArray jsonvalues = jsontag.getJSONArray("values");
            for (int j = 0; j < jsonvalues.length(); ++j) {
                JSONObject jsonvalue = jsonvalues.getJSONObject(j);
                config.put(
                        (short) jsonvalue.getInt("id"),
                        new Tuple<Double, Integer>(jsonvalue.getDouble("priority"), jsonvalue
                                .getInt("maxspeed")));
            }
        }

        return config;
    }
}
