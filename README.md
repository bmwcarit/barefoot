# Barefoot

An open source java library (Apache 2.0) for integrating the map into software and services with state-of-the-art map matching that can be used stand-alone and in the cloud. Together with OpenStreetMap data it provides a versatile foundation for scalable location-based services, spatio-temporal data analysis and machine learning on the map.

#### Extensive

Extensive set of functions for geographic operations (GeographicLib), spatial operations and standard formats (ESRI Geometry API) with access to map data (OpenStreetMap).

#### Versatile

Use as programming library, stand-alone map matching service or in scalable environments such as Apache Hadoop, Apache Spark, and Apache Storm, cf. [3].

#### Open

Brings together Open Source software (GeographicLib, ESRI Geometry API, ...) and Open Data (OpenStreetMap) under Apache 2.0.

#### State-of-the-art

Provides state-of-the-art solutions to map matching (Hidden Markov Model map matching, cf. [1] and [2]) and spatial cluster analysis (DBSCAN, cf. [4]).

## Barefoot (eco-)system

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/barefoot-ecosystem.png?raw=true" width="650">
</p>

#### Barefoot map

Fast and easy setup of Barefoot map server (as Docker container) with any OpenStreetMap extract you need. Automatic installation of database (PostgreSQL/PostGIS) and map/import tools (Osmosis and Barefoot).

#### Barefoot library

Java library for integrating the map into software and services with map-matching as basis for spatio-temporal analysis on the map.

#### Map-matching server (stand-alone)

Quick and easy to deploy stand-alone map matching service for requests over the network locally or in the cloud.

#### Scale and distribute

Support for integration in scalable and distributed environments for offline/batch processing, e.g. Apache Hadoop and Apache Spark, and online services, e.g. Apache Storm and Apache Spark Streaming, cf. [3]

## Documentation

### Manual

See [here](MANUAL.md).

### Javadoc

