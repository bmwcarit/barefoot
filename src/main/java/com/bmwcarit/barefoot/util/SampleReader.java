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
package com.bmwcarit.barefoot.util;

import java.util.Date;

import com.bmwcarit.barefoot.matcher.MatcherSample;
import com.esri.core.geometry.Polygon;

/**
 * Interface for readers of {@link MatcherSample} data from different sources.
 */
public interface SampleReader {
    /**
     * Checks if the reader is open.
     *
     * @return True if reader is open, false otherwise.
     */
    boolean isOpen();

    /**
     * Opens the reader.
     *
     * @throws SourceException thrown if an error occurs while handling the source.
     */
    void open() throws SourceException;

    /**
     * Gets next {@link MatcherSample} object from the source. If all samples have been read, it
     * returns null.
     *
     * @return {@link MatcherSample} object, null if all samples have been read.
     * @throws SourceException thrown if an error occurs while reading from source.
     */
    MatcherSample next() throws SourceException;

    /**
     * Opens the reader and restricts reading of {@link MatcherSample} objects to spatially
     * contained or overlapping with a {@link Polygon} and to a temporal timeframe starting at a
     * certain time
     *
     * @param polygon Spatial restriction for reading with a certain {@link Polygon}. Must be null
     *        to disallow spatial restriction.
     * @param time Temporal restriction for reading {@link MatcherSample} objects measured at some
     *        {@link Date} or later. Must be null to disallow temporal restriction.
     * @throws SourceException thrown if an error occurs while handling the source.
     */
    public void open(Polygon polygon, Date time) throws SourceException;

    /**
     * Closes the reader.
     *
     * @throws SourceException thrown if an error occurs while handling the source.
     */
    void close() throws SourceException;
}
