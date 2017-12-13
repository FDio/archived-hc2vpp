create host-interface name veth12
set int state host-veth12 up
set int ip address host-veth12 10.12.1.1/24
set interface unnumbered tuntap-0 use host-veth12
