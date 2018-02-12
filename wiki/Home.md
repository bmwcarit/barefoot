## Overview

Barefoot consists of a software library and a (Docker-based) map server that provides access to street map data from OpenStreetMap and is flexible to be used in distributed cloud infrastructures as map data server or side-by-side with Barefoot's stand-alone servers for offline (matcher server) and online map matching (tracker server), or other applications built with Barefoot library. Access to map data is provided with a fast and flexible in-memory map data structure. Together with GeographicLib [1] and ESRI's geometry API [2], it provides an extensive set of geographic and geometric operations for spatial data analysis on the map.
<p align="center">
<img src="https://github.com/bmwcarit/barefoot/raw/master/doc-files/com/bmwcarit/barefoot/barefoot-ecosystem.png?raw=true" width="600">
</p>

##### References

[1] [GeographicLib](http://geographiclib.sourceforge.net/).

[2] [ESRI's Geometry API](https://github.com/Esri/geometry-api-java).

## Map Server

### Docker container

#### Setup

1. Install prerequisites.

    - Docker Engine (version 1.6 or higher, see [https://docs.docker.com/installation/ubuntulinux/](https://docs.docker.com/installation/ubuntulinux/))

2. Build Docker image.

    ``` bash
    cd barefoot
    sudo docker build -t barefoot-map ./map
    ```

3. Create Docker container.

    _Note: Give the <container> a name otherwise Docker chooses a name._

    ``` bash
    sudo docker run -it -p 5432:5432 --name="<container>" -v ${PWD}/map/:/mnt/map barefoot-map
    ```

4. Import OSM extract (inside the container).

    ``` bash
    root@acef54deeedb# bash /mnt/map/osm/import.sh <osm> <database> <user> <password> <config> slim|normal
    ```
    _Note: To detach the interactive shell from a running container without stopping it, use the escape sequence Ctrl-p + Ctrl-q._

    - For importing an OpenStreetMap extract `<osm>`, the `*.osm.pbf` file must be placed into the directory `map/osm/` from outside the container, which is then accessible inside the container at `/mnt/map/osm/`.
    - The import script reads the extract and writes it into a database with specified name and credentials, i.e. `<database>`, `<user>`, and `<password>`.
    - A road type configuration `<config>` must be provided by its path and it must describe (in some JSON format) all road types to be imported. An example for roads of motorized vehicles is included at `/mnt/map/tools/road-types.json`.
    - The standard import mode `normal` runs a bunch of SQL queries to import OSM extract of any size, whereas `slim` mode runs the import in a single query which is faster but requires more memory. The latter should be used only for smaller OSM extracts (extracts of at most the size of e.g. Switzerland).

#### Docker overview
Some basic Docker commands for container administration:

- Start/stop container:
    ``` bash
    sudo docker start <container>
    sudo docker stop <container>
     ```

- Attach to container (for maintenance):
    ``` bash
    sudo docker attach <container>
    root@acef54deeedb# sudo -u postgres psql -d <database>
    psql (9.3.5)
    Type "help" for help.

    <database>=# \q
    root@acef54deeedb#
    ```
    _Note: To detach the interactive shell from a running container without stopping it, use the escape sequence Ctrl-p + Ctrl-q._

- Execute commands on container (e.g. a shell for maintenance):

    ``` bash
    sudo docker exec -it <container> /bin/bash
    root@acef54deeedb# sudo -u postgres psql -d <database>
    psql (9.3.5)
    Type "help" for help.

    <database>=# \q
    root@acef54deeedb# exit
    ```
    _Note: Here, shell can be closed and container doesn't stop._

- List containers:
    ``` bash
    $ sudo docker ps -a
    CONTAINER ID        IMAGE                     COMMAND                CREATED             STATUS              PORTS                      NAMES
    acef54deeedb        barefoot-map              /bin/sh -c 'service    3 minutes ago       Up 2 seconds        0.0.0.0:5432->5432/tcp     <container>
    ```

#### Whitebox: PostgreSQL/PostGIS

_Note: A manually PostgreSQL/PostGIS database setup is no longer a supported approach for setting up map servers. The following documentation is a 'whitebox' documentation for development of Docker-based map servers._

1. Install prerequisites.

    - PostgreSQL (version 9.3 or higher)
    - PostGIS (version 2.1 or higher)
    - Osmosis (versionb 0.43.1 or higher)
    - Python (version 2.7.6 or higher)
    - Psycopg2
    - NumPy
    - Python-GDAL

2. Setup the database and include extensions.

    ``` bash
    createdb -h <host> -p <port> -U <user> <database>
    psql -h <host> -p <port> -U <user> -d <database> -c "CREATE EXTENSION postgis;"
    psql -h <host> -p <port> -U <user> -d <database> -c "CREATE EXTENSION hstore;"
    ```

3. Import the OSM data.

    1. Download and set up OSM database schema.

        ``` bash
        psql -h <host> -p <port> -U <user> -d <database> -f map/osm/pgsnapshot_schema_0.6.sql
        ```

    2. Import OSM extract with Osmosis.

        ``` bash
        osmosis --read-pbf file=<osm.pbf-file> --write-pgsql host=<host>:<port> user=<user> database=<database> password=<password>
        ```

        __Note:__ It may be necessary to define a temporary directory for use by Osmosis, e.g. if Osmosis stops with error 'No space left on device'. To do so, run the following commands beforehand:

        ``` bash
        JAVACMD_OPTIONS=-Djava.io.tmpdir=<path-to-tmp-dir>
        export JAVACMD_OPTIONS
         ```

    3. Extract OSM ways in intermediate table.

        Import of OSM extract can be usually run in slim mode, which creates table `<ways-table>` in a single query:

        ``` bash
        map/tools/osm2ways --host <host> --port <port> --database <database> --table <ways-table> --user <user> --slim
        ```

        If memory is not sufficiently available, normal mode should be used, which requires a prefix `<prefix>` for temporary tables, which prevents overwriting of existing tables:

        ``` bash
        map/tools/osm2ways --host <host> --port <port> --database <database> --table <ways-table> --user <user> --prefix <prefix>
        ```
        _Note: To see SQL commands without being executed, use option `--printonly`._

    4. Compile Barefoot map data.

        Transform ways `<ways-table>` into routing ways `<bfmap-table>`. A road type configuration `<config>` must be provided by its path and it must describe (in some JSON format) all road types to be imported. An example for roads of motorized vehicles is included at `map/tools/road-types.json`.

        ``` bash
        map/tools/ways2bfmap --source-host <host> --source-port <port> --source-database <host> --source-table <ways-table> --source-user <user> --target-host <host> --target-port <port> --target-database <database> --target-table <bfmap-ways> --target-user <user> --config <config>
        ```

        _Note: To see SQL commands without being executed, use option `--printonly`_

## Library

### Installation

#### Local installation (Maven)

_Note: Use this approach only if Barefoot is not available with a public Maven repository._

1. Install prerequisites.

    - Git (e.g. with `sudo apt-get install git`)
    - Maven (e.g. with `sudo apt-get install maven`)
    - Java JDK (Java version 7 or higher, e.g. with `sudo apt-get install openjdk-1.7-jdk`)

1. Checkout the respository.
    ``` bash
    git clone https://github.com/bmwcarit/barefoot.git
    cd barefoot
    ```

2. Install JAR to your local Maven repository.

    _Note: Maven tests will fail if the test map server hasn't been setup as shown [here](https://github.com/bmwcarit/barefoot/wiki#test-map-server)._

    ``` bash
    mvn install
    ```
    ... and to skip tests (if test map server has not been set up) ...

    ``` bash
    mvn install -DskipTests
    ```

3. Add dependency to your Java project with Maven.

    ``` xml
    <dependency>
        <groupId>com.bmw-carit</groupId>
        <artifactId>barefoot</artifactId>
        <version>VERSION</version>
    </dependency>
    ```

4. Set up a map server, see [here](https://github.com/bmwcarit/barefoot/wiki#map-server), or a test map server, see [here](https://github.com/bmwcarit/barefoot/wiki#test-map-server).

#### Build Javadoc

``` bash
mvn javadoc:javadoc
```

See [here](http://bmwcarit.github.io/barefoot/doc/index.html).

### APIs

See [here](https://github.com/bmwcarit/barefoot/wiki#application-programming-interfaces-apis).

### Testing

#### Library (Java/Maven)

_Note: Tests for library development require a test map server, see above. The setup is the same as the standard setup but uses a pre-configuration (defaults) of the import script._

##### Test map server

1. Install prerequisites.

    - Docker Engine (version 1.6 or higher, see [https://docs.docker.com/installation/ubuntulinux/](https://docs.docker.com/installation/ubuntulinux/))

2. Download OSM extract `oberbayern.osm.pbf`.

    ``` bash
    curl http://download.geofabrik.de/europe/germany/bayern/oberbayern-latest.osm.pbf -o map/osm/oberbayern.osm.pbf
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

5. Import OSM extract (inside the container) and use defaults.

    ``` bash
    root@acef54deeedb# bash /mnt/map/osm/import.sh
    ```

##### Execute tests

``` bash
mvn test
```

#### Map tools (Python)

_Note: This is only useful, if you work on the Python import scripts._

1. Install prerequisites.

    - Docker Engine (version 1.6 or higher, see [https://docs.docker.com/installation/ubuntulinux/](https://docs.docker.com/installation/ubuntulinux/))

2. Build Docker image.

    ``` bash
    cd barefoot
    sudo docker build -t barefoot-map ./map
    ```

3. Create test container.

    ``` bash
    sudo docker run -it --name="barefoot-test" -v ${PWD}/map/:/mnt/map barefoot-map
    ```

4. Run test script (in the container).

    ``` bash
    root@8160f9e2a2c0# bash /mnt/map/tools/test/run.sh
    ```

## Stand-alone servers

### Matcher server

The matcher server is a stand-alone server for offline map matching, which is the matching of a sequence of position measurements recorded in the past (traces) for reconstruction of the object's path on the map. Offline map matching finds the best matching on the map and exploits availability of the full trace. The figure below shows a map matched GPS trace (violet markers) in Munich city area which is shown by the path's geometry on the map (orange path).

<p align="center">
<img src="https://github.com/bmwcarit/barefoot/raw/master/doc-files/com/bmwcarit/barefoot/matcher/matching-satellite.png" width="700">
<br/>
<a href="https://www.mapbox.com/about/maps/">&#xA9; Mapbox</a> <a href="http://www.openstreetmap.org/">&#xA9; OpenStreetMap</a> <a href="https://www.mapbox.com/map-feedback/"><b>Improve this map</b></a> <a href="https://www.digitalglobe.com/">&#xA9; DigitalGlobe</a> <a href="http://geojson.io">&#xA9; geojson.io</a>
</p>

#### Setup

1. Setup a map server, see [here](https://github.com/bmwcarit/barefoot/wiki#map-server), or a test map server, see [here](https://github.com/bmwcarit/barefoot/wiki#test-map-server).

2. Package Barefoot JAR. (Includes dependencies and executable main class.)

    ``` bash
    mvn package
    ```

    _Note: Add `-DskipTests` to skip tests._

3. Start server with standard configuration for map server and map matching, and option for GeoJSON output format.

    ``` bash
    java -jar target/barefoot-<VERSION>-matcher-jar-with-dependencies.jar [--slimjson|--debug|--geojson] /path/to/server/properties /path/to/mapserver/properties
    ```

    _Note: Stop server with Ctrl-c._

    - Map server properties include access information to map server.

        _Note: An example is included at `config/oberbayern.properties` which can be used as reference or for testing._

    - Server properties include configuration for the server and map matching. Settings for configuration are explained [here](https://github.com/bmwcarit/barefoot/wiki#parameters).

        _Note: An example is included at `config/server.properties` and can be used for testing. The details for parameter settings are shown below._

#### Usage

The matcher server provides a REST-like API for sending requests with GPS traces and receiving map matched traces. A simple submission script is provided and can be tested with the test map server and the provided sample data:

``` bash
python util/submit/batch.py --host localhost --port 1234  --file src/test/resources/com/bmwcarit/barefoot/matcher/x0001-015.json
```

##### Request message format

A map matching request is a text message with a JSON array of JSON objects in the following form:

``` json
[
	{"id":"x001","time":1410324847000,"point":"POINT (11.564388282625075 48.16350662940509)"},
	...
]
```

- `id` is user-specific identifier and has no further meaning for map matching or the server.
- `time` is a timestamp in milliseconds unix epoch time.
- `point` is a position (measurement) in WKT (well-known-text) format and with WGS-84 projection (SRID 4326). (In other words, this may be any GPS position in WKT format.)
- `azimuth` is (optional) heading information of the object given as azimuth in degrees from north clockwise.

##### Response message formats

The matcher server's default response format is the JSON representation of the k-state data structure, see [here](https://github.com/bmwcarit/barefoot/wiki#k-state-data-structure). To change default output format, use the following options for the server:

- `--slimjson`: Server outputs JSON format with map matched positions, consisting of road id and fraction as precise position on the road, and routes between positions as sequences of road ids.
- `--debug`: Server outputs a JSON format similar to slim JSON format, but with timestamps of the measurements and geometries of the routes in WKT format.
- `--geojson`: Server outputs the geometry of the map matching on the map in GeoJSON format.

In addition, a request may also demand a certain response format which can be specified with the submission script using option `--format=slimjson|debug|geojson`.

### Tracker server

The tracker server is a stand-alone server for online map matching. Here, objects send position updates to some the tracking server periodically. The tracker server matches each position update right away (online) and, hence, keeps track of the objects' movements on the map in (near) real-time. The setup consists of a tracker server and a monitor, as illustrated below, which provides a website that shows object movements in the browser.

<p align="center">
<img src="https://github.com/bmwcarit/barefoot/raw/master/doc-files/com/bmwcarit/barefoot/tracker/tracker-monitor.png" width="650">
</p>

#### Setup

1. Install prerequisites.

    - ZeroMQ (e.g. with `sudo apt-get install libzmq3-dev`)
    - NodeJS (e.g. with `sudo apt-get install nodejs`)

2. Package Barefoot JAR. (Includes dependencies and executable main class.)

    ``` bash
    mvn package
    ```

    _Note: Add `-DskipTests` to skip tests._

3. Start tracker with standard configuration for map server, map matching, and tracking.

    ``` bash
    java -jar target/barefoot-<VERSION>-tracker-jar-with-dependencies.jar config/tracker.properties config/oberbayern.properties
    ```

    _Note: Stop server with Ctrl-c._

4. Install and start monitor (NodeJS server).

    Install (required only once)
    ``` bash
    cd util/monitor && npm install && cd ../..
    ```
    ... and start:
    ``` bash
    node util/monitor/monitor.js 3000 127.0.0.1 1235
    ```

5. Test setup with provided sample data.

    ``` bash
    python util/submit/stream.py --host localhost --port 1234  --file src/test/resources/com/bmwcarit/barefoot/matcher/x0001-001.json
    SUCCESS
    ...
    ```

    _Note: On success, i.e. result code is SUCCESS, the tracking is visible in the browser on [http://localhost:3000](http://localhost:3000). Otherwise, result code is either TIMEOUT or ERROR._

<p align="center">
<img src="https://github.com/bmwcarit/barefoot/raw/master/doc-files/com/bmwcarit/barefoot/tracker/monitor-1600x1000.gif" width="650">
<br/>
<a href="https://www.mapbox.com/about/maps/">&#xA9; Mapbox</a> <a href="http://www.openstreetmap.org/">&#xA9; OpenStreetMap</a> <a href="https://www.mapbox.com/map-feedback/"><b>Improve this map</b></a>
</p>

#### Usage

The tracker server provides a REST-like API for sending position updates with GPS points to the tracker server. A simple submission script that simulates a stream of position messages with an included sample data is provided:

``` bash
python util/submit/stream.py --host localhost --port 1234  --file src/test/resources/com/bmwcarit/barefoot/matcher/x0001-015.json
```

##### Position message format

A position update is a text message with a JSON object of the following form:

``` json
{"id":"x001","time":1410324847000,"point":"POINT (11.564388282625075 48.16350662940509)"}
```

- `id` is user-specific identifier and has no further meaning for map matching or the server.
- `time` is a timestamp in milliseconds unix epoch time.
- `point` is a position (measurement) in WKT (well-known-text) format and with WGS-84 projection (SRID 4326). (In other words, this may be any GPS position in WKT format.)
- `azimuth` is (optional) heading information of the object given as azimuth in degrees from north clockwise.


### Known issues

The conversion of input strings to floating-point numbers depends on Java's locale settings. In case `java -XshowSettings:locale -version` shows any other language than 'English' or other region/country than 'US', use the following options to start Java applications:
``` bash
-Duser.language=en -Duser.country=US
```

### Parameters

The server properties includes configuration of the server, e.g. TCP/IP port number, number of threads, etc., and parameter settings of the map matching.

| Parameter | Default | Description |
|-----------|:---------:|-------------|
| server.port | 1234 | The port of the map matching server. |
| server.timeout.request | 15000 | Maximum time in milliseconds of waiting for a request after connection has been established. |
| server.timeout.response | 60000 | Maximum time in milliseconds of waiting for reponse processing before a timeout is triggered and processing is aborted. |
| server.connections | 20 | Maximum number of connections to be established by clients. Also number of executor threads for reponse handling (I/O), which should be a multiple of the number of processors/cores of the machine to fully exploit the machine's performance.|
| matcher.sigma | 5.0 | Standard deviation of the position measurement error in meters, which is usually 5 meters for standard GPS error, as probability measure of matching candidates for position measurements on the map. |
| matcher.lambda | 0.0 | Rate parameter of negative exponential distribution for probability measure of routes between matching candidates. (The default is 0.0 which enables adaptive parameter setting, depending on sampling rate and input data, other values (manually fixed) may be 0.1, 0.01, 0.001 ...) |
| matcher.radius.max | 200 | Maximum radius for matching candidates to be searched in the map for respective position measurements in meters. |
| matcher.distance.max | 15000 | Maximum length of routes to be searched in the map for transitions between matching candidates in meters. (This avoids searching the full map for candidates that are for some reason not connected in the map, e.g. due to missing road links.) |
| matcher.distance.min | 0 | Minimum distance in meters for measurements to be considered for matching. Any measurement taken in less than the minimum distance from the most recent measurement is skipped. (This avoids unnnecessary matching of positions with very high measurement rate, useful e.g. if the object speed varies.) |
| matcher.interval.min | 1000 | Minimum time interval in milliseconds for measurements to be considered for matching. Any measurement taken in less than the minimum interval after the most recent measurement is skipped. (This avoids unnnecessary matching of positions with very high measuremnt rate, useful e.g. if the measurement rate varies.) |
| matcher.threads | 8 | Number of executor threads for reponse processing (map matching), which should at least the number of processors/cores of the machine to fully exploit the machine's performance. |
| tracker.port | 1235 | The port of the tracker server for subscribing to state updates, used by the tracker monitor for getting state updates pushed. |
| tracker.state.ttl | 60 | Maximum time to live (TTL) for object tracking states in seconds. Each state is discarded if there was no state update over one TTL. |

## Application programming interfaces (APIs)

### HMM map matching

#### Foundation

A Hidden Markov Model (HMM) assumes that a system's state is only observable indirectly via measurements over time.
This is the same in map matching where a sequence of position measurements (trace), recorded e.g with a GPS device, contains implicit information about an object's movement on the map, i.e. roads and turns taken. Hidden Markov Model (HMM) map matching is a robust method to infer stochastic information about the object's movement on the map, cf. [1] and [2], from measurements and system knowledge that includes object state candidates (matchings) and its transitions (routes).

A sequence of position measurements _z<sub>0</sub>_, _z<sub>1</sub>_, ..., _z<sub>T</sub>_ is map matched by finding for each measurement _z<sub>t</sub>_, made at some time _t_ _(0 &le; t &le; T)_, its most likely matching on the map _s<sub>t</sub><sup>i</sup>_ from a set of ___matching candidates___ _S<sub>t</sub> = {s<sub>t</sub><sup>1</sup>, ..., s<sub>t</sub><sup>n</sup>}_. A set of matching candidates _S<sub>t</sub>_ is here referred to as a ___candidate vector___. For each consecutive pair of candidate vectors _S<sub>t</sub>_ and _S<sub>t+1</sub>_ _(0 &le; t &lt; T)_, there is a transition between each pair of matching candidates _s<sub>t</sub><sup>i</sup>_ and _s<sub>t+1</sub><sup>j</sup>_ which is the route between map positions in the map. An illustration of the HMM with matching candidates and transitions is shown in the figure below.

<p align="center">
<img src="https://github.com/bmwcarit/barefoot/raw/master/doc-files/com/bmwcarit/barefoot/markov/model.png?raw=true" width="500">
</p>

To determine the most likely matching candidate, it is necessary to quantify probabilities of matching candidates and respective transitions. A HMM defines two types of probability measures:

  - A position measurement is subject to error and uncertainty. This is quantified with an __emission probability__ _p(z<sub>t</sub> | s<sub>t</sub>)_ that defines a conditioned probability of observing measurement _z<sub>t</sub>_ given that the true position in the map is _s<sub>t</sub>_, where _p(z<sub>t</sub> | s<sub>t</sub>) &sim; p(s<sub>t</sub> | z<sub>t</sub>)_.

    Here, emission probabilities are defined with the distance between measured positions to its true position is used to model measurement errors which is described with gaussian distribution with some standard deviation _&sigma;_ (default is _&sigma; = 5 meters_).

- All transitions between different pairs of matching candidates _s<sub>t-1</sub>_ to _s<sub>t</sub>_ have some measure of plausiblity which is quantified with a __transition probability__ _p(s<sub>t</sub> | s<sub>t-1</sub>)_ that is a conditioned probability of reaching _s<sub>t</sub>_ given that _s<sub>t-1</sub>_ is the origin.

    Here, transition probabilities are quantified with the difference of routing distance and line of sight distance between respective position measurements. Transition probabilities are distributed negative exponentially with a rate parameter _&lambda;_ (default is _&lambda; = 0.1_) which is the reciprocal of the mean.

In HMM map matching, we distinguish two different solutions to map matching provided by our HMM filter:

  - Our HMM filter determines an estimate _s&#773;<sub>t</sub>_ of the object's current position which is the most likely  matching candidate _s<sub>t</sub> &#8712; S<sub>t</sub>_ given measurements _z<sub>0</sub>_ ... _z<sub>t</sub>_, which is defined as:

    _s&#773;<sub>t</sub> = argmax<sub>(s<sub>t</sub> &#8712; S<sub>t</sub>)</sub> p(s<sub>t</sub>|z<sub>0</sub> ... z<sub>t</sub>)_,

    where _p(s<sub>t</sub>|z<sub>0</sub> ... z<sub>t</sub>)_ is referred to as the ___filter probability of s<sub>t</sub>___ and can be determined recursively:

    _p(s<sub>t</sub>|z<sub>0</sub>...z<sub>t</sub>) = p(s<sub>t</sub>|z<sub>t</sub>) &middot; &#931;<sub>(s<sub>t-1</sub> &#8712; S<sub>t-1</sub>)</sub> p(s<sub>t</sub>|s<sub>t-1</sub>) &middot; p(s<sub>t-1</sub>|z<sub>0</sub> ... z<sub>t-1</sub>)_.

- In addition, we extend our HMM filter to determine an object's most likely path _s<sub>0</sub>_ ... _s<sub>T</sub>_, that is the most likely sequence of matching candidates _s<sub>t</sub> &#8712; S<sub>t</sub>_ given the full knowledge of measurements _z<sub>0</sub>_ ... _z<sub>T</sub>_, which is defined as:

    _s<sub>0</sub> ... s<sub>T</sub> = argmax<sub>(s<sub>T</sub> &#8712; S<sub>T</sub>)</sub> p(s<sub>0</sub> ... s<sub>T</sub>|z<sub>0</sub> ... z<sub>T</sub>)_,

    where we define _p(s<sub>0</sub> ... s<sub>t</sub>|z<sub>0</sub> ... z<sub>t</sub>)_ as the probability of the most likely sequence that reaches matching candidate _s<sub>t</sub>_, referred to as the ___sequence probability of s<sub>t</sub>___, and can be also determined recursively:

    _p(s<sub>0</sub> ... s<sub>t</sub>|z<sub>0</sub> ... z<sub>t</sub>) = p(s<sub>t</sub>|z<sub>t</sub>) &middot; argmax<sub>(s<sub>t-1</sub> &#8712; S<sub>t-1</sub>)</sub> p(s<sub>t</sub>|s<sub>t-1</sub>) &middot; p(s<sub>0</sub> ... s<sub>t-1</sub>|z<sub>0</sub> ... z<sub>t-1</sub>)_.

_Note: Here, p(s<sub>0</sub> ... s<sub>t</sub>|z<sub>0</sub> ... z<sub>t</sub>) is defined as the probability of the most likely sequence that reaches candidate s<sub>t</sub>, whereas in other literature, cf. [4], it is defined as the probability of the sequence s<sub>0</sub> ... s<sub>t</sub>. This is used for convenience to enable implementation of online and offline map matching in dynamic programming manner (according to online Viterbi algorithm). Nevertheless, the solution is the same._

Since both solutions are recursive both can be used for online and offline algorithms, i.e. online and offline map matching.

##### References

[1] P. Newson and J. Krumm. Hidden Markov Map Matching Through Noise and Sparseness. In _Proceedings of International Conference on Advances in Geographic Information Systems_, 2009.

[2] C.Y. Goh, J. Dauwels, N. Mitrovic, M.T. Asif, A. Oran, and P. Jaillet. Online map-matching based on Hidden Markov model for real-time traffic sensing applications. In _International IEEE Conference on Intelligent Transportation Systems_, 2012.

#### Building blocks and API

Barefoot's map matching API consists of four main components. This includes a matcher component that performs map matching with a HMM filter iteratively for each position measurement _z<sub>t</sub>_ of an object. It also includes a state memory component that stores candidate vectors _S<sub>t</sub>_ and their probabilities _p_; and it can be accessed to get the current position estimate _s&#773;<sub>t</sub>_ or the most likely path (_s<sub>0</sub>_ ... _s<sub>t</sub>_). Further, it includes a map component for spatial search of matching candidates _S<sub>t</sub>_ near the measured position _z<sub>t</sub>_; and a router component to find routes _&lang;s<sub>t-1</sub>,s<sub>t</sub>&rang;_ between pairs of candidates _(s<sub>t-1</sub>,s<sub>t</sub>)_.

<p align="center">
<img src="https://github.com/bmwcarit/barefoot/raw/master/doc-files/com/bmwcarit/barefoot/matcher/matcher-components.png?raw=true" width="600">
</p>

A map matching iteration is the processing of a position measurement _z<sub>t</sub>_ to update the state memory and includes the following steps:

1. Spatial search for matching candidates _s<sub>t</sub> &#8712; S<sub>t</sub>_ in the map given measurement _z<sub>t</sub>_. _S<sub>t</sub>_ is referred to as a ___candidate vector___ for time _t_.
2. Fetch candidate vector _S<sub>t-1</sub>_ from memory if there is a candidate vector for time _t-1_ otherwise it returns an empty vector.
3. For each pair of matching candidates _(s<sub>t-1</sub>, s<sub>t</sub>)_ with _s<sub>t-1</sub> &#8712; S<sub>t-1</sub>_ and _s<sub>t</sub> &#8712; S<sub>t</sub>_, find the route _&lang;s<sub>t-1</sub>,s<sub>t</sub>&rang;_ as the transition between matching candidates.
4. Calculate ___filter probability___ and ___sequence probability___ for matching candidate _s<sub>t</sub>_, and update state memory with probabilities _p_ and candidate vector _S<sub>t</sub>_.

A simple map matching application shows the use of the map matching API (for details, see the Javadoc):

``` java
import com.bmwcarit.barefoot.roadmap.Loader;
import com.bmwcarit.barefoot.roadmap.Road;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.roadmap.RoadPoint;
import com.bmwcarit.barefoot.roadmap.TimePriority;
import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.topology.Dijkstra;

// Load and construct road map
RoadMap map = Loader.roadmap("config/oberbayern.properties", true).construct();

// Instantiate matcher and state data structure
Matcher matcher = new Matcher(map, new Dijkstra<Road, RoadPoint>(), new TimePriority(),
        new Geography());

// Input as sample batch (offline) or sample stream (online)
List<MatcherSample> samples = readSamples();

// Match full sequence of samples
MatcherKState state = matcher.mmatch(samples, 1, 500);

// Access map matching result: sequence for all samples
for (MatcherCandidate cand : state.sequence()) {
    cand.point().edge().base().refid(); // OSM id
    cand.point().edge().base().id(); // road id
    cand.point().edge().heading(); // heading
    cand.point().geometry(); // GPS position (on the road)
    if (cand.transition() != null)
        cand.transition().route().geometry(); // path geometry from last matching candidate
}
```
Online map matching processes samples and updates state memory iteratively:

``` java
// Create initial (empty) state memory
MatcherKState state = new MatcherKState();

// Iterate over sequence (stream) of samples
for (MatcherSample sample : samples) {
	// Execute matcher with single sample and update state memory
    state.update(matcher.execute(state.vector(), state.sample(), sample), sample);

    // Access map matching result: estimate for most recent sample
    MatcherCandidate estimate = state.estimate();
    System.out.println(estimate.point().edge().base().refid()); // OSM id
}

```

#### k-State data structure

A k-State data structure is a state memory for map matching. It organizes sets of ___matching candidates___ as ___candidate vectors___, and provides access to:
- a matching ___estimate___ which is the most likely matching candidate _s&#773;<sub>t</sub>_ for time _t_ and represents an estimate of the object's current position on the map,
- and an estimate of the ___sequence___ which is the most likely sequence of matching candidates _(s<sub>0</sub>_ ... _s<sub>t</sub>)_ and represents the object's most likely path on the map.

The k-State data structure is initially empty and must be updated with state vectors _S<sub>t</sub>_. After the first update with state vector _S<sub>0</sub>_ (left subfigure), it contains matching candidates (circles) with a pointer to the estimate (thick circle). In the second matching iteration (middle subfigure), the matcher fetches state vector _S<sub>0</sub>_ and determines for each matching candidate _s<sub>1</sub><sup>i</sup> &#8712; S<sub>1</sub>_ its sequence and filter probability, and determines the most likely predecessor candidate in _S<sub>0</sub>_ (black arrows). After that, state vector _S<sub>1</sub>_ is used to update the state memory (right subfigure) which, in turn, updates pointers to the estimate and the most likely sequence (thick arrow).

<p align="center">
<img src="https://github.com/bmwcarit/barefoot/raw/master/doc-files/com/bmwcarit/barefoot/markov/kstate-1.png?raw=true" width="150" hspace="40">
<img src="https://github.com/bmwcarit/barefoot/raw/master/doc-files/com/bmwcarit/barefoot/markov/kstate-2.png?raw=true" width="150" hspace="40">
<img src="https://github.com/bmwcarit/barefoot/raw/master/doc-files/com/bmwcarit/barefoot/markov/kstate-3.png?raw=true" width="150" hspace="40">
</p>

Each matching iteration repeats basically the same procedure: fetching state vector _S<sub>t-1</sub>_ (left subfigure), calculating for each candidate _s<sub>t</sub>  &#8712; S<sub>t</sub>_ filter and sequence probability and its most likely predecessor in _S<sub>t-1</sub>_ (middle subfigure), and updating state memory (right subfigure). With every update the k-State data structure converges, i.e. it removes all matching candidates that are neither relevant to the estimate nor to the most likely sequence.

<p align="center">
<img src="https://github.com/bmwcarit/barefoot/raw/master/doc-files/com/bmwcarit/barefoot/markov/kstate-4.png?raw=true" width="150" hspace="40">
<img src="https://github.com/bmwcarit/barefoot/raw/master/doc-files/com/bmwcarit/barefoot/markov/kstate-5.png?raw=true" width="150" hspace="40">
<img src="https://github.com/bmwcarit/barefoot/raw/master/doc-files/com/bmwcarit/barefoot/markov/kstate-6.png?raw=true" width="150" hspace="40">
</p>

##### Parameters

A k-State data structure provides two parameters to configure a maximum capacity in the number of state vectors stored in k-State (mostly relevant to online map matching):

- ___k___ bounds the number of state vectors to at most _k+1_ state vectors with _k &ge; 0_, i.e. state vectors _S<sub>t-k</sub> ... S<sub>t</sub>_. If _k_ is negative it is unbounded.
- ___&tau;___ bounds the number of state vectors in time to only those state vectors that cover the time interval _[t-&tau;,t]_ where _&tau; > 0_ and _t_ is the time of the most recent state update with _S<sub>t</sub>_. If _&tau; &le; 0_ it is unbounded.

_Note: Both parameters can be combined such that both bounding conditions hold. In turn, configuring k-State with k < 0 and &tau; &le; 0 is fully unbounded, which is the default._

##### JSON format

A k-State's JSON representation is a JSON Object with parameters `k` and `t` (_&tau;_) and two JSON arrays `sequence` and `candidates`. JSON object `sequence` contains the sequence of candidate vectors where each vector element is a matching candidate that is represented by an identifier, i.e. `candid`, and an identifier to its predecessor, i.e. `predid`, in the candidate's most likely sequence. Matching candidates are contained in the JSON array `candidates` with respective identifier `id` that is referenced by `candid` and `predid`, and filter probability `filtprob` and sequence probability `seqprob`. ( The `count` is used only for convergence of the k-State data structure and is the number of matching candidates in the successor candidate vector that refer to this matching candidate as its predecessor.)

A matching candidate `candidate` further includes a position in the map `point`, i.e. road identifier `road` and fraction `frac`, as well as the transition `transition` from its predecessor, if it has one. The transition is a route `route` consisting of `source` and `target`, with `road` and `frac` to specify the respective position in the map, and a JSON array `roads` that gives the sequence of roads for the route.

``` json
{
  "sequence": [
    {
      "sample": {
        "id": "a1396ab7-7caa-4c31-9f3c-8982055e3de6",
        "point": "POINT (11.536577179945997 48.14905556426255)",
        "time": 1410325357000
      },
      "vector": [
        {
          "candid": "e649f976-564a-4760-9a74-c82ba6c4653e",
          "predid": ""
        }
      ]
    },
    {
      "sample": {
        "id": "a1396ab7-7caa-4c31-9f3c-8982055e3de6",
        "point": "POINT (11.536219651738836 48.14672536176703)",
        "time": 1410325372000
      },
      "vector": [
        {
          "candid": "648cd380-f317-4ebb-b9e2-650a80054bf7",
          "predid": "e649f976-564a-4760-9a74-c82ba6c4653e"
        },
        {
          "candid": "6b347e77-eb92-43d3-a60d-69d9bb71f9d4",
          "predid": "e649f976-564a-4760-9a74-c82ba6c4653e"
        }
      ]
    }
  ],
  "candidates": [
    {
      "count": 2,
      "candidate": {
        "filtprob": 0.11565717758307356,
        "id": "e649f976-564a-4760-9a74-c82ba6c4653e",
        "point": {
          "frac": 0.4104557158596576,
          "road": 9362030
        },
        "seqprob": -1.0999901830140701
      }
    },
    {
      "count": 0,
      "candidate": {
        "filtprob": 0.2370833183857761,
        "id": "648cd380-f317-4ebb-b9e2-650a80054bf7",
        "point": {
          "frac": 0.06531311234979269,
          "road": 8533290
        },
        "seqprob": -3.2870414276380666,
        "transition": {
          "route": {
            "roads": [
              9362030,
              ...
              8533290
            ],
            "source": {
              "frac": 0.4104557158596576,
              "road": 9362030
            },
            "target": {
              "frac": 0.06531311234979269,
              "road": 8533290
            }
          }
        }
      }
    },
    ...
  ],
  "k": -1,
  "t": -1
}
```

#### Scalable map matching

Barefoot is designed for use in parallel and distributed systems. This includes features such as:

- serializable road map (store road map as serialized object in HDFS or send it from one machine to another)
- JSON format of KState data structure (interchangeable and human readable map matching state information)
- container-based map server setup (flexible to be used on high-end server systems or in low-end environments for development)

##### Apache Spark (scalable offline map matching)

The following code example shows a simple map matching application for use in an Apache Spark cluster. It distributes a map matcher object as broadcast variable (BroadcastMatcher.scala is a simple wrapper class), which loads map data from a map server (as an alternative map data can be stored/loaded as serialized object via HDFS).

``` scala
// Instantiate map matcher as broadcast variable in Spark Context (sc).
val matcher = sc.broadcast(new BroadcastMatcher(host, port, database, user, pass, config))

// Load trace data as RDD from CSV file asset of tuples:
// (object-id: String, time: Long, position: Point)
val traces = sc.textFile("traces.csv").map(x => {
  val y = x.split(",")
  (y(0), y(1).toLong, new Point(y(2).toDouble, y(3).toDouble))
})

// Run a map job on RDD that uses the matcher instance.
val matches = traces.groupBy(x => x._1).map(x => {
  val trip = x._2.map({
    x => new MatcherSample(x._1, x._2, x._3)
  }).toList
  matcher.mmatch(trip)
)
```

The example code uses a simple wrapper of Barefoot's matcher. It initializes matcher as static member (Singleton) and loads map data on first matching invocation.

``` scala
object BroadcastMatcher {
  private var instance = null: Matcher

  private def initialize(host: String, port: Int, name: String, user: String, pass: String, config: String) {
    if (instance != null) return
    this.synchronized {
      if (instance == null) { // initialize map matcher once per Executor (JVM process/cluster node)
        val reader = new PostGISReader(host, port, name, "bfmap_ways", user, pass, Configuration.read(new JSONObject(config)))
        val map = RoadMap.Load(reader)

        map.construct();

        val router = new Dijkstra[Road, RoadPoint]()
        val cost = new TimePriority()
        val spatial = new Geography()

        instance = new Matcher(map, router, cost, spatial)
      }
    }
  }
}

@SerialVersionUID(1L)
class BroadcastMatcher(host: String, port: Int, name: String, user: String, pass: String, config: String) extends Serializable {

  def mmatch(samples: List[MatcherSample]): MatcherKState = {
    mmatch(samples, 0, 0)
  }

  def mmatch(samples: List[MatcherSample], minDistance: Double, minInterval: Int): MatcherKState = {
    BroadcastMatcher.initialize(host, port, name, user, pass, config)
    BroadcastMatcher.instance.mmatch(new ArrayList[MatcherSample](samples.asJava), minDistance, minInterval)
  }
}
```

_Note: The shown example initializes a matcher instance for each Spark Executor. That means that if your Spark cluster configuration uses two Executors per machine, a machine instantiates two matcher and two map objects. To reduce memory consumption, one might configure only one Executor per machine._

##### Apache Storm, Kafka, and Cassandra (scalable online map matching)

TBD.

### Spatial analysis

#### Spatial search

Spatial search in the road map requires access to spatial data of the road map and spatial operations for distance and point-to-line projection. Barefoot implements the following basic search operations:

- radius
- nearest
- k-nearest (kNN)

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
<img src="https://github.com/bmwcarit/barefoot/raw/master/doc-files/com/bmwcarit/barefoot/spatial/radius-satellite.png" width="700">
<br/>
<a href="https://www.mapbox.com/about/maps/">&#xA9; Mapbox</a> <a href="http://www.openstreetmap.org/">&#xA9; OpenStreetMap</a> <a href="https://www.mapbox.com/map-feedback/"><b>Improve this map</b></a> <a href="https://www.digitalglobe.com/">&#xA9; DigitalGlobe</a> <a href="http://geojson.io">&#xA9; geojson.io</a>
</p>


#### Spatial cluster analysis

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
<img src="https://github.com/bmwcarit/barefoot/raw/master/doc-files/com/bmwcarit/barefoot/analysis/dbscan-satellite.png" width="700">
<br/>
<a href="https://www.mapbox.com/about/maps/">&#xA9; Mapbox</a> <a href="http://www.openstreetmap.org/">&#xA9; OpenStreetMap</a> <a href="https://www.mapbox.com/map-feedback/"><b>Improve this map</b></a> <a href="https://www.digitalglobe.com/">&#xA9; DigitalGlobe</a> <a href="http://geojson.io">&#xA9; geojson.io</a>
</p>
