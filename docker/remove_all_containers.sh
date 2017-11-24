#!/bin/bash

ids=$(docker ps -aq)

# Stop all containers
docker stop $ids

# Delete all containers
docker rm $ids
