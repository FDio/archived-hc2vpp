create host-interface name veth21
set int state host-veth21 up
set int ip address host-veth21 10.12.1.2/24
set interface unnumbered tuntap-0 use host-veth21
