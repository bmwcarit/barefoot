# Use Java 8 as the base image since Barefoot requires Java version 7 or higher
FROM maven:3.8.6-jdk-8

# Copy the Barefoot source code into the container
COPY . /barefoot

# Set the working directory to the Barefoot directory
WORKDIR /barefoot

# Package Barefoot JAR (including dependencies and executable main class)
RUN mvn package -DskipTests

# Expose the port that the matcher server will run on
EXPOSE 1234

COPY docker/matcher/entrypoint.sh /barefoot/docker-entrypoint.sh
RUN chmod +x /barefoot/docker-entrypoint.sh

ENTRYPOINT ["/barefoot/docker-entrypoint.sh"]

# Command to start the matcher server with standard configuration for map server and map matching
# Note: Replace <VERSION> with the actual version number of Barefoot
CMD java -jar target/barefoot-0.1.5-matcher-jar-with-dependencies.jar --slimjson config/server.properties config/map.properties

