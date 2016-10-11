#!/bin/bash
set -xe
BUILD_DIR=$1

# Build deb binary only package
cd ${BUILD_DIR}
dpkg-buildpackage -b
cd -