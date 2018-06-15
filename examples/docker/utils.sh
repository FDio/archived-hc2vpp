#!/bin/bash

# Creates named container from given image.
#
# $1 - container name
# $2 - image name
#
function create_container {
  container_name=$1
  image_name=$2
  echo "Creating $container_name from $image_name"
  docker run -dt --privileged --name=$container_name $image_name
}

# Starts container
# and adds container's network namespace
# to the linux runtime data (/var/run).
#
# $1 - container name
#
# See:
# https://platform9.com/blog/container-namespaces-deep-dive-container-networking/
#
function start_container {
  container_name=$1
  echo "Starting container $container_name"

  # Remove namespace if it was present
  ip netns del $container_name

  # Start container
  docker start $container_name

  # Make container's network namespaces accessible using ip netns
  pid=$(docker inspect -f '{{.State.Pid}}' $container_name)
  ln -s /proc/$pid/ns/net /var/run/netns/$container_name
  echo "Container $container_name started sucessfully (pid=$pid)"
}

# Links two containers using a veth pair.
#
# $1 - name of the container A
# $2 - name of the veth endpoint that belongs to A
# $3 - name of the veth endpoint that belongs to B
# $4 - name of the container B
#
function create_link {
  container1=$1
  if1=$2
  if2=$3
  container2=$4
  echo "Creating link from $container1($if1) to $container2($if2)"
  ip link add $if1 type veth peer name $if2

  # Move veth endpoints to corresponding namespaces
  ip link set $if1 netns $container1
  ip link set $if2 netns $container2

  # Bring interfaces up
  ip netns exec $container1 ip link set $if1 up
  ip netns exec $container2 ip link set $if2 up

  # Set interfaces mtu to 9000 to match default VPP mtu values
  ip netns exec ${container1} ifconfig ${if1} mtu 9000
  ip netns exec ${container2} ifconfig ${if2} mtu 9000
  echo "Link created successfully"
}
