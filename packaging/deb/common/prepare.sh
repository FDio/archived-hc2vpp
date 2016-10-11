#!/bin/bash
set -xe
# $1 - source dir root
# $2 - $1/debian - just configurable for reuse
# $3 - service definition file
# $4 - service definition target during install
SOURCE_DIR=$1
VERSION=$(${SOURCE_DIR}/../../rpm/version)
RELEASE=$(${SOURCE_DIR}/../../rpm/release)
BUILD_DIR=${SOURCE_DIR}/honeycomb-${VERSION}

# Copy and unpack the archive with vpp-integration distribution
ARCHIVE_DIR=${SOURCE_DIR}/../../../vpp-integration/minimal-distribution/target/
ARCHIVE=${ARCHIVE_DIR}/vpp-integration-distribution-1.16.12-SNAPSHOT-hc.tar.gz
cp ${ARCHIVE} ${SOURCE_DIR}
ARCHIVE=${SOURCE_DIR}/vpp-integration-distribution-${VERSION}-SNAPSHOT-hc.tar.gz
tar -xf ${ARCHIVE} -C ${SOURCE_DIR}/

# Create packaging root
rm -rf ${BUILD_DIR}
mkdir ${BUILD_DIR}

# Copy contents of tar.gz
mv ${SOURCE_DIR}/vpp-integration-distribution-${VERSION}-SNAPSHOT/ ${BUILD_DIR}/
cp -r $2 ${BUILD_DIR}

# OS service definition
cp ${SOURCE_DIR}/$3 ${BUILD_DIR}

# Changelog file
cat <<EOT >> ${BUILD_DIR}/debian/changelog
honeycomb (${VERSION}-${RELEASE}) unstable; urgency=low

  * 16.12 release

 -- mmarsale <mmarsale@cisco.com>  Mon, 22 Aug 2016 09:41:37 +0200
EOT

# Install instructions
cat <<EOT >> ${BUILD_DIR}/debian/install
vpp-integration-distribution-${VERSION}-SNAPSHOT/* /opt/honeycomb/
$3 $4
EOT

echo ${BUILD_DIR}