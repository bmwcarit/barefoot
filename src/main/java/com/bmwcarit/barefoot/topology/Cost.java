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
 * Abstract cost function to be used for routing algorithms implementing {@link Router}.
 *
 * @param <E> Implementation of {@link AbstractEdge} in a directed {@link Graph}.
 */
public abstract class Cost<E extends AbstractEdge<E>> {
    /**
     * Abstract function to define a custom cost function for traversing an edge.
     *
     * @param edge {@link AbstractEdge} for which cost value shall be calculated.
     * @return Cost value for traversing the edge.
     */
    public abstract double cost(E edge);

    /**
     * Calculates cost value for traversing a certain fraction of the edge. It is assumed that costs
     * increase linearly while traversing an edge.
     *
     * @param edge {@link AbstractEdge} for which cost value shall be calculated.
     * @param fraction Fraction of the edge to be traversed.
     * @return Cost value for traversing a certain fraction of the edge.
     */
    public double cost(E edge, double fraction) {
        return cost(edge) * fraction;
    }
}
