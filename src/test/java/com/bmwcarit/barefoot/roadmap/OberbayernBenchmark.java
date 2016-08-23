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

package com.bmwcarit.barefoot.roadmap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.json.JSONException;
import org.junit.Test;

import com.bmwcarit.barefoot.road.BaseRoad;
import com.bmwcarit.barefoot.road.Configuration;
import com.bmwcarit.barefoot.road.PostGISReader;
import com.bmwcarit.barefoot.road.PostGISReaderTest;
import com.bmwcarit.barefoot.road.RoadReader;
import com.bmwcarit.barefoot.util.SourceException;
import com.bmwcarit.barefoot.util.Tuple;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;

public class OberbayernBenchmark {
    private static RoadMap map = null;

    public static class MapPostGISReader implements RoadReader {
        private final RoadReader reader;

        public MapPostGISReader() throws IOException, JSONException {
            Properties props = new Properties();
            props.load(PostGISReaderTest.class.getResourceAsStream("oberbayern.db.properties"));

            String host = props.getProperty("host");
            int port = Integer.parseInt(props.getProperty("port"));
            String database = props.getProperty("database");
            String table = props.getProperty("table");
            String user = props.getProperty("user");
            String password = props.getProperty("password");
            String path = props.getProperty("road-types");

            Map<Short, Tuple<Double, Integer>> config = Configuration.read(path);

            reader = new PostGISReader(host, port, database, table, user, password, config);
        }

        @Override
        public boolean isOpen() {
            return reader.isOpen();
        }

        @Override
        public void open() throws SourceException {
            reader.open();
        }

        @Override
        public void open(Polygon polygon, HashSet<Short> exclusion) throws SourceException {
            reader.open(polygon, exclusion);
        }

        @Override
        public void close() throws SourceException {
            reader.close();
        }

        @Override
        public BaseRoad next() throws SourceException {
            return reader.next();
        }

    }

    public static RoadMap instance() throws IOException, JSONException {
        if (map != null) {
            return map;
        }

        RoadReader reader = new MapPostGISReader();
        map = RoadMap.Load(reader);
        map.construct();

        return map;
    }

    @Test
    public void testRadiusSearch() throws IOException, JSONException {
        RoadMap map = instance();

        Point c = new Point(11.550474464893341, 48.034123185269095);
        double r = 50;
        Set<RoadPoint> points = map.spatial().radius(c, r);

        for (RoadPoint point : points) {
            System.out.println(GeometryEngine.geometryToGeoJson(point.geometry()));
            System.out.println(GeometryEngine.geometryToGeoJson(point.edge().geometry()));
        }
    }
}
