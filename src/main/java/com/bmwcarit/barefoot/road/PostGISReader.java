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

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.spatial.SpatialOperator;
import com.bmwcarit.barefoot.util.PostgresSource;
import com.bmwcarit.barefoot.util.SourceException;
import com.bmwcarit.barefoot.util.Tuple;
import com.esri.core.geometry.Geometry.Type;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.OperatorImportFromWkb;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WkbImportFlags;
import com.esri.core.geometry.WktExportFlags;

/**
 * A {@link RoadReader} for reading {@link BaseRoad} objects from PostgreSQL/PostGIS database.
 */
public class PostGISReader extends PostgresSource implements RoadReader {
    private static Logger logger = LoggerFactory.getLogger(PostGISReader.class);
    private final static SpatialOperator spatial = new Geography();
    private final String table;
    private final Map<Short, Tuple<Double, Integer>> config;
    private HashSet<Short> exclusions = null;
    private Polygon polygon = null;
    private ResultSet result_set = null;

    /**
     * Constructs {@link PostGISReader} object.
     *
     * @param host Hostname of the database server.
     * @param port Port number of the database server.
     * @param database Name of the database.
     * @param table Name of the table.
     * @param user User for accessing the database.
     * @param password Password of the user.
     * @param config Road type configuration.
     */
    public PostGISReader(String host, int port, String database, String table, String user,
            String password, Map<Short, Tuple<Double, Integer>> config) {
        super(host, port, database, user, password);
        this.table = table;
        this.config = config;
    }

    @Override
    public void open() throws SourceException {
        logger.info("open reader (standard)");
        open(null, null);
    }

    @Override
    public void open(Polygon polygon, HashSet<Short> exclusions) throws SourceException {
        logger.info("open reader (parameterized)");
        super.open();
        this.exclusions = exclusions;
        this.polygon = polygon;
    }

    @Override
    public void close() throws SourceException {
        logger.info("close reader");
        result_set = null;
        super.close();
    }

    @Override
    public BaseRoad next() throws SourceException {
        if (result_set == null) {

            String where = new String();
            if (polygon != null || exclusions != null) {
                where += " WHERE";
                if (polygon != null) {
                    String wkt =
                            GeometryEngine.geometryToWkt(polygon, WktExportFlags.wktExportPolygon);

                    logger.trace("query polygon contains/overlaps {}", wkt);

                    where += " (ST_Contains(ST_GeomFromText('" + wkt
                            + "', 4326),geom) OR ST_Overlaps(ST_GeomFromText('" + wkt
                            + "', 4326),geom))";
                }

                if (polygon != null && exclusions != null) {
                    where += " AND";
                }

                if (exclusions != null) {
                    Short[] myexclusions = new Short[exclusions.size()];
                    exclusions.toArray(myexclusions);
                    String ids = " class_id != " + myexclusions[0];
                    for (int i = 1; i < myexclusions.length; ++i) {
                        ids += " AND class_id != " + myexclusions[i];
                    }
                    logger.trace("query exclusions {}", ids);
                    where += ids;
                }
            }

            String query = "SELECT gid,osm_id,class_id,source,target,"
                    + "length,reverse,maxspeed_forward,maxspeed_backward,"
                    + "priority, ST_AsBinary(geom) as geom FROM " + table + where + ";";

            logger.info("execute query");
            logger.trace("query string: {}", query);

            result_set = execute(query);
        }

        try {
            BaseRoad road = null;

            do {
                if (!result_set.next()) {
                    return null;
                }

                long gid = Long.parseLong(result_set.getString(1));
                long osmId = Long.parseLong(result_set.getString(2));
                short classId = Short.parseShort(result_set.getString(3));
                if (!config.containsKey(classId)) {
                    continue;
                }
                long source = Long.parseLong(result_set.getString(4));
                long target = Long.parseLong(result_set.getString(5));
                // double length = Double.parseDouble(result_set.getString(6)) *
                // 1000;
                double reverse = Double.parseDouble(result_set.getString(7)) * 1000;
                int maxspeedForward = result_set.getObject(8) == null ? config.get(classId).two()
                        : Integer.parseInt(result_set.getString(8));
                int maxspeedBackward = result_set.getObject(9) == null ? config.get(classId).two()
                        : Integer.parseInt(result_set.getString(9));
                float priority = (float) config.get(classId).one().doubleValue();
                // float priority = Float.parseFloat(result_set.getString(10));
                byte[] wkb = result_set.getBytes(11);
                Polyline geometry = (Polyline) OperatorImportFromWkb.local().execute(
                        WkbImportFlags.wkbImportDefaults, Type.Polyline, ByteBuffer.wrap(wkb),
                        null);
                float length = (float) spatial.length(geometry);

                road = new BaseRoad(gid, source, target, osmId, reverse >= 0 ? false : true,
                        classId, priority, maxspeedForward, maxspeedBackward, length, wkb);
            } while (exclusions != null && exclusions.contains(road.type()));

            return road;
        } catch (SQLException e) {
            throw new SourceException("Reading query result failed: " + e.getMessage());
        }
    }
}
