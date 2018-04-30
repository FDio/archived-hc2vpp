#!/bin/bash

# Opens connections to hc2vpp containers

xfce4-terminal --title=vpp1 --command="docker exec -it vpp1 \
  bash -c '/hc2vpp/mpls_demo/init/vpp.sh vpp1; exec $SHELL'"

xfce4-terminal --title=vpp2 --command="docker exec -it vpp2 \
  bash -c '/hc2vpp/mpls_demo/init/vpp.sh vpp2; exec $SHELL'"

xfce4-terminal --title=vpp3 --command="docker exec -it vpp3 \
  bash -c '/hc2vpp/mpls_demo/init/vpp.sh vpp3; exec $SHELL'"

xfce4-terminal --title=vpp4 --command="docker exec -it vpp4 \
  bash -c '/hc2vpp/mpls_demo/init/vpp.sh vpp4; exec $SHELL'"
