#!/bin/bash

rm packages/*.deb

# Copies locally-built vpp and hc2vpp packages
cp ~/vpp/build-root/*.deb ../../packaging/deb/xenial/*.deb packages/