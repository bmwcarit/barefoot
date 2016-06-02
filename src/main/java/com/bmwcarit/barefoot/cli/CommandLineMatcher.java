package com.bmwcarit.barefoot.cli;

import com.bmwcarit.barefoot.matcher.Matcher;
import com.bmwcarit.barefoot.matcher.MatcherFactory;
import com.bmwcarit.barefoot.matcher.MatcherKState;
import com.bmwcarit.barefoot.matcher.MatcherSample;
import com.bmwcarit.barefoot.road.*;
import com.bmwcarit.barefoot.roadmap.Road;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.roadmap.RoadPoint;
import com.bmwcarit.barefoot.roadmap.TimePriority;
import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.spatial.SpatialOperator;
import com.bmwcarit.barefoot.topology.Cost;
import com.bmwcarit.barefoot.topology.Dijkstra;
import com.bmwcarit.barefoot.topology.Router;
import com.bmwcarit.barefoot.util.Tuple;
import com.esri.core.geometry.Point;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by kumare on 6/1/16.
 */
public class CommandLineMatcher {
    public static final double MATCHER_MAX_DISTANCE = 15000;
    public static final double MATCHER_MAX_RADIUS = 200;
    public static final int MATCHER_MIN_INTERVAL = 1000;
    public static final double MATCHER_MIN_DISTANCE = 0;
    public static final int MATCHER_NUM_THREADS = 8;

    public static RoadMap getDatabase() {

        String host = "localhost";
        int port = 5432;
        String database = "california";
        String table = "bfmap_ways";
        String user = "kumare";
        String password = "";
        String roadTypes = "./bfmap/road-types.json";
        File file = new File(database + ".bfmap");
        RoadMap map = null;

        Map<Short, Tuple<Double, Integer>> config = null;
        try {
            config = Configuration.read(roadTypes);
        } catch (JSONException | IOException e) {
            System.exit(1);
        }

        if (!file.exists()) {
            RoadReader reader = new PostGISReader(host, port, database, table, user, password, config);
            map = RoadMap.Load(reader);

            reader = map.reader();
            RoadWriter writer = new BfmapWriter(file.getAbsolutePath());
            BaseRoad road = null;
            reader.open();
            writer.open();

            while ((road = reader.next()) != null) {
                writer.write(road);
            }

            writer.close();
            reader.close();
        } else {
            map = RoadMap.Load(new BfmapReader(file.getAbsolutePath()));
        }
        return map;
    }


    private static void runMatcher(MatcherKState state, Matcher matcher, Iterator<MatcherSample> sampleIterator) {
        MatcherKState previousState = state;

        while (sampleIterator.hasNext()) {
            MatcherSample sample = sampleIterator.next();
            previousState = matcher.mmatch(sample, MATCHER_MIN_DISTANCE, MATCHER_MIN_INTERVAL, previousState);
        }
    }

    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.out.println("Usage: cli file_path");
            return;
        }
        String filePath = args[0];
        Iterator<MatcherSample> samples =
                Files.lines(Paths.get(filePath)).map(CommandLineMatcher::lineToSample).iterator();
        Router<Road, RoadPoint> router = new Dijkstra<Road, RoadPoint>();
        Cost<Road> cost = new TimePriority();
        SpatialOperator spatial = new Geography();

        RoadMap map = getDatabase();
        map.construct();

        Matcher matcher = new Matcher(map, router, cost, spatial);
        matcher.setMaxRadius(MATCHER_MAX_RADIUS);
        matcher.setMaxDistance(MATCHER_MAX_DISTANCE);
        matcher.setLambda(matcher.getLambda());
        matcher.setSigma(matcher.getSigma());

        MatcherKState kState = new MatcherKState();
        runMatcher(kState, matcher, samples);
    }

    private static MatcherSample lineToSample(String line) {
        String[] parts = line.split(",");
        return new MatcherSample(
                Long.parseLong(parts[2]),
                new Point(
                        Double.parseDouble(parts[0]),
                        Double.parseDouble(parts[1])
                )
        );
    }
}
