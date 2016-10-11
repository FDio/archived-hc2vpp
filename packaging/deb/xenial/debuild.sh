#!/bin/bash
set -xe
DIR=$(dirname $0)
DIR=$(readlink -f ${DIR})

BUILD_FOLDER=$(${DIR}/../common/prepare.sh ${DIR} ${DIR}/../trusty/debian honeycomb.service /lib/systemd/system)
cp -r ${DIR}/debian/* ${BUILD_FOLDER}/debian/
${DIR}/../common/debuild.sh ${BUILD_FOLDER}