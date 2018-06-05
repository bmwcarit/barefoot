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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.junit.Test;

import com.bmwcarit.barefoot.util.Stopwatch;
import com.bmwcarit.barefoot.util.Triple;
import com.esri.core.geometry.Geometry.Type;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WktImportFlags;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

;

public class GeographyTest {

    private static final boolean benchmark = false;
    private static final boolean wktexport = false;

    private static Stopwatch sw1 = new Stopwatch();
    private static Stopwatch sw2 = new Stopwatch();
    private static Stopwatch sw3 = new Stopwatch();
    private static Stopwatch sw4 = new Stopwatch();
    private static SpatialOperator spatial = new Geography();

    private static Triple<Point, Double, Double> intercept(Point a, Point b, Point c) {
        int iter = 1000;

        Triple<Point, Double, Double> res = new Triple<>(a, spatial.distance(a, c), 0d);

        for (int f = 1; f <= iter; ++f) {

            Point p = spatial.interpolate(a, b, (double) f / iter);
            double s = spatial.distance(p, c);

            if (s < res.two()) {

                res.one(p);
                res.two(s);
                res.three((double) f / iter);
            }
        }

        return res;
    }

    private static double distance(Point a, Point b) {
        return GeometryEngine.geodesicDistanceOnWGS84(a, b);
    }

    @Test
    public void testDistance() {
        Point reyk = new Point(-21.933333, 64.15);
        Point berl = new Point(13.408056, 52.518611);
        Point mosk = new Point(37.616667, 55.75);

        sw1.start();
        double dist_geog = spatial.distance(mosk, reyk);
        sw1.stop();

        sw2.start();
        double dist_esri = distance(mosk, reyk);
        sw2.stop();

        if (benchmark) {
            System.out.println(
                    "[Geography] distance " + sw1.us() + " us (GeoLib) " + sw2.us() + " us (ESRI)");
        }

        assertEquals(dist_geog, dist_esri, 10E-6);

        sw1.start();
        dist_geog = spatial.distance(berl, reyk);
        sw1.stop();

        sw2.start();
        dist_esri = distance(berl, reyk);
        sw2.stop();

        if (benchmark) {
            System.out.println(
                    "[Geography] distance " + sw1.us() + " us (GeoLib) " + sw2.us() + " us (ESRI)");
        }

        assertEquals(dist_geog, dist_esri, 10E-6);
    }

    @Test
    public void testGnomonic() throws FileNotFoundException {
        Point reyk = new Point(-21.933333, 64.15);
        Point berl = new Point(13.408056, 52.518611);
        Point mosk = new Point(37.616667, 55.75);

        sw1.start();
        double f = spatial.intercept(reyk, mosk, berl);
        sw1.stop();

        sw2.start();
        Point p = spatial.interpolate(reyk, mosk, f);
        sw2.stop();

        if (benchmark) {
            System.out.println("[Geography] interception " + sw1.us()
                    + " us (gnomonic), interpolation " + sw2.us() + " us (geodesic)");
        }

        sw1.start();
        Triple<Point, Double, Double> res = intercept(reyk, mosk, berl);
        sw1.stop();

        if (benchmark) {
            System.out.println(
                    "[Geography] interception & interpolation " + sw1.us() + " us (iterative)");
        }

        assertEquals(f, res.three(), 0.1);
        assertEquals(p.getX(), res.one().getX(), 10E-2);
        assertEquals(p.getY(), res.one().getY(), 10E-2);

        if (wktexport) {
            {
                Polyline line = new Polyline();
                line.startPath(mosk);
                for (double _f = 10E-3; _f < 1; _f += 10E-3) {
                    line.lineTo(spatial.interpolate(mosk, reyk, _f));
                }
                line.lineTo(reyk);
                PrintWriter writer = new PrintWriter("geog_mosk-reyk.json");
                writer.write(GeometryEngine.geometryToGeoJson(line));
                writer.close();
            }

            {
                Polyline line = new Polyline();
                line.startPath(berl);
                for (double _f = 10E-3; _f < 1; _f += 10E-3) {
                    line.lineTo(spatial.interpolate(berl, p, _f));
                }
                line.lineTo(p);
                PrintWriter writer = new PrintWriter("geog_berl-intc.json");
                writer.write(GeometryEngine.geometryToGeoJson(line));
                writer.close();
            }
        }
    }

