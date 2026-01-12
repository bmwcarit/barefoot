# Barefoot

An open source Java library for online and offline map matching with OpenStreetMap. Together with its extensive set of geometric and spatial functions, an in-memory map data structure and basic machine learning functions, it is a versatile basis for scalable location-based services and spatio-temporal data analysis on the map. It is designed for use in parallel and distributed systems and, hence, includes a stand-alone map matching server and can be used in distributed systems for map matching services in the cloud.

#### Flexible and extensive

Barefoot consists of a software library and a (Docker-based) map server that provides access to street map data from OpenStreetMap and is flexible to be used in distributed cloud infrastructures as map data server or side-by-side with Barefoot's stand-alone servers for offline (matcher server) and online map matching (tracker server), or other applications built with Barefoot library. Access to map data is provided with a fast and flexible in-memory map data structure. Together with GeographicLib [1] and ESRI's geometry API [2], it provides an extensive set of geographic and geometric operations for spatial data analysis on the map.

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/barefoot-ecosystem.png?raw=true" width="650">
</p>

#### State-of-the-art map matching

Barefoot includes a Hidden Markov Model map matching implementation for both, offline map matching as proposed by Newson and Krumm in [3] and online map matching as proposed by Goh et al. in [4]. Offline map matching is the path reconstruction of a moving object from a recorded GPS trace. In contrast, online map matching determines an object's position and movement on the map iteratively from live GPS position updates in real-time.

#### Scalable and versatile

Barefoot is designed for use in parallel and distributed high-throughput systems [5]. For map matching large batches of GPS traces (offline map matching), it can be easily integrated in Apache Hadoop or Apache Spark (see example below), whereas Apache Storm and Apache Spark Streaming provide a runtime environment for processing massive data streams (online map matching). To support other data analysis functions, Barefoot comes with basic machine learning support, e.g., DBSCAN for spatial cluster analysis [6].

#### Open source and open data

Barefoot is licensed under the business-friendly Apache License 2.0 and uses only business-friendly open source software with open map data from OpenStreetMap.

## BAREFOOT-DOCKER-READY

Barefoot is now ready to run with Docker Compose! This is the easiest way to get started with the full stack (Map Server, Matcher Server, and Tracker Server).

### Quick Start

