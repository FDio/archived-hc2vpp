#! /bin/bash

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

./clean_demo.sh
sleep 5
./start_demo.sh ${demo_type}
