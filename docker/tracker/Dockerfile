# Use Java 8 as the base image since Barefoot requires Java version 7 or higher
FROM maven:3.8.6-jdk-8

# Copy the Barefoot source code into the container
COPY . /barefoot

# Set the working directory to the Barefoot directory
WORKDIR /barefoot

# Package Barefoot JAR (including dependencies and executable main class)
RUN mvn package -DskipTests

# Expose the port that the tracker server will run on (1234 for API, 1235 for tracker)
EXPOSE 1234 1235

COPY docker/tracker/entrypoint.sh /barefoot/docker-entrypoint.sh
RUN chmod +x /barefoot/docker-entrypoint.sh

ENTRYPOINT ["/barefoot/docker-entrypoint.sh"]

# Command to start the tracker server
CMD java -jar target/barefoot-0.1.5-tracker-jar-with-dependencies.jar config/tracker.properties config/map.properties
