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
import java.util.Set;

import org.json.JSONException;

import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;

public class Testmap {
    private static RoadMap map = null;

    public static RoadMap instance() throws IOException, JSONException {
        if (map != null) {
            return map;
        } else {
            return (map = Loader.roadmap("config/oberbayern.properties", true).construct());
        }
    }

    // @Test
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
