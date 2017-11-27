MPLS SR demo
---------------------------------------------------

Provides examples of MPLS SR config using Honeycomb
for the following topology:

                    A
                   /
 vpp1 – vpp2 – vpp4
    \          //  \
     –– vpp3 ––     B

Each vpp node represents docker container
with honeycomb running.

A and B represent egrees nodes modeled using tap interfaces.

1) Create & start containers (requires hc2vpp image)
sudo ./build_topology.sh

3) Run vpp and honeycomb with preconfigured veth interfaces

Either manually connect
docker exec -it vpp1 bash
docker exec -it vpp2 bash
...

and then run vpp and honeycomb:
/hc2vpp/mpls_demo/init/vpp.sh vpp1
/hc2vpp/mpls_demo/init/vpp.sh vpp2
...

or run everything via script (uses xfce4-terminal):
./run_terminals.sh

4) Use postman_collection.json for MPLS configuration examples
