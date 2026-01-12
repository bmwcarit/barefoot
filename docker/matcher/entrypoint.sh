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

# Ensure server.properties exists with defaults
if [ ! -f "config/server.properties" ]; then
    echo "Creating config/server.properties with defaults..."
    cat <<EOF > config/server.properties
server.port=1234
server.timeout.request=150000
server.timeout.response=600000
server.connections=20
matcher.sigma=10
matcher.lambda=0.0
matcher.distance.max=15000
matcher.radius.max=200
matcher.interval.min=1000
matcher.distance.min=0
matcher.threads=8
matcher.shortenturns=true
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

# Update server.properties
update_properties "SERVER__" "config/server.properties"

# Update map.properties
update_properties "MAP__" "config/map.properties"

# Execute the command
exec "$@"
