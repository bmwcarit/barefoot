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

package com.bmwcarit.barefoot.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * PostgreSQL source for connecting and querying a PostgreSQL databases.
 */
public class PostgresSource {
    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    private Connection connection = null;

    /**
     * Creates a {@link PostgresSource} object.
     *
     * @param host Host name of the database server.
     * @param port Port of the database server.
     * @param database Name of the database.
     * @param user User for accessing the database.
     * @param password Password of the user.
     */
    public PostgresSource(String host, int port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    /**
     * Checks if the database connection has been established.
     *
     * @return True if database connection is established, false otherwise.
     */
    public boolean isOpen() {
        try {
            if (connection != null && connection.isValid(5)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Connects to the database.
     *
     * @throws SourceException thrown if opening database connection failed.
     */
    public void open() throws SourceException {
        try {
            String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", password);
            // props.setProperty("ssl","true");
            connection = DriverManager.getConnection(url, props);
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new SourceException("Opening PostgreSQL connection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Closes database connection.
     *
     * @throws SourceException thrown if closing database connection failed.
     */
    public void close() throws SourceException {
        try {
            connection.close();
            connection = null;
        } catch (SQLException e) {
            throw new SourceException("Closing PostgreSQL connection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Executes a query that is specified as a query string.
     *
     * @param query Query string statement.
     * @return Result of the query as {@link ResultSet} object.
     * @throws SourceException thrown if execution of query failed.
     */
    protected ResultSet execute(String query) throws SourceException {
        ResultSet query_result = null;

        if (!isOpen()) {
            throw new SourceException("PostgreSQL connection is closed or invalid.");
        }

        try {
            Statement statement = connection.createStatement();
            statement.setFetchSize(100);
            query_result = statement.executeQuery(query);

        } catch (SQLException e) {
            throw new SourceException("Executing PostgreSQL query failed: " + e.getMessage(), e);
        }

        return query_result;
    }
}
