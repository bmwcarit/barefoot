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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.HashSet;

import com.bmwcarit.barefoot.util.SourceException;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;

/**
 * Barefoot map road reader for reading {@link BaseRoad} object from barefoot map files, usually
 * with file extension 'bfmap'.
 */
public class BfmapReader implements RoadReader {

    private final String path;
    private ObjectInput reader = null;
    private HashSet<Short> exclusions = null;
    private Polygon polygon = null;

    /**
     * Constructs a {@link BfmapReader} object reading from a file.
     *
     * @param path Path to barefoot map file to be read.
     */
    public BfmapReader(String path) {
        this.path = path;
    }

    @Override
    public boolean isOpen() {
        if (reader != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void open() throws SourceException {
        open(null, null);
    }

    @Override
    public void open(Polygon polygon, HashSet<Short> exclusions) throws SourceException {
        try {
            this.reader = new ObjectInputStream(new FileInputStream(path));
            this.exclusions = exclusions;
            this.polygon = polygon;
        } catch (FileNotFoundException e) {
            throw new SourceException("File could not be found for path: " + path);
        } catch (IOException e) {
            throw new SourceException("Opening reader failed: " + e.getMessage());
        }
    }

    @Override
    public void close() throws SourceException {
        try {
            reader.close();
        } catch (IOException e) {
            throw new SourceException("Closing file failed.");
        }
    }

    @Override
    public BaseRoad next() throws SourceException {
        if (!isOpen()) {
            throw new SourceException("File is closed or invalid.");
        }

        try {
            BaseRoad road = null;
            do {
                road = (BaseRoad) reader.readObject();
                if (road == null) {
                    return null;
                }
            } while (exclusions != null && exclusions.contains(road.type()) || polygon != null
                    && !GeometryEngine.contains(polygon, road.geometry(),
                            SpatialReference.create(4326))
                    && !GeometryEngine.overlaps(polygon, road.geometry(),
                            SpatialReference.create(4326)));

            return road;
        } catch (ClassNotFoundException e) {
            throw new SourceException("File is corrupted, read object is not a road.");
        } catch (IOException e) {
            throw new SourceException("Reading file failed: " + e.getMessage());
        }
    }
}