    @Test
    public void testLineInterception() {
        Polyline ab = (Polyline) GeometryEngine.geometryFromWkt(
                "LINESTRING(11.4047661 48.1403687,11.4053519 48.141055)",
                WktImportFlags.wktImportDefaults, Type.Polyline);
        Point a = ab.getPoint(0), b = ab.getPoint(1);

        String points[] = new String[] {"POINT(11.406501117689324 48.14051652560591)", // East
                "POINT(11.406713245538327 48.14182906667162)", // Northeast
                "POINT(11.404923416812364 48.14258477213369)", // North
                "POINT(11.403300759321036 48.14105540093837)", // Northwest
                "POINT(11.403193249043934 48.140881120346386)", // West
                "POINT(11.40327279698731 48.13987351306362)", // Southwest
                "POINT(11.405221721600025 48.1392039845402)", // South
                "POINT(11.406255844863914 48.13963486923349)" // Southeast
        };

        for (int i = 0; i < points.length; ++i) {
            Point c = (Point) GeometryEngine.geometryFromWkt(points[i],
                    WktImportFlags.wktImportDefaults, Type.Point);

            sw1.start();
            double f = spatial.intercept(a, b, c);
            Point p = spatial.interpolate(a, b, f);
            sw1.stop();

            sw2.start();
            Triple<Point, Double, Double> res = intercept(a, b, c);
            sw2.stop();

            sw3.start();
            double s = spatial.distance(p, c);
            sw3.stop();

            sw4.start();
            double s_esri = distance(p, c);
            sw4.stop();

            if (benchmark) {
                System.out.println("[Geography] interception & interpolation " + sw1.us()
                        + " us (gnomonic/geodesic) " + sw2.us() + " us (iterative)");
                System.out.println("[Geography] distance " + sw3.us() + " us (GeoLib) " + sw4.us()
                        + " us (ESRI)");
            }

            assertEquals(f > 1 ? 1 : f < 0 ? 0 : f, res.three(), 0.2);
            assertEquals(p.getX(), res.one().getX(), 10E-2);
            assertEquals(p.getY(), res.one().getY(), 10E-2);
            assertEquals(s, s_esri, 10E-6);
        }
    }

    private static double azimuth(Point a, Point b, boolean left) {
        GeodesicData geod = Geodesic.WGS84.Inverse(a.getY(), a.getX(), b.getY(), b.getX());
        double azi = left ? geod.azi1 : geod.azi2;
        return azi < 0 ? azi + 360 : azi;
    }

    @Test
    public void testLineAzimuth() {
        Point reyk = new Point(-21.933333, 64.15);
        Point berl = new Point(13.408056, 52.518611);
        Point mosk = new Point(37.616667, 55.75);

        assertEquals(azimuth(berl, mosk, true), spatial.azimuth(berl, mosk, 0f), 1E-9);
        assertEquals(azimuth(berl, mosk, false), spatial.azimuth(berl, mosk, 1f), 1E-9);
        assertEquals(azimuth(berl, reyk, true), spatial.azimuth(berl, reyk, 0f), 1E-9);
        assertTrue(spatial.azimuth(berl, mosk, 0f) < spatial.azimuth(berl, mosk, 0.5)
                && spatial.azimuth(berl, mosk, 0.5) < spatial.azimuth(berl, mosk, 1f));
    }

