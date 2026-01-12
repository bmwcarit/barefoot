#!/bin/bash
set -e

# Function to update properties file from env vars
update_properties() {
    local prefix=$1
    local file=$2
    
    # Create file if it doesn't exist (should be handled by defaults below, but safe to keep)
    if [ ! -f "$file" ]; then
        touch "$file"
    fi

    # Loop through environment variables starting with prefix
    env | grep "^$prefix" | while read -r line; do
        # Split into key and value
        key=$(echo "$line" | cut -d= -f1)
        value=$(echo "$line" | cut -d= -f2-)
        
        # Remove prefix
        prop_name=${key#$prefix}
        
        # Lowercase
        prop_name=$(echo "$prop_name" | tr '[:upper:]' '[:lower:]')
        
        # Replace _ with .
        prop_name=$(echo "$prop_name" | tr '_' '.')
        
        # Append to file (overriding previous values if loaded sequentially)
        echo "$prop_name=$value" >> "$file"
    done
}

# Ensure tracker.properties exists with defaults
if [ ! -f "config/tracker.properties" ]; then
    echo "Creating config/tracker.properties with defaults..."
    cat <<EOF > config/tracker.properties
server.port=1234
server.timeout.request=1000
server.timeout.response=10000
server.connections=100
matcher.sigma=10
matcher.lambda=0.0
matcher.distance.max=5000
matcher.radius.max=200
matcher.interval.min=1000
matcher.distance.min=0
matcher.threads=8
matcher.shortenturns=true
tracker.state.ttl=60
tracker.port=1235
EOF
fi

# Ensure map.properties exists with defaults
if [ ! -f "config/map.properties" ]; then
    echo "Creating config/map.properties with defaults..."
    cat <<EOF > config/map.properties
database.host=localhost
database.port=5432
database.name=osm
database.table=bfmap_ways
database.user=osm
database.password=osm
database.road-types=./map/tools/road-types.json
EOF
fi

# Update tracker.properties
update_properties "TRACKER__" "config/tracker.properties"

# Update map.properties
update_properties "MAP__" "config/map.properties"

# Execute the command
exec "$@"
