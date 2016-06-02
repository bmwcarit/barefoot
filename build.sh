docker build --rm=true -t barefoot ./docker
docker run -t -i -p 127.0.0.1:5432:5432 --name="barefoot_test" -v ${PWD}/bfmap/:/mnt/bfmap -v ${PWD}/docker/osm/:/mnt/osm barefoot

/mnt/osm/import.sh /mnt/osm/california.osm.pbf ca test test /mnt/bfmap/road-types.json slim
