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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.bmwcarit.barefoot.util.Triple;
import com.bmwcarit.barefoot.util.Tuple;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WktImportFlags;

public class QuadTreeIndexTest {
    private static List<Polyline> geometries() {
        /*
         * (p2) (p3) ----- (e1) : (p1) -> (p2) ----------------------------------------------------
         * - \ / --------- (e2) : (p3) -> (p1) ----------------------------------------------------
         * | (p1) | ------ (e3) : (p4) -> (p1) ----------------------------------------------------
         * - / \ --------- (e4) : (p1) -> (p5) ----------------------------------------------------
         * (p4) (p5) ----- (e5) : (p2) -> (p4) ----------------------------------------------------
         * --------------- (e6) : (p5) -> (p3) ----------------------------------------------------
         */
        String p1 = "11.3441505 48.0839963";
        String p2 = "11.3421209 48.0850624";
        String p3 = "11.3460348 48.0850108";
        String p4 = "11.3427522 48.0832129";
        String p5 = "11.3469701 48.0825356";

        List<Polyline> geometries = new LinkedList<>();
        geometries
                .add((Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p1 + "," + p2 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline));
        geometries
                .add((Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p3 + "," + p1 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline));
        geometries
                .add((Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p4 + "," + p1 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline));
        geometries
                .add((Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p1 + "," + p5 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline));
        geometries
                .add((Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p2 + "," + p4 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline));
        geometries
                .add((Polyline) GeometryEngine.geometryFromWkt("LINESTRING(" + p5 + "," + p3 + ")",
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline));

        return geometries;
    }

    @Test
    public void testIndexNearest() {
        SpatialOperator spatial = new Geography();
        QuadTreeIndex index = new QuadTreeIndex();
        List<Polyline> lines = geometries();
        {
            int i = 0;
            for (Polyline line : lines) {
                index.add(i++, line);
            }
        }

        {
            Point c = new Point(11.343629, 48.083797);

            double dmin = Double.MAX_VALUE;
            int nearest = 0;
            for (int i = 0; i < lines.size(); ++i) {
                double f = spatial.intercept(lines.get(i), c);
                Point p = spatial.interpolate(lines.get(i), f);
                double d = spatial.distance(c, p);

                if (d < dmin) {
                    dmin = d;
                    nearest = i;
                }
            }

            Set<Tuple<Integer, Double>> points = index.nearest(c);

            assertEquals(1, points.size());
            assertEquals(nearest, (int) points.iterator().next().one());
        }
        {
            Point c = new Point(11.344827, 48.083752);

            double dmin = Double.MAX_VALUE;
            int nearest = 0;
            for (int i = 0; i < lines.size(); ++i) {
                double f = spatial.intercept(lines.get(i), c);
                Point p = spatial.interpolate(lines.get(i), f);
                double d = spatial.distance(c, p);

                if (d < dmin) {
                    dmin = d;
                    nearest = i;
                }
            }

            Set<Tuple<Integer, Double>> points = index.nearest(c);

            assertEquals(1, points.size());
            assertEquals(nearest, (int) points.iterator().next().one());
        }
    }

    @Test
    public void testIndexRadius() {
        SpatialOperator spatial = new Geography();
        QuadTreeIndex index = new QuadTreeIndex();
        List<Polyline> lines = geometries();
        {
            int i = 0;
            for (Polyline line : lines) {
                index.add(i++, line);
            }
        }

        {
            Point c = new Point(11.343629, 48.083797);
            double r = 50;

            Set<Integer> neighbors = new HashSet<>();
            for (int i = 0; i < lines.size(); ++i) {
                double f = spatial.intercept(lines.get(i), c);
                Point p = spatial.interpolate(lines.get(i), f);
                double d = spatial.distance(c, p);

                if (d <= r) {
                    neighbors.add(i);
                }
            }
            assertEquals(4, neighbors.size());

            Set<Tuple<Integer, Double>> points = index.radius(c, r);

            assertEquals(neighbors.size(), points.size());
            for (Tuple<Integer, Double> point : points) {
                assertTrue(neighbors.contains(point.one()));
            }
        }
        {
            Point c = new Point(11.344827, 48.083752);
            double r = 10;

            Set<Integer> neighbors = new HashSet<>();
            for (int i = 0; i < lines.size(); ++i) {
                double f = spatial.intercept(lines.get(i), c);
                Point p = spatial.interpolate(lines.get(i), f);
                double d = spatial.distance(c, p);

                if (d <= r) {
                    neighbors.add(i);
                }
            }
            assertEquals(1, neighbors.size());

            Set<Tuple<Integer, Double>> points = index.radius(c, r);

            assertEquals(neighbors.size(), points.size());
            for (Tuple<Integer, Double> point : points) {
                assertTrue(neighbors.contains(point.one()));
            }
        }
        {
            Point c = new Point(11.344827, 48.083752);
            double r = 5;

            Set<Integer> neighbors = new HashSet<>();
            for (int i = 0; i < lines.size(); ++i) {
                double f = spatial.intercept(lines.get(i), c);
                Point p = spatial.interpolate(lines.get(i), f);
                double d = spatial.distance(c, p);

                if (d <= r) {
                    neighbors.add(i);
                }
            }
            assertEquals(0, neighbors.size());

            Set<Tuple<Integer, Double>> points = index.radius(c, r);

            assertEquals(neighbors.size(), points.size());
            for (Tuple<Integer, Double> point : points) {
                assertTrue(neighbors.contains(point.one()));
            }
        }
    }

