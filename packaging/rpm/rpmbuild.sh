#!/bin/bash
set -xe
DIR=$(dirname $0)
DIR=$(readlink -f $DIR)
ZIPDIR=${DIR}/../../v3po/karaf/target/
ZIPFILE=${ZIPDIR}/v3po-karaf-1.0.0-SNAPSHOT.zip
mkdir -p ${DIR}/SOURCES/
cp $ZIPFILE ${DIR}/SOURCES/
cp ${DIR}/honeycomb.spec ${DIR}/SOURCES/
rpmbuild -bb --define "_topdir ${DIR}"  ${DIR}/honeycomb.spec
