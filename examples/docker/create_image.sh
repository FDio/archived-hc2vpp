#!/bin/bash

# Build docker image with vpp and hc2vpp installed
docker build -t hc2vpp -f Dockerfile .

# make backup copy of image (uncomment next line if desired)
# docker save -o hc2vpp-latest.tar hc2vpp:latest
