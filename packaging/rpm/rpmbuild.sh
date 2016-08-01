#!/bin/bash
set -xe
DIR=$(dirname $0)
DIR=$(readlink -f $DIR)
ZIPDIR=${DIR}/../../vpp-integration/karaf/target/
ZIPFILE=${ZIPDIR}/vpp-integration-karaf-1.0.0-SNAPSHOT.zip
mkdir -p ${DIR}/SOURCES/
cp $ZIPFILE ${DIR}/SOURCES/
cp ${DIR}/honeycomb.spec ${DIR}/SOURCES/
cd ${DIR}
rpmbuild -bb --define "_topdir ${DIR}"  ${DIR}/honeycomb.spec
cd -
