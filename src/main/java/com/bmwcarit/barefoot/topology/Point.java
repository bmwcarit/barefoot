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

package com.bmwcarit.barefoot.topology;

/**
 * Point in a directed {@link Graph} which is a point on an {@link AbstractEdge} with a fraction in
 * the interval <i>[0,1]</i> that defines the exact position as linear interpolation along the
 * {@link AbstractEdge} from its source to target.
 *
 * @param <E> {@link AbstractEdge} type of the graph.
 */
public class Point<E extends AbstractEdge<E>> {
    private final E edge;
    private final double fraction;

    /**
     * Creates a {@link Point} object by reference to an {@link AbstractEdge} and an exact position
     * defined by a fraction.
     *
     * @param edge {@link AbstractEdge} of the point in the graph.
     * @param fraction Fraction that defines the exact position on the {@link AbstractEdge}.
     */
    public Point(E edge, double fraction) {
        this.edge = edge;
        this.fraction = fraction;
    }

    /**
     * Gets the {@link AbstractEdge} of the point.
     *
     * @return {@link AbstractEdge} of the point.
     */
    public E edge() {
        return edge;
    }

    /**
     * Gets the fraction of the point.
     *
     * @return Fraction of the point.
     */
    public double fraction() {
        return fraction;
    }
}
