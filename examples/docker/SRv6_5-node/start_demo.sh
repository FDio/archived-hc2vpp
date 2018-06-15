#! /bin/bash

source ../utils.sh

if [[ $UID != 0 ]]; then
    echo "Please run this script with sudo:"
    echo "sudo $0 $*"
    exit 1
fi

if [ -z "$1" ]; then
    echo "Demo type argument missing!"
    echo "Usage:"
    echo -e "\n'$0 <demo_type>' to run this command!\n"
    echo "<demo_type> - vpp or hc"
    exit 2
fi

demo_type=$1

if [[ "$demo_type" != "vpp" && "$demo_type" != "hc" ]]; then
    echo "Demo type argument is wrong!"
    echo "<demo_type> - vpp or hc"
    exit 3
fi

echo "Creating docker containers..."
create_container vppA hc2vpp:latest 0.75
create_container vppB hc2vpp:latest 0.75
create_container vppC hc2vpp:latest 0.75
create_container vppD hc2vpp:latest 0.75
create_container vppE hc2vpp:latest 0.75

echo "Starting docker containers..."
start_container vppA &
start_container vppB &
start_container vppC &
start_container vppD &
start_container vppE &
wait

# Connect containers using veth interfaces
echo "Creating SRv6 5 node demo topology."
create_link vppA vethAB vethBA vppB
create_link vppA vethAC vethCA vppC
create_link vppB vethBD vethDB vppD
create_link vppB vethBE vethEB vppE
create_link vppC vethCD vethDC vppD
create_link vppD vethDE vethED vppE

echo "Creating hostA and hostE namespaces."
sudo ip netns add VNET_A
sudo ip netns add VNET_E
create_link VNET_A vethA veth0 vppA
create_link VNET_E vethE veth0 vppE

sudo ip netns exec VNET_A ip addr add A::1/64 dev vethA
sudo ip netns exec VNET_A ip route add E::/64 via A::A
sudo ip netns exec VNET_E ip addr add E::1/64 dev vethE
sudo ip netns exec VNET_E ip route add A::/64 via E::E

echo "Starting VPPs in containers:"
start_vpp_in_container vppA &
start_vpp_in_container vppB &
start_vpp_in_container vppC &
start_vpp_in_container vppD &
start_vpp_in_container vppE &
wait

echo "Configuring VPP interfaces and SRv6 routing:"
echo "vppA:"
#interface setup for A::
sudo docker exec vppA vppctl create host-interface name veth0
sudo docker exec vppA vppctl set interface state host-veth0 up
sudo docker exec vppA vppctl set interface ip address host-veth0 A::A/64
sudo docker exec vppA vppctl create host-interface name vethAB hw-addr 00:00:00:00:ab:0a
sudo docker exec vppA vppctl set interface state host-vethAB up
sudo docker exec vppA vppctl set interface ip address host-vethAB AB::A/64
sudo docker exec vppA vppctl create host-interface name vethAC hw-addr 00:00:00:00:ac:0a
sudo docker exec vppA vppctl set interface state host-vethAC up
sudo docker exec vppA vppctl set interface ip address host-vethAC AC::A/64
#setup neighbors
sudo docker exec vppA vppctl set ip6 neighbor host-vethAC C::100 00:00:00:00:ac:0c
sudo docker exec vppA vppctl set ip6 neighbor host-vethAB B::100 00:00:00:00:ab:0b
sudo docker exec vppA vppctl ip route add C::100/128 via host-vethAC
sudo docker exec vppA vppctl ip route add B::100/128 via host-vethAB
#SR setup for A::
if [[ "$demo_type" = "vpp" ]]; then
    echo "VPPA - Configuring SRv6 via VPP:"
    sudo docker exec vppA vppctl sr localsid address A::201 behavior end.dx6 host-veth0 A::1
    sudo docker exec vppA vppctl set sr encaps source addr A::1
    sudo docker exec vppA vppctl sr policy add bsid A::E next C::100 next D::100 next E::201 encap
    sudo docker exec vppA vppctl sr steer l3 E::/64 via bsid A::E
