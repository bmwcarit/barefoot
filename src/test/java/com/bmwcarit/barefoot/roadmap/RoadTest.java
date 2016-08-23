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

import static org.junit.Assert.assertEquals;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import com.bmwcarit.barefoot.road.BaseRoad;
import com.bmwcarit.barefoot.road.Heading;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WktImportFlags;

public class RoadTest {

    @Test
    public void testInvert() {
        {
            String wkt = "LINESTRING(11.3136273 48.0972002)";
            Polyline line = (Polyline) GeometryEngine.geometryFromWkt(wkt,
                    WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);

            Polyline invert = Road.invert(line);

            assertEquals(line.getPointCount(), invert.getPointCount());

            for (int i = 0; i < line.getPointCount(); ++i) {
                Point p1 = line.getPoint(i);
                Point p2 = invert.getPoint(invert.getPointCount() - (i + 1));

                assertEquals(p1.getX(), p2.getX(), 1E-6);
                assertEquals(p1.getY(), p2.getY(), 1E-6);
            }
        }
        {
            String wkt = "LINESTRING(11.3136273 48.0972002,11.3138846 48.0972999)";
            Polyline line = (Polyline) GeometryEngine.geometryFromWkt(wkt,
                    WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);

            Polyline invert = Road.invert(line);

            assertEquals(line.getPointCount(), invert.getPointCount());

            for (int i = 0; i < line.getPointCount(); ++i) {
                Point p1 = line.getPoint(i);
                Point p2 = invert.getPoint(invert.getPointCount() - (i + 1));

                assertEquals(p1.getX(), p2.getX(), 1E-6);
                assertEquals(p1.getY(), p2.getY(), 1E-6);
            }
        }
        {
            String wkt =
                    "LINESTRING(11.3136273 48.0972002,11.3138846 48.0972999,11.3144345 48.097396, "
                            + "11.315083 48.0974541,11.3160925 48.0975102,11.3164787 48.0974529,"
                            + "11.3166131 48.0973939,11.31675 48.0972933,11.3168554 48.0971529,"
                            + "11.3168846 48.0969582,11.3167847 48.0967698,11.3166735 48.0966731,"
                            + "11.316501 48.096578,11.316015 48.0964988,11.3153612 48.0964801,"
                            + "11.3141303 48.0965022)";
            Polyline line = (Polyline) GeometryEngine.geometryFromWkt(wkt,
                    WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);

            Polyline invert = Road.invert(line);

            assertEquals(line.getPointCount(), invert.getPointCount());

            for (int i = 0; i < line.getPointCount(); ++i) {
                Point p1 = line.getPoint(i);
                Point p2 = invert.getPoint(invert.getPointCount() - (i + 1));

                assertEquals(p1.getX(), p2.getX(), 1E-6);
                assertEquals(p1.getY(), p2.getY(), 1E-6);
            }
        }
    }

    @Test
    public void testJSON() throws JSONException {
        String wkt = "LINESTRING(11.3136273 48.0972002,11.3138846 48.0972999)";
        BaseRoad osm = new BaseRoad(0L, 1L, 2L, 4L, true, (short) 5, 5.1F, 6.1F, 6.2F, 7.1F,
                (Polyline) GeometryEngine.geometryFromWkt(wkt, WktImportFlags.wktImportDefaults,
                        Geometry.Type.Polyline));

        Road road = new Road(osm, Heading.forward);
        RoadMap map = new RoadMap();
        map.add(road);

        String json = road.toJSON().toString();
        Road road2 = Road.fromJSON(new JSONObject(json), map);

        assertEquals(road, road2);
    }
}
