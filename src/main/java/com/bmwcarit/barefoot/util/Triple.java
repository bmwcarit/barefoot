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
 * Generic 3-tuple (triple).
 *
 * @param <X> Type of first element.
 * @param <Y> Type of second element.
 * @param <Z> Type of third element.
 */
public class Triple<X, Y, Z> extends Tuple<X, Y> implements Serializable {
    private static final long serialVersionUID = 1L;
    private Z three = null;

    /**
     * Creates a {@link Triple} object.
     *
     * @param one First element.
     * @param two Second element.
     * @param three Third element.
     */
    public Triple(X one, Y two, Z three) {
        super(one, two);
        this.three = three;
    }

    /**
     * Gets third element.
     *
     * @return Third element, may be null if set to null previously.
     */
    public Z three() {
        return three;
    }

    /**
     * Sets third element.
     *
     * @param three Third element.
     */
    public void three(Z three) {
        this.three = three;
    }
}