1. **Start all services:**

    ```bash
    docker compose up -d
    ```

    > [!IMPORTANT]
    > **Before starting:** You must provide an OpenStreetMap PBF file.
    > 1. Download your desired map region (e.g., from [Geofabrik](http://download.geofabrik.de/)).
    > 2. Place it at `./map/osm/map.osm.pbf` (or update the volume mapping in `docker-compose.yml` to point to your file).
    > 3. Ensure the file is readable.

    This command will:
    * Pull the latest images from GitHub Container Registry:
        * `ghcr.io/joseedsouza/barefoot/barefoot-map:latest`
        * `ghcr.io/joseedsouza/barefoot/barefoot-matcher:latest`
        * `ghcr.io/joseedsouza/barefoot/barefoot-tracker:latest`
    * Start all three containers in the background.

2. **Access the services:**

    * **Map Server:** Running on port `5438` (mapped to internal `5432`).
    * **Matcher Server:** Running on port `1234`.
    * **Tracker Server:** Running on port `1235` (tracker) and `1236` (state).

3. **Verify:**

    You can check the status of the containers with:

    ```bash
    docker compose ps
    ```

    To view logs:

    ```bash
    docker compose logs -f
    ```

### Building from Source (Local Development)

If you want to build the images locally (e.g., for development or if you've made changes to the code), use the `local.docker-compose.yaml` file:

```bash
docker compose -f local.docker-compose.yaml up -d --build
```

This will build the images from the local context instead of pulling them from the registry.

### Configuration & Features

The `docker-compose.yml` file is pre-configured with sensible defaults, but it's designed to be flexible.

#### 1. Environment Variables

You can configure the servers using environment variables in `docker-compose.yml`. The entrypoint scripts (`docker/matcher/entrypoint.sh` and `docker/tracker/entrypoint.sh`) automatically map these variables to the properties files.

* **Mapping Logic:**
    1. The script looks for variables starting with a specific prefix.
    2. It removes the prefix.
    3. It converts the rest of the name to **lowercase**.
    4. It replaces all underscores `_` with dots `.`.
  * *Example:* `SERVER__MATCHER_THREADS=8` becomes `matcher.threads=8`.

* **Prefixes:**
  * **Matcher Server:**
    * `SERVER__`: Maps to `config/server.properties` (e.g., `SERVER__SERVER_PORT`).
    * `MAP__`: Maps to `config/map.properties`.
  * **Tracker Server:**
    * `TRACKER__`: Maps to `config/tracker.properties` (e.g., `TRACKER__TRACKER_STATE_TTL`).
    * `MAP__`: Maps to `config/map.properties`.

* **Defaults:** The entrypoint scripts check if the configuration files exist. If not, they create them with default values (e.g., `matcher.threads=8`, `database.host=localhost`). This ensures the container runs out-of-the-box.

#### 2. Automatic Map Import

The `map-server` is smart! On the first initialization, if the database is empty, it automatically imports the OpenStreetMap data.

* **Process:**
    1. **Check:** The entrypoint (`map/entrypoint.sh`) checks if the configured database exists.
    2. **Import:** If not, it executes `map/osm/import.sh`.
        * **Database Setup:** Creates the database and installs PostGIS extensions.
        * **Osmosis:** Uses `osmosis` to read the PBF file (`MAP_OSM_PBF_PATH`) and populate the database.
        * **Routing Data:** Uses `osm2ways.py` and `ways2bfmap.py` to extract routing topology and create the `bfmap_ways` table.
    3. **Start:** Finally, it starts the PostgreSQL server in the foreground.

* **Configuration:**
  * `MAP_OSM_PBF_PATH`: Path to the input PBF file (default: `/mnt/data/map.osm.pbf`).
  * `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`: Database credentials.
  * `MAP_MODE`: Import mode (`slim` or `normal`).

* **Persistence:** The data is stored in the `barefoot-db` volume, so the import only happens once. Subsequent restarts are instant.

#### 3. Port Mapping

The services are exposed on the following ports:

* **Map Server (`5438:5432`):** The PostGIS database is accessible on host port `5438`. This allows you to connect with tools like QGIS or `psql` without conflicts if you have a local Postgres running on 5432.
* **Matcher Server (`1234:1234`):** The API for map matching.
* **Tracker Server:**
  * `1235:1234`: The tracker API port (mapped to internal 1234).
  * `1236:1235`: The tracker state port (mapped to internal 1235).

#### 4. Default Configurations

The base configuration files are located in the `./config` directory of the repository.

* These files (`server.properties`, `map.properties`, etc.) serve as the template.
* The Docker images use these templates and apply the environment variable overrides at runtime.

## Documentation

### Manual

See [wiki](https://github.com/bmwcarit/barefoot/wiki).

### Javadoc

See [Javadoc](http://bmwcarit.github.io/barefoot/doc/index.html).

## Showcases and Quick Starts

### Online and offline HMM map matching

Barefoot provides a HMM map matching solution that can be used via the software library API, see the [wiki](https://github.com/bmwcarit/barefoot/wiki#hmm-map-matching), or via REST-like APIs provided with the stand-alone servers (matcher and tracker servers), see below or the [wiki](https://github.com/bmwcarit/barefoot/wiki#stand-alone-servers). This map matching solution covers both, online and offline map matching:

* **Offline map matching**: Most map matching applications rely on the matching of a sequence of position measurements recorded in the past (traces) for reconstruction of the object's path on the map. Offline map matching finds the best matching  on the map and exploits availability of the full trace.
* **Online map matching**: In many other applications, objects send position updates to some monitoring system periodically. An online map matching system matches each position update right away and, hence, keeps track of the objects' movements on the map in (near) real-time.

Accuracy of map matching depends mostly on the quality and quantity of input data, which consists of a sequence of measurement samples over time (including position measurement). Samples are submitted as a whole sequence for offline map matching or one after another for online map matching. A single sample includes the following information:

* **time** of the measurement sample which is given in unix time.
* **position** of the object (point in space, e.g. GPS measurement).
* **heading** of the object (azimuth measurement) which is optional and, if available, increases map matching accuracy.

#### Matcher server (Quick Start)

Map matching of a GPS trace (violet markers) in Munich city area shown as geometrical path (orange path)

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/matcher/matching-satellite.png" width="700">
<br/>
<a href="https://www.mapbox.com/about/maps/">&#xA9; Mapbox</a> <a href="http://www.openstreetmap.org/">&#xA9; OpenStreetMap</a> <a href="https://www.mapbox.com/map-feedback/"><b>Improve this map</b></a> <a href="https://www.digitalglobe.com/">&#xA9; DigitalGlobe</a> <a href="http://geojson.io">&#xA9; geojson.io</a>
</p>

##### Map server

*Note: The following example uses the setup of the test map server. For further details, see the [wiki](https://github.com/bmwcarit/barefoot/wiki#map-server).*

1. Install prerequisites.

    * Docker Engine (version 1.6 or higher, see [https://docs.docker.com/installation/ubuntulinux/](https://docs.docker.com/installation/ubuntulinux/))

2. Download OSM extract (examples require `oberbayern.osm.pbf`)

    ``` bash
    curl http://download.geofabrik.de/europe/germany/bayern/oberbayern-latest.osm.pbf -o barefoot/map/osm/oberbayern.osm.pbf
    ```

3. Build Docker image.

    ``` bash
    cd barefoot
    sudo docker build -t barefoot-map ./map
    ```

4. Create Docker container.

    ``` bash
    sudo docker run -it -p 5432:5432 --name="barefoot-oberbayern" -v ${PWD}/map/:/mnt/map barefoot-map
    ```

5. Import OSM extract (in the container).
  
    ``` bash
    root@acef54deeedb# bash /mnt/map/osm/import.sh
    ```

    *Note: To detach the interactive shell from a running container without stopping it, use the escape sequence Ctrl-p + Ctrl-q.*

6. Make sure the container is running ("up").

    ``` bash
    sudo docker ps -a
    ...
    ```

    *Note: The output of the last command should show status 'Up x seconds'.*

##### Matcher server

*Note: The following example is a quick start setup. For further details, see the [wiki](https://github.com/bmwcarit/barefoot/wiki#matcher-server).*

1. Install prerequisites.

    * Maven (e.g. with `sudo apt-get install maven`)
    * Java JDK (Java version 7 or higher, e.g. with `sudo apt-get install openjdk-1.7-jdk`)

2. Package Barefoot JAR. (Includes dependencies and executable main class.)

    ``` bash
    mvn package
    ```

    *Note: Add `-DskipTests` to skip tests.*

3. Start server with standard configuration for map server and map matching, and option for GeoJSON output format.

    ``` bash
    java -jar target/barefoot-<VERSION>-matcher-jar-with-dependencies.jar --geojson config/server.properties config/oberbayern.properties
    ```

    *Note: Stop server with Ctrl-c.*

    *Note: In case of 'parse errors', use the following Java options: `-Duser.language=en -Duser.country=US`*

4. Test setup with provided sample data.

    ``` bash
    python util/submit/batch.py --host localhost --port 1234  --file src/test/resources/com/bmwcarit/barefoot/matcher/x0001-015.json
    SUCCESS
    ...
    ```

    *Note: On success, i.e. result code is SUCCESS, the output can be visualized with [http://geojson.io/](http://geojson.io/) and should show the same path as in the figure above. Otherwise, result code is either TIMEOUT or ERROR.*

#### Tracker server (Quick Start)

Online (real-time) map matching of a GPS trace in Munich city area with most likely position (blue dot) and alternative possible positions and routes (green dots and paths with transparency according to their probability). Alternative positions and routes disappear with continuously processed updates, which shows the principle of online map matching converging alternatives over time.

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/tracker/monitor-1600x1000.gif" width="650">
<br/>
<a href="https://www.mapbox.com/about/maps/">&#xA9; Mapbox</a> <a href="http://www.openstreetmap.org/">&#xA9; OpenStreetMap</a> <a href="https://www.mapbox.com/map-feedback/"><b>Improve this map</b></a>
</p>

##### Map server

(see above)

##### Tracker server

*Note: The following example is a quick start setup. For further details, see the [wiki](https://github.com/bmwcarit/barefoot/wiki#tracker-server).*

1. Install prerequisites.

    * Maven (e.g. with `sudo apt-get install maven`)
    * Java JDK (Java version 7 or higher, e.g. with `sudo apt-get install openjdk-1.7-jdk`)
    * ZeroMQ (e.g. with `sudo apt-get install libzmq3-dev`)
    * NodeJS (e.g. with `sudo apt-get install nodejs`)

2. Package Barefoot JAR. (Includes dependencies and executable main class.)

    ``` bash
    mvn package
    ```

    *Note: Add `-DskipTests` to skip tests.*

3. Start tracker with standard configuration for map server, map matching, and tracking.

    ``` bash
    java -jar target/barefoot-<VERSION>-tracker-jar-with-dependencies.jar config/tracker.properties config/oberbayern.properties
    ```

    *Note: Stop server with Ctrl-c.*

    *Note: In case of 'parse errors', use the following Java options: `-Duser.language=en -Duser.country=US`*

4. Install and start monitor (NodeJS server).

    Install (required only once)

    ``` bash
    cd util/monitor && npm install && cd ../..
    ```

    ... and start:

    ``` bash
    node util/monitor/monitor.js 3000 127.0.0.1 1235
      ```

### Tracker Payload

The tracker server publishes state updates as JSON messages. Each update includes the following fields:

* **id**: Object identifier.
* **time**: Timestamp of the update.
* **point**: Matched position on the map.
* **osm_id**: OpenStreetMap ID of the road segment.
* **osm_type**: Type of the road (e.g., motorway, residential).
* **edge_gid**: Internal ID of the road segment.
* **source**: Source node ID of the road segment.
* **target**: Target node ID of the road segment.
* **path_osm_ids**: List of OpenStreetMap IDs representing the trajectory (path) sequence.

### Matcher Payload

The matcher server (when using `GeoJSONOutputFormatter`) provides a GeoJSON response with map matching results. Significant fields include:

* **path_osm_ids**: List of OpenStreetMap IDs representing the matched trajectory sequence, deduplicated to show unique road segments in order. (Note: Only available in `geojson` format).

5. Test setup with provided sample data.

    ``` bash
    python util/submit/stream.py --host localhost --port 1234 --file src/test/resources/com/bmwcarit/barefoot/matcher/x0001-001.json
    SUCCESS
    ...
    ```

    *Note: On success, i.e. result code is SUCCESS, the tracking is visible in the browser on [http://localhost:3000](http://localhost:3000). Otherwise, result code is either TIMEOUT or ERROR.*

### Spatial search and operations

#### Spatial operations

A straight line between two points, here Reykjavik (green marker) and Moskva (blue marker), on the earth surface is  a geodesic (orange). The closest point on a geodesic to another point, here Berlin (violet marker), is referred to as the interception point (red marker).

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/spatial/intercept-satellite.png" width="700">
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

* Geodesics on ellipsoid of rotation (lines on earth surface)
* Calculation of distances, interception, intersection, etc.
* WKT (well-known-text) import/export
* GeoJSON import/export
* Geometric operations (convex hull, overlap, contains, etc.)
* Quad-tree spatial index

#### Spatial search

Spatial search in the road map requires access to spatial data of the road map and spatial operations for distance and point-to-line projection. Barefoot implements the following basic search operations:

* radius
* nearest
* k-nearest (kNN)

The following code snippet shows a radius search given a certain map:

``` java
import com.bmwcarit.barefoot.roadmap.Loader;
import com.bmwcarit.barefoot.roadmap.Road;
import com.bmwcarit.barefoot.roadmap.RoadMap;

import com.esri.core.geometry.GeometryEngine;

RoadMap map = Loader.roadmap("config/oberbayern.properties", true).construct();

Point c = new Point(11.550474464893341, 48.034123185269095);
double r = 50; // radius search within 50 meters
Set<RoadPoint> points = map.spatial().radius(c, r);

for (RoadPoint point : points) {
 GeometryEngine.geometryToGeoJson(point.geometry()));
 GeometryEngine.geometryToGeoJson(point.edge().geometry()));
}
```

A radius search, given a center point (red marker), returns road segments (colored lines) with their closest points (colored markers) on the road.

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/spatial/radius-satellite.png" width="700">
<br/>
<a href="https://www.mapbox.com/about/maps/">&#xA9; Mapbox</a> <a href="http://www.openstreetmap.org/">&#xA9; OpenStreetMap</a> <a href="https://www.mapbox.com/map-feedback/"><b>Improve this map</b></a> <a href="https://www.digitalglobe.com/">&#xA9; DigitalGlobe</a> <a href="http://geojson.io">&#xA9; geojson.io</a>
</p>

### Simple routing (Dijkstra)

TBD.

### Spatial cluster analysis

Spatial cluster analysis aggregates point data to high density clusters for detecting e.g. points of interest like frequent start and end points of trips. For that purpose, Barefoot includes a DBSCAN implementation for simple density-based spatial cluster analysis, which is an unsupervised machine learning algorithm. For details, see the [wiki](https://github.com/bmwcarit/barefoot/wiki#spatial-cluster-analysis).

The following code snippet shows the simple usage of the algorithm:

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

As an example, the figure below shows typical locations for standing times of a New York City taxi driver (hack license BA96DE419E711691B9445D6A6307C170) derived by spatial cluster analysis of start and end points of all trips in January 2013. For details on the data set, see below.

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/analysis/dbscan-satellite.png" width="700">
<br/>
<a href="https://www.mapbox.com/about/maps/">&#xA9; Mapbox</a> <a href="http://www.openstreetmap.org/">&#xA9; OpenStreetMap</a> <a href="https://www.mapbox.com/map-feedback/"><b>Improve this map</b></a> <a href="https://www.digitalglobe.com/">&#xA9; DigitalGlobe</a> <a href="http://geojson.io">&#xA9; geojson.io</a>
</p>

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

* Sebastian Mattheis

## Dependencies

#### Barefoot library

*The following dependencies are linked only dynamically in the Java source of Barefoot. They can be resolved usually automatically with Maven. For details, see [NOTICE.txt](NOTICE.txt).*

* ESRI Geometry API, 1.1, Apache-2.0 ([https://github.com/Esri/geometry-api-java](https://github.com/Esri/geometry-api-java))
  * Java, JSON 20090211 (see below)
  * Jackson, 1.9.12, Apache-2.0 ([https://github.com/FasterXML/jackson-core](https://github.com/FasterXML/jackson-core))
* GeographicLib-Java, 1.46, MIT License ([http://geographiclib.sourceforge.net/](http://geographiclib.sourceforge.net/))
* Java JSON, 20090211, MIT License with extra clause "The Software shall be used for Good, not Evil." ([http://www.json.org](http://www.json.org))
* PostgreSQL JDBC, 9.2-1003-jdbc4, BSD 3-Clause ([http://jdbc.postgresql.org/about/license.html](http://jdbc.postgresql.org/about/license.html))
* JUnit, 4.11, CPL-1.0 ([https://github.com/junit-team/junit](https://github.com/junit-team/junit))
  * Hamcrest, 1.3, BSD 3-Clause ([https://github.com/hamcrest/JavaHamcrest](https://github.com/hamcrest/JavaHamcrest))
* SLF4J, 1.7.10, MIT License ([http://slf4j.org](http://slf4j.org))
* Logback, 1.0.9, LGPL-2.1 ([http://logback.qos.ch](http://logback.qos.ch/))
  * SLF4J, 1.7.10 (see above)
  * Logback core, 1.0.9, LGPL-2.1 ([http://logback.qos.ch](http://logback.qos.ch/))
* JeroMQ, 0.3.5, LGPL-3.0 ([https://github.com/zeromq/jeromq](https://github.com/zeromq/jeromq))

#### Barefoot map

*The following dependencies are not linked in any source but used for setting up map servers.*

* Docker, Apache License 2.0 ([https://www.docker.com](https://www.docker.com))<br/>
  *Note: Docker is used only as a tool for setting up map databases, e.g. in the test setup.*
* PostgreSQL, PostgreSQL License ([http://www.postgresql.org/](http://www.postgresql.org/))
* PostGIS, GPL-2.0 ([http://postgis.net/](http://postgis.net/))
* Osmosis, GPL-3 ([https://github.com/openstreetmap/osmosis](https://github.com/openstreetmap/osmosis))<br/>
  *Note: A single file from Osmosis project, i.e. pgsnapshot_schema_0.6.sql, is included in directory map/osm but is not compiled in any binary.*
* OpenStreetMap, ODbL ([http://download.geofabrik.de](http://download.geofabrik.de))<br/>
  *Note: A sample, i.e. oberbayern.osm.pbf, is required for testing but not included in the source repository.*

##### Barefoot map tools

*The following dependencies are linked only dynamically in the Python source of map server tools for importing data into the map server.*

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

#### Barefoot utilities

##### Barefoot monitor

*The following dependencies are linked only dynamically in the NodeJS source of the monitor.*

* Socket.io, 1.4.4, and packages, MIT License ([http://socket.io/](http://socket.io/))
* Express, 4.13.4, and packages, MIT License ([https://github.com/strongloop/express](https://github.com/strongloop/express))
  * inherits, 2.0.1, ISC License ([https://www.npmjs.com/package/inherits](https://www.npmjs.com/package/inherits))
  * qs, 4.0.0, BSD 3-Clause ([https://www.npmjs.com/package/qs](https://www.npmjs.com/package/qs))
* ZeroMQ (NodeJS binding), 2.14.0, MIT License ([https://github.com/JustinTulloss/zeromq.node](https://github.com/JustinTulloss/zeromq.node))
  * ZeroMQ (C++), LGPL-3.0 ([https://github.com/zeromq/libzmq](https://github.com/zeromq/libzmq))

*The following dependencies are linked only dynamically in the Javascript source of the monitor.*

* OpenLayers 3, BSD 2-Clause ([https://github.com/openlayers/ol3](https://github.com/openlayers/ol3))
* jQuery, MIT License ([http://jquery.com/](http://jquery.com/))
* Socket.io.js, MIT License ([http://socket.io/](http://socket.io/))

##### Job submission scripts

*The following dependencies are linked only dynamically in the Python source of the submission scripts.*

* Requests, 2.6.2, Apache License 2.0 ([https://github.com/kennethreitz/requests](https://github.com/kennethreitz/requests))
* Python Standard Library 2.7.3, Python License ([https://docs.python.org/2/license.html](https://docs.python.org/2/license.html))
  * 'os' ([https://docs.python.org/2/library/os.html](https://docs.python.org/2/library/os.html))
  * 'sys' ([https://docs.python.org/2/library/sys.html](https://docs.python.org/2/library/sys.html))
  * 'optparse' ([https://docs.python.org/2/library/optparse.html](https://docs.python.org/2/library/optparse.html))
  * 'json' ([https://docs.python.org/2/library/json.html](https://docs.python.org/2/library/json.html))
  * 'subprocess' ([https://docs.python.org/2/library/subprocess.html](https://docs.python.org/2/library/subprocess.html))
  * 'time' ([https://docs.python.org/2/library/time.html](https://docs.python.org/2/library/time.html))
  * 'datetime' ([https://docs.python.org/2/library/datetime.html](https://docs.python.org/2/library/datetime.html))

##### Documentation

* OpenJUMP, GPL-2.0 ([http://www.openjump.org](http://www.openjump.org))<br/>
  *Note: OpenJUMP project files included in directory openjump for test and debugging purposes.*
* Documents and graphics, CC BY 4.0 ([http://creativecommons.org/licenses/by/4.0/legalcode](http://creativecommons.org/licenses/by/4.0/legalcode))<br/>
  *Note: The documentation includes PNG, PDF, TikZ/LaTeX, and Markdown files for this project (mainly included in directory doc-files) and is licensed under CC BY 4.0.*

##### Datasets

* Some tests and examples use an extract of NYC taxi data which is included in the source repository. The data is licensed under CC0 license (Public Domain). For details see:
  *Brian Donovan and Daniel B. Work  “New York City Taxi Trip Data (2010-2013)”. 1.0. University of Illinois at Urbana-Champaign. Dataset. <http://dx.doi.org/10.13012/J8PN93H8>, 2014.*

## References

[1] [GeographicLib](http://geographiclib.sourceforge.net/).

[2] [ESRI's Geometry API](https://github.com/Esri/geometry-api-java).

[3] P. Newson and J. Krumm. [Hidden Markov Map Matching Through Noise and Sparseness](http://research.microsoft.com/en-us/um/people/jckrumm/Publications%202009/map%20matching%20ACM%20GIS%20camera%20ready.pdf). In *Proceedings of International Conference on Advances in Geographic Information Systems*, 2009.

[4] C.Y. Goh, J. Dauwels, N. Mitrovic, M.T. Asif, A. Oran, and P. Jaillet. [Online map-matching based on Hidden Markov model for real-time traffic sensing applications](http://www.mit.edu/~jaillet/general/map_matching_itsc2012-final.pdf). In *International IEEE Conference on Intelligent Transportation Systems*, 2012.

[5] S. Mattheis, K. Al-Zahid, B. Engelmann, A. Hildisch, S. Holder, O. Lazarevych, D. Mohr, F. Sedlmeier, and R. Zinck. [Putting the car on the map: A scalable map matching system for the Open Source Community](http://subs.emis.de/LNI/Proceedings/Proceedings232/2109.pdf). In *INFORMATIK 2014: Workshop Automotive Software Engineering*, 2014.

[6] M. Ester, H.-P. Kriegel, J. Sander, X. Xu. [A Density-based algorithm for discovering clusters in large spatial databases with noise](https://www.aaai.org/Papers/KDD/1996/KDD96-037.pdf). In *Proceedings of the Second International Conference on Knowledge Discovery and Data Mining (KDD-96)*, 1996.
