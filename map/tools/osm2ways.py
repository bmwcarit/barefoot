#!/usr/bin/env python

#
# Copyright (C) 2015, BMW Car IT GmbH
#
# Author: Sebastian Mattheis <sebastian.mattheis@bmw-carit.de>
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
# in compliance with the License. You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
# writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
# language governing permissions and limitations under the License.
#
from __future__ import print_function, division, absolute_import

__author__ = "sebastian.mattheis@bmw-carit.de"
__copyright__ = "Copyright 2015 BMW Car IT GmbH"
__license__ = "Apache-2.0"

import optparse
import getpass

import database


def create_parser():
    parser = optparse.OptionParser("osm2ways.py [options]")
    parser.add_option("--host", dest="host", help="Hostname of the database.")
    parser.add_option("--port", dest="port", help="Port of the database.")
    parser.add_option("--database", dest="database",
                      help="Name of the database.")
    parser.add_option("--table", dest="table", help="Name of the table.")
    parser.add_option("--user", dest="user", help="User of the database.")
    parser.add_option("--password", dest="password", help="User password.")
    parser.add_option("--slim", action="store_true", default=False,
                      help="""Slim mode runs everything in a single query. This requires memory 
                          to be sufficiently available.""")
    parser.add_option("--prefix", dest="prefix",
                      help="If not using slim mode, use this prefix for intermediate tables.")
    parser.add_option("--printonly", action="store_true",
                      default=False,
                      help="Do not execute commands, but print it.")
    return parser


class Osm2Ways(database.Database):
    """Contain methods to create a table with ways."""
    def __init__(self, host, port, database, user, password, printonly):
        super(Osm2Ways, self).__init__(host, port, database, user,
                                       password, printonly)

    def exists(self, table):
        """Check whether the table already exists in the database."""
        query = """SELECT COUNT(tablename) FROM pg_tables WHERE 
                schemaname='public' AND tablename=%s;"""
        self.do_query(query, params=(table, ))
        if self.printonly:
            return True
        if self.cursor.fetchone()[0] == 0:
            return False
        else:
            return True

    def drop(self, table):
        """Remove a table from the database."""
        print('Dropping table {}.'.format(table))
        query = "DROP TABLE {};".format(table)
        self.do_query(query)

    def drop_if_exists(self, table):
        """Drop the table if it exists."""
        if self.exists(table):
            print('Table {} already exists, dropping it.'.format(table))
            self.drop(table)

    def slim(self, table):
        """Slim execution."""
        query = """
            CREATE TABLE {} AS
            SELECT tmp_way_aggs.way_id,
                   ways.tags AS tags,
                   tmp_way_aggs.seq AS seq,
                   tmp_way_aggs.nodes AS nodes,
                   tmp_way_aggs.counts AS counts,
                   tmp_way_aggs.geoms AS geoms
            FROM ( 
                SELECT tmp_way_nodes.way_id AS way_id,
                       array_agg(tmp_way_nodes.seq_id) AS seq,
                       array_agg(tmp_way_nodes.node_id) AS nodes,
                       array_agg(tmp_node_counts.count) AS counts,
                       array_agg(ST_AsBinary(tmp_way_nodes.geom)) AS geoms
                FROM (
                    SELECT way_nodes.way_id AS way_id,
                           way_nodes.node_id AS node_id,
                           way_nodes.seq_id AS seq_id,
                           nodes.geom AS geom
                    FROM (
                        SELECT way_id,
                               node_id,
                               sequence_id AS seq_id
                        FROM way_nodes
                    ) AS way_nodes
                    INNER JOIN nodes ON (way_nodes.node_id=nodes.id)
                ) AS tmp_way_nodes
                INNER JOIN (
                    SELECT node_id,
                           count(way_id) AS COUNT
                    FROM way_nodes
                    GROUP BY node_id
                ) AS tmp_node_counts 
                ON (tmp_way_nodes.node_id=tmp_node_counts.node_id)
                GROUP BY tmp_way_nodes.way_id
            ) AS tmp_way_aggs
            INNER JOIN ways ON (tmp_way_aggs.way_id=ways.id);
        """.format(table)
        self.do_query(query)

    # Normal execution

    def way_nodes(self, prefix):
        query = """
            SET enable_hashagg = FALSE;
            CREATE TABLE {}_way_nodes AS
            SELECT way_nodes.way_id AS way_id,
                   way_nodes.node_id AS node_id,
                   way_nodes.seq_id AS seq_id,
                   nodes.geom AS geom
            FROM
              ( SELECT way_id,
                       node_id,
                       sequence_id AS seq_id
               FROM way_nodes ) AS way_nodes
            INNER JOIN nodes ON (way_nodes.node_id=nodes.id);
        """.format(prefix)
        self.do_query(query)

    def node_counts(self, prefix):
        query = """
            SET enable_hashagg = FALSE;
            CREATE TABLE {}_node_counts AS
            SELECT way_nodes.node_id,
                   count(way_nodes.way_id) AS COUNT
            FROM way_nodes
            GROUP BY node_id;
        """.format(prefix)
        self.do_query(query)

    def way_counts(self, prefix):
        query = """
            SET enable_hashagg = FALSE;
            CREATE TABLE {0}_way_counts AS
            SELECT {0}_way_nodes.way_id,
                   {0}_way_nodes.node_id AS node_id,
                   {0}_way_nodes.seq_id AS seq_id, 
                   {0}_way_nodes.geom AS geom,
                   {0}_node_counts.count AS COUNT
            FROM {0}_way_nodes
            INNER JOIN {0}_node_counts
                ON ({0}_way_nodes.node_id={0}_node_counts.node_id);
        """.format(prefix)  # noqa
        self.do_query(query)

    def way_aggs(self, prefix):
        query = """
            SET enable_hashagg = FALSE;
            CREATE TABLE {0}_way_aggs AS
            SELECT {0}_way_counts.way_id,
                   array_agg({0}_way_counts.seq_id) AS seq,
                   array_agg({0}_way_counts.node_id) AS nodes,
                   array_agg({0}_way_counts.count) AS counts,
                   array_agg(ST_AsBinary({0}_way_counts.geom)) AS geoms
            FROM {0}_way_counts
            GROUP BY {0}_way_counts.way_id;
        """.format(prefix)
        self.do_query(query)

    def ways(self, table, prefix):
        query = """
            SET enable_hashagg = FALSE;
            CREATE TABLE {0} AS
            SELECT {1}_way_aggs.way_id,
                   ways.tags AS tags,
                   {1}_way_aggs.seq AS seq,
                   {1}_way_aggs.nodes AS nodes,
                   {1}_way_aggs.counts AS counts,
                   {1}_way_aggs.geoms AS geoms
            FROM {1}_way_aggs
            INNER JOIN ways ON ({1}_way_aggs.way_id=ways.id);
        """.format(table, prefix)
        self.do_query(query)

    def create_index(self, table, column):
        """Setup index on geom"""
        print("Create index on intermediate table {}.".format(table))
        query = "CREATE INDEX idx_{0}_{1} ON {0} ({1});".format(table, column)
        self.do_query(query)


