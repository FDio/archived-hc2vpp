#!/bin/bash

ids=$(docker ps -aq --filter ancestor=hc2vpp)

# Stop all hc2vpp based containers
echo "Stopping containers: $ids"
docker stop $ids

# Delete all hc2vpp based containers
echo "Removing containers: $ids"
docker rm $ids
