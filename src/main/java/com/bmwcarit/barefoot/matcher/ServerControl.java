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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmwcarit.barefoot.matcher.Server.DebugJSONOutputFormatter;
import com.bmwcarit.barefoot.matcher.Server.GeoJSONOutputFormatter;
import com.bmwcarit.barefoot.matcher.Server.InputFormatter;
import com.bmwcarit.barefoot.matcher.Server.OutputFormatter;
import com.bmwcarit.barefoot.matcher.Server.SlimJSONOutputFormatter;
import com.bmwcarit.barefoot.road.BaseRoad;
import com.bmwcarit.barefoot.road.BfmapReader;
import com.bmwcarit.barefoot.road.BfmapWriter;
import com.bmwcarit.barefoot.road.Configuration;
import com.bmwcarit.barefoot.road.PostGISReader;
import com.bmwcarit.barefoot.road.RoadReader;
import com.bmwcarit.barefoot.road.RoadWriter;
import com.bmwcarit.barefoot.roadmap.Road;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.roadmap.RoadPoint;
import com.bmwcarit.barefoot.roadmap.TimePriority;
import com.bmwcarit.barefoot.scheduler.StaticScheduler;
import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.spatial.SpatialOperator;
import com.bmwcarit.barefoot.topology.Cost;
import com.bmwcarit.barefoot.topology.Dijkstra;
import com.bmwcarit.barefoot.topology.Router;
import com.bmwcarit.barefoot.util.Tuple;

/**
 * Server control of stand-alone offline map matching server ({@link Server}).
 */
public abstract class ServerControl {
    private final static Logger logger = LoggerFactory.getLogger(ServerControl.class);
    private static Server matcherServer = null;
    private static Properties databaseProperties = new Properties();
    private static Properties serverProperties = new Properties();

    /**
     * Initializes stand-alone map matching server.
     *
     * @param serverFile Properties file with server parameters.
     * @param databaseFile Properties file with database connection parameters.
     * @param input {@link InputFormatter} to be used for input formatting.
     * @param output {@link OutputFormatter} to be used for output formattering.
     */
    public static void initServer(String serverFile, String databaseFile, InputFormatter input,
            OutputFormatter output) {

        logger.info("initializing server");

        try {
            logger.info("reading database properties from file {}", databaseFile);
            databaseProperties.load(new FileInputStream(databaseFile));
        } catch (FileNotFoundException e) {
            logger.error("file {} not found", databaseFile);
            System.exit(1);
        } catch (IOException e) {
            logger.error("reading database properties from file {} failed - {}", databaseFile,
                    e.getMessage());
            System.exit(1);
        }

        String host = databaseProperties.getProperty("host", "");
        int port = Integer.parseInt(databaseProperties.getProperty("port", "0"));
        String database = databaseProperties.getProperty("database");
        String table = databaseProperties.getProperty("table");
        String user = databaseProperties.getProperty("user");
        String password = databaseProperties.getProperty("password");
        String path = databaseProperties.getProperty("road-types");

        if (host == null || port == 0 || database == null || table == null || user == null
                || password == null || path == null) {
            logger.info("database property not found");
            System.exit(1);
        }


        Map<Short, Tuple<Double, Integer>> config = null;
        try {
            config = Configuration.read(path);
        } catch (JSONException | IOException e) {
            logger.error("reading road type configuration from file {} failed - {}", path,
                    e.getMessage());
            System.exit(1);
        }

        File file = new File(database + ".bfmap");
        RoadMap map = null;

        if (!file.exists()) {
            logger.info("loading map from database {} at {}:{}", database, host, port);
            RoadReader reader =
                    new PostGISReader(host, port, database, table, user, password, config);
            map = RoadMap.Load(reader);

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
        } else {
            logger.info("loading map from file {}", file.getAbsolutePath());
            map = RoadMap.Load(new BfmapReader(file.getAbsolutePath()));
        }

        Router<Road, RoadPoint> router = new Dijkstra<Road, RoadPoint>();
        Cost<Road> cost = new TimePriority();
        SpatialOperator spatial = new Geography();
        map.construct();
        Matcher matcher = new Matcher(map, router, cost, spatial);

        try {
            logger.info("initializing server settings with properties from {}", serverFile);
            serverProperties.load(new FileInputStream(serverFile));
        } catch (FileNotFoundException e) {
            logger.error("file {} not found", serverFile);
            System.exit(1);
        } catch (IOException e) {
            logger.error("reading server properties from file {} failed - {}", databaseFile,
                    e.getMessage());
            System.exit(1);
        }

        matcher.setMaxRadius(Double.parseDouble(serverProperties.getProperty("matcherMaxRadius",
                Double.toString(matcher.getMaxRadius()))));
        matcher.setMaxDistance(Double.parseDouble(serverProperties.getProperty(
                "matcherMaxDistance", Double.toString(matcher.getMaxDistance()))));
        matcher.setLambda(Double.parseDouble(serverProperties.getProperty("matcherLambda",
                Double.toString(matcher.getLambda()))));
        matcher.setSigma(Double.parseDouble(serverProperties.getProperty("matcherSigma",
                Double.toString(matcher.getSigma()))));

        matcherServer =
                new Server(
                        Integer.parseInt(serverProperties.getProperty("portNumber", "1234")),
                        Integer.parseInt(serverProperties.getProperty("maxRequestTime", "15000")),
                        Integer.parseInt(serverProperties.getProperty("maxResponseTime", "60000")),
                        Integer.parseInt(serverProperties.getProperty("maxConnectionCount", "20")),
                        Integer.parseInt(serverProperties.getProperty("numExecutorThreads", "40")),
                        Integer.parseInt(serverProperties.getProperty("matcherMinInterval", "5000")),
                        Integer.parseInt(serverProperties.getProperty("matcherMinDistance", "10")),
                        map, matcher, input, output);

        StaticScheduler.reset(Integer.parseInt(serverProperties.getProperty("matcherNumThreads",
                "8")));
    }

    /**
     * Gets {@link Server} object (singleton).
     *
     * @return {@link Server} object (singleton).
     */
    public static Server getServer() {
        return matcherServer;
    }

    /**
     * Starts/runs server.
     */
    public static void runServer() {
        logger.info("starting server on port {} with map {}",
                serverProperties.getProperty("portNumber"),
                databaseProperties.getProperty("database"));

        matcherServer.runServer();
        logger.info("server stopped");
    }

    /**
     * Stops server.
     */
    public static void stopServer() {
        logger.info("stopping server");
        if (matcherServer != null) {
            matcherServer.stopServer();
        } else {
            logger.error("stopping server failed, not yet started");
        }
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            logger.error("missing arguments\nusage: [--slimjson|--debug|--geojson] /path/to/server/properties /path/to/mapserver/properties");
            System.exit(1);
        }

        InputFormatter input = new InputFormatter();
        OutputFormatter output = new OutputFormatter();

        if (args.length > 2) {
            for (int i = 0; i < args.length - 2; ++i) {
                switch (args[i]) {
                    case "--debug":
                        output = new DebugJSONOutputFormatter();
                        break;
                    case "--slimjson":
                        output = new SlimJSONOutputFormatter();
                        break;
                    case "--geojson":
                        output = new GeoJSONOutputFormatter();
                        break;
                    default:
                        logger.warn("invalid option {} ignored", args[i]);
                        break;
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                stopServer();
            }
        });

        initServer(args[args.length - 2], args[args.length - 1], input, output);
        runServer();
    }
}
