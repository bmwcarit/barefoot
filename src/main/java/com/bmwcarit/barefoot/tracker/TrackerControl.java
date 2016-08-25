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

package com.bmwcarit.barefoot.tracker;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmwcarit.barefoot.roadmap.Loader;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.util.SourceException;

/**
 * Server control of stand-alone online map matching (tracker) server ({@link TrackerServer}).
 */
public abstract class TrackerControl {
    private final static Logger logger = LoggerFactory.getLogger(TrackerControl.class);
    private static TrackerServer trackerServer = null;
    private static Properties databaseProperties = new Properties();
    private static Properties serverProperties = new Properties();

    /**
     * Initializes stand-alone online map matching server (tracker). Server properties file must
     * include matcher, server, and tracker properties, see
     * {@link TrackerServer#TrackerServer(Properties, RoadMap)}. Database properties file must
     * include database connection properties, see {@link Loader#roadmap(Properties, boolean)}.
     *
     * @param pathServerProperties Path to server properties file.
     * @param pathDatabaseProperties Path to database properties file.
     */
    public static void initServer(String pathServerProperties, String pathDatabaseProperties) {
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
            logger.error(e.getMessage());
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

        trackerServer = new TrackerServer(serverProperties, map);
    }

    /**
     * Gets {@link TrackerServer} object (singleton).
     *
     * @return {@link TrackerServer} object (singleton).
     */
    public static TrackerServer getServer() {
        return trackerServer;
    }

    /**
     * Starts/runs server.
     */
    public static void runServer() {
        logger.info("starting server on port {} with map {}", trackerServer.getPortNumber(),
                databaseProperties.getProperty("database.name"));

        trackerServer.runServer();
        logger.info("server stopped");
    }

    /**
     * Stops server.
     */
    public static void stopServer() {
        logger.info("stopping server");
        if (trackerServer != null) {
            trackerServer.stopServer();
        } else {
            logger.error("stopping server failed, not yet started");
        }
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            logger.error(
                    "missing arguments\nusage: /path/to/server/properties /path/to/mapserver/properties");
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                stopServer();
            }
        });

        initServer(args[args.length - 2], args[args.length - 1]);
        runServer();
    }
}
