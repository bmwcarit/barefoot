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

import unittest
import binascii
import sys

# Add parent folder to path to make import work in Python 3
sys.path.insert(0, '../')
import bfmap
import ways2bfmap


class TestBfmap(unittest.TestCase):

    def test_waysort(self):
        hstore = get_osm_tags()
        seq = [3, 5, 7, 6, 0, 1, 4, 2]
        nodes = [21092556, 1015838859, 564144, 1015838846,
                 564143, 1015824338, 1015824359, 1015824357]
        counts = [1, 1, 3, 1, 2, 1, 1, 1]
        geoms = get_geom()
        row = (2557090, hstore, seq, nodes, counts, geoms)

        tags = bfmap.tags_str_to_dict(row[1])
        way = bfmap.waysort(row)

        self.assertEqual(tags["highway"], "trunk")

        nodes_sorted = [564143, 1015824338, 1015824357,
                        21092556, 1015824359, 1015838859, 1015838846, 564144]
        counts_sorted = [2, 1, 1, 1, 1, 1, 1, 3]

        previous = None
        for e in way[:, 0]:
            if previous is not None:
                self.assertGreater(e, previous)
            previous = e

        for i in range(0, len(way[:, 0])):
            self.assertEqual(nodes_sorted[i], int(way[i, 1]))
            self.assertEqual(counts_sorted[i], int(way[i, 2]))

    def test_type(self):
        config = {"highway": {"trunk": (101, 1.0, 120), "teriary": (102, 1.0, 120)}}
        tags = {"highway": "trunk", "lanes": "2"}
        key, value = bfmap.get_type(config, tags)

        self.assertEqual("highway", key)
        self.assertEqual("trunk", value)

        tags = {"highway": "primary", "lanes": "2"}
        key, value = bfmap.get_type(config, tags)

        self.assertEqual(None, key)
        self.assertEqual(None, value)

    def test_segment(self):
        hstore = get_osm_tags()
        seq = [3, 5, 7, 6, 0, 1, 4, 2]
        nodes = [21092556, 1015838859, 564144, 1015838846,
                 564143, 1015824338, 1015824359, 1015824357]
        counts = [1, 1, 3, 1, 2, 1, 1, 1]
        geoms = get_geom()
        row = (2557090, hstore, seq, nodes, counts, geoms)
        config = {"highway": {"trunk": (101, 1.0, 120)}}

        segments = bfmap.create_segments(config, row)
        self.assertEqual(1, len(segments))
        self.assertEqual(564143, int(segments[0][2]))
        self.assertEqual(564144, int(segments[0][3]))
        self.assertEqual(-1, segments[0][5])
        self.assertEqual(60, segments[0][6])

    def test_segment2(self):
        hstore = get_osm_tags()
        seq = [3, 5, 7, 6, 0, 1, 4, 2]
        nodes = [21092556, 1015838859, 564144, 1015838846,
                 564143, 1015824338, 1015824359, 1015824357]
        counts = [1, 1, 3, 1, 2, 1, 2, 1]
        geoms = get_geom()
        row = (2557090, hstore, seq, nodes, counts, geoms)
        config = {"highway": {"trunk": (101, 1.0, 120)}}

        segments = bfmap.create_segments(config, row)
        self.assertEqual(2, len(segments))

    def test_maxspeed(self):
        tags = {"maxspeed": "60 mph"}
        (fwd, bwd) = bfmap.maxspeed(tags)
        self.assertEqual(60 * 1.609, fwd)
        self.assertEqual(60 * 1.609, bwd)

        tags = {"maxspeed": "60"}
        (fwd, bwd) = bfmap.maxspeed(tags)
        self.assertEqual(60, fwd)
        self.assertEqual(60, bwd)

        tags = {"maxspeed:forward": "60 mph", "maxspeed:backward": "60 mph"}
        (fwd, bwd) = bfmap.maxspeed(tags)
        self.assertEqual(60 * 1.609, fwd)
        self.assertEqual(60 * 1.609, bwd)

        tags = {"maxspeed:forward": "60", "maxspeed:backward": "60"}
        (fwd, bwd) = bfmap.maxspeed(tags)
        self.assertEqual(60, fwd)
        self.assertEqual(60, bwd)

        tags = {"maxspeed": "60mph"}
        (fwd, bwd) = bfmap.maxspeed(tags)
        self.assertEqual("null", fwd)
        self.assertEqual("null", bwd)

    def test_ways2bfmap(self):
        properties = dict(line.strip().split('=')
                          for line in open('/mnt/map/tools/test/test.properties'))
        db = bfmap.Bfmap("localhost", 5432, properties["database"],
                         properties["user"], properties["password"], False)
        db.create_schema("bfmap_ways")
        config = ways2bfmap.read_config_file(properties["config"])
        db.ways2bfmap("localhost", 5432, properties["database"], "temp_ways",
                      properties["user"], properties["password"], "bfmap_ways",
                      config)


def get_osm_tags():
    return ('"hgv"=>"delivery", "ref"=>"B 2R", "name"=>"Isarring", "lanes"=>"2", '
            '"oneway"=>"yes", "highway"=>"trunk", "maxspeed"=>"60", "motorroad"=>"yes"')


def get_geom():
    geom_strings = [
        '0101000000831f306a523127404908a062e6144840', '010100000048567e198c31274092bd9470d7144840',
        '01010000007989fbd9d931274028806264c9144840', '0101000000dcedc4f6a4312740651d8eaed2144840',
        '0101000000427452a9233127404f8f1260fd144840', '0101000000a27197b32d31274059c6866ef6144840',
        '01010000009cf232d472312740b9c83d5ddd144840', '0101000000f32444543c312740210e6d5bef144840',
    ]
    return [binascii.unhexlify(x) for x in geom_strings]


if __name__ == '__main__':
    unittest.main()
