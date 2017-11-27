#!/bin/bash

# Obtain IP of the container
# $1 - container name
ip=$(/hc2vpp/test/docker_ip.sh $1)
url="https://$ip:8445/restconf/operational/ietf-interfaces:interfaces-state/"
echo "GET $url"

# Show interfaces-state
curl --insecure -X GET $url \
  -H 'authorization: Basic YWRtaW46YWRtaW4=' \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json'

echo
