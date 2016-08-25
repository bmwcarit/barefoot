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

import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import org.json.JSONException;
import org.junit.Test;

import com.bmwcarit.barefoot.roadmap.Loader;
import com.esri.core.geometry.Geometry.Type;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.WktImportFlags;

public class PostGISReaderTest {

    @Test
    public void testPostGISReader() throws IOException, JSONException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("config/oberbayern.properties"));
        RoadReader reader = Loader.reader(properties);
        boolean readone = false;

        reader.open();

        while ((reader.next()) != null) {
            readone = true;
            break; // Read only first line for testing
        }

        reader.close();

        assertTrue(readone);
    }

    @Test
    public void testPolygon() throws IOException, JSONException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("config/oberbayern.properties"));
        RoadReader reader = Loader.reader(properties);
        Polygon polygon = (Polygon) GeometryEngine.geometryFromWkt(
                "POLYGON ((11.40848 47.93157, 11.45109 47.93157,11.45109 47.89296,11.40848 47.89296,11.40848 47.93157))",
                WktImportFlags.wktImportDefaults, Type.Polygon);
        BaseRoad road = null;

        reader.open(polygon, null);
        int count = 0;

        while ((road = reader.next()) != null) {
            assertTrue(
                    GeometryEngine.overlaps(polygon, road.geometry(), SpatialReference.create(4326))
                            || GeometryEngine.contains(polygon, road.geometry(),
                                    SpatialReference.create(4326)));
            count += 1;
        }

        reader.close();
        assertTrue(count > 0);
    }

    @Test
    public void testExclusion() throws IOException, JSONException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("config/oberbayern.properties"));
        RoadReader reader = Loader.reader(properties);
        Polygon polygon = (Polygon) GeometryEngine.geometryFromWkt(
                "POLYGON ((11.40848 47.93157, 11.45109 47.93157,11.45109 47.89296,11.40848 47.89296,11.40848 47.93157))",
                WktImportFlags.wktImportDefaults, Type.Polygon);
        HashSet<Short> exclusion = new HashSet<>(Arrays.asList((short) 117));
        BaseRoad road = null;

        reader.open(polygon, exclusion);
        int count = 0;

        while ((road = reader.next()) != null) {
            assertTrue(
                    GeometryEngine.overlaps(polygon, road.geometry(), SpatialReference.create(4326))
                            || GeometryEngine.contains(polygon, road.geometry(),
                                    SpatialReference.create(4326)));
            assertTrue(!exclusion.contains(road.type()));
            count += 1;
        }

        reader.close();
        assertTrue(count > 0);
    }
}
