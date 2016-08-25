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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmwcarit.barefoot.road.BaseRoad;
import com.bmwcarit.barefoot.road.Heading;
import com.bmwcarit.barefoot.road.RoadReader;
import com.bmwcarit.barefoot.spatial.QuadTreeIndex;
import com.bmwcarit.barefoot.spatial.SpatialIndex;
import com.bmwcarit.barefoot.topology.Graph;
import com.bmwcarit.barefoot.util.SourceException;
import com.bmwcarit.barefoot.util.Tuple;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;

/**
 * Implementation of a road map with (directed) roads, i.e. {@link Road} objects. It provides a road
 * network for routing that is derived from {@link Graph} and spatial search of roads with a
 * {@link SpatialIndex}.
 * <p>
 * <b>Note:</b> Since {@link Road} objects are directed representations of {@link BaseRoad} objects,
 * identifiers have a special mapping, see {@link Road}.
 */
public class RoadMap extends Graph<Road> implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(RoadMap.class);
    private transient Index index = null;

    static Collection<Road> split(BaseRoad base) {
        ArrayList<Road> roads = new ArrayList<>();

        roads.add(new Road(base, Heading.forward));

        if (!base.oneway()) {
            roads.add(new Road(base, Heading.backward));
        }

        return roads;
    }

    private class Index implements SpatialIndex<RoadPoint>, Serializable {
        private static final long serialVersionUID = 1L;
        private final QuadTreeIndex index = new QuadTreeIndex();

        public void put(Road road) {
            int id = (int) road.base().id();

            if (index.contains(id)) {
                return;
            }

            index.add(id, road.base().wkb());
        }

        public void clear() {
            index.clear();
        }

        private Set<RoadPoint> split(Set<Tuple<Integer, Double>> points) {
            Set<RoadPoint> neighbors = new HashSet<>();

            /*
             * This uses the road
             */
            for (Tuple<Integer, Double> point : points) {
                neighbors.add(new RoadPoint(edges.get((long) point.one() * 2), point.two()));

                if (edges.containsKey((long) point.one() * 2 + 1)) {
                    neighbors.add(new RoadPoint(edges.get((long) point.one() * 2 + 1),
                            1.0 - point.two()));
                }
            }

            return neighbors;
        }

        @Override
        public Set<RoadPoint> nearest(Point c) {
            return split(index.nearest(c));
        }

        @Override
        public Set<RoadPoint> radius(Point c, double r) {
            return split(index.radius(c, r));
        }

        @Override
        public Set<RoadPoint> knearest(Point c, int k) {
            return split(index.knearest(c, k));
        }
    };

    /**
     * Loads and creates a {@link RoadMap} object from {@link BaseRoad} objects loaded with a
     * {@link RoadReader}.
     *
     * @param reader {@link RoadReader} to load {@link BaseRoad} objects.
     * @return {@link RoadMap} object.
     * @throws SourceException thrown if error occurs while loading roads.
     */
    public static RoadMap Load(RoadReader reader) throws SourceException {

        long memory = 0;

        System.gc();
        memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        if (!reader.isOpen()) {
            reader.open();
        }

        logger.info("inserting roads ...");

        RoadMap roadmap = new RoadMap();

        int osmcounter = 0, counter = 0;
        BaseRoad road = null;
        while ((road = reader.next()) != null) {
            osmcounter += 1;

            for (Road uni : split(road)) {
                counter += 1;
                roadmap.add(uni);
            }

            if (osmcounter % 100000 == 0) {
                logger.info("inserted {} ({}) roads", osmcounter, counter);
            }
        }

        logger.info("inserted {} ({}) roads and finished", osmcounter, counter);

        reader.close();

        System.gc();
        memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) - memory;
        logger.info("~{} megabytes used for road data (estimate)",
                Math.max(0, Math.round(memory / 1E6)));

        return roadmap;
    }

    /**
     * Constructs road network topology and spatial index.
     */
    @Override
    public RoadMap construct() {
        long memory = 0;

        System.gc();
        memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        logger.info("index and topology constructing ...");

        super.construct();

        index = new Index();
        for (Road road : edges.values()) {
            index.put(road);
        }

        logger.info("index and topology constructed");

        System.gc();
        memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) - memory;
        logger.info("~{} megabytes used for spatial index (estimate)",
                Math.max(0, Math.round(memory / 1E6)));

        return this;
    }

    /**
     * Destroys road network topology and spatial index. (Necessary if roads have been added and
     * road network topology and spatial index must be reconstructed.)
     */
    @Override
    public void deconstruct() {
        logger.info("destructing ...");

        super.deconstruct();

        index.clear();
        index = null;

        logger.info("destructed");
    }

    /**
     * Returns instance of {@link SpatialIndex} for spatial search of {@link Road} objects.
     *
     * @return Instance of {@link SpatialIndex} or <i>null</i>, if the map hasn't been constructed (
     *         {@link RoadMap#construct()}) or has been deconstructed (
     *         {@link RoadMap#deconstruct()}).
     */
    public SpatialIndex<RoadPoint> spatial() {
        if (index == null)
            throw new RuntimeException("index not constructed");
        else
            return index;
    }

    /**
     * Gets {@link RoadReader} of roads in this {@link RoadMap}.
     *
     * @return {@link RoadReader} object.
     */
    public RoadReader reader() {
        return new RoadReader() {
            Iterator<Road> iterator = null;
            HashSet<Short> exclusions = null;
            Polygon polygon = null;

            @Override
            public boolean isOpen() {
                return (iterator != null);
            }

            @Override
            public void open() throws SourceException {
                open(null, null);
            }

            @Override
            public void open(Polygon polygon, HashSet<Short> exclusions) throws SourceException {
                iterator = edges.values().iterator();
                this.exclusions = exclusions;
                this.polygon = polygon;
            }

            @Override
            public void close() throws SourceException {
                iterator = null;
            }

            @Override
            public BaseRoad next() throws SourceException {
                BaseRoad road = null;
                do {
                    if (!iterator.hasNext()) {
                        return null;
                    }

                    Road _road = iterator.next();

                    if (_road.id() % 2 == 1) {
                        continue;
                    }

                    road = _road.base();
                } while (road == null || exclusions != null && exclusions.contains(road.type())
                        || polygon != null
                                && !GeometryEngine.contains(polygon, road.geometry(),
                                        SpatialReference.create(4326))
                                && !GeometryEngine.overlaps(polygon, road.geometry(),
                                        SpatialReference.create(4326)));
                return road;
            }
        };
    }
}
