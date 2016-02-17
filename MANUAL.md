# Barefoot manual

## Map server

There are two variants for setting up a Barefoot map server. The first variant uses an easy-to-use Docker container ('blue pill' - recommended in almost every case). The other variant enables you to set up a map server within an existing PostgreSQL/PostGIS server ('red pill' - self-maintained database).

### Docker container

#### Setup
The following approach uses Docker (version 1.6 or higher, see [here](https://docs.docker.com/installation/ubuntulinux/)) to automatically install PostgreSQL/PostGIS database server in an isolated container environment. With simple scripts an OpenStreetMap extract can be easily imported into the database.

1. Build Docker image.
  ``` bash
cd barefoot
sudo docker build --rm=true -t barefoot ./docker
  ```

2. Create Docker container.

  _Note: Give the container a name by replacing `<container>` respectively._

  ``` bash
sudo docker run -t -i -p 127.0.0.1:5432:5432 --name="<container>" -v ${PWD}/bfmap/:/mnt/bfmap -v ${PWD}/docker/osm/:/mnt/osm barefoot
  ```

3. Import OSM extract (in the container).

  ``` bash
root@acef54deeedb# bash /mnt/osm/import.sh <osm> <database> <user> <password> <config> slim|normal
  ```

  - To import an OpenStreetMap extract `<osm>` put the file in the directory `docker/osm` from outside the container which is then accessible inside the container at `/mnt/osm/`.
  - The script imports the extract into a database with specified name and credentials, i.e. `<database>`, `<user>`, and `<password>`.
  - A road type specification `<config>` must be provided as path to a file with the specification in JSON format. An example is included at `bfmap/road-types.json`.
  - Standard import mode `normal` executes a bunch of SQL queries to import OSM data extract of any size, whereas `slim` mode runs the import in a single query which is fast but requires lots of memory. The latter should be used only for small OSM data extracts (extracts of at most the size of e.g. Switzerland).

  _Note: To detach the interactive shell from a running container without stopping it, use the escape sequence Ctrl-p + Ctrl-q._

#### Setup test map server

The test map server setup follows the standard setup but uses a pre-configured import script.

1. Download OSM extract (examples require `oberbayern.osm.pbf`)

  ``` bash
curl http://download.geofabrik.de/europe/germany/bayern/oberbayern-latest.osm.pbf -o barefoot/docker/osm/oberbayern.osm.pbf
  ```

2. Build Docker image (if not done yet).

  ``` bash
cd barefoot
sudo docker build --rm=true -t barefoot ./docker
  ```

3. Create Docker container.

  ``` bash
sudo docker run -t -i -p 127.0.0.1:5432:5432 --name="barefoot-oberbayern" -v ${PWD}/bfmap/:/mnt/bfmap -v ${PWD}/docker/osm/:/mnt/osm barefoot
  ```

4. Import OSM extract (in the container) with default parameters.

  ``` bash
root@acef54deeedb# bash /mnt/osm/import.sh
  ```

#### Docker commands

Some general commands for administration of the Docker container:

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

- Show containers:
  ``` bash
$ sudo docker ps -a
CONTAINER ID        IMAGE                     COMMAND                CREATED             STATUS              PORTS                      NAMES
acef54deeedb        <database>-image:latest   /bin/sh -c 'service    3 minutes ago       Up 2 seconds        127.0.0.1:5432->5432/tcp   <container>
  ```

#### Test import scripts

_Note: This is only useful, if you work on the Python import scripts._

1. Build Docker image (if not done yet).

  ``` bash
cd barefoot
sudo docker build --rm=true -t barefoot ./docker
  ```

2. Create test container.

  ``` bash
sudo docker run -t -i -p 127.0.0.1:5432:5432 --name="barefoot-test" -v ${PWD}/bfmap/:/mnt/bfmap -v ${PWD}/docker/osm/:/mnt/osm barefoot
  ```

3. Run test script (in the container).

  ``` bash
root@8160f9e2a2c0# bash /mnt/bfmap/test/run.sh
  ```

### Self-maintained database

_Note: Currently, there is no support for compiling a barefoot map from XML-based OSM data directly (*.osm or *.osm.pbf files). Hence, it is necessary to use Osmosis as shown in the following steps._

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
wget https://trac.openstreetmap.org/export/22680/subversion/applications/utils/osmosis/trunk/package/script/pgsql_simple_schema_0.6.sql
psql -h <host> -p <port> -U <user> -d <database> -f pgsql_simple_schema_0.6.sql
    ```

  2.  Import OSM extract with osmosis.

    ``` bash
osmosis --read-pbf file=<osm.pbf-file> --write-pgsql host=<host>:<port> user=<user> database=<database> password=<password>
    ```

     __Note:__ In some cases, it is necessary to specify to a temporary directory to be used by osmosis. (This is necessary for example if osmosis stops with error 'No space left on device'.) To do so, run the following commands beforehand:

     ``` bash
JAVACMD_OPTIONS=-Djava.io.tmpdir=<path-to-tmp-dir>
export JAVACMD_OPTIONS
     ```

4. Extract OSM ways in intermediate table.

  Usually, you can run the import in slim mode, which creates the table `<ways-table>` in a single query:

  ``` bash
bfmap/osm2ways --host <host> --port <port> --database <database> --table <ways-table> --user <user> --slim
  ```

  However, if memory is not sufficiently available, use normal mode. This requires to specify a prefix `<prefix>` for temporary tables. This prevents deleting and overwriting existing tables:

  ``` bash
bfmap/osm2ways --host <host> --port <port> --database <database> --table <ways-table> --user <user> --prefix <prefix>
  ```

  _Note: To see what the commands are doing, you can add the option `--printonly` to see the SQL commands to be executed._


4. Compile Barefoot map data.

  Transform ways table `<ways-table>` into routing table `<bfmap-table>`. The configuration `<config>`, use e.g. `bfmap/road-types.xml`

  ``` bash
bfmap/ways2bfmap --source-host <host> --source-port <port> --source-database <host> --source-table <ways-table> --source-user <user> --target-host <host> --target-port <port> --target-database <database> --target-table <bfmap-ways> --target-user <user> --config <config>
  ```

  _Note: To see what the commands are doing, you can add the option `--printonly` to see the SQL commands to be executed._

## Map matching server (stand-alone)

### Setup

1. Setup a map server, see [here](MANUAL.md#map-server).

2. Assemble Barefoot JAR with dependencies. (Includes the executable main class of the server.)

  ``` bash
cd barefoot
mvn compile assembly:single
  ```

3. Start map matching server.

  A map matching server is a Java process that is accessible via TCP/IP socket and requires configuration for access to the map server and settings of the map matching server.

  ``` bash
java -jar target/barefoot-0.0.2-jar-with-dependencies.jar [--slimjson|--debug|--geojson] /path/to/server/properties /path/to/mapserver/properties
  ```

  - Map server properties contains access information to map server.

    _Note: An example is included at `config/oberbayern.properties` which can be used as reference or for testing._

  - Server properties contains configuration for the server and map matching. Settings for configuration are explained [below](MANUAL.md#parameter-settings).

    _Note: An example is included at `config/server.properties` and can be used for testing. The details for parameter settings are shown below._

### Usage

A map matching request is sent to the server via TCP/IP connection. For simple testing, `netcat` can be used to send requests. An example request is included at `src/test/resources/com/bmwcarit/barefoot/matcher/x0001-015.json`. 

``` bash
cat src/test/resources/com/bmwcarit/barefoot/matcher/x0001-015.json | netcat 127.0.0.1 1234
```

#### Request format (default)

A map matching request is a text message with a JSON array of JSON objects in the following form:

``` json
[
	{"id":"a1396ab7-7caa-4c31-9f3c-8982055e3de6","time":1410324847000,"point":"POINT (11.564388282625075 48.16350662940509)"},
	...
]
```
  - `id` is user-specific identifier and has no further meaning for map matching or the server.
  - `time` is a timestamp in milliseconds unix epoch time.
  - `point` is a position (measurement) in WKT (well-known-text) format and with WGS-84 projection (SRID 4326). (In other words, this may be any GPS position in WKT format.)

#### Response formats

Default response format is the JSON representation of the k-state data structure, see [k-State data structure](MANUAL.md#k-state-data-structure). To change default output format, use the following options:

  - `--slimjson`: Server outputs JSON format with map matched positions, consisting of road id and fraction as precise position on the road, and routes between positions as sequences of road ids.
  - `--debug`: Server outputs a JSON format similar to slim JSON format, but with timestamps of the measurements and geometries of the routes in WKT format.
  - `--geojson`: Server outputs the geometry of the map matching on the map in GeoJSON format.

A request may demand another response format by encapsulating the request in a JSON object of the form:

``` json
{
	"format": "<format>",
	"request": <request>
}
```

For example, a request using `netcat` may be as follows: 

``` bash
echo "{\"format\": \"geojson\", \"request\": $(cat src/test/resources/com/bmwcarit/barefoot/matcher/x0001-015.json | tr -d '\n\r')}" | netcat 127.0.0.1 1234
```

### Server settings

The server properties includes configuration of the server, e.g. TCP/IP port number, number of threads, etc., and parameter settings of the map matching.

| Parameter | Default value | Description |
|:-----------:|:---------:|-------------|
| server.port | 1234 | The port of the map matching server. |
| server.timeout.request | 15000 | Maximum time in milliseconds of waiting for a request after connection has been established. |
| server.timeout.response | 60000 | Maximum time in milliseconds of waiting for reponse processing before a timeout is triggered and processing is aborted. |
| server.connections | 20 | Maximum number of connections to be established by clients. Also number of executor threads for reponse handling (I/O), which should be a multiple of the number of processors/cores of the machine to fully exploit the machine's performance.|
| matcher.sigma | 5.0 | Standard deviation of the position measurement error in meters, which is usually 5 meters for standard GPS error, as probability measure of matching candidates for position measurements on the map. |
| matcher.lambda | 0.0 | Rate parameter of negative exponential distribution for probability measure of routes between matching candidates. (The default is 0.0 which enables adaptive parameter setting, depending on sampling rate and input data, other values (manually fixed) may be 0.1, 0.01, 0.001 ...) |
| matcher.distance.max | 15000 | Maximum length of routes to be searched in the map for transitions between matching candidates in meters. (This avoids searching the full map for candidates that are for some reason not connected in the map, e.g. due to missing road links.) |
| matcher.radius.max | 200 | Maximum radius for matching candidates to be searched in the map for respective position measurements in meters. |
| matcher.interval.min | 5000 | Minimum time interval in milliseconds for measurements to be considered for matching. Any measurement taken in less than the minimum interval after the most recent measurement is skipped. (This avoids unnnecessary matching of positions with very high measuremnt rate, useful e.g. if the measurement rate varies.) |
| matcher.distance.min | 0 | Minimum distance in meters for measurements to be considered for matching. Any measurement taken in less than the minimum distance from the most recent measurement is skipped. (This avoids unnnecessary matching of positions with very high measurement rate, useful e.g. if the object speed varies.) |
| matcher.threads | 8 | Number of executor threads for reponse processing (map matching), which should at least the number of processors/cores of the machine to fully exploit the machine's performance. |

## Barefoot library

_Note: Barefoot library requires Java 7 or higher._

### Installation with Maven

1. Add dependency to your Java project with Maven:

  ``` xml
<dependency>
		<groupId>de.bmw-carit.barefoot</groupId>
		<artifactId>barefoot</artifactId>
		<version>0.0.2</version>
</dependency>
  ```

2. Set up a map server, see [here](MANUAL.md#map-server).

#### Local installation with Maven

_Note: Use this approach only if Barefoot is not available with a public Maven repository._

1. Checkout the respository.
  ``` bash
git clone https://github.com/bmwcarit/barefoot.git
cd barefoot
  ```

2. Install JAR to your local Maven repository.

  _Note: Maven tests will fail if the test map server hasn't been setup as shown [here](MANUAL.md#test-map-server)._

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
		<version>0.0.2</version>
</dependency>
  ```

4. Set up a map server, see [here](MANUAL.md#map-server).

### Javadoc

  ``` bash
mvn javadoc:javadoc
  ```

See [here](http://bmwcarit.github.io/barefoot/doc/index.html).

### Map matching API

#### Background

A Hidden Markov Model (HMM) assumes that a system's state is only observable indirectly via measurements over time.
This is the same in map matching where a sequence of position measurements (trace), recorded e.g with a GPS device, contains implicit information about an object's movement on the map, i.e. roads and turns taken. Hidden Markov Model (HMM) map matching is a robust method to infer stochastic information about the object's movement on the map, cf. [1] and [2], from measurements and system knowledge that includes object state candidates (matchings) and its transitions (routes).

A sequence of position measurements _z<sub>0</sub>_, _z<sub>1</sub>_, ..., _z<sub>T</sub>_ is map matched by finding for each measurement _z<sub>t</sub>_, made at some time _t_ _(0 &le; t &le; T)_, its most likely matching on the map _s<sub>t</sub><sup>i</sup>_ from a set of ___matching candidates___ _S<sub>t</sub> = {s<sub>t</sub><sup>1</sup>, ..., s<sub>t</sub><sup>n</sup>}_. A set of matching candidates _S<sub>t</sub>_ is here referred to as a ___candidate vector___. For each consecutive pair of candidate vectors _S<sub>t</sub>_ and _S<sub>t+1</sub>_ _(0 &le; t &lt; T)_, there is a transition between each pair of matching candidates _s<sub>t</sub><sup>i</sup>_ and _s<sub>t+1</sub><sup>j</sup>_ which is the route between map positions in the map. An illustration of the HMM with matching candidates and transitions is shown in the figure below.

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/markov/model.png?raw=true" width="500">
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

#### Map matching components

Barefoot's map matching API consists of four main components. This includes a matcher component that performs map matching with a HMM filter iteratively for each position measurement _z<sub>t</sub>_ of an object. It also includes a state memory component that stores candidate vectors _S<sub>t</sub>_ and their probabilities _p_; and it can be accessed to get the current position estimate _s&#773;<sub>t</sub>_ or the most likely path (_s<sub>0</sub>_ ... _s<sub>t</sub>_). Further, it includes a map component for spatial search of matching candidates _S<sub>t</sub>_ near the measured position _z<sub>t</sub>_; and a router component to find routes _&lang;s<sub>t-1</sub>,s<sub>t</sub>&rang;_ between pairs of candidates _(s<sub>t-1</sub>,s<sub>t</sub>)_.

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/matcher/matcher-components.png?raw=true" width="600">
</p>

A map matching iteration is the processing of a position measurement _z<sub>t</sub>_ to update the state memory and includes the following steps:

1. Spatial search for matching candidates _s<sub>t</sub> &#8712; S<sub>t</sub>_ in the map given measurement _z<sub>t</sub>_. _S<sub>t</sub>_ is referred to as a ___candidate vector___ for time _t_.
2. Fetch candidate vector _S<sub>t-1</sub>_ from memory if there is a candidate vector for time _t-1_ otherwise it returns an empty vector.
3. For each pair of matching candidates _(s<sub>t-1</sub>, s<sub>t</sub>)_ with _s<sub>t-1</sub> &#8712; S<sub>t-1</sub>_ and _s<sub>t</sub> &#8712; S<sub>t</sub>_, find the route _&lang;s<sub>t-1</sub>,s<sub>t</sub>&rang;_ as the transition between matching candidates.
4. Calculate ___filter probability___ and ___sequence probability___ for matching candidate _s<sub>t</sub>_, and update state memory with probabilities _p_ and candidate vector _S<sub>t</sub>_.

A simple map matching program shows the use of the map matching API (for details, see the Javadoc):

``` java
import com.bmwcarit.barefoot.matcher.Matcher;
import com.bmwcarit.barefoot.matcher.MatcherKState;
import com.bmwcarit.barefoot.matcher.MatcherCandidate;
import com.bmwcarit.barefoot.matcher.MatcherSample;
import com.bmwcarit.barefoot.matcher.MatcherTransition;
import com.bmwcarit.barefoot.road.PostGISReader;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.roadmap.Road;
import com.bmwcarit.barefoot.roadmap.RoadPoint;
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
MatcherKState state = new MatcherKState();

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
	MatcherTransition transition = estimate.transition();
	if (transition != null) {
		// first point will have a null transition
		Route route = transition.route(); // route to position
	}
}

// Offline map matching results
List<MatcherCandidate> sequence = state.sequence(); // most likely sequence of positions
```

#### k-State data structure
A k-State data structure is a state memory for map matching. It organizes sets of ___matching candidates___ as ___candidate vectors___, and provides access to:
- a matching ___estimate___ which is the most likely matching candidate _s&#773;<sub>t</sub>_ for time _t_ and represents an estimate of the object's current position on the map,
- and an estimate of the ___sequence___ which is the most likely sequence of matching candidates _(s<sub>0</sub>_ ... _s<sub>t</sub>)_ and represents the object's most likely path on the map.

The k-State data structure is initially empty and must be updated with state vectors _S<sub>t</sub>_. After the first update with state vector _S<sub>0</sub>_ (left subfigure), it contains matching candidates (circles) with a pointer to the estimate (thick circle). In the second matching iteration (middle subfigure), the matcher fetches state vector _S<sub>0</sub>_ and determines for each matching candidate _s<sub>1</sub><sup>i</sup> &#8712; S<sub>1</sub>_ its sequence and filter probability, and determines the most likely predecessor candidate in _S<sub>0</sub>_ (black arrows). After that, state vector _S<sub>1</sub>_ is used to update the state memory (right subfigure) which, in turn, updates pointers to the estimate and the most likely sequence (thick arrow).

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/markov/kstate-1.png?raw=true" width="150" hspace="40">
<img src="doc-files/com/bmwcarit/barefoot/markov/kstate-2.png?raw=true" width="150" hspace="40">
<img src="doc-files/com/bmwcarit/barefoot/markov/kstate-3.png?raw=true" width="150" hspace="40">
</p>

Each matching iteration repeats basically the same procedure: fetching state vector _S<sub>t-1</sub>_ (left subfigure), calculating for each candidate _s<sub>t</sub>  &#8712; S<sub>t</sub>_ filter and sequence probability and its most likely predecessor in _S<sub>t-1</sub>_ (middle subfigure), and updating state memory (right subfigure). With every update the k-State data structure converges, i.e. it removes all matching candidates that are neither relevant to the estimate nor to the most likely sequence.

<p align="center">
<img src="doc-files/com/bmwcarit/barefoot/markov/kstate-4.png?raw=true" width="150" hspace="40">
<img src="doc-files/com/bmwcarit/barefoot/markov/kstate-5.png?raw=true" width="150" hspace="40">
<img src="doc-files/com/bmwcarit/barefoot/markov/kstate-6.png?raw=true" width="150" hspace="40">
</p>

##### Parameter settings

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

## References

[1] P. Newson and J. Krumm. Hidden Markov Map Matching Through Noise and Sparseness. In _Proceedings of International Conference on Advances in Geographic Information Systems_, 2009.

[2] C.Y. Goh, J. Dauwels, N. Mitrovic, M.T. Asif, A. Oran, and P. Jaillet. Online map-matching based on Hidden Markov model for real-time traffic sensing applications. In _International IEEE Conference on Intelligent Transportation Systems_, 2012.

[3] S. Mattheis, K. Al-Zahid, B. Engelmann, A. Hildisch, S. Holder, O. Lazarevych, D. Mohr, F. Sedlmeier, and R. Zinck. Putting the car on the map: A scalable map matching system for the Open Source Community. In _INFORMATIK 2014: Workshop Automotive Software Engineering_, 2014.

[4] S. Russel and P. Norvig. Artificial Intelligence: A Modern Approach. _Pearson, 3rd Edition_, 2009.
