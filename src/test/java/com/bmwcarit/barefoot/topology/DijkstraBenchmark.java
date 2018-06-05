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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmwcarit.barefoot.matcher.MatcherSample;
import com.bmwcarit.barefoot.matcher.MatcherTest;
import com.bmwcarit.barefoot.roadmap.Distance;
import com.bmwcarit.barefoot.roadmap.Road;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.roadmap.RoadPoint;
import com.bmwcarit.barefoot.roadmap.Route;
import com.bmwcarit.barefoot.roadmap.Testmap;
import com.bmwcarit.barefoot.roadmap.Time;
import com.bmwcarit.barefoot.roadmap.TimePriority;
import com.bmwcarit.barefoot.util.Stopwatch;
import com.bmwcarit.barefoot.util.Tuple;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.WktExportFlags;

public class DijkstraBenchmark {
    private static Logger logger = LoggerFactory.getLogger(DijkstraBenchmark.class);
    private final RoadMap map;

    public DijkstraBenchmark() throws IOException, JSONException {
        this.map = Testmap.instance();
    }

    @Test
    public void testShortest() throws FileNotFoundException {
        Set<RoadPoint> sources = map.spatial().nearest(new Point(11.58424, 48.17635));
        Set<RoadPoint> targets = map.spatial().nearest(new Point(11.56656, 48.17683));

        assertTrue(!sources.isEmpty());
        assertTrue(!targets.isEmpty());

        RoadPoint source = sources.iterator().next();
        RoadPoint target = targets.iterator().next();

        Router<Road, RoadPoint> algo = new Dijkstra<>();

        Stopwatch sw = new Stopwatch();
        sw.start();
        List<Road> edges = algo.route(source, target, new Distance());
        sw.stop();

        Route route = new Route(source, target, edges);

        logger.info(
                "Ruemannstr. -> Petuelring (shortest): {} ms, {} meters route distance, {} seconds travel time",
                sw.ms(), route.length(), route.time());

        Point start = route.geometry().getPoint(0);
        Point end = route.geometry().getPoint(route.geometry().getPointCount() - 1);

        assertTrue(source.geometry().equals(start));
        assertTrue(target.geometry().equals(end));

        if (logger.isTraceEnabled()) {
            String filename = "diskstra_shortest.wkt";
            PrintWriter writer = new PrintWriter(filename);
            writer.println(GeometryEngine.geometryToWkt(route.geometry(),
                    WktExportFlags.wktExportLineString));
            writer.close();
            logger.trace("route written to file {} (WKT)", filename);
        }
    }

    @Test
    public void testFastest() throws FileNotFoundException {
        Set<RoadPoint> sources = map.spatial().nearest(new Point(11.58424, 48.17635));
        Set<RoadPoint> targets = map.spatial().nearest(new Point(11.72661, 48.39594));

        assertTrue(!sources.isEmpty());
        assertTrue(!targets.isEmpty());

        RoadPoint source = sources.iterator().next();
        RoadPoint target = targets.iterator().next();

        Router<Road, RoadPoint> algo = new Dijkstra<>();

        Stopwatch sw = new Stopwatch();
        sw.start();
        List<Road> edges = algo.route(source, target, new Time());
        sw.stop();

        Route route = new Route(source, target, edges);

        logger.info(
                "Leopoldstr. -> Freising (fastest): {} ms, {} meters route distance, {} seconds travel time",
                sw.ms(), route.length(), route.time());

        Point start = route.geometry().getPoint(0);
        Point end = route.geometry().getPoint(route.geometry().getPointCount() - 1);

        assertTrue(source.geometry().equals(start));
        assertTrue(target.geometry().equals(end));

        if (logger.isTraceEnabled()) {
            String filename = "dijkstra_fastest.wkt";
            PrintWriter writer = new PrintWriter(filename);
            writer.println(GeometryEngine.geometryToWkt(route.geometry(),
                    WktExportFlags.wktExportLineString));
            writer.close();
            logger.trace("route written to file {} (WKT)", filename);
        }
    }

