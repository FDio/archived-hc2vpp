#!/bin/bash

# Create & start container
# $1 - container name
docker run -dt --privileged --name=$1 hc2vpp
docker start $1
