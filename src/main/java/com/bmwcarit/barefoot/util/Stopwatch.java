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

/**
 * Stopwatch of performance benchmarks.
 * <p>
 * <b>Note:</b> It uses the {@link System#nanoTime()} and, consequently, inherits its accuracy for
 * time measurements.
 */
public class Stopwatch {
    private long timestamp = 0;
    private long time = 0;

    /**
     * Starts time measurement.
     */
    public void start() {
        timestamp = System.nanoTime();
    }

    /**
     * Stops time measurement.
     */
    public void stop() {
        time = System.nanoTime() - timestamp;
    }

    /**
     * Gets time measurement in nanosceonds.
     *
     * @return Time measurement in nanoseconds.
     */
    public long ns() {
        return time;
    }

    /**
     * Gets time measurement in microsceonds.
     *
     * @return Time measurement in microseconds.
     */
    public long us() {
        return time / 1000;
    }

    /**
     * Gets time measurement in milliseconds.
     *
     * @return Time measurement in milliseconds.
     */
    public long ms() {
        return time / 1000000;
    }
}
