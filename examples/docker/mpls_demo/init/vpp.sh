#!/bin/bash

# Starts and initializes vpp.
# Then starts honeycomb
#
# $1 - node name
#

/hc2vpp/vpp/start.sh
echo "Waiting for vpp to start"
sleep 5

# Configure veth interfaces
# (not yet fully supported by honeycomb)
echo "Configuring vpp"
vppctl exec /hc2vpp/mpls_demo/init/$1.cmd

echo "Starting honeycomb"
/hc2vpp/honeycomb/start.sh
