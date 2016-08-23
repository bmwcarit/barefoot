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
 * Simple implementation of {@link AbstractEdge}.
 */
public class Edge extends AbstractEdge<Edge> {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long source;
    private final long target;

    /**
     * Creates an {@link Edge} object.
     *
     * @param id Edge identifier.
     * @param source Identifier of the edge's source vertex.
     * @param target Identifier of the edge's target vertex.
     */
    public Edge(long id, long source, long target) {
        this.id = id;
        this.source = source;
        this.target = target;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public long source() {
        return source;
    }

    @Override
    public long target() {
        return target;
    }
}
