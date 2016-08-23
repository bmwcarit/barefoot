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

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.spatial.SpatialOperator;
import com.bmwcarit.barefoot.topology.Path;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;

/**
 * Route in a {@link Road} network that consists of a start and end {@link RoadPoint} and sequence
 * of {@link Road}s.
 */
public class Route extends Path<Road> {
    private final static SpatialOperator spatial = new Geography();

    private Double length = null;
    private Double time = null;

    /**
     * Creates a {@link Route} object. (This is a base case that consists of only one
     * {@link RoadPoint}.)
     *
     * @param point {@link RoadPoint} that is start and end point.
     */
    public Route(RoadPoint point) {
        super(point);
    }

    /**
     * Creates a {@link Route} object.
     *
     * @param source Source/start {@link RoadPoint} of the route.
     * @param target Target/end {@link RoadPoint} of the route.
     * @param roads Sequence of {@link Road}s that make the route.
     */
    public Route(RoadPoint source, RoadPoint target, List<Road> roads) {
        super(source, target, roads);
    }

    /**
     * Gets size of the route, i.e. the number of {@link Road}s that make the route.
     *
     * @return Number of the {@link Road}s that make the route.
     */
    public int size() {
        return path().size();
    }

    /**
     * Gets {@link Road} by index of the sequence of {@link Road} objects.
     *
     * @param index Index of the road to be returned from the sequence of roads.
     * @return Road at the given index.
     */
    public Road get(int index) {
        return path().get(index);
    }

    /**
     * Gets length of the {@link Route} in meters, uses cost function {@link Distance} to determine
     * the length.
     *
     * @return Length of the {@link Route} in meters.
     */
    public double length() {
        if (length != null) {
            return length;
        } else {
            length = this.cost(new Distance());
            return length;
        }
    }

    /**
     * Gets travel time for the {@link Route} in seconds, uses cost function {@link Time} to
     * determine travel time.
     *
     * @return Travel time of the {@link Route} in seconds.
     */
    public double time() {
        if (time != null) {
            return time;
        } else {
            time = cost(new Time());
            return time;
        }
    }

    @Override
    public RoadPoint source() {
        return (RoadPoint) super.source();
    }

    @Override
    public RoadPoint target() {
        return (RoadPoint) super.target();
    }

    @Override
    public boolean add(Path<Road> other) {
        time = null;
        length = null;
        return super.add(other);
    }

    /**
     * Gets geometry of the {@link Route} from start point to end point by concatenating the
     * geometries of the roads in the route's road sequence respectively.
     *
     * @return Geometry of the route.
     */
    public Polyline geometry() {
        Polyline geometry = new Polyline();

        geometry.startPath(source().geometry());

        if (source().edge().id() != target().edge().id()) {
            {
                double f = source().edge().length() * source().fraction(), s = 0;
                Point a = source().edge().geometry().getPoint(0);

                for (int i = 1; i < source().edge().geometry().getPointCount(); ++i) {
                    Point b = source().edge().geometry().getPoint(i);
                    s += spatial.distance(a, b);
                    a = b;

                    if (s <= f) {
                        continue;
                    }

                    geometry.lineTo(b);
                }
            }
            for (int i = 1; i < path().size() - 1; ++i) {
                Polyline segment = path().get(i).geometry();

                for (int j = 1; j < segment.getPointCount(); ++j) {
                    geometry.lineTo(segment.getPoint(j));
                }
            }
            {
                double f = target().edge().length() * target().fraction(), s = 0;
                Point a = target().edge().geometry().getPoint(0);

                for (int i = 1; i < target().edge().geometry().getPointCount() - 1; ++i) {
                    Point b = target().edge().geometry().getPoint(i);
                    s += spatial.distance(a, b);
                    a = b;

                    if (s >= f) {
                        break;
                    }

                    geometry.lineTo(b);
                }
            }
        } else {
            double sf = source().edge().length() * source().fraction();
            double tf = target().edge().length() * target().fraction();
            double s = 0;
            Point a = source().edge().geometry().getPoint(0);

            for (int i = 1; i < source().edge().geometry().getPointCount() - 1; ++i) {
                Point b = source().edge().geometry().getPoint(i);
                s += spatial.distance(a, b);
                a = b;

                if (s <= sf) {
                    continue;
                }
                if (s >= tf) {
                    break;
                }

                geometry.lineTo(b);
            }
        }

        geometry.lineTo(target().geometry());

        return geometry;
    }

    /**
     * Creates a {@link Route} object from its JSON representation.
     *
     * @param json JSON representation of the {@link Route}.
     * @param map {@link RoadMap} object as the reference of {@link RoadPoint}s and {@link Road}s.
     * @return {@link Route} object.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public static Route fromJSON(JSONObject json, RoadMap map) throws JSONException {
        LinkedList<Road> roads = new LinkedList<>();

        JSONObject jsontarget = json.getJSONObject("target");
        JSONObject jsonsource = json.getJSONObject("source");
        RoadPoint target = RoadPoint.fromJSON(jsontarget, map);
        RoadPoint source = RoadPoint.fromJSON(jsonsource, map);

        JSONArray jsonroads = json.getJSONArray("roads");

        for (int j = 0; j < jsonroads.length(); ++j) {
            JSONObject jsonroad = jsonroads.getJSONObject(j);
            roads.add(Road.fromJSON(jsonroad, map));
        }

        return new Route(source, target, roads);
    }

    /**
     * Gets a JSON representation of the {@link Route}.
     *
     * @return {@link JSONObject} object.
     * @throws JSONException thrown on JSON extraction or parsing error.
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("target", target().toJSON());
        json.put("source", source().toJSON());

        JSONArray jsonroads = new JSONArray();
        for (Road road : path()) {
            jsonroads.put(road.toJSON());
        }

        json.put("roads", jsonroads);

        return json;
    }
}
