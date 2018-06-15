#! /bin/bash

vpp_node=$1
packet_count=$2

if [ -z "$1" ]; then
    echo "VPP node argument missing!"
    echo "Usage:"
    echo -e "\n'$0 <vpp_node>' to run this command!\n"
    exit 1
fi

watch -d -n 1 sudo docker exec ${vpp_node} vppctl sh int
