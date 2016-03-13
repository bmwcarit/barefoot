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

FROM ubuntu:14.04

MAINTAINER sebastian.mattheis@bmw-carit.de

ADD /pgsql/ /opt/pgsql/
RUN apt-get update && apt-get -y install patch postgresql-9.3-postgis-2.1 git openjdk-7-jdk python-psycopg2 python-numpy python-gdal
RUN patch /etc/postgresql/9.3/main/postgresql.conf < /opt/pgsql/postgresql.conf.patch && patch /etc/postgresql/9.3/main/pg_hba.conf < /opt/pgsql/pg_hba.conf.patch && echo "export HOME=/root" >> /root/.bashrc
RUN cd /opt/ && git clone https://github.com/openstreetmap/osmosis.git && cd osmosis && git checkout tags/0.43.1 && ./gradlew assemble && echo "export PATH=${PATH}:/opt/osmosis/package/bin" >> /root/.bashrc
CMD service postgresql start && /bin/bash --rcfile /root/.bashrc
