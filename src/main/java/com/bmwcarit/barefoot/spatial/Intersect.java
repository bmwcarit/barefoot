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

/**
 * Implementation of the net.sf.geographiclib.Intersect class
 */
package com.bmwcarit.barefoot.spatial;

import net.sf.geographiclib.GeoMath;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.Gnomonic;
import net.sf.geographiclib.GnomonicData;

/**
 * Geodesic intersection.
 * <p>
 * <i>Note: Intersect.java has been ported to Java from its C++ equivalent Intersect.cpp, authored
 * by C. F. F. Karney and licensed under MIT/X11 license. The following documentation is mostly the
 * same as for its C++ equivalent, but has been adopted to apply to this Java implementation.</i>
 * <p>
 * Simple solution to the intersection problem using the gnomonic projection: The intersectio
 * problem is, given two geodesics <i>a</i> and <i>b</i>, determine the point of intersection of
 * <i>a</i> and <i>b</i>. The gnomonic projection and the solution to the intersection problem are
 * derived in Section 8 of
 * <ul>
 * <li>C. F. F. Karney, <a href="http://dx.doi.org/10.1007/s00190-012-0578-z"> Algorithms for
 * geodesics</a>, J. Geodesy <b>87</b>, 43--55 (2013); DOI:
 * <a href="http://dx.doi.org/10.1007/s00190-012-0578-z"> 10.1007/s00190-012-0578-z</a>; addenda:
 * <a href="http://geographiclib.sf.net/geod-addenda.html"> geod-addenda.html</a>.</li>
 * </ul>
 * <p>
 * In gnomonic projection geodesics are nearly straight; and they are exactly straight if they go
 * through the center of projection. The intersection can then be found as follows: Guess an
 * intersection point. Project the two line segments into gnomonic, compute their intersection in
 * this projection, use this intersection point as the new center, and repeat.
 * <p>
 * <b>CAUTION:</b> The solution to the intersection problem is valid only under the following
 * conditions:
 * <ul>
 * <li>The two points defining the geodesic and the point of interception must be in the same
 * hemisphere centered at the intersection point for the gnomonic projection to be defined.</li>
 * </ul>
 */
public class Intersect {

    private static final double eps = 0.01 * Math.sqrt(GeoMath.epsilon);
    /**
     * Maximum number of iterations for calculation of interception point. (The solution should
     * usually converge before reaching the maximum number of iterations. The default is 10.)
     */
    public static int maxit = 10;
    private Geodesic earth;
    private Gnomonic gnom;

    /**
     * Constructor for Intersect.
     * <p>
     * 
     * @param earth the {@link Geodesic} object to use for geodesic calculations. By default the
     *        WGS84 ellipsoid should be used.
     */
    public Intersect(Geodesic earth) {
        this.earth = earth;
        this.gnom = new Gnomonic(this.earth);
    }

    /**
     * Intersection of geodesics.
     * <p>
     * 
     * @param lata1 latitude of point <i>1</i> of geodesic <i>a</i> (degrees).
     * @param lona1 longitude of point <i>1</i> of geodesic <i>a</i> (degrees).
     * @param lata2 latitude of point <i>2</i> of geodesic <i>a</i> (degrees).
     * @param lona2 longitude of point <i>2</i> of geodesic <i>a</i> (degrees).
     * @param latb1 latitude of point <i>1</i> of geodesic <i>b</i> (degrees).
     * @param lonb1 longitude of point <i>1</i> of geodesic <i>b</i> (degrees).
     * @param latb2 latitude of point <i>2</i> of geodesic <i>b</i> (degrees).
     * @param lonb2 longitude of point <i>2</i> of geodesic <i>b</i> (degrees).
     * @return a {@link GeodesicData} object, defining a geodesic from point <i>1</i> of geodesic
     *         <i>a</i> to the intersection point, with the following fields: <i>lat1</i>,
     *         <i>lon1</i>, <i>azi1</i>, <i>lat2</i>, <i>lon2</i>, <i>azi2</i>, <i>s12</i>,
     *         <i>a12</i>.
     *         <p>
     *         <i>lat1</i> should be in the range [&minus;90&deg;, 90&deg;]; <i>lon1</i> and
     *         <i>azi1</i> should be in the range [&minus;540&deg;, 540&deg;). The values of
     *         <i>lon2</i> and <i>azi2</i> returned are in the range [&minus;180&deg;, 180&deg;).
     */
    public GeodesicData intersect(double lata1, double lona1, double lata2, double lona2,
            double latb1, double lonb1, double latb2, double lonb2) {

        double latp0 = (lata1 + lata2 + latb1 + latb2) / 4, latp0_ = Double.NaN,
                lonp0_ = Double.NaN;
        double lonp0 = ((lona1 >= 0 ? lona1 % 360 : (lona1 % 360) + 360)
                + (lona2 >= 0 ? lona2 % 360 : (lona2 % 360) + 360)
                + (lonb1 >= 0 ? lonb1 % 360 : (lonb1 % 360) + 360)
                + (lonb2 >= 0 ? lonb2 % 360 : (lonb2 % 360) + 360)) / 4;
        lonp0 = (lonp0 > 180 ? lonp0 - 360 : lonp0);

        for (int i = 0; i < maxit; ++i) {
            GnomonicData xa1 = gnom.Forward(latp0, lonp0, lata1, lona1);
            GnomonicData xa2 = gnom.Forward(latp0, lonp0, lata2, lona2);
            GnomonicData xb1 = gnom.Forward(latp0, lonp0, latb1, lonb1);
            GnomonicData xb2 = gnom.Forward(latp0, lonp0, latb2, lonb2);

            Vector va1 = new Vector(xa1.x, xa1.y, 1);
            Vector va2 = new Vector(xa2.x, xa2.y, 1);
            Vector vb1 = new Vector(xb1.x, xb1.y, 1);
            Vector vb2 = new Vector(xb2.x, xb2.y, 1);
            Vector la = va1.cross(va2);
            Vector lb = vb1.cross(vb2);
            Vector p0 = la.cross(lb);
            p0 = p0.multiply(1d / p0.z);

            latp0_ = latp0;
            lonp0_ = lonp0;

            GnomonicData rev = gnom.Reverse(latp0, lonp0, p0.x, p0.y);
            latp0 = rev.lat;
            lonp0 = rev.lon;

            if (Math.abs(lonp0_ - lonp0) < eps && Math.abs(latp0_ - latp0) < eps) {
                break;
            }
        }

        return earth.Inverse(lata1, lona1, latp0, lonp0);
    }
}
