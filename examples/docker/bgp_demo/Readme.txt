BGP demo
---------------------------------------------------

Provides examples of BGP config using Honeycomb
two VPP nodes (docker containers).

Configuration was tested using VPP 18.01 and hc2vpp 1.18.01.

1) Create & start containers (requires hc2vpp image)
sudo ./build_topology.sh

3) Run vpp and honeycomb with preconfigured veth interfaces

Either manually connect
docker exec -it vpp1 bash
docker exec -it vpp2 bash

and then run vpp and honeycomb on vpp1 and vpp2 respectively:
/hc2vpp/mpls_demo/init/vpp.sh 1
/hc2vpp/mpls_demo/init/vpp.sh 2

or run everything via script (uses xfce4-terminal):
./run_terminals.sh

After vpp and honeycomb stars,
you should be able to ping vpp2 from vpp1.

Either using vppctl:
vppctl ping 10.12.1.2

or directly (we are using tuntap):
ping 10.12.1.2

4) Use postman_collection.json for BGP configuration examples

First configure loop0 interface on vpp2.

Then configure eBGP peers on vpp1 and vpp2
and program route to loop0 using application peer from vpp2.

Now loop0 is reachable from vpp1:
vppctl ping 10.100.1.1
