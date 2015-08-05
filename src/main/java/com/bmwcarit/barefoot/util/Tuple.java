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

import java.io.Serializable;

/**
 * Generic 2-tuple (tuple).
 *
 * @param <X> Type of first element.
 * @param <Y> Type of second element.
 */
public class Tuple<X, Y> implements Serializable {
    private static final long serialVersionUID = 1L;
    private X one = null;
    private Y two = null;

    /**
     * Creates a {@link Tuple} object.
     *
     * @param one First element.
     * @param two Second element.
     */
    public Tuple(X one, Y two) {
        this.one = one;
        this.two = two;
    }

    /**
     * Gets first element.
     *
     * @return First element, may be null if set to null previously.
     */
    public X one() {
        return one;
    }

    /**
     * Gets second element.
     *
     * @return Second element, may be null if set to null previously.
     */
    public Y two() {
        return two;
    }

    /**
     * Sets first element.
     *
     * @param one First element.
     */
    public void one(X one) {
        this.one = one;
    }

    /**
     * Sets second element.
     *
     * @param two Second element.
     */
    public void two(Y two) {
        this.two = two;
    }
}
