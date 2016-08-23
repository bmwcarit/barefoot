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

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;

/**
 * Interface of spatial operations on geometries {@link Point} and {@link Polyline} which may be
 * implemented for different projections and coordinate systems, e.g. WGS-84 (see {@link Geography})
 * or UTM (not implemented yet).
 */
public interface SpatialOperator {

    /**
     * Gets the distance between two {@link Point}s <i>a</i> and <i>b</i>.
     *
     * @param a First point.
     * @param b Second point.
     * @return Distance between points in meters.
     */
    double distance(Point a, Point b);

    /**
     * Gets interception point of a straight line, defined by {@link Point}s <i>a</i> and <i>b</i>,
     * intercepted by {@link Point} <i>c</i>. The interception point is described as the linearly
     * interpolated fraction <i>f</i> in the interval <i>[0,1]</i> of the line from <i>a</i> to
     * <i>b</i>. A fraction of <i>f=0</i> is the same as {@link Point} at <i>a</i> and <i>f=1</i> is
     * the same as {@link Point} <i>b</i>.
     * <p>
     * <b>Note:</b> The coordinates of the interception point can be determined by interpolation of
     * the fraction along the straight line, e.g. with
     * {@link SpatialOperator#interpolate(Point, Point, double)}.
     *
     * @param a Start point of straight line <i>a</i> to <i>b</i>.
     * @param b End point of straight line <i>a</i> to <i>b</i>.
     * @param c {@link Point} that intercepts straight line <i>a</i> to <i>b</i>.
     * @return Interception point described as the linearly interpolated fraction <i>f</i> in the
     *         interval <i>[0,1]</i> of the line from <i>a</i> to <i>b</i>.
     */
    double intercept(Point a, Point b, Point c);

    /**
     * Gets {@link Point} from linear interpolation of a fraction <i>f</i>, in the interval
     * <i>[0,1]</i>, on a straight line, defined by two points <i>a</i> and <i>b</i>. A fraction of
     * <i>f=0</i> is the same as {@link Point} at <i>a</i> and <i>f=1</i> is the same as
     * {@link Point} <i>b</i>.
     *
     * @param a Start point of straight line <i>a</i> to <i>b</i>.
     * @param b End point of straight line from <i>a</i> to <i>b</i>.
     * @param f Fraction <i>f</i>, in the interval <i>[0,1]</i>, to be linearly interpolated on
     *        straight line from <i>a</i> to <i>b</i>.
     * @return {@link Point} linearly interpolated from fraction <i>f</i> on a straight line of
     *         points <i>a</i> and <i>b</i>.
     */
    Point interpolate(Point a, Point b, double f);

    /**
     * Gets azimuth of a point on straight line defined by {@link Point}s <i>a</i> and <i>b</i>by
     * fraction <i>f</i>. The point is defined as the linearly interpolated fraction <i>f</i> in the
     * interval <i>[0,1]</i> of the line from <i>a</i> to <i>b</i>.
     *
     * @param a Start point of straight line <i>a</i> to <i>b</i>.
     * @param b End point of straight line from <i>a</i> to <i>b</i>.
     * @param f Fraction <i>f</i>, in the interval <i>[0,1]</i>, that defines the point on the line
     *        <i>a</i> to <i>b</i>.
     * @return Azimuth in degrees from north (clockwise).
     */
    double azimuth(Point a, Point b, double f);

    /**
     * Gets length of a {@link Polyline} in meters.
     *
     * @param p {@link Polyline} to be measured.
     * @return Length of {@link Polyline} in meters.
     */
    double length(Polyline p);

    /**
     * Gets interception point of a {@link Polyline} intercepted by {@link Point} <i>c</i>. This is
     * analog to {@link SpatialOperator#intercept(Point, Point, Point)}. The fraction <i>f</i>
     * refers to the full length of the {@link Polyline}.
     *
     * @param p {@link Polyline} to be intercepted.
     * @param c {@link Point} that intercepts straight line <i>a</i> to <i>b</i>.
     * @return Interception point described as the linearly interpolated fraction <i>f</i> in the
     *         interval <i>[0,1]</i> of the {@link Polyline}.
     */
    double intercept(Polyline p, Point c);

    /**
     * Gets {@link Point} from linear interpolation of a fraction <i>f</i>, in the interval
     * <i>[0,1]</i>, on a {@link Polyline}. This is analog to
     * {@link SpatialOperator#interpolate(Point, Point, double)}.The fraction refers to the full
     * length of the {@link Polyline}.
     *
     * @param p {@link Polyline} of interpolation.
     * @param f Fraction <i>f</i>, in the interval <i>[0,1]</i>, to be linearly interpolated on
     *        {@link Polyline}.
     * @return {@link Point} linearly interpolated from fraction <i>f</i> on a {@link Polyline}.
     */
    Point interpolate(Polyline p, double f);

    /**
     * Gets {@link Point} from linear interpolation of a fraction <i>f</i>, in the interval
     * <i>[0,1]</i>, on a {@link Polyline}. This is an extension of
     * {@link SpatialOperator#interpolate(Polyline, double)} and takes the length of the
     * {@link Polyline} as parameter reduce computational effort.
     *
     * @param p {@link Polyline} of interpolation.
     * @param l Length of the {@link Polyline} in meters.
     * @param f Fraction <i>f</i>, in the interval <i>[0,1]</i>, to be linearly interpolated on
     *        {@link Polyline}.
     * @return {@link Point} linearly interpolated from fraction <i>f</i> on a {@link Polyline}.
     */
    Point interpolate(Polyline p, double l, double f);

    /**
     * Gets azimuth of a {@link Polyline} at some point defined by fraction <i>f</i>, in the
     * interval <i>[0,1]</i>. This is an extension of
     * {@link SpatialOperator#azimuth(Point, Point, double)}.
     *
     * @param p {@link Polyline}.
     * @param f Fraction <i>f</i>, in the interval <i>[0,1]</i>, that defines the point on the
     *        {@link Polyline}.
     * @return Azimuth in degrees from north (clockwise).
     */
    double azimuth(Polyline p, double f);

    /**
     * Gets azimuth of a {@link Polyline} at some point defined by fraction <i>f</i>, in the
     * interval <i>[0,1]</i>. This is an extension of
     * {@link SpatialOperator#azimuth(Polyline, double)} and takes the length of the
     * {@link Polyline} as parameter reduce computational effort.
     *
     * @param p {@link Polyline}.
     * @param l Length of the {@link Polyline} in meters.
     * @param f Fraction <i>f</i>, in the interval <i>[0,1]</i>, that defines the point on the
     *        {@link Polyline}.
     * @return Azimuth in degrees from north (clockwise).
     */
    double azimuth(Polyline p, double l, double f);

    /**
     * Gets {@link Envelope2D} that encloses of a circular area defined by a center {@link Point}
     * <i>c</i> and radius <i>r</i> given in meters.
     *
     * @param c Center {@link Point} of the circle.
     * @param r Radius of the circle in meters.
     * @return Enclosing {@link Envelope2D} of the circular area defined by center {@link Point}
     *         <i>c</i> and radius <i>r</i>.
     */
    Envelope2D envelope(Point c, double r);

}
