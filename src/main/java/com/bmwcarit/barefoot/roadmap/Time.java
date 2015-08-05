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

import com.bmwcarit.barefoot.topology.Cost;

/**
 * Time cost function for routing in {@link Road} networks.
 */
public class Time extends Cost<Road> {
    private static final float heuristic_speed = 130;
    private static final Distance distance = new Distance();

    /**
     * Gets traveling time for passing the road.
     *
     * @return Time in seconds for passing the road.
     */
    @Override
    public double cost(Road road) {
        return (distance.cost(road) * 3.6 / Math.min(road.maxspeed(), heuristic_speed));
    }
}
