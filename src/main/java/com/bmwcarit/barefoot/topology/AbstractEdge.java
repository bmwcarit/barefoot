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

import java.io.Serializable;
import java.util.Iterator;

/**
 * Abstract edge in a directed {@link Graph}.
 * <p>
 * <b>Note:</b> Connectivity between directed edges is maintained with distributed linked lists
 * where edges with the same source vertex link to each other cyclically. Each edge must provide a
 * reference to a successor edge as well as to a neighbor edge. For example, a vertex has two
 * incoming edges <i>e<sub>1</sub></i> and <i>e<sub>2</sub></i> and three outgoing edges
 * <i>e<sub>3</sub></i>, <i>e<sub>4</sub></i>, and <i>e<sub>5</sub></i>. The outgoing edges,
 * referred to as successor edges, reference each other to form a cyclic linked list, referred to as
 * neighbors, which are <i>e<sub>3</sub></i> &#8594; <i>e<sub>4</sub></i> &#8594;
 * <i>e<sub>5</sub></i> &#8594; <i>e<sub>3</sub></i>. The incoming edges <i>e<sub>1</sub></i> and
 * <i>e<sub>2</sub></i> reference one successor edge which is one edge of the cyclic linked list
 * <i>e<sub>1</sub></i> &#8594; <i>e<sub>4</sub></i> and <i>e<sub>2</sub></i> &#8594;
 * <i>e<sub>3</sub></i>. To iterate over successors edges, one can use the following approach:
 *
 * <pre>
 * E successor = edge.successor();
 * E neighbor = successor;
 * if (successor != null) {
 *     do {
 *         neighbor = neighbor.neighbor();
 *     } while (successor != neighbor);
 * }
 * </pre>
 *
 * @param <E> Implementation of {@link AbstractEdge} in a directed {@link Graph}. (Uses the
 *        curiously recurring template pattern (CRTP) for type-safe use of customized
 *        {@link AbstractEdge} type.)
 */
public abstract class AbstractEdge<E extends AbstractEdge<E>> implements Serializable {
    private static final long serialVersionUID = 1L;
    private transient E successor = null;
    private transient E neighbor = null;

    /**
     * Gets the edge's identifier.
     *
     * @return Edge identifier.
     */
    public abstract long id();

    /**
     * Gets the edge's source vertex.
     *
     * @return Identifier of the edge's source vertex.
     */
    public abstract long source();

    /**
     * Gets the edge's target vertex.
     *
     * @return Identifier of the edge's target vertex.
     */
    public abstract long target();

    /**
     * Gets the edge's successor.
     *
     * @return An edge's successor edge.
     */
    protected E successor() {
        return successor;
    }

    /**
     * Sets the edge's successor.
     *
     * @param successor An edge's successor edge.
     */
    protected void successor(E successor) {
        this.successor = successor;
    }

    /**
     * Gets the edge's neighbor.
     *
     * @return The edge's neighbor edge.
     */
    protected E neighbor() {
        return neighbor;
    }

    /**
     * Sets the edge's neighbor.
     *
     * @param neighbor The edge's neighbor edge.
     */
    protected void neighbor(E neighbor) {
        this.neighbor = neighbor;
    }

    /**
     * Gets iterator over the edge's successor edges.
     *
     * @return Iterator over the edge's successor edges.
     */
    public Iterator<E> successors() {
        return new Iterator<E>() {
            E successor = successor();
            E iterator = successor;

            @Override
            public boolean hasNext() {
                return (iterator != null);
            }

            @Override
            public E next() {
                if (iterator == null)
                    return null;

                E next = iterator;
                iterator = iterator.neighbor() == successor ? null : iterator.neighbor();

                return next;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}

