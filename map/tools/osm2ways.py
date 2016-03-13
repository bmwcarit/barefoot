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

__author__ = "sebastian.mattheis@bmw-carit.de"
__copyright__ = "Copyright 2015 BMW Car IT GmbH"
__license__ = "Apache-2.0"

import optparse
import getpass
import ways

parser = optparse.OptionParser("osm2ways.py [options]")
parser.add_option("--host", dest="host", help="Hostname of the database.")
parser.add_option("--port", dest="port", help="Port of the database.")
parser.add_option("--database", dest="database", help="Name of the database.")
parser.add_option("--table", dest="table", help="Name of the table.")
parser.add_option("--user", dest="user", help="User of the database.")
parser.add_option("--password", dest="password", help="User password.")
parser.add_option("--slim", action="store_true", default=False,
                  help="""Slim mode runs everything in a single query. This requires memory 
                  to be sufficiently available.""")
parser.add_option("--prefix", dest="prefix",
                  help="If not using slim mode, use this prefix for intermediate tables.")
parser.add_option("--printonly", action="store_true",
                  default=False, help="Do not execute commands, but print it.")


(options, args) = parser.parse_args()

if options.host == None or \
        options.port == None or \
        options.database == None or \
        options.table == None or \
        options.user == None or \
        (options.slim == False and options.prefix == None):
    parser.print_help()
    exit(1)

if options.password == None:
    password = getpass.getpass("Password:")
else:
    password = options.password

if ways.exists(options.host, options.port, options.database, options.table,
               options.user, password):
    print("Table '%s' already exists in database '%s'." %
          (options.table, options.database))
    while True:
        value = raw_input(
            "Do you want to remove table '%s' (y/n)? [n]: " % options.table).lower()
        if value == '' or value == 'n':
            print("Cancelled by user.")
            exit(0)
        elif value == 'y':
            break
    ways.remove(options.host, options.port, options.database,
                options.table, options.user, password, options.printonly)
    print("Table '%s' has been removed." % options.table)

if options.slim == True:
    print("Execute in slim mode ...")
    ways.slim(options.host, options.port, options.database,
              options.table, options.user, password, options.printonly)
    print("Done.")
else:
    print("Execute in normal mode ...")
    # ways.hashagg(options.host, options.port, options.database, options.user, password, False)
    # print("Turned hash aggregation off.")
    # Way nodes
    way_nodes = "%s_way_nodes" % options.prefix
    print("(1/5) Create intermediate table %s ..." % way_nodes)
    ways.way_nodes(options.host, options.port, options.database,
                   options.prefix, options.user, password, options.printonly)
    print("Done.")
    print("Create index on intermediate table %s ..." % way_nodes)
    ways.index(options.host, options.port, options.database,
               way_nodes, "node_id", options.user, password, options.printonly)
    print("Done.")

    # Node counts
    node_counts = "%s_node_counts" % options.prefix
    print("(2/5) Create intermediate table %s ..." % node_counts)
    ways.node_counts(options.host, options.port, options.database,
                     options.prefix, options.user, password, options.printonly)
    print("Done.")
    print("Create index on intermediate table %s ..." % node_counts)
    ways.index(options.host, options.port, options.database,
               node_counts, "node_id", options.user, password, options.printonly)
    print("Done.")

    # Way counts (joins way nodes and node counts)
    way_counts = "%s_way_counts" % options.prefix
    print("(3/5) Create intermediate table %s ..." % way_counts)
    ways.way_counts(options.host, options.port, options.database,
                    options.prefix, options.user, password, options.printonly)
    print("Done.")
    print("Drop intermediate tables %s and %s." % (way_nodes, node_counts))
    ways.remove(options.host, options.port, options.database,
                way_nodes, options.user, password, options.printonly)
    ways.remove(options.host, options.port, options.database,
                node_counts, options.user, password, options.printonly)
    print("Done.")
    print("Create index on intermediate table %s ..." % way_counts)
    ways.index(options.host, options.port, options.database,
               way_counts, "way_id", options.user, password, options.printonly)
    print("Done.")

    # Way aggregations (uses way counts)
    way_aggs = "%s_way_aggs" % options.prefix
    print("(4/5) Create intermediate table %s ..." % way_aggs)
    ways.way_aggs(options.host, options.port, options.database,
                  options.prefix, options.user, password, options.printonly)
    print("Done.")
    print("Drop intermediate table %s." % way_counts)
    ways.remove(options.host, options.port, options.database,
                way_counts, options.user, password, options.printonly)
    print("Done.")
    print("Create index on intermediate table %s ..." % way_aggs)
    ways.index(options.host, options.port, options.database,
               way_aggs, "way_id", options.user, password, options.printonly)
    print("Done.")

    # Target table (uses way aggregations)
    print("(5/5) Create table %s ..." % options.table)
    ways.ways(options.host, options.port, options.database, options.table,
              options.prefix, options.user, password, options.printonly)
    print("Done.")
    print("Drop intermediate table %s." % way_aggs)
    ways.remove(options.host, options.port, options.database,
                way_aggs, options.user, password, options.printonly)
    print("Done.")

    # ways.hashagg(options.host, options.port, options.database, options.user, password, True)
    # print("Turned hash aggregation on again.")
    print("Finished.")
