#!/bin/bash

# Installs jvpp jar to local maven repository.
#
# $1 - jvpp jar file path
# $1 - target artifact version
#
function install_jvpp_jar {
  jarfile=$1
  version=$2

  # Filename (includes version suffix), e.g. jvpp-core-18.07
  basefile=$(basename -s .jar "$jarfile")

  # Remove version suffix
  artifactId=$(echo "$basefile" | rev | cut -d '-' -f 2- | rev)

  mvn install:install-file \
    -Dfile=$jarfile \
    -DgroupId=io.fd.vpp \
    -DartifactId=$artifactId \
    -Dversion=$version \
    -Dpackaging=jar
}
