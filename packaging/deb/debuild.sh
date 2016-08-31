#!/bin/bash
set -xe
DIR=$(dirname $0)
DIR=$(readlink -f $DIR)
VERSION=$(${DIR}/../rpm/version)
RELEASE=$(${DIR}/../rpm/release)
PACKAGING_ROOT=${DIR}/honeycomb-${VERSION}

# Copy and unpack the archive with vpp-integration distribution
ARCHIVE_DIR=${DIR}/../../vpp-integration/minimal-distribution/target/
ARCHIVE=${ARCHIVE_DIR}/vpp-integration-distribution-1.16.12-SNAPSHOT-hc.tar.gz
cp ${ARCHIVE} ${DIR}
ARCHIVE=${DIR}/vpp-integration-distribution-${VERSION}-SNAPSHOT-hc.tar.gz
tar -xf ${ARCHIVE} -C ${DIR}/

# Create packaging root
rm -rf ${PACKAGING_ROOT}
mkdir ${PACKAGING_ROOT}

# Copy contents of tar.gz
mv ${DIR}/vpp-integration-distribution-${VERSION}-SNAPSHOT/ ${PACKAGING_ROOT}/
cp -r ${DIR}/debian/ ${PACKAGING_ROOT}

# Upstart configuration
cp ${DIR}/honeycomb.conf ${PACKAGING_ROOT}

# Changelog file
cat <<EOT >> ${PACKAGING_ROOT}/debian/changelog
honeycomb (${VERSION}-${RELEASE}) unstable; urgency=low

  * Initial release

 -- mmarsale <mmarsale@cisco.com>  Mon, 22 Aug 2016 09:41:37 +0200
EOT

# Install instructions
cat <<EOT >> ${PACKAGING_ROOT}/debian/install
vpp-integration-distribution-${VERSION}-SNAPSHOT/* /opt/honeycomb/
honeycomb.conf /etc/init/
EOT

# Build deb binary only package
cd ${PACKAGING_ROOT}
dpkg-buildpackage -b
cd -