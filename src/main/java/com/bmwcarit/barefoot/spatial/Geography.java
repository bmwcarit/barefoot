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

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;

/**
 * {@link SpatialOperator} as collection of spatial operations in WGS-84 projection (SRID 4326).
 */
public class Geography implements SpatialOperator {
    @Override
    public double distance(Point a, Point b) {
        return Geodesic.WGS84.Inverse(a.getY(), a.getX(), b.getY(), b.getX()).s12;
    }

    @Override
    public double intercept(Point a, Point b, Point c) {

        if (a.getX() == b.getX() && a.getY() == b.getY()) {
            return 0;
        }
        Intercept inter = new Intercept(Geodesic.WGS84);
        GeodesicData ci =
                inter.intercept(a.getY(), a.getX(), b.getY(), b.getX(), c.getY(), c.getX());
        GeodesicData ai = Geodesic.WGS84.Inverse(a.getY(), a.getX(), ci.lat2, ci.lon2);
        GeodesicData ab = Geodesic.WGS84.Inverse(a.getY(), a.getX(), b.getY(), b.getX());

        return (Math.abs(ai.azi1 - ab.azi1) < 1) ? ai.s12 / ab.s12 : (-1) * ai.s12 / ab.s12;
    }

    @Override
    public Point interpolate(Point a, Point b, double f) {
        GeodesicData inv = Geodesic.WGS84.Inverse(a.getY(), a.getX(), b.getY(), b.getX());
        GeodesicData pos = Geodesic.WGS84.Line(inv.lat1, inv.lon1, inv.azi1).Position(inv.s12 * f);

        return new Point(pos.lon2, pos.lat2);
    }

    @Override
    public double azimuth(Point a, Point b, double f) {
        double azi = 0;
        if (f < 0 + 1E-10) {
            azi = Geodesic.WGS84.Inverse(a.getY(), a.getX(), b.getY(), b.getX()).azi1;
        } else if (f > 1 - 1E-10) {
            azi = Geodesic.WGS84.Inverse(a.getY(), a.getX(), b.getY(), b.getX()).azi2;
        } else {
            Point c = interpolate(a, b, f);
            azi = Geodesic.WGS84.Inverse(a.getY(), a.getX(), c.getY(), c.getX()).azi2;
        }
        return azi < 0 ? azi + 360 : azi;
    }

    @Override
    public double length(Polyline p) {
        double d = 0;

        for (int i = 1; i < p.getPointCount(); ++i) {
            d += distance(p.getPoint(i - 1), p.getPoint(i));
        }

        return d;
    }

    @Override
    public double intercept(Polyline p, Point c) {
        double d = Double.MAX_VALUE;
        Point a = p.getPoint(0);
        double s = 0, sf = 0, ds = 0;

        for (int i = 1; i < p.getPointCount(); ++i) {
            Point b = p.getPoint(i);

            ds = distance(a, b);

            double f_ = intercept(a, b, c);
            f_ = (f_ > 1) ? 1 : (f_ < 0) ? 0 : f_;
            Point x = interpolate(a, b, f_);
            double d_ = distance(c, x);

            if (d_ < d) {
                sf = (f_ * ds) + s;
                d = d_;
            }

            s = s + ds;
            a = b;
        }

        return s == 0 ? 0 : sf / s;
    }

    @Override
    public Point interpolate(Polyline path, double f) {
        return interpolate(path, length(path), f);
    }

    @Override
    public Point interpolate(Polyline p, double l, double f) {
        assert (f >= 0 && f <= 1);

        Point a = p.getPoint(0);
        double d = l * f;
        double s = 0, ds = 0;

        if (f < 0 + 1E-10) {
            return p.getPoint(0);
        }

        if (f > 1 - 1E-10) {
            return p.getPoint(p.getPointCount() - 1);
        }

        for (int i = 1; i < p.getPointCount(); ++i) {
            Point b = p.getPoint(i);
            ds = distance(a, b);

            if ((s + ds) >= d) {
                return interpolate(a, b, (d - s) / ds);
            }

            s = s + ds;
            a = b;
        }

        return null;
    }

    @Override
    public double azimuth(Polyline p, double f) {
        return azimuth(p, length(p), f);
    }

    @Override
    public double azimuth(Polyline p, double l, double f) {
        assert (f >= 0 && f <= 1);

        Point a = p.getPoint(0);
        double d = l * f;
        double s = 0, ds = 0;

        if (f < 0 + 1E-10) {
            return azimuth(p.getPoint(0), p.getPoint(1), 0);
        }

        if (f > 1 - 1E-10) {
            return azimuth(p.getPoint(p.getPointCount() - 2), p.getPoint(p.getPointCount() - 1), f);
        }

        for (int i = 1; i < p.getPointCount(); ++i) {
            Point b = p.getPoint(i);
            ds = distance(a, b);

            if ((s + ds) >= d) {
                return azimuth(a, b, (d - s) / ds);
            }

            s = s + ds;
            a = b;
        }

        return Double.NaN;
    }

    @Override
    public Envelope2D envelope(Point c, double radius) {
        Envelope2D env = new Envelope2D();

        double ymax = Geodesic.WGS84.Direct(c.getY(), c.getX(), 0, radius).lat2;
        double ymin = Geodesic.WGS84.Direct(c.getY(), c.getX(), -180, radius).lat2;
        double xmax = Geodesic.WGS84.Direct(c.getY(), c.getX(), 90, radius).lon2;
        double xmin = Geodesic.WGS84.Direct(c.getY(), c.getX(), -90, radius).lon2;

        env.setCoords(xmin, ymin, xmax, ymax);

        return env;
    }
}
