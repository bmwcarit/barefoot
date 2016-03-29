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
import numpy
import binascii
import osgeo.ogr as ogr
import json

# Import OSM (osmosis) to route


def ways2bfmap(src_host, src_port, src_database, src_table, src_user, src_password,
               tgt_host, tgt_port, tgt_database, tgt_table, tgt_user, tgt_password,
               config, printonly):

   # Open database onnection
    try:
        src_con = psycopg2.connect(
            host=src_host, port=src_port, database=src_database, user=src_user,
            password=src_password)
        src_cur = src_con.cursor("%s_cursor" % src_table)
        tgt_con = psycopg2.connect(
            host=tgt_host, port=tgt_port, database=tgt_database, user=tgt_user,
            password=tgt_password)
        tgt_cur = tgt_con.cursor()
    except Exception as e:
        print("Connection to database failed. %s" % e)
        exit(1)

    try:
        src_cur.execute(
            "SELECT way_id,tags,seq,nodes,counts,geoms FROM %s;" % src_table)
    except Exception, e:
        print("Database transaction failed. (%s)" % e.pgerror)
        exit(1)

    rowcount = 0
    roadcount = 0

    # Process chunks
    while True:
        rows = src_cur.fetchmany(10000)
        if len(rows) == 0:
            break

        segments = []

        for row in rows:
            if len(row[1]) < 1 or len(row[2]) < 2:
                continue

            segments_ = segment(config, row)
            segments += segments_
            roadcount += len(segments_)

        rowcount += len(rows)

        try:
            query = """INSERT INTO %s (osm_id,class_id,source,target,length,reverse,
                maxspeed_forward,maxspeed_backward,priority,geom) VALUES %s;""" % (
                tgt_table, ",".join("""('%s','%s','%s','%s','%s','%s', %s, %s,'%s',
                ST_GeomFromText('%s',4326))""" % segment for segment in segments))
            if printonly == False:
                tgt_cur.execute(query)
            print("%s segments from %s ways inserted." % (roadcount, rowcount))
        except Exception, e:
            print("Database transaction failed. (%s)" % e.pgerror)
            exit(1)

    print("%s segments from %s ways inserted and finished." % (roadcount, rowcount))

    tgt_con.commit()
    src_cur.close()
    tgt_cur.close()

    # Close connection
    src_con.close()
    tgt_con.close()


def waysort(row):
    tags = dict((k.strip(), v.strip()) for k, v in (
        item.split("\"=>\"") for item in row[1][1:-1].split("\", \"")))

    row[5][:] = [binascii.hexlify(x) for x in row[5]]
    way = numpy.array((row[2], row[3], row[4], row[5])).T
    way = way[numpy.argsort([int(i) for i in way[:, 0]])]

    return (tags, way)


def type(config, tags):
    key = None
    value = None
    for tag in tags.keys():
        tag_ = tag.decode('utf-8')
        if tag_ in config.keys():
            if tags[tag_] in config[tag_].keys():
                key = tag_
                value = tags[tag_]

    return (key, value)


def is_oneway(tags):
    if ("oneway" in tags.keys() and tags["oneway"] == "yes") \
            or ("oneway" in tags.keys() and tags["oneway"] == "true") \
            or ("oneway" in tags.keys() and tags["oneway"] == "1") \
            or ("junction" in tags.keys()) \
            or ("roundabout" in tags.keys()):
        return True
    else:
        return False


def maxspeed(tags):
    # maxspeed_forward = int(config[key][value][2])
    forward = "null"
    if ("maxspeed" in tags.keys()):
        try:
            if "mph" in tags["maxspeed"]:
                forward = int(tags["maxspeed"].split(" ")[0]) * 1.609
            else:
                forward = int(tags["maxspeed"])
        except:
            pass
    if ("maxspeed:forward" in tags.keys()):
        try:
            if "mph" in tags["maxspeed:forward"]:
                forward = int(tags["maxspeed:forward"].split(" ")[0]) * 1.609
            else:
                forward = int(tags["maxspeed:forward"])
        except:
            pass

    # maxspeed_backward = maxspeed_forward
    backward = "null"
    if ("maxspeed" in tags.keys()):
        try:
            if "mph" in tags["maxspeed"]:
                backward = int(tags["maxspeed"].split(" ")[0]) * 1.609
            else:
                backward = int(tags["maxspeed"])
        except:
            pass
    if ("maxspeed:backward" in tags.keys()):
        try:
            if "mph" in tags["maxspeed:backward"]:
                backward = int(tags["maxspeed:backward"].split(" ")[0]) * 1.609
            else:
                backward = int(tags["maxspeed:backward"])
        except:
            pass

    return (forward, backward)

