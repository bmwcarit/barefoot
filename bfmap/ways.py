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

import psycopg2

# Slim execution

def slim(host, port, database, table, user, password, printonly):
    try:
        dbcon = psycopg2.connect(
            host=host, port=port, database=database, user=user, password=password)
        cursor = dbcon.cursor()
    except:
        print("Connection to database failed.")
        exit(1)

    try:
        query = """create table %s as select tmp_way_aggs.way_id,ways.tags as tags,
        tmp_way_aggs.seq as seq,tmp_way_aggs.nodes as nodes,tmp_way_aggs.counts as 
        counts,tmp_way_aggs.geoms as geoms 
        from (
        select tmp_way_nodes.way_id as way_id,array_agg(tmp_way_nodes.seq_id) 
        as seq,array_agg(tmp_way_nodes.node_id) as nodes, 
        array_agg(tmp_node_counts.count) as counts,
        array_agg(ST_AsBinary(tmp_way_nodes.geom)) as geoms 
        from ( 
        select way_nodes.way_id as way_id,way_nodes.node_id as 
        node_id,way_nodes.seq_id as seq_id, nodes.geom as geom 
        from ( 
        select way_id,node_id,sequence_id as seq_id 
        from way_nodes 
        ) as way_nodes 
        inner join nodes on (way_nodes.node_id=nodes.id) 
        ) as tmp_way_nodes 
        inner join ( 
        select node_id,count(way_id) as count 
        from way_nodes 
        group by node_id 
        ) as tmp_node_counts on (tmp_way_nodes.node_id=tmp_node_counts.node_id) 
        group by tmp_way_nodes.way_id 
        ) as tmp_way_aggs 
        inner join ways on (tmp_way_aggs.way_id=ways.id);""" % table
        if printonly == True:
            print(query)
        else:
            cursor.execute(query)
            dbcon.commit()
    except Exception, e:
        print("Database transaction failed. (%s)" % e.pgerror)
        exit(1)

    cursor.close()
    dbcon.close()

# Normal execution


def way_nodes(host, port, database, prefix, user, password, printonly):
    try:
        dbcon = psycopg2.connect(
            host=host, port=port, database=database, user=user, password=password)
        cursor = dbcon.cursor()
    except:
        print("Connection to database failed.")
        exit(1)

    try:
        query = """set enable_hashagg = false; create table %s_way_nodes as select 
        way_nodes.way_id as way_id,way_nodes.node_id as node_id,way_nodes.seq_id as 
        seq_id, nodes.geom as geom 
        from ( 
        select way_id,node_id,sequence_id as seq_id 
        from way_nodes 
        ) as way_nodes 
        inner join nodes on (way_nodes.node_id=nodes.id);""" % prefix
        if printonly == True:
            print(query)
        else:
            cursor.execute(query)
            dbcon.commit()
    except Exception, e:
        print("Database transaction failed. (%s)" % e.pgerror)
        exit(1)

    cursor.close()
    dbcon.close()


def node_counts(host, port, database, prefix, user, password, printonly):
    try:
        dbcon = psycopg2.connect(
            host=host, port=port, database=database, user=user, password=password)
        cursor = dbcon.cursor()
    except:
        print("Connection to database failed.")
        exit(1)

    try:
        query = """set enable_hashagg = false; create table %s_node_counts as select 
        way_nodes.node_id,count(way_nodes.way_id) as count 
        from way_nodes 
        group by node_id;""" % prefix
        if printonly == True:
            print(query)
        else:
            cursor.execute(query)
            dbcon.commit()
    except Exception, e:
        print("Database transaction failed. (%s)" % e.pgerror)
        exit(1)

    cursor.close()
    dbcon.close()


def way_counts(host, port, database, prefix, user, password, printonly):
    try:
        dbcon = psycopg2.connect(
            host=host, port=port, database=database, user=user, password=password)
        cursor = dbcon.cursor()
    except:
        print("Connection to database failed.")
        exit(1)

    try:
        query = """set enable_hashagg = false; create table %s_way_counts as select 
        %s_way_nodes.way_id,%s_way_nodes.node_id as node_id,%s_way_nodes.seq_id as seq_id,
        %s_way_nodes.geom as geom,%s_node_counts.count as count \
        from %s_way_nodes \
        inner join %s_node_counts on 
        (%s_way_nodes.node_id=%s_node_counts.node_id);
        """ % (prefix, prefix, prefix, prefix, prefix, prefix, prefix, prefix, prefix, prefix)
        if printonly == True:
            print(query)
        else:
            cursor.execute(query)
            dbcon.commit()
    except Exception, e:
        print("Database transaction failed. (%s)" % e.pgerror)
        exit(1)

    cursor.close()
    dbcon.close()


