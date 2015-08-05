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
 * Generic 6-tuple (sextuple).
 *
 * @param <A> Type of first element.
 * @param <B> Type of second element.
 * @param <C> Type of third element.
 * @param <D> Type of fourth element.
 * @param <E> Type of fifth element.
 * @param <F> Type of sixth element.
 */
public class Sextuple<A, B, C, D, E, F> extends Quintuple<A, B, C, D, E> {
    private static final long serialVersionUID = 1L;
    private F six = null;

    /**
     * Creates a {@link Sextuple} object.
     *
     * @param one First element.
     * @param two Second element.
     * @param three Third element.
     * @param four Fourth element.
     * @param five Fifth element.
     * @param six Sixth element.
     */
    public Sextuple(A one, B two, C three, D four, E five, F six) {
        super(one, two, three, four, five);
        this.six = six;
    }

    /**
     * Sets sixth element.
     *
     * @param six Sixth element.
     */
    public void six(F six) {
        this.six = six;
    }

    /**
     * Gets sixth element.
     *
     * @return Sixth element, may be null if set to null previously.
     */
    public F six() {
        return six;
    }
}
