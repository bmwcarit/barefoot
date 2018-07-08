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

import psycopg2.extensions
import numpy
import binascii
import osgeo.ogr as ogr

import common

# The default encoding is 'ascii', which leads to errors in Python 3.
psycopg2.extensions.encodings['SQLASCII'] = 'utf-8'


class Bfmap(common.Database):
    """Create the Barefoot segments table and fill it."""
    def __init__(self, host, port, database, user, password, printonly):
        super(Bfmap, self).__init__(host, port, database, user, password, printonly)

    def create_schema(self, table):
        """Create the Barefoot segments table."""
        query = """
            CREATE TABLE {} (
                gid BIGSERIAL,
                osm_id bigint NOT NULL,
                class_id integer NOT NULL,
                SOURCE bigint NOT NULL,
                target bigint NOT NULL,
                LENGTH double precision NOT NULL,
                reverse double precision NOT NULL,
                maxspeed_forward integer, 
                maxspeed_backward integer, 
                priority double precision NOT NULL
            );
            SELECT AddGeometryColumn(%s, 'geom', 4326, 'LINESTRING', 2);
        """.format(table)
        self.execute(query, params=(table, ))

        query = "CREATE INDEX idx_{0}_geom ON {0} USING gist(geom);".format(table)
        self.execute(query)

    def ways2bfmap(self, src_host, src_port, src_database, src_table, src_user,
                   src_password, tgt_table, config):
        """Make Barefoot segments from ways and insert them in the database."""
        db_source = common.Database(src_host, src_port, src_database, src_user,
                                    src_password, self.printonly)
        db_source.cursor = db_source.connection.cursor('{}_cursor'.format(src_table))

        query = "SELECT way_id,tags,seq,nodes,counts,geoms FROM {};".format(src_table)
        db_source.execute(query, commit=False)

        rowcount = 0
        roadcount = 0
        query_template = ("('{}','{}','{}','{}','{}','{}', {}, {},'{}',"
                          "ST_GeomFromText('{}',4326))")
        while True:
            rows = db_source.cursor.fetchmany(10000)
            if len(rows) == 0:
                break
            segments = []
            for row in rows:
                segments.extend(create_segments(config, row))
            roadcount += len(segments)
            rowcount += len(rows)
            vals = ",".join([query_template.format(*seg) for seg in segments])
            query = """
                INSERT INTO {} (osm_id,class_id,source,target,length,reverse,
                maxspeed_forward,maxspeed_backward,priority,geom) VALUES {};
            """.format(tgt_table, vals)
            self.execute(query)
            print("{} segments from {} ways inserted.".format(roadcount, rowcount))

        print("{} segments from {} ways inserted and finished.".format(roadcount, rowcount))
        db_source.close()


def create_segments(config, row):
    """Create segments from a single way."""
    segments = []

    if len(row[1]) < 1 or len(row[2]) < 2:
        return segments

    tags = tags_str_to_dict(row[1])
    key, value = get_type(config, tags)
    if key is None or value is None:
        return segments

    way = waysort(row)

    osm_id = row[0]
    class_id = int(config[key][value][0])
    source = way[0][1]
    length = 1
    reverse = -1 if is_oneway(tags) else 1
    maxspeed_forward, maxspeed_backward = maxspeed(tags)
    priority = float(config[key][value][1])

    line = ogr.Geometry(ogr.wkbLineString)
    point = ogr.CreateGeometryFromWkb(binascii.unhexlify(way[0][3]))
    line.AddPoint(point.GetX(), point.GetY())
    for i in range(1, len(way[:, 0])):
        point = ogr.CreateGeometryFromWkb(binascii.unhexlify(way[i][3]))
        line.AddPoint(point.GetX(), point.GetY())
        if int(way[i][2]) >= 2 or i == (len(way[:, 0]) - 1):
            line.FlattenTo2D()
            segment = (
                osm_id,
                class_id,
                source.decode('utf-8'),
                way[i][1].decode('utf-8'),
                length,
                reverse,
                maxspeed_forward,
                maxspeed_backward,
                priority,
                line.ExportToWkt()
            )
            segments.append(segment)
            source = way[i][1]
            line = ogr.Geometry(ogr.wkbLineString)
            point = ogr.CreateGeometryFromWkb(binascii.unhexlify(way[i][3]))
            line.AddPoint(point.GetX(), point.GetY())
    return segments


def tags_str_to_dict(tags_str):
    """Convert a single string with OSM tags to a dictionary."""
    return dict((k.strip(), v.strip()) for k, v in (
        item.split("\"=>\"") for item in tags_str[1:-1].split("\", \"")))


def waysort(row):
    row[5][:] = [binascii.hexlify(x) for x in row[5]]
    way = numpy.array((row[2], row[3], row[4], row[5])).T
    way = way[numpy.argsort([int(i) for i in way[:, 0]])]
    return way


def get_type(config, tags):
    key = None
    value = None
    for tag in tags.keys():
        tag_ = tag
        if tag_ in config.keys():
            if tags[tag_] in config[tag_].keys():
                key = tag_
                value = tags[tag_]
    return key, value


def is_oneway(tags):
    """Return a boolean whether the road is one way."""
    if "junction" in tags.keys() or "roundabout" in tags.keys():
        return True
    elif "oneway" in tags.keys():
        if tags["oneway"] in ["yes", "true", "1"]:
            return True
    return False


def maxspeed(tags):
    """Return the forward and backward maximum speed (or 'null')."""
    if "maxspeed:forward" in tags.keys():
        forward = _get_maxspeed(tags, "maxspeed:forward")
    else:
        forward = _get_maxspeed(tags, "maxspeed")
    if "maxspeed:backward" in tags.keys():
        backward = _get_maxspeed(tags, "maxspeed:backward")
    else:
        backward = _get_maxspeed(tags, "maxspeed")
    return forward, backward


def _get_maxspeed(tags, key):
    """Get the maximum speed with the given key from the tags.

    If it was not found, or could not be converted to int correctly,
    'null' is returned.
    """
    if key in tags.keys():
        try:
            if "mph" in tags[key]:
                return int(tags[key].split(" ")[0]) * 1.609
            else:
                return int(tags[key])
        except ValueError:
            pass
    return 'null'