def way_aggs(host, port, database, prefix, user, password, printonly):
    try:
        dbcon = psycopg2.connect(
            host=host, port=port, database=database, user=user, password=password)
        cursor = dbcon.cursor()
    except:
        print("Connection to database failed.")
        exit(1)

    try:
        query = """set enable_hashagg = false; create table %s_way_aggs as select 
        %s_way_counts.way_id, array_agg(%s_way_counts.seq_id) as seq,
        array_agg(%s_way_counts.node_id) as nodes, array_agg(%s_way_counts.count) 
        as counts,array_agg(ST_AsBinary(%s_way_counts.geom)) as geoms 
        from %s_way_counts group by  %s_way_counts.way_id;
        """ % (prefix, prefix, prefix, prefix, prefix, prefix, prefix, prefix)
        if printonly == True:
            print(query)
        else:
            cursor.execute(query)
            dbcon.commit()
    except Exception, e:
        print("Database transaction failed. (%s)" % e.pgerror)
        exit(1)

    cursor.close()
    dbcon.close()


def ways(host, port, database, table, prefix, user, password, printonly):
    try:
        dbcon = psycopg2.connect(
            host=host, port=port, database=database, user=user, password=password)
        cursor = dbcon.cursor()
    except:
        print("Connection to database failed.")
        exit(1)

    try:
        query = """set enable_hashagg = false; create table %s as 
        select %s_way_aggs.way_id, ways.tags as tags,%s_way_aggs.seq 
        as seq,%s_way_aggs.nodes as nodes,%s_way_aggs.counts as counts,
        %s_way_aggs.geoms as geoms from %s_way_aggs inner join ways on 
        (%s_way_aggs.way_id=ways.id);""" % (table, prefix, prefix, prefix, prefix, prefix, prefix, prefix)
        if printonly == True:
            print(query)
        else:
            cursor.execute(query)
            dbcon.commit()
    except Exception, e:
        print("Database transaction failed. (%s)" % e.pgerror)
        exit(1)

    cursor.close()
    dbcon.close()

# Check if table exists


def exists(host, port, database, table, user, password):
    try:
        dbcon = psycopg2.connect(
            host=host, port=port, database=database, user=user, password=password)
        cursor = dbcon.cursor()
    except:
        print("Connection to database failed.")
        exit(1)

    try:
        cursor.execute(
            """SELECT COUNT(tablename) FROM pg_tables WHERE 
            schemaname='public' AND tablename='%s';""" % table)
        dbcon.commit()
    except Exception, e:
        print("Database transaction failed. (%s)" % e.pgerror)
        exit(1)

    if cursor.fetchone()[0] == 0:
        result = False
    else:
        result = True

    cursor.close()
    dbcon.close()

    return result

# Clear table data


def remove(host, port, database, table, user, password, printonly):
    try:
        dbcon = psycopg2.connect(
            host=host, port=port, database=database, user=user, password=password)
        cursor = dbcon.cursor()
    except:
        print("Connection to database failed.")
        exit(1)

    try:
        query = "DROP TABLE %s;" % table
        if printonly == True:
            print(query)
        else:
            cursor.execute(query)
            dbcon.commit()
    except Exception, e:
        print("Database transaction failed. (%s)" % e.pgerror)
        exit(1)

    cursor.close()
    dbcon.close()

# Setup index on geom


def index(host, port, database, table, column, user, password, printonly):
    try:
        dbcon = psycopg2.connect(
            host=host, port=port, database=database, user=user, password=password)
        cursor = dbcon.cursor()
    except:
        print("Connection to database failed.")
        exit(1)

    try:
        query = "CREATE INDEX idx_%s_%s ON %s (%s);" % (
            table, column, table, column)
        if printonly == True:
            print(query)
        else:
            cursor.execute(query)
            dbcon.commit()
    except Exception, e:
        print("Database transaction failed. (%s)" % e.pgerror)
        exit(1)

    cursor.close()
    dbcon.close()
