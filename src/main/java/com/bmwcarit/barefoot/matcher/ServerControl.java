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

        String host = "", database = "", table = "", user = "", password = "", path = "";
        int port = 0;

        try {
            logger.info("initializing database settings with properties from {}", databaseFile);
            databaseProperties.load(new FileInputStream(databaseFile));

            host = databaseProperties.getProperty("host");
            port = Integer.parseInt(databaseProperties.getProperty("port"));
            database = databaseProperties.getProperty("database");
            table = databaseProperties.getProperty("table");
            user = databaseProperties.getProperty("user");
            password = databaseProperties.getProperty("password");
            path = databaseProperties.getProperty("road-types");
        } catch (FileNotFoundException e) {
            logger.error("properties file {} not found", databaseFile);
            System.exit(1);
        } catch (IOException e) {
            logger.error("reading file {} - {}", databaseFile, e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            logger.error("reading database properties failed {}", e.getMessage());
            System.exit(1);
        }

        Map<Short, Tuple<Double, Integer>> config;
        try {
            config = Configuration.read(path);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

        Router<Road, RoadPoint> router = new Dijkstra<Road, RoadPoint>();
        Cost<Road> cost = new TimePriority();
        SpatialOperator spatial = new Geography();

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

        map.construct();

        int portNumber = 0, maxRequestTime = 0, maxResponseTime = 0, maxConnectionCount = 0, numExecutorThreads =
                0, matcherNumThreads = 0, matcherMinInterval = 0;
        double matcherMaxRadius = 0, matcherMaxDistance = 0, matcherLambda = 0, matcherSigma = 0;

        try {
            logger.info("initializing server settings with properties from {}", serverFile);
            serverProperties.load(new FileInputStream(serverFile));

            matcherMaxRadius = Double.parseDouble(serverProperties.getProperty("matcherMaxRadius"));
            matcherMaxDistance =
                    Double.parseDouble(serverProperties.getProperty("matcherMaxDistance"));
            matcherLambda = Double.parseDouble(serverProperties.getProperty("matcherLambda"));
            matcherSigma = Double.parseDouble(serverProperties.getProperty("matcherSigma"));
            matcherMinInterval =
                    Integer.parseInt(serverProperties.getProperty("matcherMinInterval"));
            matcherNumThreads = Integer.parseInt(serverProperties.getProperty("matcherNumThreads"));
            portNumber = Integer.parseInt(serverProperties.getProperty("portNumber"));
            maxRequestTime = Integer.parseInt(serverProperties.getProperty("maxRequestTime"));
            maxResponseTime = Integer.parseInt(serverProperties.getProperty("maxResponseTime"));
            maxConnectionCount =
                    Integer.parseInt(serverProperties.getProperty("maxConnectionCount"));
            numExecutorThreads =
                    Integer.parseInt(serverProperties.getProperty("numExecutorThreads"));

        } catch (FileNotFoundException e) {
            logger.error("file {} not found", serverFile);
            System.exit(1);
        } catch (IOException e) {
            logger.error("reading file {} - {}", serverFile, e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            logger.error("reading server properties failed {}", e.getMessage());
            System.exit(1);
        }

        Matcher matcher = new Matcher(map, router, cost, spatial);
        matcher.setMaxRadius(matcherMaxRadius);
        matcher.setMaxDistance(matcherMaxDistance);
        matcher.setLambda(matcherLambda);
        matcher.setSigma(matcherSigma);
        StaticScheduler.reset(matcherNumThreads);

        matcherServer =
                new Server(portNumber, maxRequestTime, maxResponseTime, maxConnectionCount,
                        numExecutorThreads, matcherMinInterval, map, matcher, input, output);
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
                if (args[i].compareTo("--debug") == 0) {
                    output = new DebugJSONOutputFormatter();
                }

                if (args[i].compareTo("--slimjson") == 0) {
                    output = new SlimJSONOutputFormatter();
                }

                if (args[i].compareTo("--geojson") == 0) {
                    output = new GeoJSONOutputFormatter();
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
