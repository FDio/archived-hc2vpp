#! /bin/bash

if [[ $UID != 0 ]]; then
    echo "Please run this script with sudo:"
    echo "sudo $0 $*"
    exit 1
fi

echo "Stopping docker containers:"
sudo docker stop vppA
sudo docker stop vppB
sudo docker stop vppC
sudo docker stop vppD
sudo docker stop vppE
echo "Deleting docker containers:"
sudo docker rm vppA
sudo docker rm vppB
sudo docker rm vppC
sudo docker rm vppD
sudo docker rm vppE

echo "Cleaning namespaces."
sudo ip -all netns delete

echo "Cleanup finished."
