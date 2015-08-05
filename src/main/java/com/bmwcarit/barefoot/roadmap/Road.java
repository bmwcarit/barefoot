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

import com.bmwcarit.barefoot.road.BaseRoad;
import com.bmwcarit.barefoot.road.Heading;
import com.bmwcarit.barefoot.topology.AbstractEdge;
import com.esri.core.geometry.Polyline;

/**
 * Directed road wrapper of {@link BaseRoad} objects in a directed road map ({@link RoadMap}). *
 * <p>
 * <b>Note:</b> Since {@link Road} objects are directional representations of {@link BaseRoad}
 * objects, each {@link BaseRoad} is split into two {@link Road} objects. For that purpose, it uses
 * the identifier <i>i</i> of each {@link BaseRoad} to define identifiers of the respective
 * {@link Road} objects, where <i>i * 2</i> is the identifier of the forward directed {@link Road}
 * and <i>i * 2 + 1</i> of the backward directed {@link Road}.
 */
public class Road extends AbstractEdge<Road> {
    private static final long serialVersionUID = 1L;
    private final BaseRoad base;
    private final Heading heading;

    static Polyline invert(Polyline geometry) {
        Polyline reverse = new Polyline();
        int last = geometry.getPointCount() - 1;
        reverse.startPath(geometry.getPoint(last));

        for (int i = last - 1; i >= 0; --i) {
            reverse.lineTo(geometry.getPoint(i));
        }

        return reverse;
    }

    /**
     * Constructs {@link Road} object.
     *
     * @param base {@link BaseRoad} object to be referred to.
     * @param heading {@link Heading} of the directed {@link Road}.
     */
    public Road(BaseRoad base, Heading heading) {
        this.base = base;
        this.heading = heading;
    }

    @Override
    public long id() {
        return heading == Heading.forward ? base.id() * 2 : base.id() * 2 + 1;
    }

    @Override
    public long source() {
        return heading == Heading.forward ? base.source() : base.target();
    }

    @Override
    public long target() {
        return heading == Heading.forward ? base.target() : base.source();
    }

    /**
     * Gets road's type identifier.
     *
     * @return Road type identifier.
     */
    public short type() {
        return base.type();
    }

    /**
     * Gets road's priority factor, i.e. an additional cost factor for routing, and must be greater
     * or equal to one. Higher priority factor means higher costs.
     *
     * @return Road's priority factor.
     */
    public float priority() {
        return base.priority();
    }

    /**
     * Gets road's maximum speed in kilometers per hour.
     *
     * @return Maximum speed in kilometers per hour.
     */
    public float maxspeed() {
        return base.maxspeed(heading);
    }

    /**
     * Gets road length in meters.
     *
     * @return Road length in meters.
     */
    public float length() {
        return base.length();
    }

    /**
     * Gets road's geometry as a {@link Polyline} from the road's source to its target.
     *
     * @return Road's geometry as {@link Polyline} from source to target.
     */
    public Polyline geometry() {
        return heading == Heading.forward ? base.geometry() : invert(base.geometry());
    }

    /**
     * Gets referred {@link BaseRoad} object.
     *
     * @return {@link BaseRoad} object.
     */
    public BaseRoad base() {
        return base;
    }
}
