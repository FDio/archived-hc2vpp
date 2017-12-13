#!/bin/bash
source ../utils.sh

if [[ $UID != 0 ]]; then
    echo "Please run this script with sudo:"
    echo "sudo $0 $*"
    exit 1
fi

# Cleanup
../remove_hc2vpp_containers.sh

# Create directory for storing network namespaces
mkdir -p /var/run/netns

# Build topology
#
# vpp1 – vpp2
#
create_container vpp1 hc2vpp
create_container vpp2 hc2vpp

start_container vpp1
start_container vpp2

# Connect containers using veth interfaces
create_link vpp1 veth12 veth21 vpp2
