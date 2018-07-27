#!/bin/bash

# Replaces version string in all files from the Git index
# Usage:
# ./bump_version.sh 1.2.3-SNAPSHOT 1.2.3-RC1

if [ "$#" -ne 2 ]; then
    echo "Usage: ./bump_version.sh OLD_VERSION NEW_VERSION"
    exit 1
fi

OLD_VERSION=$1
NEW_VERSION=$2
BUMP_SCRIPT_FILENAME=$(basename "$0")
GIT_ROOT=$(git rev-parse --show-toplevel)

cd $GIT_ROOT

for i in $(git ls-files); do
  sed -i "s/${OLD_VERSION}/${NEW_VERSION}/g" $i
done

cd -
