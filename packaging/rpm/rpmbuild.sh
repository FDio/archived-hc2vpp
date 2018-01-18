#!/bin/bash
set -xe
DIR=$(dirname $0)
DIR=$(readlink -f $DIR)
HC2VPP_VERSION=$(${DIR}/hc2vpp_version)
ZIPDIR=${DIR}/../../vpp-integration/minimal-distribution/target/
ZIPFILE=${ZIPDIR}/vpp-integration-distribution-${HC2VPP_VERSION}-hc.zip
mkdir -p ${DIR}/SOURCES/
cp $ZIPFILE ${DIR}/SOURCES/
cp ${DIR}/honeycomb.spec ${DIR}/SOURCES/
cd ${DIR}
rpmbuild -bb --define "_topdir ${DIR}"  ${DIR}/honeycomb.spec
cd -

