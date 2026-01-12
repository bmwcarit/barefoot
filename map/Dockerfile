FROM ubuntu:14.04

LABEL maintainer="sebastian.mattheis@bmw-carit.de"

ADD /pgsql/ /opt/pgsql/

RUN apt-get update \
    && apt-get install -y software-properties-common \
    && add-apt-repository ppa:openjdk-r/ppa \
    && apt-get update \
    && apt-get -y install \
    patch \
    postgresql-9.3-postgis-2.1 \
    git \
    openjdk-8-jdk \
    python-psycopg2 \
    python-numpy \
    python-gdal

# patches do postgres
RUN patch /etc/postgresql/9.3/main/postgresql.conf < /opt/pgsql/postgresql.conf.patch \
    && patch /etc/postgresql/9.3/main/pg_hba.conf < /opt/pgsql/pg_hba.conf.patch \
    && echo "export HOME=/root" >> /root/.bashrc

ENV HOME="/root"

# build osmosis
RUN cd /opt/ \
    && git clone https://github.com/openstreetmap/osmosis.git \
    && cd osmosis \
    && git fetch --tags \
    && git checkout tags/0.47.3 \
    && ./gradlew assemble \
    && echo "export PATH=\$PATH:/opt/osmosis/package/bin" >> /root/.bashrc

ENV PATH="/opt/osmosis/package/bin:/usr/lib/postgresql/9.3/bin:${PATH}"

ENV POSTGRES_USER="osm" \
    POSTGRES_PASSWORD="osm" \
    POSTGRES_DB="osm" \
    MAP_MODE="slim" \
    MAP_OSM_PBF_PATH="NULL"

COPY osm/import.sh /mnt/map/osm/
COPY osm/pgsnapshot_schema_0.6.sql /mnt/map/osm/
COPY tools /mnt/map/tools/
COPY pgsql /mnt/map/pgsql/
COPY samples /mnt/map/samples/

ADD ./entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
