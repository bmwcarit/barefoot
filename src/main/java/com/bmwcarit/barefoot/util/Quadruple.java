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
 * Generic 4-tuple (quadruple).
 *
 * @param <A> Type of first element.
 * @param <B> Type of second element.
 * @param <C> Type of third element.
 * @param <D> Type of fourth element.
 */
public class Quadruple<A, B, C, D> extends Triple<A, B, C> implements Serializable {
    private static final long serialVersionUID = 1L;
    private D four = null;

    /**
     * Creates a {@link Quadruple} object.
     *
     * @param one First element.
     * @param two Second element.
     * @param three Third element.
     * @param four Fourth element.
     */
    public Quadruple(A one, B two, C three, D four) {
        super(one, two, three);
        this.four = four;
    }

    /**
     * Gets fourth element.
     *
     * @return Fourth element, may be null if set to null previously.
     */
    public D four() {
        return four;
    }

    /**
     * Sets fourth element.
     *
     * @param four Fourth element.
     */
    public void four(D four) {
        this.four = four;
    }
}
