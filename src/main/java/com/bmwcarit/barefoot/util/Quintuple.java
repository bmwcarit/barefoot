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
 * Generic 5-tuple (quintuple).
 *
 * @param <A> Type of first element.
 * @param <B> Type of second element.
 * @param <C> Type of third element.
 * @param <D> Type of fourth element.
 * @param <E> Type of fifth element.
 */
public class Quintuple<A, B, C, D, E> extends Quadruple<A, B, C, D> implements Serializable {
    private static final long serialVersionUID = 1L;
    private E five = null;

    /**
     * Creates a {@link Quintuple} object.
     *
     * @param one First element.
     * @param two Second element.
     * @param three Third element.
     * @param four Fourth element.
     * @param five Fifth element.
     */
    public Quintuple(A one, B two, C three, D four, E five) {
        super(one, two, three, four);
        this.five = five;
    }

    /**
     * Gets fifth element.
     *
     * @return Fifth element, may be null if set to null previously.
     */
    public E five() {
        return five;
    }

    /**
     * Sets fifth element.
     *
     * @param five Fifth element.
     */
    public void five(E five) {
        this.five = five;
    }
}
