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

package com.bmwcarit.barefoot.spatial;

import java.util.Set;

import com.esri.core.geometry.Point;

/**
 * Interface of spatial index for searching objects by means of spatial properties. There may be
 * different implementations of this interface providing different underlying data structures for
 * efficient data access, e.g. Quad-tree (see {@link QuadTreeIndex}) or R-Tree (not implemented
 * yet).
 *
 * @param <T> Result types depend on the implementation.
 */
public interface SpatialIndex<T> {
    /**
     * Gets nearest object stored in the index, which may be nevertheless multiple results if they
     * have the same distance.
     *
     * @param c Point of reference for nearest search.
     * @return Result set of nearest object(s), may be multiple objects if they have the same
     *         distance.
     */
    Set<T> nearest(Point c);

    /**
     * Gets objects stored in the index that are within a certain radius or overlap a certain
     * radius.
     *
     * @param c Center point for radius search.
     * @param r Radius in meters.
     * @return Result set of object(s) that are within a the given radius or overlap the radius.
     */
    Set<T> radius(Point c, double r);

    /**
     * Gets <i>k</i> nearest objects stored in the index.
     *
     * @param c Point of reference for nearest search.
     * @param k Number of objects to be searched.
     * @return Result set of nearest objects (exactly k objects).
     */
    Set<T> knearest(Point c, int k);
}
