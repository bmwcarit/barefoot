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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashSet;

import org.json.JSONException;
import org.junit.Test;

import com.bmwcarit.barefoot.roadmap.Testmap;
import com.bmwcarit.barefoot.roadmap.Road;
import com.bmwcarit.barefoot.roadmap.RoadMap;

public class BfmapReaderTest {

    @Test
    public void testBfmapReader() throws IOException, JSONException {
        {
            RoadWriter writer = new BfmapWriter(
                    BfmapReaderTest.class.getResource("").getPath() + "oberbayern.bfmap.test");
            RoadReader reader = Testmap.instance().reader();
            BaseRoad road = null;

            writer.open();
            reader.open();
            while ((road = reader.next()) != null) {
                writer.write(road);
            }
            reader.close();
            writer.close();
        }

        HashSet<Long> set = new HashSet<>();

        {
            RoadMap map = Testmap.instance();
            RoadReader reader = new BfmapReader(
                    BfmapReaderTest.class.getResource("oberbayern.bfmap.test").getPath());
            BaseRoad road = null;

            reader.open();
            while ((road = reader.next()) != null) {
                Road other = map.get(road.id() * 2);

                if (other == null) {
                    fail();
                }

                assertTrue(road.source() == other.source());
                assertTrue(road.target() == other.target());
                assertTrue(road.refid() == other.base().refid());

                if (set.contains(road.id())) {
                    fail();
                } else {
                    set.add(road.id());
                }
            }
            reader.close();
        }

        {
            RoadReader reader = Testmap.instance().reader();
            BaseRoad road = null;

            reader.open();
            while ((road = reader.next()) != null) {
                if (!set.contains(road.id())) {
                    fail();
                }
            }
            reader.close();
        }
    }
}
