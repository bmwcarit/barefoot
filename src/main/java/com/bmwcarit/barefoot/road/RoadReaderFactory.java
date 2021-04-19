package com.bmwcarit.barefoot.road;

import java.util.Map;

import com.bmwcarit.barefoot.util.Tuple;
import com.bmwcarit.barefoot.roadmap.RoadMap;

public class RoadReaderFactory {
	/**
	 * Creates a RoadReader that can read BaseRoad objects from a PostgreSQL/PostGIS database
	 * 
	 * @param host The hostname of the database server.
	 * @param port The port number of the database server.
	 * @param database The name of the database.
	 * @param table The name of the table in the database to read from.
	 * @param user The username of a user accessing the database.
	 * @param password The password of a user accessing the database.
	 * @param config Configuration of road type data for RoadReader to read
	 * @return A RoadReader that reads BaseRoad objects from a PostgreSQL/PostGIS database (PostGISReader)
	 */
	public RoadReader getRoadReader(String host, int port, String database, String table, String user,
			String password, Map<Short, Tuple<Double, Integer>> config) {
		return new PostGISReader(host, port, database, table, user, password, config);
	}
	
	/**
	 * Creates a RoadReader that can read BaseRoad objects from a barefoot map files (file extension 'bfmap')
	 * 
	 * @param path The path to the barefoot map file to read.
	 * @return A RoadReader that reads BaseRoad objects from a barefoot map file.
	 */
	public RoadReader getRoadReader(String path) {
		return new BfmapReader(path);
	}
	
	/**
	 * Creates a RoadReader that reads BaseRoad objects from a RoadMap object.
	 * 
	 * @param src Source of BaseRoad objects to read from.
	 * @return A RoadReader that reads BaseRoad objects from a RoadMap.
	 */
	public RoadReader getRoadReader(RoadMap src) {
		return new RoadMapReader(src);
	}
}