    @Test
    public void testPathInterception1() {
        String point = "POINT(11.410624 48.144161)";
        String line =
                "LINESTRING(11.4047013 48.1402147,11.4047038 48.1402718,11.4047661 48.1403687,11.4053519 48.141055,11.4054617 48.1411901,11.4062664 48.1421968,11.4064586 48.1424479,11.4066449 48.1427372,11.4067254 48.1429028,11.4067864 48.1430673,11.4068647 48.1433303,11.4069456 48.1436822,11.4070524 48.1440368,11.4071569 48.1443314,11.4072635 48.1445915,11.4073887 48.1448641,11.4075228 48.1450729,11.407806 48.1454843,11.4080135 48.1458112,11.4083012 48.1463167,11.4086211 48.1469061,11.4087461 48.1471386,11.4088719 48.1474078,11.4089422 48.1476014,11.409028 48.1478353,11.409096 48.1480701,11.4091568 48.1483459,11.4094282 48.1498536)";

        Point c = (Point) GeometryEngine.geometryFromWkt(point, WktImportFlags.wktImportDefaults,
                Type.Point);
        Polyline ab = (Polyline) GeometryEngine.geometryFromWkt(line,
                WktImportFlags.wktImportDefaults, Type.Polyline);

        sw1.start();
        double f = spatial.intercept(ab, c);
        sw1.stop();

        double l = spatial.length(ab);

        sw2.start();
        Point p = spatial.interpolate(ab, l, f);
        sw2.stop();

        double d = spatial.distance(p, c);

        assertEquals(p.getX(), 11.407547966254612, 10E-6);
        assertEquals(p.getY(), 48.14510945890138, 10E-6);
        assertEquals(f, 0.5175157549609246, 10E-6);
        assertEquals(l, 1138.85464239099, 10E-6);
        assertEquals(d, 252.03375312704165, 10E-6);

        if (benchmark) {
            System.out.println("[Geography] path interception " + sw1.us()
                    + " us (gnomonic), path interpolation " + sw2.us() + " us (geodesic)");
        }
    }

    @Test
    public void testPathInterception2() {
        String point = "POINT(11.584009286555187 48.17578656762985)";
        String line =
                "LINESTRING(11.5852021 48.1761996, 11.585284 48.175924, 11.5852937 48.1758945)";

        Point c = (Point) GeometryEngine.geometryFromWkt(point, WktImportFlags.wktImportDefaults,
                Type.Point);
        Polyline ab = (Polyline) GeometryEngine.geometryFromWkt(line,
                WktImportFlags.wktImportDefaults, Type.Polyline);

        sw1.start();
        double f = spatial.intercept(ab, c);
        sw1.stop();

        double l = spatial.length(ab);

        sw2.start();
        Point p = spatial.interpolate(ab, l, f);
        sw2.stop();

        double d = spatial.distance(p, c);

        assertEquals(p.getX(), 11.585274842230357, 10E-6);
        assertEquals(p.getY(), 48.17595481677191, 10E-6);
        assertEquals(f, 0.801975106391962, 10E-6);
        assertEquals(l, 34.603061318901396, 10E-6);
        assertEquals(d, 95.96239015496631, 10E-6);

        if (benchmark) {
            System.out.println("[Geography] path interception " + sw1.us()
                    + " us (gnomonic), path interpolation " + sw2.us() + " us (geodesic)");
        }
    }

    @Test
    public void testPathAzimuth() {
        Point reyk = new Point(-21.933333, 64.15);
        Point berl = new Point(13.408056, 52.518611);
        Point mosk = new Point(37.616667, 55.75);

        Polyline p = new Polyline();
        p.startPath(berl);
        p.lineTo(mosk);
        p.lineTo(reyk);

        assertEquals(azimuth(berl, mosk, true), spatial.azimuth(p, 0f), 1E-9);
        assertEquals(azimuth(mosk, reyk, false), spatial.azimuth(p, 1f), 1E-9);
        assertEquals(azimuth(berl, mosk, false),
                spatial.azimuth(p, spatial.distance(berl, mosk) / spatial.length(p)), 1E-9);
        Point c = spatial.interpolate(berl, mosk, 0.5);
        assertEquals(azimuth(berl, c, false),
                spatial.azimuth(p, spatial.distance(berl, c) / spatial.length(p)), 1E-9);
        Point d = spatial.interpolate(mosk, reyk, 0.5);
        assertEquals(azimuth(mosk, d, false), spatial.azimuth(p,
                (spatial.distance(berl, mosk) + spatial.distance(mosk, d)) / spatial.length(p)),
                1E-9);
    }
}
