#!/bin/bash

# Starts and initializes vpp.
# Then starts honeycomb
#

NODE_ID=$1
NODE_NAME=vpp$1

echo "Enable tuntap interface in startup.conf"
echo -e "tuntap {\n  enable\n}\n" >> /etc/vpp/startup.conf

/hc2vpp/vpp/start.sh & VPP_PID=$!
echo "Waiting 5s for vpp to start"
sleep 5
VPP_VERSION=$(vppctl show version)
if [ "${VPP_VERSION}" != "" ]
then
  echo "VPP started successfully. Version:"
  echo "${VPP_VERSION}"
else
  echo "VPP failed to start. Stopping initialization script."
  exit 1
fi

# Configure veth interfaces using VPP CLI
# (not fully supported by hc2vpp 18.01)
echo "Configuring vpp"
vppctl exec /hc2vpp/bgp_demo/init/$NODE_NAME.cmd

# Update address of interface BGP is listening on
IP="10.12.1.${NODE_ID}"
jshon -s $IP -i "bgp-binding-address" -I -F /opt/honeycomb/config/bgp.json

# Set AS number
AS_NUMBER=$((65000+NODE_ID))
jshon -n $AS_NUMBER -i "bgp-as-number" -I -F /opt/honeycomb/config/bgp.json

# Update module configuration
# Enables BGP and disables some of the modules not used in the example
cp /hc2vpp/bgp_demo/init/*-module-config /opt/honeycomb/modules

echo "Starting honeycomb"
/hc2vpp/honeycomb/start.sh
