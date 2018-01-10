#!/bin/bash

# Installs jvpp jars from vpp-api-java package to local maven repository.
# Use before building hc2vpp to make sure it matches installed vpp.
# Not needed when using honeycomb package from nexus.fd.io
# (compatible vpp version is given as package dependency).

DIR_NAME=$(dirname $0)
source ${DIR_NAME}/common.sh

# Directory used by vpp-api-java package
JAR_DIR="/usr/share/java/"
echo "Installing vpp-api-java package jars from $JAR_DIR"

JARS=$(find "$JAR_DIR" -type f -iname 'jvpp-*.jar')
echo "Found:"
echo "$JARS"

JVPP_VERSION=`$DIR_NAME/version`
echo "Target jvpp version: $JVPP_VERSION"

for i in ${JARS}
do
    install_jvpp_jar "$i" "JVPP_VERSION"
done