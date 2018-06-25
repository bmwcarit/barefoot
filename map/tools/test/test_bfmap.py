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
import bfmap


class TestBfmap(unittest.TestCase):

    def test_waysort(self):
        hstore = '"hgv"=>"delivery", "ref"=>"B 2R", "name"=>"Isarring", "lanes"=>"2", "oneway"=>"yes", "highway"=>"trunk", "maxspeed"=>"60", "motorroad"=>"yes"'
        seq = [3, 5, 7, 6, 0, 1, 4, 2]
        nodes = [21092556, 1015838859, 564144, 1015838846,
                 564143, 1015824338, 1015824359, 1015824357]
        counts = [1, 1, 3, 1, 2, 1, 1, 1]
        geoms = [binascii.unhexlify(x) for x in ['0101000000831f306a523127404908a062e6144840', '010100000048567e198c31274092bd9470d7144840', '01010000007989fbd9d931274028806264c9144840', '0101000000dcedc4f6a4312740651d8eaed2144840',
                                                 '0101000000427452a9233127404f8f1260fd144840', '0101000000a27197b32d31274059c6866ef6144840', '01010000009cf232d472312740b9c83d5ddd144840', '0101000000f32444543c312740210e6d5bef144840']]

        row = (2557090, hstore, seq, nodes, counts, geoms)

        (tags, way) = bfmap.waysort(row)

        self.assertEqual(tags["highway"], "trunk")

        nodes_sorted = [564143, 1015824338, 1015824357,
                        21092556, 1015824359, 1015838859, 1015838846, 564144]
        counts_sorted = [2, 1, 1, 1, 1, 1, 1, 3]

        previous = None
        for e in way[:, 0]:
            if (previous != None):
                self.assertGreater(e, previous)
            previous = e

        for i in range(0, len(way[:, 0])):
            self.assertEqual(nodes_sorted[i], int(way[i, 1]))
            self.assertEqual(counts_sorted[i], int(way[i, 2]))

    def test_type(self):
        config = {
            "highway": {"trunk": (101, 1.0, 120), "teriary": (102, 1.0, 120)}}
        tags = {"highway": "trunk", "lanes": "2"}
        (key, value) = bfmap.type(config, tags)

        self.assertEquals("highway", key)
        self.assertEquals("trunk", value)

        tags = {"highway": "primary", "lanes": "2"}
        (key, value) = bfmap.type(config, tags)

        self.assertEquals(None, key)
        self.assertEquals(None, value)

    def test_segment(self):
        hstore = '"hgv"=>"delivery", "ref"=>"B 2R", "name"=>"Isarring", "lanes"=>"2", "oneway"=>"yes", "highway"=>"trunk", "maxspeed"=>"60", "motorroad"=>"yes"'
        seq = [3, 5, 7, 6, 0, 1, 4, 2]
        nodes = [21092556, 1015838859, 564144, 1015838846,
                 564143, 1015824338, 1015824359, 1015824357]
        counts = [1, 1, 3, 1, 2, 1, 1, 1]
        geoms = [binascii.unhexlify(x) for x in ['0101000000831f306a523127404908a062e6144840', '010100000048567e198c31274092bd9470d7144840', '01010000007989fbd9d931274028806264c9144840', '0101000000dcedc4f6a4312740651d8eaed2144840',
                                                 '0101000000427452a9233127404f8f1260fd144840', '0101000000a27197b32d31274059c6866ef6144840', '01010000009cf232d472312740b9c83d5ddd144840', '0101000000f32444543c312740210e6d5bef144840']]
        row = (2557090, hstore, seq, nodes, counts, geoms)
        config = {"highway": {"trunk": (101, 1.0, 120)}}

        segments = bfmap.segment(config, row)
        self.assertEquals(1, len(segments))
        self.assertEquals(564143, int(segments[0][2]))
        self.assertEquals(564144, int(segments[0][3]))
        self.assertEquals(-1, segments[0][5])
        self.assertEquals(60, segments[0][6])

    def test_segment2(self):
        hstore = '"hgv"=>"delivery", "ref"=>"B 2R", "name"=>"Isarring", "lanes"=>"2", "oneway"=>"yes", "highway"=>"trunk", "maxspeed"=>"60", "motorroad"=>"yes"'
        seq = [3, 5, 7, 6, 0, 1, 4, 2]
        nodes = [21092556, 1015838859, 564144, 1015838846,
                 564143, 1015824338, 1015824359, 1015824357]
        counts = [1, 1, 3, 1, 2, 1, 2, 1]
        geoms = [binascii.unhexlify(x) for x in ['0101000000831f306a523127404908a062e6144840', '010100000048567e198c31274092bd9470d7144840', '01010000007989fbd9d931274028806264c9144840', '0101000000dcedc4f6a4312740651d8eaed2144840',
                                                 '0101000000427452a9233127404f8f1260fd144840', '0101000000a27197b32d31274059c6866ef6144840', '01010000009cf232d472312740b9c83d5ddd144840', '0101000000f32444543c312740210e6d5bef144840']]
        row = (2557090, hstore, seq, nodes, counts, geoms)
        config = {"highway": {"trunk": (101, 1.0, 120)}}

        segments = bfmap.segment(config, row)
        self.assertEquals(2, len(segments))

    def test_extract_limit_number(self):
        self.assertEquals(60, bfmap.extract_speed_limit("60"))

    def test_extract_limit_space_mph(self):
        self.assertEquals(60 * 1.609, bfmap.extract_speed_limit("60 mph"))

    def test_extract_limit_nospace_mph(self):
        self.assertEquals(60 * 1.609, bfmap.extract_speed_limit("60mph"))

    def test_extract_limit_unrecognised(self):
        self.assertEquals(None, bfmap.extract_speed_limit("national"))

    def test_maxspeed_space_mph(self):
        tags = {"maxspeed": "60 mph"}
        (fwd, bwd) = bfmap.maxspeed(tags)
        self.assertEquals(60 * 1.609, fwd)
        self.assertEquals(60 * 1.609, bwd)

    def test_maxspeed_number(self):
        tags = {"maxspeed": "60"}
        (fwd, bwd) = bfmap.maxspeed(tags)
        self.assertEquals(60, fwd)
        self.assertEquals(60, bwd)

    def test_maxspeed_fb_space_mph(self):
        tags = {"maxspeed:forward": "60 mph", "maxspeed:backward": "60 mph"}
        (fwd, bwd) = bfmap.maxspeed(tags)
        self.assertEquals(60 * 1.609, fwd)
        self.assertEquals(60 * 1.609, bwd)

    def test_maxspeed_fb_kph(self):
        tags = {"maxspeed:forward": "60", "maxspeed:backward": "60"}
        (fwd, bwd) = bfmap.maxspeed(tags)
        self.assertEquals(60, fwd)
        self.assertEquals(60, bwd)

    def test_maxspeed_nospace_mph(self):
        tags = {"maxspeed": "60mph"}
        (fwd, bwd) = bfmap.maxspeed(tags)
        self.assertEquals(60 * 1.609, fwd)
        self.assertEquals(60 * 1.609, bwd)

    def test_maxspeed_specific_overrides_general(self):
        tags = {"maxspeed": "20", "maxspeed:backward": "30"}
        (fwd, bwd) = bfmap.maxspeed(tags)
        self.assertEquals(20, fwd)
        self.assertEquals(30, bwd)

    def test_ways2bfmap(self):
        from os.path import join, dirname, abspath
        prop_file = join(dirname(abspath(__file__)), 'test.properties')
        properties = dict(line.strip().split('=') for line in open(prop_file))
        bfmap.schema("localhost", 5432, properties[
                     "database"], "bfmap_ways", properties["user"], properties["password"], False)
        config = bfmap.config(properties["config"])
        bfmap.ways2bfmap("localhost", 5432, properties["database"], "temp_ways", properties["user"], properties[
                         "password"], "localhost", 5432, properties["database"], "bfmap_ways", properties["user"], properties["password"], config, False)
        return

if __name__ == '__main__':
    unittest.main()
