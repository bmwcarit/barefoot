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
package com.bmwcarit.barefoot.spatial;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import com.bmwcarit.barefoot.roadmap.Loader;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.roadmap.RoadPoint;
import com.bmwcarit.barefoot.util.SourceException;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;

public class SpatialExample {

    @Test
    public void test() throws SourceException, IOException {
        // Load and construct road map
        RoadMap map = Loader.roadmap("oberbayern.properties", true).construct();

        Point c = new Point(11.550474464893341, 48.034123185269095);
        double r = 50; // radius search within 50 meters
        Set<RoadPoint> points = map.spatial().radius(c, r);

        for (RoadPoint point : points) {
            GeometryEngine.geometryToGeoJson(point.geometry());
            GeometryEngine.geometryToGeoJson(point.edge().geometry());
        }
    }

}
