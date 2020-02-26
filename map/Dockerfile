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

FROM ubuntu:18.04

MAINTAINER sebastian.mattheis@bmw-carit.de

ADD /pgsql/ /opt/pgsql/
ADD setup_tz.sh /usr/bin/setup_tz.sh
RUN /usr/bin/setup_tz.sh
RUN apt-get -y install sudo patch postgresql-10-postgis-2.4 git openjdk-8-jdk-headless python-psycopg2 python-numpy python-gdal
RUN patch /etc/postgresql/10/main/postgresql.conf < /opt/pgsql/postgresql.conf.patch && patch /etc/postgresql/10/main/pg_hba.conf < /opt/pgsql/pg_hba.conf.patch && echo "export HOME=/root" >> /root/.bashrc
RUN cd /opt/ && git clone https://github.com/openstreetmap/osmosis.git && cd osmosis && git checkout tags/0.47 && ./gradlew assemble && echo "export PATH=${PATH}:/opt/osmosis/package/bin" >> /root/.bashrc
CMD service postgresql start && /bin/bash --rcfile /root/.bashrc
