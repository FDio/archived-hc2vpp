create host-interface name veth42
set int state host-veth42 up
set int ip address host-veth42 10.24.1.4/24

create host-interface name veth431
set int state host-veth431 up
set int ip address host-veth431 10.34.1.4/24

create host-interface name veth432
set int state host-veth432 up
set int ip address host-veth432 10.34.2.4/24
