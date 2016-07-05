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
package com.bmwcarit.barefoot.matcher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.esri.core.geometry.Point;

public class MatcherSampleTest {

    @Test
    public void testHeading() {
        {
            MatcherSample sample = new MatcherSample(0L, new Point(1, 1), -0.1);
            assertEquals(sample.azimuth(), 359.9, 1E-10);
        }
        {
            MatcherSample sample = new MatcherSample(0L, new Point(1, 1), -359.9);
            assertEquals(sample.azimuth(), 0.1, 1E-10);
        }
        {
            MatcherSample sample = new MatcherSample(0L, new Point(1, 1), -360.1);
            assertEquals(sample.azimuth(), 359.9, 1E-10);
        }
        {
            MatcherSample sample = new MatcherSample(0L, new Point(1, 1), 360);
            assertEquals(sample.azimuth(), 0.0, 1E-10);
        }
        {
            MatcherSample sample = new MatcherSample(0L, new Point(1, 1), 360.1);
            assertEquals(sample.azimuth(), 0.1, 1E-10);
        }
        {
            MatcherSample sample = new MatcherSample(0L, new Point(1, 1), 720.1);
            assertEquals(sample.azimuth(), 0.1, 1E-10);
        }
        {
            MatcherSample sample = new MatcherSample(0L, new Point(1, 1), -719.9);
            assertEquals(sample.azimuth(), 0.1, 1E-10);
        }
        {
            MatcherSample sample = new MatcherSample(0L, new Point(1, 1), -720.1);
            assertEquals(sample.azimuth(), 359.9, 1E-10);
        }
    }
}
