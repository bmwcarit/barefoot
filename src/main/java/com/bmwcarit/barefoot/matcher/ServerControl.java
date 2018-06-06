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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmwcarit.barefoot.matcher.MatcherServer.DebugJSONOutputFormatter;
import com.bmwcarit.barefoot.matcher.MatcherServer.GeoJSONOutputFormatter;
import com.bmwcarit.barefoot.matcher.MatcherServer.InputFormatter;
import com.bmwcarit.barefoot.matcher.MatcherServer.OutputFormatter;
import com.bmwcarit.barefoot.matcher.MatcherServer.SlimJSONOutputFormatter;
import com.bmwcarit.barefoot.roadmap.Loader;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.util.SourceException;

/**
 * Server control of stand-alone offline map matching server ({@link MatcherServer}).
 */
public abstract class ServerControl {
    private final static Logger logger = LoggerFactory.getLogger(ServerControl.class);
    private static MatcherServer matcherServer = null;
    private static Properties databaseProperties = new Properties();
    private static Properties serverProperties = new Properties();

    /**
     * Initializes stand-alone offline map matching server. Server properties file must include
     * matcher and server properties, see
     * {@link MatcherServer#MatcherServer(Properties, RoadMap, InputFormatter, OutputFormatter)}.
     * Database properties file must include database connection properties, see
     * {@link Loader#roadmap(Properties, boolean)}.
     *
     * @param pathServerProperties Path to server properties file.
     * @param pathDatabaseProperties Path to database properties file.
     * @param input {@link InputFormatter} to be used for input formatting.
     * @param output {@link OutputFormatter} to be used for output formatting.
     */
    public static void initServer(String pathServerProperties, String pathDatabaseProperties,
            InputFormatter input, OutputFormatter output) {
        logger.info("initialize server");

        try {
            logger.info("read database properties from file {}", pathDatabaseProperties);
            databaseProperties.load(new FileInputStream(pathDatabaseProperties));
        } catch (FileNotFoundException e) {
            logger.error("file {} not found", pathDatabaseProperties);
            System.exit(1);
        } catch (IOException e) {
            logger.error("reading database properties from file {} failed: {}",
                    pathDatabaseProperties, e.getMessage());
            System.exit(1);
        }

        RoadMap map = null;
        try {
            map = Loader.roadmap(databaseProperties, true);
        } catch (SourceException e) {
            logger.error("loading map failed:", e);
            System.exit(1);
        }
        map.construct();

        try {
            logger.info("read tracker properties from file {}", pathServerProperties);
            serverProperties.load(new FileInputStream(pathServerProperties));
        } catch (FileNotFoundException e) {
            logger.error("file {} not found", pathServerProperties);
            System.exit(1);
        } catch (IOException e) {
            logger.error("reading tracker properties from file {} failed: {}",
                    pathDatabaseProperties, e.getMessage());
            System.exit(1);
        }

        matcherServer = new MatcherServer(serverProperties, map, input, output);
    }

    /**
     * Gets {@link MatcherServer} object (singleton).
     *
     * @return {@link MatcherServer} object (singleton).
     */
    public static MatcherServer getServer() {
        return matcherServer;
    }

    /**
     * Starts/runs server.
     */
    public static void runServer() {
        logger.info("starting server on port {} with map {}", matcherServer.getPortNumber(),
                databaseProperties.getProperty("database.name"));

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
            logger.error(
                    "missing arguments\nusage: [--slimjson|--debug|--geojson] /path/to/server/properties /path/to/mapserver/properties");
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
