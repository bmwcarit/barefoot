#!/usr/bin/env python

#
# Copyright (C) 2016, BMW Car IT GmbH
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
__copyright__ = "Copyright 2016 BMW Car IT GmbH"
__license__ = "Apache-2.0"

import optparse
import json
import subprocess
import time
import datetime
from .flyweight import Flyweight

samples = Flyweight.PARSER.setParser

for sample in samples:
    if options.id != None:
        sample["id"] = options.id
    if isinstance(sample['time'], (int, long)):
        current = time.mktime(datetime.datetime.fromtimestamp(sample['time'] / 1000).timetuple())
    else:
        current = time.mktime(datetime.datetime.strptime(sample['time'][:-5], "%Y-%m-%d %H:%M:%S").timetuple())
    if options.step == True:
        raw_input("Press Enter to continue...")
    elif previous != None:
        time.sleep(current - previous)
    previous = current
    print(json.dumps(sample))
    subprocess.call("echo '%s' | netcat %s %s" % (json.dumps(sample), options.host, options.port), shell=True)
