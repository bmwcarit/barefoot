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

if [ "$#" -eq "4" ]
then
	input=$1
	database=$2
	user=$3
	password=$4
elif [ "$#" -eq "0" ]
then
	input=/mnt/map/samples/x0001-015.sql
	database=samples
	user=sampleuser
	password=pass
else
	echo "Error. Say '$0 sql-file database user password' or run with defaults '$0'."
	exit
fi

echo "Start creation and initialization of database '${database}' ..."
sudo -u postgres createdb ${database}
sudo -u postgres psql -d ${database} -c "CREATE EXTENSION hstore;"
sudo -u postgres psql -d ${database} -c "CREATE EXTENSION postgis;"
echo "Done."

echo "Start creation of user and initialization of credentials ..."
sudo -u postgres psql -c "CREATE USER ${user} PASSWORD '${password}';"
sudo -u postgres psql -c "GRANT ALL ON DATABASE ${database} TO ${user};"
passphrase="localhost:5432:${database}:${user}:${password}"
if [ ! -e ~/.pgpass ] || [ `less ~/.pgpass | grep -c "$passphrase"` -eq 0 ]
then
	echo "$passphrase" >> ~/.pgpass
	chmod 0600 ~/.pgpass
fi
echo "Done."

echo "Start population of sample data ..."
psql -h localhost -d ${database} -U ${user} -f ${input}
echo "Done."
