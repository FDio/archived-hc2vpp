#!/bin/bash

# Installs jvpp jars from vpp build dir to local maven repository.
# Use before building hc2vpp to make sure it matches your locally-built vpp.
# Not needed when using honeycomb package from nexus.fd.io
# (compatible vpp version is given as package dependency).

DIR_NAME=$(dirname $0)
source ${DIR_NAME}/common.sh

# Set VPP_DIR if not defined
DEFAULT_VPP_DIR="$HOME/vpp"
VPP_DIR=${VPP_DIR:-"$DEFAULT_VPP_DIR"}
echo "Installing jvpp jars from VPP_DIR=$VPP_DIR"

JARS="$(find "$VPP_DIR/build-root/install-vpp-native/vpp/share/java/" -type f -iname 'jvpp-*.jar')"
echo "Found:"
echo "$JARS"

JVPP_VERSION=`$DIR_NAME/../jvpp-version`
echo "Target jvpp version: $JVPP_VERSION"

for i in ${JARS}
do
    install_jvpp_jar "$i" "JVPP_VERSION"
done