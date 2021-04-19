# Flyweight enum class that defines a parser
# Parser object to be used by batch.py and stream.py

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

import optparse
import json
import subprocess
import time
import datetime

class Flyweight(Enum):
    PARSER = optparse.OptionParser("stream.py [options]")

    def setParser(self):
        self.add_option("--host", dest="host", help="IP address of tracker.")
        self.add_option("--port", dest="port", help="Port of tracker.")
        self.add_option("--file", dest="file", help="JSON file with sample data.")
        self.add_option("--id", dest="id", help="Object id.")
        self.add_option("--step", action="store_true", dest="step", default=False, help="Send stepwise.")

        (options, args) = self.parse_args()

        if options.file == None or options.host == None or options.port == None:
            self.print_help()
            exit(1)

        with open(options.file) as jsonfile:
            samples = json.load(jsonfile)

        previous = None

        return samples

   