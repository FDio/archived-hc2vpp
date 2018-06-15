#! /bin/bash

vpp_node=$1
packet_count=$2

if [ -z "$1" ]; then
    echo "VPP node argument missing!"
    echo "Usage:"
    echo -e "\n'$0 <vpp_node> <packet_count>' to run this command!\n"
    exit 1
fi

if [ -z "$2" ]; then
    echo "Packet count argument missing!"
    echo "Usage:"
    echo -e "\n'$0 <vpp_node> <packet_count>' to run this command!\n"
    exit 1
fi

sudo docker exec ${vpp_node} vppctl clear trace
sudo docker exec ${vpp_node} vppctl trace add af-packet-input ${packet_count}
sleep ${packet_count}
sudo docker exec ${vpp_node} vppctl show trace