fi

echo "vppB:"
#interface setup for B::
sudo docker exec vppB vppctl create host-interface name vethBA hw-addr 00:00:00:00:ab:0b
sudo docker exec vppB vppctl set interface state host-vethBA up
sudo docker exec vppB vppctl set interface ip address host-vethBA AB::B/64
sudo docker exec vppB vppctl create host-interface name vethBD hw-addr 00:00:00:00:bd:0b
sudo docker exec vppB vppctl set interface state host-vethBD up
sudo docker exec vppB vppctl set interface ip address host-vethBD BD::B/64
sudo docker exec vppB vppctl create host-interface name vethBE hw-addr 00:00:00:00:be:0b
sudo docker exec vppB vppctl set interface state host-vethBE up
sudo docker exec vppB vppctl set interface ip address host-vethBE BE::E/64
#setup neighbors
sudo docker exec vppB vppctl set ip6 neighbor host-vethBA A::201 00:00:00:00:ab:0a
sudo docker exec vppB vppctl set ip6 neighbor host-vethBD D::100 00:00:00:00:bd:0d
sudo docker exec vppB vppctl ip route add A::201/128 via host-vethBA
sudo docker exec vppB vppctl ip route add D::100/128 via host-vethBD
#SR setup for B::
if [[ "$demo_type" = "vpp" ]]; then
    echo "VPPB - Configuring SRv6 via VPP:"
    sudo docker exec vppB vppctl sr localsid address B::100 behavior end
fi

echo "vppC:"
#interface setup for C::
sudo docker exec vppC vppctl create host-interface name vethCA hw-addr 00:00:00:00:ac:0c
sudo docker exec vppC vppctl set interface state host-vethCA up
sudo docker exec vppC vppctl set interface ip address host-vethCA AC::C/64
sudo docker exec vppC vppctl create host-interface name vethCD hw-addr 00:00:00:00:cd:0c
sudo docker exec vppC vppctl set interface state host-vethCD up
sudo docker exec vppC vppctl set interface ip address host-vethCD CD::C/64
#setup neighbors
sudo docker exec vppC vppctl set ip6 neighbor host-vethCD D::100 00:00:00:00:cd:0d
sudo docker exec vppC vppctl set ip6 neighbor host-vethCA A::201 00:00:00:00:ac:0a
sudo docker exec vppC vppctl ip route add D::100/128 via host-vethCD
sudo docker exec vppC vppctl ip route add A::201/128 via host-vethCA
#SR setup for C::
if [[ "$demo_type" = "vpp" ]]; then
    echo "VPPC - Configuring SRv6 via VPP:"
    sudo docker exec vppC vppctl sr localsid address C::100 behavior end
fi

echo "vppD:"
#interface setup for D::
sudo docker exec vppD vppctl create host-interface name vethDB hw-addr 00:00:00:00:bd:0d
sudo docker exec vppD vppctl set interface state host-vethDB up
sudo docker exec vppD vppctl set interface ip address host-vethDB BD::D/64
sudo docker exec vppD vppctl create host-interface name vethDC hw-addr 00:00:00:00:cd:0d
sudo docker exec vppD vppctl set interface state host-vethDC up
sudo docker exec vppD vppctl set interface ip address host-vethDC CD::D/64
sudo docker exec vppD vppctl create host-interface name vethDE hw-addr 00:00:00:00:de:0d
sudo docker exec vppD vppctl set interface state host-vethDE up
sudo docker exec vppD vppctl set interface ip address host-vethDE DE::D/64
#setup neighbors
sudo docker exec vppD vppctl set ip6 neighbor host-vethDE E::201 00:00:00:00:de:0e
sudo docker exec vppD vppctl set ip6 neighbor host-vethDB B::100 00:00:00:00:bd:0b
sudo docker exec vppD vppctl set ip6 neighbor host-vethDC C::100 00:00:00:00:cd:0c
sudo docker exec vppD vppctl ip route add E::201/128 via host-vethDE
sudo docker exec vppD vppctl ip route add B::100/128 via host-vethDB
sudo docker exec vppD vppctl ip route add C::100/128 via host-vethDC
#SR setup for D::
if [[ "$demo_type" = "vpp" ]]; then
    echo "VPPD - Configuring SRv6 via VPP:"
    sudo docker exec vppD vppctl sr localsid address D::100 behavior end