    @Test
    public void testFastestPriority() throws FileNotFoundException {
        Set<RoadPoint> sources = map.spatial().nearest(new Point(11.58424, 48.17635));
        Set<RoadPoint> targets = map.spatial().nearest(new Point(11.72661, 48.39594));

        assertTrue(!sources.isEmpty());
        assertTrue(!targets.isEmpty());

        RoadPoint source = sources.iterator().next();
        RoadPoint target = targets.iterator().next();

        Router<Road, RoadPoint> algo = new Dijkstra<>();

        Stopwatch sw = new Stopwatch();
        sw.start();
        List<Road> edges = algo.route(source, target, new TimePriority());
        sw.stop();

        Route route = new Route(source, target, edges);

        logger.info(
                "Leopoldstr. -> Freising (fastest, priority): {} ms, {} meters route distance, {} seconds travel time",
                sw.ms(), route.length(), route.time());

        Point start = route.geometry().getPoint(0);
        Point end = route.geometry().getPoint(route.geometry().getPointCount() - 1);

        assertTrue(source.geometry().equals(start));
        assertTrue(target.geometry().equals(end));

        if (logger.isTraceEnabled()) {
            String filename = "diskstra_fastest-priority.wkt";
            PrintWriter writer = new PrintWriter(filename);
            writer.println(GeometryEngine.geometryToWkt(route.geometry(),
                    WktExportFlags.wktExportLineString));
            writer.close();
            logger.trace("route written to file {} (WKT)", filename);
        }
    }

    @Test
    public void testNoRoute() {
        Set<RoadPoint> sources = map.spatial().nearest(new Point(11.58424, 48.17635));
        Set<RoadPoint> targets = map.spatial().nearest(new Point(11.59151, 48.15231));

        assertTrue(!sources.isEmpty());
        assertTrue(!targets.isEmpty());

        RoadPoint source = sources.iterator().next();
        RoadPoint target = targets.iterator().next();

        Router<Road, RoadPoint> algo = new Dijkstra<>();

        Stopwatch sw = new Stopwatch();
        sw.start();
        List<Road> edges = algo.route(source, target, new TimePriority());
        sw.stop();

        assertTrue(edges == null);

        logger.info("no route example (fastest, priority): {} ms", sw.ms());
    }

    @Test
    public void testNoRouteBound() {
        Set<RoadPoint> sources = map.spatial().nearest(new Point(11.58424, 48.17635));
        Set<RoadPoint> targets = map.spatial().nearest(new Point(11.59151, 48.15231));

        assertTrue(!sources.isEmpty());
        assertTrue(!targets.isEmpty());

        RoadPoint source = sources.iterator().next();
        RoadPoint target = targets.iterator().next();

        Router<Road, RoadPoint> algo = new Dijkstra<>();

        Stopwatch sw = new Stopwatch();
        sw.start();
        List<Road> edges = algo.route(source, target, new Distance(), new Distance(), 10000d);
        sw.stop();

        assertTrue(edges == null);

        logger.info("no route bound example (fastest, priority): {} ms", sw.ms());
    }

