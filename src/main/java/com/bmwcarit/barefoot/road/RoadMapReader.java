package com.bmwcarit.barefoot.road;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.GeometryEngine;

import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.util.SourceException;

/**
 * Barefoot map reader for reading BaseRoad objects from a supplied RoadMap object.
 * Used to get BaseRoad data from file buffer.
 */
public class RoadMapReader implements RoadReader{
    private RoadMap readerSrc;
	
    private Iterator<Road> iterator;
    private HashSet<Short> exclusions;
    private Polygon polygon;
	
    public RoadMapReader(RoadMap src) {
        iterator = null;
        exclusions = null;
        polygon = null;
		
        readerSrc = src;
    }
	
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
        iterator = readerSrc.getEdges().values().iterator();
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
}
