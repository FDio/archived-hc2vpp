#!/bin/bash

# Obtain IP address of the container
# See http://blog.oddbit.com/2014/08/11/four-ways-to-connect-a-docker/
docker inspect --format '{{ .NetworkSettings.IPAddress }}' "$@"