def create_ways_table(options, password):
    db = Osm2Ways(
        options.host,
        options.port,
        options.database,
        options.user,
        password,
        options.printonly
    )
    if db.exists(options.table):
        while True:
            print("Table '{}' already exists in database '{}'."
                  .format(options.table, options.database))
            msg = "Do you want to remove the table (y/n)? [n]: "
            try:
                value = raw_input(msg)
            except NameError:
                value = input(msg)
            if value.lower() == 'y':
                break
            elif value.lower() == 'n' or value == '':
                print("Cancelled by user.")
                exit(0)
        db.drop(options.table)
        print("Table '{}' has been removed.".format(options.table))
    if options.slim:
        print("Execute in slim mode.")
        db.slim(options.table)
    else:
        print("Execute in normal mode.")
        # Way nodes
        way_nodes = "{}_way_nodes".format(options.prefix)
        print("(1/5) Create intermediate table {}.".format(way_nodes))
        db.drop_if_exists(way_nodes)
        db.way_nodes(options.prefix)
        db.create_index(way_nodes, "node_id")
        # Node counts
        node_counts = "{}_node_counts".format(options.prefix)
        print("(2/5) Create intermediate table {}.".format(node_counts))
        db.drop_if_exists(node_counts)
        db.node_counts(options.prefix)
        db.create_index(node_counts, "node_id")
        # Way counts (joins way nodes and node counts)
        way_counts = "{}_way_counts".format(options.prefix)
        print("(3/5) Create intermediate table {}.".format(way_counts))
        db.drop_if_exists(way_counts)
        db.way_counts(options.prefix)
        db.drop(way_nodes)
        db.drop(node_counts)
        db.create_index(way_counts, "way_id")
        # Way aggregations (uses way counts)
        way_aggs = "{}_way_aggs".format(options.prefix)
        print("(4/5) Create intermediate table {}.".format(way_aggs))
        db.drop_if_exists(way_aggs)
        db.way_aggs(options.prefix)
        db.drop(way_counts)
        db.create_index(way_aggs, "way_id")
        # Target table (uses way aggregations)
        print("(5/5) Create table {}.".format(options.table))
        db.ways(options.table, options.prefix)
        db.drop(way_aggs)
    db.close_connection()
    print("Finished.")


def main():
    parser = create_parser()
    options, args = parser.parse_args()
    if (options.host is None
            or options.port is None
            or options.database is None
            or options.table is None
            or options.user is None
            or (not options.slim and options.prefix is None)):
        parser.print_help()
        exit(1)
    if options.password is None:
        password = getpass.getpass("Password:")
    else:
        password = options.password
    create_ways_table(options, password)


if __name__ == '__main__':
    main()
