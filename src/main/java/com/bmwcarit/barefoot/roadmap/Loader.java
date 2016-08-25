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
package com.bmwcarit.barefoot.roadmap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmwcarit.barefoot.road.BaseRoad;
import com.bmwcarit.barefoot.road.BfmapReader;
import com.bmwcarit.barefoot.road.BfmapWriter;
import com.bmwcarit.barefoot.road.PostGISReader;
import com.bmwcarit.barefoot.road.RoadReader;
import com.bmwcarit.barefoot.road.RoadWriter;
import com.bmwcarit.barefoot.util.SourceException;
import com.bmwcarit.barefoot.util.Tuple;

/**
 * Standard map loader that loads road map from database connection or file buffer.
 */
public class Loader {
    private static Logger logger = LoggerFactory.getLogger(Loader.class);

    /**
     * Loads {@link RoadMap} object from database (or file buffer, if set to true) using database
     * connection parameters provided with the properties. For details on properties, see
     * {@link Loader#roadmap(Properties, boolean)}.
     *
     * @param propertiesPath Path to properties file.
     * @param buffer Indicates if map shall be read from file buffer and written to file buffer.
     * @return {@link RoadMap} read from source. (Note: It is not yet constructed!)
     * @throws SourceException thrown if reading properties, road types or road map data fails.
     * @throws IOException thrown if opening properties file fails.
     */
    public static RoadMap roadmap(String propertiesPath, boolean buffer)
            throws SourceException, IOException {
        InputStream is = new FileInputStream(propertiesPath);
        Properties props = new Properties();
        props.load(is);
        is.close();
        return roadmap(props, buffer);
    }

    /**
     * Loads {@link RoadMap} object from database (or file buffer, if set to true) using database
     * connection parameters provided with the following properties:
     * <ul>
     * <li>database.host (e.g. localhost)</li>
     * <li>database.port (e.g. 5432)</li>
     * <li>database.name (e.g. barefoot-oberbayern)</li>
     * <li>database.table (e.g. bfmap_ways)</li>
     * <li>database.user (e.g. osmuser)</li>
     * <li>database.password</li>
     * <li>database.road-types (e.g. /path/to/road-types.json)</li>
     * </ul>
     *
     * @param properties {@link Properties} object with database connection parameters.
     * @param buffer Indicates if map shall be read from file buffer and written to file buffer.
     * @return {@link RoadMap} read from source. (Note: It is not yet constructed!)
     * @throws SourceException thrown if reading properties, road types or road map data fails.
     */
    public static RoadMap roadmap(Properties properties, boolean buffer) throws SourceException {
        String database = properties.getProperty("database.name");
        if (database == null) {
            throw new SourceException("could not read database properties");
        }

        File file = new File(database + ".bfmap");
        RoadMap map = null;

        if (!file.exists() || !buffer) {
            logger.info("load map from database {}", database);
            RoadReader reader = reader(properties);
            map = RoadMap.Load(reader);

            if (buffer) {
                reader = map.reader();
                RoadWriter writer = new BfmapWriter(file.getAbsolutePath());
                BaseRoad road = null;
                reader.open();
                writer.open();

                while ((road = reader.next()) != null) {
                    writer.write(road);
                }

                writer.close();
                reader.close();
            }
        } else {
            logger.info("load map from file {}", file.getAbsolutePath());
            map = RoadMap.Load(new BfmapReader(file.getAbsolutePath()));
        }

        return map;
    }

    /**
     * Reads road type configuration from file.
     *
     * @param path Path of the road type configuration file.
     * @return Mapping of road class identifiers to priority factor and default maximum speed.
     * @throws JSONException thrown on JSON extraction or parsing error.
     * @throws IOException thrown on file reading error.
     */
    public static Map<Short, Tuple<Double, Integer>> read(String path)
            throws JSONException, IOException {
        BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        String line = null, json = new String();
        while ((line = file.readLine()) != null) {
            json += line;
        }
        file.close();

        return roadtypes(new JSONObject(json));
    }

    /**
     * Reads road type configuration from JSON representation.
     *
     * @param jsonconfig JSON representation of the road type configuration.
     * @return Mapping of road class identifiers to priority factor and default maximum speed.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public static Map<Short, Tuple<Double, Integer>> roadtypes(JSONObject jsonconfig)
            throws JSONException {

        Map<Short, Tuple<Double, Integer>> config = new HashMap<>();

        JSONArray jsontags = jsonconfig.getJSONArray("tags");
        for (int i = 0; i < jsontags.length(); ++i) {
            JSONObject jsontag = jsontags.getJSONObject(i);
            JSONArray jsonvalues = jsontag.getJSONArray("values");
            for (int j = 0; j < jsonvalues.length(); ++j) {
                JSONObject jsonvalue = jsonvalues.getJSONObject(j);
                config.put((short) jsonvalue.getInt("id"),
                        new Tuple<>(jsonvalue.getDouble("priority"), jsonvalue.getInt("maxspeed")));
            }
        }

        return config;
    }

    public static PostGISReader reader(Properties properties) {
        String host = properties.getProperty("database.host");
        int port = Integer.parseInt(properties.getProperty("database.port", "0"));
        String database = properties.getProperty("database.name");
        String table = properties.getProperty("database.table");
        String user = properties.getProperty("database.user");
        String password = properties.getProperty("database.password");
        String path = properties.getProperty("database.road-types");

        if (host == null || port == 0 || database == null || table == null || user == null
                || password == null || path == null) {
            throw new SourceException("could not read database properties");
        }

        logger.info("open road reader for database {} at {}:{}", database, host, port);

        logger.info("database.host={}", host);
        logger.info("database.port={}", port);
        logger.info("database.name={}", database);
        logger.info("database.table={}", table);
        logger.info("database.user={}", user);
        logger.info("database.road-types={}", path);

        Map<Short, Tuple<Double, Integer>> config = null;
        try {
            config = read(path);
        } catch (JSONException | IOException e) {
            throw new SourceException("could not read road types from file " + path);
        }

        return new PostGISReader(host, port, database, table, user, password, config);
    }
}