See [here](http://bmwcarit.github.io/barefoot/doc/index.html).

## Showcases

### Map matching

Map matching of a GPS trace (violet markers) in Munich city area shown as geometrical path (orange path).

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/matcher/matching-satellite.png?raw=true" width="700">
<br/>
<a href="https://www.mapbox.com/about/maps/">&#xA9; Mapbox</a> <a href="http://www.openstreetmap.org/">&#xA9; OpenStreetMap</a> <a href="https://www.mapbox.com/map-feedback/"><b>Improve this map</b></a> <a href="https://www.digitalglobe.com/">&#xA9; DigitalGlobe</a> <a href="http://geojson.io">&#xA9; geojson.io</a>
</p>

#### Map matching server (quick start)

##### Map server

_Note: The following example uses the setup of the test map server. For further details, see the [manual](MANUAL.md#map-server)._

1. Install prerequisites.

  - Docker (version 1.6 or higher, see [https://docs.docker.com/installation/ubuntulinux/](https://docs.docker.com/installation/ubuntulinux/))
  - Maven (e.g. with `sudo apt-get install maven`)
  - Java JDK (Java version 7 or higher, e.g. with `sudo apt-get install openjdk-1.7-jdk`)

2. Download OSM extract (examples require `oberbayern.osm.pbf`)

  ``` bash
curl http://download.geofabrik.de/europe/germany/bayern/oberbayern-latest.osm.pbf -o barefoot/docker/osm/oberbayern.osm.pbf
  ```

3. Build Docker image.

  ``` bash
cd barefoot
sudo docker build --rm=true -t barefoot ./docker
  ```

4. Create Docker container.

  ``` bash
sudo docker run -t -i -p 127.0.0.1:5432:5432 --name="barefoot-oberbayern" \
  -v ${PWD}/bfmap/:/mnt/bfmap -v ${PWD}/docker/osm/:/mnt/osm barefoot
  ```

5. Import OSM extract (in the container).
  
  ``` bash
root@acef54deeedb# bash /mnt/osm/import.sh
  ```

  _Note: To detach the interactive shell from a running container without stopping it, use the escape sequence Ctrl-p + Ctrl-q._

6. Make sure the container is running ("up").

  ``` bash
sudo docker ps -a
...
  ```

  _Note: The output of the last command should show status 'Up x seconds'._

##### Map matching server

_Note: The following example is a quick start setup. For further details, see the [manual](MANUAL.md#map-matching-server-stand-alone)._

1. Assemble Barefoot JAR package with dependencies. (Includes the executable main class of the server.)

  ``` bash
mvn compile assembly:single
  ```

2. Start server with standard configuration for map server and map matching, and option for GeoJSON output format.

  ``` bash
java -jar target/barefoot-0.0.1-jar-with-dependencies.jar --geojson config/server.properties config/oberbayern.properties
  ```

  _Note: Stop server with Ctrl-c._

3. Test setup with a provided sample data.

  ``` bash
cat src/test/resources/com/bmwcarit/barefoot/matcher/x0001-015.json | netcat 127.0.0.1 1234
SUCCESS
...
  ```

  _Note: On success, i.e. result code is SUCCESS, the output can be visualized with [http://geojson.io/](http://geojson.io/) and should show the same path as in the figure above. Otherwise, result code is either TIMEOUT or ERROR._

#### Map matching API (online and offline)

The Barefoot library provides an easy-to-use HMM map matching API for online and offline map matching.

- In (near) real-time systems, objects measure their position with some frequency producing a stream of position measurement. Each position measurement of the stream is map matched right away, which is referred to as __online map matching__.
- In contrast, many applications use batches of position measurements recorded some time in the past. Map matching such a batches is referred to as __offline map matching__.

The following code shows the exemplary usage of Barefoot's HMM map matching API for both, online and offline map matching. For further API details and information on the theory of HMM map matching, see the [manual](MANUAL.md#map-matching-api).

``` java
import com.bmwcarit.barefoot.matcher.Matcher;
import com.bmwcarit.barefoot.matcher.KState;
import com.bmwcarit.barefoot.matcher.MatcherCandidate;
import com.bmwcarit.barefoot.matcher.MatcherSample;
import com.bmwcarit.barefoot.matcher.MatcherTransition;
import com.bmwcarit.barefoot.road.PostGISReader;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.roadmap.Route;
import com.bmwcarit.barefoot.roadmap.TimePriority;
import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.topology.Dijkstra;

import com.esri.core.geometry.Point;

// Load and construct road map
RoadMap map = RoadMap.Load(new PostGISReader(...));
map.construct();

// Instantiate matcher and state data structure
Matcher matcher = new Matcher(map, new Dijkstra<Road, RoadPoint>(),
			new TimePriority(), new Geography());
KState<MatcherCandidate, MatcherTransition, MatcherSample> state =
			new KState<MatcherCandidate, MatcherTransition, MatcherSample>();

// Input as sample batch (offline) or sample stream (online)
List<MatcherSample> samples = new LinkedList<MatcherSample>();

// Iterative map matching of sample batch (offline) or sample stream (online)
for (MatcherSample sample : samples) {
	Set<MatcherCandidate> vector = matcher.execute(state.vector(), state.sample(),
			sample);
	state.update(vector, sample);

	// Online map matching result
	MatcherCandidate estimate = state.estimate(); // most likely position estimate

	long id = estimate.point().edge().id(); // road id
	Point position = estimate.point().geometry(); // position
	Route route = estimate.transition().route(); // route to position
}

// Offline map matching results
List<MatcherCandidate> sequence = state.sequence(); // most likely sequence of positions
```

_Note: For scaling and distributing a map matching service, objects' states need to be distributed (e.g. send to other machines in a cloud). This is supported by the provided functions for JSON representation of an object's state. This way object states can be sent to or stored on remote machines and even different systems._

### Spatial search and operations

#### Spatial operations

A straight line between two points, here Reykjavik (green marker) and Moskva (blue marker), on the earth surface is  a geodesic (orange). The closest point on a geodesic to another point, here Berlin (violet marker), is referred to as the interception point (red marker).

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/spatial/intercept-satellite.png?raw=true" width="700">
<br/>
<a href="https://www.mapbox.com/about/maps/">&#xA9; Mapbox</a> <a href="http://www.openstreetmap.org/">&#xA9; OpenStreetMap</a> <a href="https://www.mapbox.com/map-feedback/"><b>Improve this map</b></a> <a href="https://www.digitalglobe.com/">&#xA9; DigitalGlobe</a> <a href="http://geojson.io">&#xA9; geojson.io</a>
</p>

``` java
import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.spatial.SpatialOperator;

import com.esri.core.geometry.Point;

SpatialOperator spatial = new Geography();

Point reykjavik = new Point(-21.933333, 64.15);
Point moskva = new Point(37.616667, 55.75);
Point berlin = new Point(13.408056, 52.518611);

double f = spatial.intercept(reykjavik, moskva, berlin);
Point interception = spatial.interpolate(reykjavik, moskva, f);
```

Other spatial operations and formats provided with GeographicLib and ESRI Geometry API:

- Geodesics on ellipsoid of rotation (lines on earth surface)
- Calculation of distances, interception, intersection, etc.
- WKT (well-known-text) import/export
- GeoJSON import/export
- Geometric operations (convex hull, overlap, contains, etc.)
- Quad-tree spatial index

#### Spatial search

Spatial radius search in the road map given a center point (red marker) returns road segments (colored lines) and closest points (colored markers) on the road.

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/spatial/radius-satellite.png?raw=true" width="700">
<br/>
<a href="https://www.mapbox.com/about/maps/">&#xA9; Mapbox</a> <a href="http://www.openstreetmap.org/">&#xA9; OpenStreetMap</a> <a href="https://www.mapbox.com/map-feedback/"><b>Improve this map</b></a> <a href="https://www.digitalglobe.com/">&#xA9; DigitalGlobe</a> <a href="http://geojson.io">&#xA9; geojson.io</a>
</p>

``` java
import com.bmwcarit.barefoot.road.PostGISReader;
import com.bmwcarit.barefoot.road.RoadReader;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.roadmap.RoadPoint;

import com.esri.core.geometry.GeometryEngine;

RoadReader reader = new PostGISReader(...);
RoadMap map = RoadMap.Load(reader);
map.construct();

Point c = new Point(11.550474464893341, 48.034123185269095);
double r = 50; // radius search within 50 meters
Set<RoadPoint> points = map.spatial().radius(c, r);

for (RoadPoint point : points) {
	GeometryEngine.geometryToGeoJson(point.geometry()));
	GeometryEngine.geometryToGeoJson(point.edge().geometry()));
}
```

The provided spatial search operations are:

- radius
- nearest
- k-nearest neighbors

### Simple routing (Dijkstra)

TBD.

### Spatial cluster analysis

Spatial cluster analysis of trip start and target points for a New York City taxi driver in January 2013. (The shown example is data of the New York hack license BA96DE419E711691B9445D6A6307C170. For details of the dataset, see below.)

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/analysis/dbscan-satellite.png?raw=true" width="700">
<br/>
<a href="https://www.mapbox.com/about/maps/">&#xA9; Mapbox</a> <a href="http://www.openstreetmap.org/">&#xA9; OpenStreetMap</a> <a href="https://www.mapbox.com/map-feedback/"><b>Improve this map</b></a> <a href="https://www.digitalglobe.com/">&#xA9; DigitalGlobe</a> <a href="http://geojson.io">&#xA9; geojson.io</a>
</p> 

``` java
import com.bmwcarit.barefoot.analysis.DBSCAN;

import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.Point;

List<Point> points = new LinkedList<Point>();
...
// DBSCAN algorithm with radius neighborhood of 100 and minimum density of 10
Set<List<Point>> clusters = DBSCAN.cluster(points, 100, 10);

for (List<Point> cluster : clusters) {
	MultiPoint multipoint = new MultiPoint();
	for (Point point : cluster) {
		multipoint.add(point);
	}
	GeometryEngine.geometryToGeoJson(multipoint);
}
```

## License

Copyright 2015 BMW Car IT GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Maintainer(s)

- Sebastian Mattheis

## Contributors

- Richard Zinck (review and feedback)
- Muthu Kumar Kumar (visualization for debugging)
- Sahbi Chaieb (map matching evaluation)
- Bastian Beggel (in-memory road map review and evaluation)

## Dependencies

##### Barefoot library

_These dependencies are linked only dynamically in the Java source of Barefoot. They can be resolved usually automatically with Maven. For details, see [NOTICE.txt](NOTICE.txt)._

* ESRI Geometry API, Apache License 2.0 ([https://github.com/Esri/geometry-api-java](https://github.com/Esri/geometry-api-java))
 * Java JSON (see below)
 * Jackson, Apache License 2.0 ([https://github.com/FasterXML/jackson-core](https://github.com/FasterXML/jackson-core))
* GeographicLib-Java, MIT License ([http://geographiclib.sourceforge.net/](http://geographiclib.sourceforge.net/))
* Java JSON, MIT License with extra clause "The Software shall be used for Good, not Evil." ([http://www.json.org/java/index.html](http://www.json.org/java/index.html))
* PostgreSQL JDBC, BSD 3-Clause ([http://jdbc.postgresql.org/about/license.html](http://jdbc.postgresql.org/about/license.html))
* JUnit, CPL-1.0 ([https://github.com/junit-team/junit](https://github.com/junit-team/junit))
 * Hamcrest, BSD 3-Clause ([https://github.com/hamcrest/JavaHamcrest](https://github.com/hamcrest/JavaHamcrest))
* SLF4J, MIT License ([http://slf4j.org](http://slf4j.org))
* Logback, LGPL 2.1 ([http://logback.qos.ch](http://logback.qos.ch/))
 * SLF4J (see above)
 * Logback core, LGPL 2.1 ([http://logback.qos.ch](http://logback.qos.ch/))

##### Barefoot map

_These dependencies are not linked in any source but used for setting up map servers._

* Docker, Apache License 2.0 ([https://www.docker.com](https://www.docker.com))<br/>
  _Note: Docker is used only as a tool for setting up map databases, e.g. in the test setup._
* PostgreSQL, PostgreSQL License ([http://www.postgresql.org/](http://www.postgresql.org/))
* PostGIS, GPL-2.0 ([http://postgis.net/](http://postgis.net/))
* Osmosis, GPL-3 ([https://github.com/openstreetmap/osmosis](https://github.com/openstreetmap/osmosis))<br/>
  _Note: A single file from Osmosis project, i.e. pgsql_simple_schema_0.6.sql, is included in directory docker/osm but is not compiled in any binary._
* OpenStreetMap, ODbL ([http://download.geofabrik.de](http://download.geofabrik.de))<br/>
  _Note: A sample, i.e. oberbayern.osm.pbf, is included in directory docker/osm but is not compiled in any binary._

##### Barefoot map tools

_These dependencies are linked only dynamically in the source of map server tools for populating data to the map server._

* psycopg 'psycopg2', LGPL-3.0 ([http://initd.org/psycopg/license/](http://initd.org/psycopg/license/))
* NumPy 'numpy', BSD 3-Clause ([http://www.numpy.org/](http://www.numpy.org/))
* GDAL OGR 'osgeo.ogr' 1.10, MIT License ([http://trac.osgeo.org/gdal/wiki/GdalOgrInPython](http://trac.osgeo.org/gdal/wiki/GdalOgrInPython))
* Python Standard Library 2.7.3, Python License ([https://docs.python.org/2/license.html](https://docs.python.org/2/license.html))
 * 'os' ([https://docs.python.org/2/library/os.html](https://docs.python.org/2/library/os.html))
 * 'optparse' ([https://docs.python.org/2/library/optparse.html](https://docs.python.org/2/library/optparse.html))
 * 'getpass' ([https://docs.python.org/2/library/getpass.html](https://docs.python.org/2/library/getpass.html))
 * 'binascii' ([https://docs.python.org/2/library/binascii.html](https://docs.python.org/2/library/binascii.html))
 * 'json' ([https://docs.python.org/2/library/json.html](https://docs.python.org/2/library/json.html))
 * 'unittest' ([https://docs.python.org/2/library/unittest.html](https://docs.python.org/2/library/unittest.html))

##### Documentation
* OpenJUMP, GPL-2.0 ([http://www.openjump.org](http://www.openjump.org))<br/>
  _Note: OpenJUMP project files included in directory openjump for test and debugging purposes._
* Documents and graphics, CC BY 4.0 ([http://creativecommons.org/licenses/by/4.0/legalcode](http://creativecommons.org/licenses/by/4.0/legalcode))<br/>
  _Note: The documentation includes PNG, PDF, TikZ/LaTeX, and Markdown files for this project (mainly included in directory doc-files) and is licensed under CC BY 4.0._

##### Datasets

* Some tests and examples use an extract of NYC taxi data which is included in the source repository. The data is licensed under CC0 license (Public Domain). For details see:
  _Brian Donovan and Daniel B. Work  “New York City Taxi Trip Data (2010-2013)”. 1.0. University of Illinois at Urbana-Champaign. Dataset. http://dx.doi.org/10.13012/J8PN93H8, 2014._

## References

[1] P. Newson and J. Krumm. Hidden Markov Map Matching Through Noise and Sparseness. In _Proceedings of International Conference on Advances in Geographic Information Systems_, 2009.

[2] C.Y. Goh, J. Dauwels, N. Mitrovic, M.T. Asif, A. Oran, and P. Jaillet. Online map-matching based on Hidden Markov model for real-time traffic sensing applications. In _International IEEE Conference on Intelligent Transportation Systems_, 2012.

[3] S. Mattheis, K. Al-Zahid, B. Engelmann, A. Hildisch, S. Holder, O. Lazarevych, D. Mohr, F. Sedlmeier, and R. Zinck. Putting the car on the map: A scalable map matching system for the Open Source Community. In _INFORMATIK 2014: Workshop Automotive Software Engineering_, 2014.

[4] M. Ester, H.-P. Kriegel, J. Sander, X. Xu. A Density-based algorithm for discovering clusters in large spatial databases with noise. In _Proceedings of the Second International Conference on Knowledge Discovery and Data Mining (KDD-96)_, 1996. 