    @Test
    public void testIndexKNearest() {
        final String[] wkts = {
                "LINESTRING (11.4235408 48.1010922, 11.4235077 48.101068, 11.4231626 48.1008749)",
                "LINESTRING (11.4209648 48.0995957, 11.420596 48.0993468, 11.4203029 48.0991585)",
                "LINESTRING (11.4234794 48.102708, 11.423487 48.1026804, 11.4235585 48.1024194, 11.4235555 48.1020172, 11.4235546 48.1018929, 11.4235942 48.1015978, 11.4236333 48.1015246)",
                "LINESTRING (11.4342071 48.1036417, 11.434099 48.1036362)",
                "LINESTRING (11.424749 48.1075811, 11.424589 48.1074943)",
                "LINESTRING (11.4233874 48.10403, 11.4233392 48.1036034, 11.4233178 48.1033183, 11.423331 48.1030842, 11.4233452 48.1030028)",
                "LINESTRING (11.426896 48.1086681, 11.4267471 48.1085396, 11.4265761 48.1084146, 11.4263784 48.1083004, 11.4261893 48.1081984, 11.4259574 48.1080886, 11.4256389 48.1079628)",
                "LINESTRING (11.4296155 48.1040403, 11.4291788 48.104216, 11.4276585 48.1048256, 11.4267428 48.1051668, 11.4260504 48.1053754)",
                "LINESTRING (11.4233715 48.102927, 11.4233234 48.1028774, 11.4232315 48.1028237, 11.4231667 48.1028091)",
                "LINESTRING (11.423901 48.1067007, 11.4238735 48.1066368)",
                "LINESTRING (11.4291276 48.1025703, 11.4283449 48.1021036)",
                "LINESTRING (11.4196228 48.0987412, 11.419544 48.0986937, 11.4192787 48.0985466, 11.419083 48.0984455)",
                "LINESTRING (11.4315239 48.1033907, 11.4314949 48.103268, 11.431445 48.1032298)",
                "LINESTRING (11.4241922 48.105846, 11.4239102 48.1058577, 11.4237211 48.1058502)",
                "LINESTRING (11.4240852 48.1070322, 11.4239786 48.1068676, 11.423901 48.1067007)",
                "LINESTRING (11.429366 48.1026713, 11.4291276 48.1025703)",
                "LINESTRING (11.4233715 48.102927, 11.4234794 48.102708)",
                "LINESTRING (11.434099 48.1036362, 11.4339195 48.1036199)",
                "LINESTRING (11.4279771 48.1021004, 11.427739 48.1021063, 11.4274622 48.1020796, 11.4272785 48.1020453)",
                "LINESTRING (11.4203343 48.1064247, 11.4200027 48.1065159)",
                "LINESTRING (11.4327695 48.1034661, 11.4324386 48.1034103, 11.4322638 48.1033878, 11.4320837 48.103374, 11.4317987 48.1033749, 11.4315239 48.1033907)",
                "LINESTRING (11.4232825 48.1058113, 11.4231094 48.1057922)",
                "LINESTRING (11.4212843 48.0998197, 11.4209648 48.0995957)",
                "LINESTRING (11.4246926 48.1014583, 11.4242786 48.1014318, 11.4241056 48.1014114, 11.4239619 48.1013638, 11.4237734 48.1012623)",
                "LINESTRING (11.4203029 48.0991585, 11.4202116 48.0990999)",
                "LINESTRING (11.424589 48.1074943, 11.4245557 48.1074727, 11.4244429 48.1073825, 11.4242763 48.1072459, 11.4241703 48.107132, 11.4240852 48.1070322)",
                "LINESTRING (11.4256389 48.1079628, 11.4252166 48.1077985)",
                "LINESTRING (11.4296286 48.1027528, 11.4294025 48.1026865, 11.429366 48.1026713)",
                "LINESTRING (11.423522 48.1058375, 11.4234677 48.1058286)",
                "LINESTRING (11.4189939 48.1067931, 11.4185333 48.1069367, 11.417415 48.1073033, 11.4167637 48.1075141)",
                "LINESTRING (11.4252269 48.1015127, 11.4250265 48.1014863, 11.4246926 48.1014583)",
                "LINESTRING (11.4261995 48.1017542, 11.4260869 48.1017195)",
                "LINESTRING (11.4227463 48.1057586, 11.4225241 48.1058169, 11.4222246 48.1059396)",
                "LINESTRING (11.4315239 48.1033907, 11.4310888 48.1034897, 11.4307594 48.1035925, 11.4304346 48.1037122, 11.4299221 48.1039175)",
                "LINESTRING (11.4272785 48.1020453, 11.4272214 48.1020346)",
                "LINESTRING (11.4234228 48.1044161, 11.4234068 48.1042436, 11.4233968 48.1041474)",
                "LINESTRING (11.4233968 48.1041474, 11.4233909 48.1040614, 11.4233874 48.10403)",
                "LINESTRING (11.431288 48.1031457, 11.4309557 48.1030701)",
                "LINESTRING (11.4283449 48.1021036, 11.4281273 48.1020855, 11.4279771 48.1021004)",
                "LINESTRING (11.4222246 48.1059396, 11.4220781 48.1059888, 11.4213633 48.1061695)",
                "LINESTRING (11.4346668 48.1036677, 11.4342071 48.1036417)",
                "LINESTRING (11.4231626 48.1008749, 11.4226747 48.100628, 11.4222092 48.1003989, 11.4218485 48.1001882, 11.4212843 48.0998197)",
                "LINESTRING (11.4246184 48.1057797, 11.4241922 48.105846)",
                "LINESTRING (11.4237211 48.1058502, 11.4236695 48.1054044, 11.4234864 48.1048008, 11.4234491 48.1046087, 11.4234228 48.1044161)",
                "LINESTRING (11.4254268 48.1055632, 11.4246184 48.1057797)",
                "LINESTRING (11.4202116 48.0990999, 11.419921 48.098921)",
                "LINESTRING (11.4260869 48.1017195, 11.425897 48.1016611, 11.4255831 48.1015849, 11.4252269 48.1015127)",
                "LINESTRING (11.431445 48.1032298, 11.4313772 48.103178, 11.431288 48.1031457)",
                "LINESTRING (11.4237698 48.1062692, 11.4237211 48.1058502)",
                "LINESTRING (11.419083 48.0984455, 11.4187162 48.0983106)",
                "LINESTRING (11.4213633 48.1061695, 11.4206946 48.1063362)",
                "LINESTRING (11.419921 48.098921, 11.4196228 48.0987412)",
                "LINESTRING (11.4229218 48.1057609, 11.4227463 48.1057586)",
                "LINESTRING (11.4339195 48.1036199, 11.4337703 48.1036037, 11.4327695 48.1034661)",
                "LINESTRING (11.4206946 48.1063362, 11.4203343 48.1064247)",
                "LINESTRING (11.4237211 48.1058502, 11.423522 48.1058375)",
                "LINESTRING (11.4260504 48.1053754, 11.4258914 48.1054237, 11.4254268 48.1055632)",
                "LINESTRING (11.4266892 48.1019015, 11.4261995 48.1017542)",
                "LINESTRING (11.4272214 48.1020346, 11.427052 48.1020029, 11.4266892 48.1019015)",
                "LINESTRING (11.4236333 48.1015246, 11.4237734 48.1012623)",
                "LINESTRING (11.4299221 48.1039175, 11.4296155 48.1040403)",
                "LINESTRING (11.4272459 48.1090405, 11.426896 48.1086681)",
                "LINESTRING (11.4238735 48.1066368, 11.4238315 48.1065376, 11.4237781 48.1063403, 11.4237698 48.1062692)",
                "LINESTRING (11.4237734 48.1012623, 11.4235408 48.1010922)",
                "LINESTRING (11.4187162 48.0983106, 11.4183193 48.0982035, 11.4179521 48.098084, 11.4176973 48.0979896, 11.4173972 48.0978488)",
                "LINESTRING (11.4308462 48.1030444, 11.430253 48.1029024)",
                "LINESTRING (11.4233452 48.1030028, 11.4233715 48.102927)",
                "LINESTRING (11.4231094 48.1057922, 11.4229218 48.1057609)",
                "LINESTRING (11.430253 48.1029024, 11.4296286 48.1027528)",
                "LINESTRING (11.4309557 48.1030701, 11.4308462 48.1030444)",
                "LINESTRING (11.4252166 48.1077985, 11.4249964 48.1077091, 11.4247926 48.1076039, 11.424749 48.1075811)",
                "LINESTRING (11.4234677 48.1058286, 11.4232825 48.1058113)",
                "LINESTRING (11.4200027 48.1065159, 11.4189939 48.1067931)",
                "LINESTRING (11.4360303 48.1037204, 11.4346668 48.1036677)"};

        final SpatialOperator spatial = new Geography();

        {
            final List<Triple<Integer, Polyline, Double>> lines = new ArrayList<>();
            final QuadTreeIndex index = new QuadTreeIndex();
            final Point c = new Point(11.429859, 48.105382);
            final int k = 20;
            int i = 0;
            for (String wkt : wkts) {
                int id = i++;
                Polyline line = (Polyline) GeometryEngine.geometryFromWkt(wkt,
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);

                double f = spatial.intercept(line, c);
                Point p = spatial.interpolate(line, f);
                double d = spatial.distance(c, p);

                lines.add(new Triple<>(id, line, d));
                index.add(id, line);
            }

            Collections.sort(lines, new Comparator<Triple<Integer, Polyline, Double>>() {
                @Override
                public int compare(Triple<Integer, Polyline, Double> left,
                        Triple<Integer, Polyline, Double> right) {
                    return left.three() < right.three() ? -1
                            : left.three() > right.three() ? +1 : 0;
                }
            });

            Set<Integer> neighbors = new HashSet<>();

            for (int j = 0; j < k; ++j) {
                Tuple<Integer, Polyline> e = lines.get(j);
                neighbors.add(e.one());
            }

            Set<Tuple<Integer, Double>> points = index.knearest(c, k);

            assertEquals(points.size(), neighbors.size());

            for (Tuple<Integer, Double> point : points) {
                assertTrue(neighbors.contains(point.one()));
            }
        }
        {
            final List<Triple<Integer, Polyline, Double>> lines = new ArrayList<>();
            final QuadTreeIndex index = new QuadTreeIndex();
            final Point c = new Point(11.429859, 48.105382);
            final int k = 3;
            int i = 0;
            for (String wkt : wkts) {
                int id = i++;
                Polyline line = (Polyline) GeometryEngine.geometryFromWkt(wkt,
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);

                double f = spatial.intercept(line, c);
                Point p = spatial.interpolate(line, f);
                double d = spatial.distance(c, p);

                lines.add(new Triple<>(id, line, d));
                index.add(id, line);
            }

            Collections.sort(lines, new Comparator<Triple<Integer, Polyline, Double>>() {
                @Override
                public int compare(Triple<Integer, Polyline, Double> left,
                        Triple<Integer, Polyline, Double> right) {
                    return left.three() < right.three() ? -1
                            : left.three() > right.three() ? +1 : 0;
                }
            });

            Set<Integer> neighbors = new HashSet<>();

            for (int j = 0; j < k; ++j) {
                Triple<Integer, Polyline, Double> e = lines.get(j);
                neighbors.add(e.one());
            }

            Set<Tuple<Integer, Double>> points = index.knearest(c, k);

            assertEquals(points.size(), neighbors.size());

            for (Tuple<Integer, Double> point : points) {
                assertTrue(neighbors.contains(point.one()));
            }
        }
        {
            final List<Triple<Integer, Polyline, Double>> lines = new ArrayList<>();
            final QuadTreeIndex index = new QuadTreeIndex();
            final Point c = new Point(11.42096, 48.10318);
            final int k = 10;
            int i = 0;
            for (String wkt : wkts) {
                int id = i++;
                Polyline line = (Polyline) GeometryEngine.geometryFromWkt(wkt,
                        WktImportFlags.wktImportDefaults, Geometry.Type.Polyline);

                double f = spatial.intercept(line, c);
                Point p = spatial.interpolate(line, f);
                double d = spatial.distance(c, p);

                lines.add(new Triple<>(id, line, d));
                index.add(id, line);
            }

            Collections.sort(lines, new Comparator<Triple<Integer, Polyline, Double>>() {
                @Override
                public int compare(Triple<Integer, Polyline, Double> left,
                        Triple<Integer, Polyline, Double> right) {
                    return left.three() < right.three() ? -1
                            : left.three() > right.three() ? +1 : 0;
                }
            });

            Set<Integer> neighbors = new HashSet<>();

            for (int j = 0; j < k; ++j) {
                Tuple<Integer, Polyline> e = lines.get(j);
                neighbors.add(e.one());
            }

            Set<Tuple<Integer, Double>> points = index.knearest(c, k);

            assertEquals(points.size(), neighbors.size());

            for (Tuple<Integer, Double> point : points) {
                assertTrue(neighbors.contains(point.one()));
            }
        }
    }
}