    @Test
    public void testSSMT1() throws FileNotFoundException {
        Set<RoadPoint> sources = map.spatial().nearest(new Point(11.58551, 48.17705));
        Set<RoadPoint> _targets = map.spatial().radius(new Point(11.57318, 48.17802), 100);

        assertTrue(!sources.isEmpty());
        assertTrue(!_targets.isEmpty());

        RoadPoint source = sources.iterator().next();

        HashSet<RoadPoint> targets = new HashSet<>();
        for (RoadPoint target : _targets) {
            targets.add(target);
        }

        Stopwatch sw = new Stopwatch();
        sw.start();
        Dijkstra<Road, RoadPoint> algo = new Dijkstra<>();
        Map<RoadPoint, List<Road>> routes = algo.route(source, targets, new TimePriority());
        sw.stop();

        logger.info("SSMT (fastest, priority): {} ms", sw.ms());

        sw.start();
        Map<RoadPoint, List<Road>> routes2 = new HashMap<>();
        for (RoadPoint target : targets) {
            routes2.put(target, algo.route(source, target, new TimePriority()));
        }
        sw.stop();

        logger.info("1 x n routes (fastest, priority): {} ms", sw.ms());

        String filename = null;
        PrintWriter writer = null;

        if (logger.isTraceEnabled()) {
            filename = "diskstra_ssmt1.wkt";
            writer = new PrintWriter(filename);
        }

        for (RoadPoint target : routes.keySet()) {
            if (routes.get(target) == null) {
                assertEquals(routes2.get(target), null);
                break;
            }
            Route route = new Route(source, target, routes.get(target));
            Route route2 = new Route(source, target, routes2.get(target));

            assertEquals(route.size(), route2.size());

            for (int i = 0; i < route.size(); ++i) {
                assertEquals(route.get(i).id(), route2.get(i).id());
            }

            if (logger.isTraceEnabled()) {
                writer.println(GeometryEngine.geometryToWkt(route.geometry(),
                        WktExportFlags.wktExportLineString));
            }
        }

        if (logger.isTraceEnabled()) {
            writer.close();
            logger.trace("route(s) written to file {} (WKT)", filename);
        }
    }

    @Test
    public void testSSMT2() throws FileNotFoundException {
        Set<RoadPoint> sources = map.spatial().nearest(new Point(11.58551, 48.17705));
        Set<RoadPoint> _targets = map.spatial().radius(new Point(11.57318, 48.17802), 100);

        assertTrue(!sources.isEmpty());
        assertTrue(!_targets.isEmpty());

        RoadPoint source = sources.iterator().next();

        HashSet<RoadPoint> targets = new HashSet<>();
        for (RoadPoint target : _targets) {
            targets.add(target);
        }

        Stopwatch sw = new Stopwatch();
        sw.start();
        Dijkstra<Road, RoadPoint> algo = new Dijkstra<>();
        Map<RoadPoint, List<Road>> routes = algo.route(source, targets, new Time());
        sw.stop();

        logger.info("SSMT (fastest): {} ms", sw.ms());

        sw.start();
        Map<RoadPoint, List<Road>> routes2 = new HashMap<>();
        for (RoadPoint target : targets) {
            routes2.put(target, algo.route(source, target, new Time()));
        }
        sw.stop();

        logger.info("1 x n routes (fastest): {} ms", sw.ms());

        String filename = null;
        PrintWriter writer = null;

        if (logger.isTraceEnabled()) {
            filename = "diskstra_ssmt2.wkt";
            writer = new PrintWriter(filename);
        }

        for (RoadPoint target : routes.keySet()) {
            if (routes.get(target) == null) {
                assertEquals(routes2.get(target), null);
                break;
            }
            Route route = new Route(source, target, routes.get(target));
            Route route2 = new Route(source, target, routes2.get(target));

            assertEquals(route.size(), route2.size());

            for (int i = 0; i < route.size(); ++i) {
                assertEquals(route.get(i).id(), route2.get(i).id());
            }

            if (logger.isTraceEnabled()) {
                writer.println(GeometryEngine.geometryToWkt(route.geometry(),
                        WktExportFlags.wktExportLineString));
            }
        }

        if (logger.isTraceEnabled()) {
            writer.close();
            logger.trace("route(s) written to file {} (WKT)", filename);
        }
    }

