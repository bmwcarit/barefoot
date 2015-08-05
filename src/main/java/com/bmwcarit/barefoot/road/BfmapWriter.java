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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import com.bmwcarit.barefoot.util.SourceException;

/**
 * Barefoot map road writer for writing {@link BaseRoad} to barefoot map files, usually with file
 * extension 'bfmap'.
 */
public class BfmapWriter implements RoadWriter {
    private final String path;
    private ObjectOutput writer = null;

    /**
     * Constructs a {@link BfmapWriter} object writing to a file.
     * <p>
     * <b>Note:</b> If the file exists, it will be overwritten.
     *
     * @param path Path to the barefoot map file to be written.
     */
    public BfmapWriter(String path) {
        this.path = path;
    }

    @Override
    public boolean isOpen() {
        if (writer != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void open() throws SourceException {
        try {
            writer = new ObjectOutputStream(new FileOutputStream(path));
        } catch (FileNotFoundException e) {
            throw new SourceException("File could not be found.");
        } catch (IOException e) {
            throw new SourceException("Opening writer failed: " + e.getMessage());
        }
    }

    @Override
    public void close() throws SourceException {
        try {
            writer.writeObject(null);
            writer.close();
            writer = null;
        } catch (IOException e) {
            throw new SourceException("Closing writer failed: " + e.getMessage());
        }
    }

    @Override
    public void write(BaseRoad road) throws SourceException {
        if (!isOpen()) {
            throw new SourceException("Writer is not open.");
        }

        try {
            writer.writeObject(road);
        } catch (IOException e) {
            throw new SourceException("Writing failed: " + e.getMessage());
        }
    }
}
