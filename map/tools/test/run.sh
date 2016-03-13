#!/bin/bash

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

. /mnt/map/tools/test/test.properties
export PYTHONPATH=/mnt/map/tools/
cd /mnt/map/tools/test

echo "Create ${database} database ..."
sudo -u postgres psql -q -c "CREATE DATABASE ${database};"
sudo -u postgres psql -q -d ${database} -c "CREATE EXTENSION hstore;"
sudo -u postgres psql -q -d ${database} -c "CREATE EXTENSION postgis;"
sudo -u postgres psql -q -c "CREATE USER ${user} PASSWORD '${password}';"
sudo -u postgres psql -q -c "GRANT ALL ON DATABASE ${database} TO ${user};"

echo "Run bfmap test ..."
sudo -u postgres psql -q -d ${database} -f /mnt/map/tools/test/${bfmap_table}
sudo -u postgres psql -q -d ${database} -c "GRANT ALL ON TABLE temp_ways TO ${user};"
python -m unittest test_bfmap

echo "Delete ${database} database ..."
sudo -u postgres psql -q -c "DROP DATABASE ${database};"
sudo -u postgres psql -q -c "DROP USER IF EXISTS ${user};"
echo "Done."