fi

echo "vppE:"
#interface setup for E::
sudo docker exec vppE vppctl create host-interface name veth0
sudo docker exec vppE vppctl set interface state host-veth0 up
sudo docker exec vppE vppctl set interface ip address host-veth0 E::E/64
sudo docker exec vppE vppctl create host-interface name vethEB hw-addr 00:00:00:00:be:0e
sudo docker exec vppE vppctl set interface state host-vethEB up
sudo docker exec vppE vppctl set interface ip address host-vethEB BE::E/64
sudo docker exec vppE vppctl create host-interface name vethED hw-addr 00:00:00:00:de:0e
sudo docker exec vppE vppctl set interface state host-vethED up
sudo docker exec vppE vppctl set interface ip address host-vethED DE::E/64
#setup neighbors
sudo docker exec vppE vppctl set ip6 neighbor host-vethEB B::100 00:00:00:00:be:0b
sudo docker exec vppE vppctl set ip6 neighbor host-vethED D::100 00:00:00:00:de:0d
sudo docker exec vppE vppctl ip route add D::100/128 via host-vethED
sudo docker exec vppE vppctl ip route add B::100/128 via host-vethEB
#SR setup for E::
if [[ "$demo_type" = "vpp" ]]; then
    echo "VPPE - Configuring SRv6 via VPP:"
    sudo docker exec vppE vppctl sr localsid address E::201 behavior end.dx6 host-veth0 E::1
    sudo docker exec vppE vppctl set sr encaps source addr E::1
    sudo docker exec vppE vppctl sr policy add bsid E::A next D::100 next B::100 next A::201 encap
    sudo docker exec vppE vppctl sr steer l3 A::/64 via bsid E::A
fi

if [[ "$demo_type" = "hc" ]]; then
    hc_plugins=`sudo docker exec vppA dpkg -L honeycomb |grep "io/fd/hc2vpp/srv6"`;

    if [ -z "$hc_plugins" ]; then
        echo "NO SRv6 plugins detected for Honeycomb in docker containers!!!"
        echo "Ensure that you provided valid honeycomb version"
        echo "Then recreate the docker image and restart demo."
        exit 4
    fi

    echo "Configuring SRv6 via HC:"
    echo "Starting Honeycomb in containers:"
    start_hc_in_container vppA &
    start_hc_in_container vppB &
    start_hc_in_container vppC &
    start_hc_in_container vppD &
    start_hc_in_container vppE &
    wait

    echo "Configuring Honeycomb in containers:"
    hc_configurations/hc_vpp_a_rest_config.sh &
    hc_configurations/hc_vpp_b_rest_config.sh &
    hc_configurations/hc_vpp_c_rest_config.sh &
    hc_configurations/hc_vpp_d_rest_config.sh &
    hc_configurations/hc_vpp_e_rest_config.sh &
    wait
fi

echo "Demo started."

echo "Ping hostA -> hostE, desired path: (A::201)->(C::100)->(D::100)->(E::201)"
echo "ip netns exec VNET_A ping6 e::1 -c 10 -i 0.25"
sudo ip netns exec VNET_A ping6 e::1 -c 10 -i 0.25

echo "Ping hostE -> hostA, desired path: (E::201)->(D::100)->(B::100)->(A::201)"
echo "ip netns exec VNET_E ping6 a::1 -c 10 -i 0.25"
sudo ip netns exec VNET_E ping6 a::1 -c 10 -i 0.25
