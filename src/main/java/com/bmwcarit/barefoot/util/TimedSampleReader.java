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

import java.util.Calendar;
import java.util.Date;

import com.bmwcarit.barefoot.matcher.MatcherSample;
import com.esri.core.geometry.Polygon;

/**
 * Timed reader that wraps other {@link SampleReader} implementations, i.e. it delays output of
 * function {@link SampleReader#next()} to generate a real-time stream. It implements the interface
 * {@link SampleReader#next()} itself where {@link TimedSampleReader#next()} blocks the current
 * thread to apply delays.
 */
public class TimedSampleReader implements SampleReader {
    private Long start = null, first = null, last = null;
    private final SampleReader reader;

    /**
     * Creates {@link TimedSampleReader} as wrapper of another {@link SampleReader} object.
     *
     * @param reader Reader to be wrapped.
     */
    public TimedSampleReader(SampleReader reader) {
        this.reader = reader;
    }

    @Override
    public boolean isOpen() {
        return reader.isOpen();
    }

    @Override
    public void open() throws SourceException {
        if (!reader.isOpen()) {
            reader.open();
        }
    }

    @Override
    public void open(Polygon polygon, Date time) throws SourceException {
        if (!reader.isOpen()) {
            reader.open(polygon, time);
        }
    }

    @Override
    public MatcherSample next() throws SourceException {
        MatcherSample sample = reader.next();

        if (sample == null) {
            return null;
        }

        if (start == null || first == null) {
            start = Calendar.getInstance().getTime().getTime();
            first = sample.time();
            last = first;
            return sample;
        }

        if (last > sample.time()) {
            throw new SourceException("Stream is unordered from source.");
        }

        long diff = (sample.time() - first) - (Calendar.getInstance().getTime().getTime() - start);
        try {
            Thread.sleep(diff < 0 ? 0 : diff);
        } catch (InterruptedException e) {
            System.out.println("Delaying thread for timing failed.");
            e.printStackTrace();
        }

        return sample;
    }

    @Override
    public void close() throws SourceException {
        reader.close();
        start = null;
        first = null;
        last = null;
    }
}
