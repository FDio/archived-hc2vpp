FROM ubuntu:16.04

WORKDIR /hc2vpp

ADD . /hc2vpp

RUN apt-get update && apt-get install -y \
    # Utils
    iproute2 iputils-ping net-tools vim-tiny jshon telnet \
    # VPP package dependencies
    libnuma1 libssl1.0.0 libmbedcrypto0 libmbedtls10 libmbedx509-0 \
    # Honeycomb package dependencies
    openjdk-8-jre-headless \

# Install packages
&& dpkg -i packages/vpp-lib_*.deb \
&& dpkg -i packages/vpp_*.deb \
&& dpkg -i packages/vpp-plugins_*.deb \
&& dpkg -i packages/honeycomb_*.deb \

# Disable DPDK for PCI devices
&& echo "dpdk {\n  no-pci\n}" >> /etc/vpp/startup.conf \

# Reduce image size
&& rm -rf /var/lib/apt/lists/* `# clear apt cache` \
&& rm -rf packages
