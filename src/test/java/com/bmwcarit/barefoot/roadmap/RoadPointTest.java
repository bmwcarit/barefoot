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
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WktImportFlags;

public class RoadPointTest {

    @Test
    public void testJSON() throws JSONException {
        String wkt = "LINESTRING(11.3136273 48.0972002,11.3138846 48.0972999)";
        BaseRoad osm = new BaseRoad(0L, 1L, 2L, 4L, true, (short) 5, 5.1F, 6.1F, 6.2F, 7.1F,
                (Polyline) GeometryEngine.geometryFromWkt(wkt, WktImportFlags.wktImportDefaults,
                        Geometry.Type.Polyline));

        RoadMap map = new RoadMap();
        map.add(new Road(osm, Heading.forward));

        RoadPoint point1 = new RoadPoint(map.get(0L), 0.2);

        String json = point1.toJSON().toString();
        RoadPoint point2 = RoadPoint.fromJSON(new JSONObject(json), map);

        assertEquals(point1.edge().id(), point2.edge().id());
        assertEquals(point1.fraction(), point2.fraction(), 1E-6);
        assertEquals(point1.edge().source(), point2.edge().source());
        assertEquals(point1.edge().target(), point2.edge().target());
    }
}
