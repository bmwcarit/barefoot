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

import java.util.HashSet;

import com.bmwcarit.barefoot.util.SourceException;
import com.esri.core.geometry.Polygon;

/**
 * Interface for readers of {@link BaseRoad} objects from different sources which depends on
 * implementation.
 */
public interface RoadReader {

    /**
     * Checks if the reader is open to read.
     *
     * @return True if reader is open, false otherwise.
     */
    boolean isOpen();

    /**
     * Opens the reader.
     *
     * @throws SourceException thrown on error while opening the source.
     */
    void open() throws SourceException;

    /**
     * Opens the reader and restricts reading of {@link BaseRoad} objects to spatially contained or
     * overlapping with a {@link Polygon} and to only certain road types (see
     * {@link BaseRoad#type()} ) that are not excluded.
     *
     * @param polygon Spatial restriction for reading with a certain {@link Polygon}. Must be null
     *        to disallow spatial restriction.
     * @param exclusion Set of excluded road types. Must be null to disallow type exclusions.
     * @throws SourceException thrown on error while opening the source.
     */
    void open(Polygon polygon, HashSet<Short> exclusion) throws SourceException;

    /**
     * Closes the reader.
     *
     * @throws SourceException thrown on error while closing the source.
     */
    void close() throws SourceException;

    /**
     * Gets next {@link BaseRoad} object from the source. If all roads have been read, it returns
     * null.
     *
     * @return {@link BaseRoad} object, null if all roads have been read.
     * @throws SourceException thrown on error while reading from the source.
     */
    BaseRoad next() throws SourceException;
}
