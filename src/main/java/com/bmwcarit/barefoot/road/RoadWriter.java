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

import com.bmwcarit.barefoot.util.SourceException;

/**
 * Interface for writers {@link BaseRoad} objects, for writing to different types of sources.
 */
public interface RoadWriter {

    /**
     * Checks if the writer is open to write.
     *
     * @return True if writer is open, false otherwise.
     */
    public boolean isOpen();

    /**
     * Opens the writer.
     *
     * @throws SourceException thrown on error while opening the source.
     */
    public void open() throws SourceException;

    /**
     * Closes the writer.
     *
     * @throws SourceException thrown on error while closing the source.
     */
    public void close() throws SourceException;

    /**
     * Writes {@link BaseRoad} object to source.
     *
     * @param road {@link BaseRoad} object to be written to source.
     * @throws SourceException thrown on error while writing to the source.
     */
    public void write(BaseRoad road) throws SourceException;

}
