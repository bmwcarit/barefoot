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

package com.bmwcarit.barefoot.matcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.bmwcarit.barefoot.roadmap.Road;
import com.bmwcarit.barefoot.roadmap.RoadPoint;

/**
 * Minimizes a set of matching candidates represented as {@link RoadPoint} to remove semantically
 * redundant candidates.
 */
public abstract class Minset {
    /**
     * Floating point precision for considering a {@link RoadPoint} be the same as a vertex,
     * fraction is zero or one (default: 1E-8).
     */
    public static double precision = 1E-8;

    private static double round(double value) {
        return Math.round(value / precision) * precision;
    }

    /**
     * Removes semantically redundant matching candidates from a set of matching candidates (as
     * {@link RoadPoint} object) and returns a minimized (reduced) subset.
     * <p>
     * Given a position measurement, a matching candidate is each road in a certain radius of the
     * measured position, and in particular that point on each road that is closest to the measured
     * position. Hence, there are as many state candidates as roads in that area. The idea is to
     * conserve only possible routes through the area and use each route with its closest point to
     * the measured position as a matching candidate. Since roads are split into multiple segments,
     * the number of matching candidates is significantly higher than the respective number of
     * routes. To give an example, assume the following matching candidates as {@link RoadPoint}
     * objects with a road id and a fraction:
     *
     * <ul>
     * <li><i>(r<sub>i</sub>, 0.5)</i>
     * <li><i>(r<sub>j</sub>, 0.0)</i>
     * <li><i>(r<sub>k</sub>, 0.0)</i>
     * </ul>
     *
     * where they are connected as <i>r<sub>i</sub> &#8594; r<sub>j</sub></i> and <i>r<sub>i</sub>
     * &#8594; r<sub>k</sub></i>. Here, matching candidates <i>r<sub>j</sub></i> and
     * <i>r<sub>k</sub></i> can be removed if we see routes as matching candidates. This is because
     * both, <i>r<sub>j</sub></i> and <i>r<sub>k</sub></i>, are reachable from <i>r<sub>i</sub></i>.
     * <p>
     * <b>Note:</b> Of course, <i>r<sub>j</sub></i> and <i>r<sub>k</sub></i> may be seen as relevant
     * matching candidates, however, in the present HMM map matching algorithm there is no
     * optimization of matching candidates along the road, instead it only considers the closest
     * point of a road as a matching candidate.
     *
     * @param candidates Set of matching candidates as {@link RoadPoint} objects.
     * @return Minimized (reduced) set of matching candidates as {@link RoadPoint} objects.
     */
    public static Set<RoadPoint> minimize(Set<RoadPoint> candidates) {

        HashMap<Long, RoadPoint> map = new HashMap<>();
        HashMap<Long, Integer> misses = new HashMap<>();
        HashSet<Long> removes = new HashSet<>();

        for (RoadPoint candidate : candidates) {
            map.put(candidate.edge().id(), candidate);
            misses.put(candidate.edge().id(), 0);
        }

        for (RoadPoint candidate : candidates) {
            Iterator<Road> successors = candidate.edge().successors();
            Long id = candidate.edge().id();

            while (successors.hasNext()) {
                Road successor = successors.next();

                if (!map.containsKey(successor.id())) {
                    misses.put(id, misses.get(id) + 1);
                }

                if (map.containsKey(successor.id())
                        && round(map.get(successor.id()).fraction()) == 0) {
                    removes.add(successor.id());
                    misses.put(id, misses.get(id) + 1);
                }
            }
        }

        for (RoadPoint candidate : candidates) {
            Long id = candidate.edge().id();
            if (map.containsKey(id) && !removes.contains(id) && round(candidate.fraction()) == 1
                    && misses.get(id) == 0) {
                removes.add(id);
            }
        }

        for (Long id : removes) {
            map.remove(id);
        }

        return new HashSet<>(map.values());
    }
}
