#!/bin/bash

# Opens connections to hc2vpp containers

xfce4-terminal --title=vpp1 --command="docker exec -it vpp1 \
  bash -c '/hc2vpp/bgp_demo/init/init.sh 1; exec $SHELL'"

xfce4-terminal --title=vpp2 --command="docker exec -it vpp2 \
  bash -c '/hc2vpp/bgp_demo/init/init.sh 2; exec $SHELL'"