def segment(config, row):
    segments = []

    if len(row[1]) < 1 or len(row[2]) < 2:
        return segments

    (tags, way) = waysort(row)
    (key, value) = type(config, tags)

    if key == None or value == None:
        return segments

    osm_id = row[0]
    class_id = int(config[key][value][0])
    source = way[0][1]
    length = 1
    if (is_oneway(tags)):
        reverse = -1
    else:
        reverse = 1
    (maxspeed_forward, maxspeed_backward) = maxspeed(tags)
    priority = float(config[key][value][1])

    line = ogr.Geometry(ogr.wkbLineString)
    point = ogr.CreateGeometryFromWkb(binascii.unhexlify(way[0][3]))
    line.AddPoint(point.GetX(), point.GetY())

    for i in range(1, len(way[:, 0])):
        point = ogr.CreateGeometryFromWkb(
            binascii.unhexlify(way[i][3]))
        line.AddPoint(point.GetX(), point.GetY())

        if ((int(way[i][2]) >= 2) or (i == (len(way[:, 0]) - 1))):
            line.FlattenTo2D()
            segment = (osm_id, class_id, source, way[i][
                1], length, reverse, maxspeed_forward,
                maxspeed_backward,
                priority, line.ExportToWkt())
            segments.append(segment)

            source = way[i][1]
            line = ogr.Geometry(ogr.wkbLineString)
            point = ogr.CreateGeometryFromWkb(
                binascii.unhexlify(way[i][3]))
            line.AddPoint(point.GetX(), point.GetY())

    return segments

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
            """SELECT COUNT(tablename) FROM pg_tables 
            WHERE schemaname='public' AND tablename='%s';""" % table)
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

# Create table schema

def schema(host, port, database, table, user, password, printonly):
    while not exists(host, port, database, table, user, password):
        try:
            dbcon = psycopg2.connect(
                host=host, port=port, database=database, user=user, password=password)
            cursor = dbcon.cursor()
        except:
            print("Connection to database failed.")
            exit(1)
    
        try:
            query = """CREATE TABLE %s(gid bigserial,
    				osm_id bigint NOT NULL,
    				class_id integer NOT NULL,
    				source bigint NOT NULL,
    				target bigint NOT NULL,
    				length double precision NOT NULL,
    				reverse double precision NOT NULL,
    				maxspeed_forward integer,
    				maxspeed_backward integer,
    				priority double precision NOT NULL);
    				SELECT AddGeometryColumn('%s','geom',4326,
    				'LINESTRING',2);""" % (table, table)
            if printonly == True:
                print(query)
            else:
                cursor.execute(query)
                dbcon.commit()
        except Exception, e:
            print("Database transaction failed. (%s)" % e.pgerror)
            
        try:
            query = "CREATE INDEX idx_%s_geom ON %s USING gist(geom);" % (
                table, table)
            if printonly == True:
                print(query)
            else:
                cursor.execute(query)
                dbcon.commit()
        except Exception, e:
            print("Database transaction failed. (%s)" % e.pgerror)
    
        cursor.close()
        dbcon.close()

# Setup index on geom

def index(host, port, database, table, user, password, printonly):
    try:
        dbcon = psycopg2.connect(
            host=host, port=port, database=database, user=user, password=password)
        cursor = dbcon.cursor()
    except:
        print("Connection to database failed.")
        exit(1)

    try:
        query = "CREATE INDEX idx_%s_geom ON %s USING gist(geom);" % (
            table, table)
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

# Read config file of road types

def config(file):
    jsonfile = open(file)
    jsondata = json.load(jsonfile)
    config = {}

    for tag in jsondata["tags"]:
        tagconfig = {}
        for value in tag["values"]:
            tagconfig[value["name"]] = (value["id"],
                                        value["priority"], value["maxspeed"])
        config[tag["tag"]] = tagconfig
    jsonfile.close()
    return config