    @Test
    public void testSSMTstream() throws JSONException, IOException {
        logger.info("SSMT (fastest, priority) stream test");

        Dijkstra<Road, RoadPoint> algo = new Dijkstra<>();
        JSONArray jsonsamples = new JSONArray(new String(
                Files.readAllBytes(
                        Paths.get(MatcherTest.class.getResource("x0001-015.json").getPath())),
                Charset.defaultCharset()));

        assertTrue(jsonsamples.length() > 1);

        MatcherSample sample1 = new MatcherSample(jsonsamples.getJSONObject(0)), sample2 = null;

        for (int i = 1; i < jsonsamples.length(); ++i) {
            sample2 = new MatcherSample(jsonsamples.getJSONObject(i));

            Set<RoadPoint> sources = map.spatial().radius(sample1.point(), 100);
            Set<RoadPoint> targets = map.spatial().radius(sample2.point(), 100);

            assertTrue(!sources.isEmpty());
            assertTrue(!targets.isEmpty());

            Stopwatch sw = new Stopwatch();
            sw.start();

            int valids = 0;
            for (RoadPoint source : sources) {
                Map<RoadPoint, List<Road>> routes =
                        algo.route(source, targets, new TimePriority(), new Distance(), 10000.0);

                for (List<Road> route : routes.values()) {
                    if (route != null) {
                        valids += 1;
                    }
                }
            }

            sw.stop();

            logger.info("{} x {} routes with {} ({}) valid ({} ms)", sources.size(), targets.size(),
                    valids, sources.size() * targets.size(), sw.ms());

            sample1 = sample2;
        }
    }

    @Test
    public void testMSMT1() {
        Set<RoadPoint> _sources = map.spatial().radius(new Point(11.58551, 48.17705), 100);
        Set<RoadPoint> _targets = map.spatial().radius(new Point(11.57318, 48.17802), 100);

        assertTrue(!_sources.isEmpty());
        assertTrue(!_targets.isEmpty());

        HashSet<RoadPoint> sources = new HashSet<>();
        for (RoadPoint source : _sources) {
            sources.add(source);
        }

        HashSet<RoadPoint> targets = new HashSet<>();
        for (RoadPoint target : _targets) {
            targets.add(target);
        }

        Stopwatch sw = new Stopwatch();
        sw.start();
        Dijkstra<Road, RoadPoint> algo = new Dijkstra<>();
        @SuppressWarnings("unused")
        Map<RoadPoint, Tuple<RoadPoint, List<Road>>> routes =
                algo.route(sources, targets, new TimePriority());
        sw.stop();

        logger.info("MSMT (fastest, priority): {} ms", sw.ms());

        sw.start();
        Map<RoadPoint, Map<RoadPoint, List<Road>>> routes2 = new HashMap<>();
        for (RoadPoint source : sources) {
            Map<RoadPoint, List<Road>> paths = new HashMap<>();
            for (RoadPoint target : targets) {
                paths.put(target, algo.route(source, target, new TimePriority()));
            }
            routes2.put(source, paths);
        }
        sw.stop();

        logger.info("m x n routes (fastest, priority): {} ms", sw.ms());
    }

    @Test
    public void testMSMT2() {
        Set<RoadPoint> _sources = map.spatial().radius(new Point(11.58551, 48.17705), 100);
        Set<RoadPoint> _targets = map.spatial().radius(new Point(11.57318, 48.17802), 100);

        assertTrue(!_sources.isEmpty());
        assertTrue(!_targets.isEmpty());

        HashSet<RoadPoint> sources = new HashSet<>();
        for (RoadPoint source : _sources) {
            sources.add(source);
        }

        HashSet<RoadPoint> targets = new HashSet<>();
        for (RoadPoint target : _targets) {
            targets.add(target);
        }

        Stopwatch sw = new Stopwatch();
        sw.start();
        Dijkstra<Road, RoadPoint> algo = new Dijkstra<>();
        @SuppressWarnings("unused")
        Map<RoadPoint, Tuple<RoadPoint, List<Road>>> routes =
                algo.route(sources, targets, new Time());
        sw.stop();


        logger.info("MSMT (fastest): {} ms", sw.ms());

        sw.start();
        Map<RoadPoint, Map<RoadPoint, List<Road>>> routes2 = new HashMap<>();
        for (RoadPoint source : sources) {
            Map<RoadPoint, List<Road>> paths = new HashMap<>();
            for (RoadPoint target : targets) {
                paths.put(target, algo.route(source, target, new Time()));
            }
            routes2.put(source, paths);
        }
        sw.stop();

        logger.info("m x n routes (fastest): {} ms", sw.ms());
    }
}
