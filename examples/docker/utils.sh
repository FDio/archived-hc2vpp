#!/bin/bash

# Creates named container from given image.
#
# $1 - container name
# $2 - image name
# $3 - cpu allocation (default = 1.0)
#
function create_container {
  container_name=$1
  image_name=$2
  if [ -z "$3" ]
  then
      cpu="1.0"
  else
      cpu=$3
  fi
  echo "Creating $container_name from $image_name, cpu allocation: $cpu"
  docker run -dt --cpus=${cpu} --privileged --name=${container_name} ${image_name} /bin/bash
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
  ip netns del ${container_name} 2> /dev/null

  # Start container
  docker start ${container_name}

  # Make container's network namespaces accessible using ip netns
  pid=$(docker inspect -f '{{.State.Pid}}' ${container_name})
  sudo mkdir -p /var/run/netns
  sudo ln -sf /proc/${pid}/ns/net /var/run/netns/${container_name}
  echo "Container $container_name started successfully (pid=$pid)"
}

# Starts vpp in container
#
# $1 - container name
#
function start_vpp_in_container {
  container=$1
  echo "Starting vpp in $container"

  # Start VPP in container
  sudo docker exec -d ${container} ./vpp/start.sh
  result="stopped"
  echo -e "Waiting for vpp in ${container} to start\n"
  while [ "$result" != "started" ]
  do
      found=`sudo docker exec ${container} vppctl sh int 2> /dev/null |grep "local0"`
      if [[ ${found} = *"local0"* ]]; then
          result="started"
      fi
      echo -ne "."
      sleep 1
  done

  echo -e "\nVPP in $container started successfully"
}

# Starts vpp in container
#
# $1 - container name
#
function start_hc_in_container {
  container=$1
  echo "Starting hc in $container"

  # Start HC in container
  sudo docker exec -d ${container} ./honeycomb/start.sh
  match="Honeycomb started successfully!"
  result="stopped"
  echo "Waiting for hc in ${container} to start"
  while [ "$result" != "started" ]
  do
      found=`sudo docker exec ${container} tail /var/log/honeycomb/honeycomb.log 2> /dev/null | grep "${match}"`

      if [[ ${found} = *"${match}"* ]]; then
          result="started"
      fi
      echo -ne "."
      sleep 1
  done
  echo -e "\nHC in $container started successfully"
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
  echo -ne "Creating link from $container1($if1) to $container2($if2)"
  ip link add ${if1} type veth peer name ${if2}

  # Move veth endpoints to corresponding namespaces
  ip link set ${if1} netns ${container1}
  ip link set ${if2} netns ${container2}

  # Bring interfaces up
  ip netns exec ${container1} ip link set ${if1} up
  ip netns exec ${container2} ip link set ${if2} up

  # Set interfaces mtu to 9000 to match default VPP mtu values
  ip netns exec ${container1} ifconfig ${if1} mtu 9000
  ip netns exec ${container2} ifconfig ${if2} mtu 9000

  echo " -> done."
}
