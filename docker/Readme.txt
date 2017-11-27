HC2VPP Docker image building&testing instructions
---------------------------------------------------

1) Copy vpp and hc2vpp packages to docker/packages dir

2) Build hc2vpp image
./create_image.sh

3) Create & start container
./start_hc2vpp_container.sh vpp1

4) Start vpp & honeycomb
docker exec -it vpp1 bash
./vpp/start.sh
./honeycomb/start.sh

5) Test
./test/show_interfaces_state.sh vpp1
