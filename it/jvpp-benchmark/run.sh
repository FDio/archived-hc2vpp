#!/bin/bash -x
#
# Copyright (c) 2018 Cisco and/or its affiliates.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at:
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

echo "Stopping VPP"
sudo service vpp stop

echo "Single JVM fork 20x2s warmup iterations, 100x2s measurment iterations, aclSize=100"
sudo java -jar ./target/jvpp-benchmark*executable.jar

echo "Single JVM fork 20x2s warmup iterations, 100x2s measurment iterations, aclSize=1000"
sudo java -jar ./target/jvpp-benchmark*executable.jar -p aclSize=1000

echo "100 JVP forks, 1 invocation each (single shot mode), no warmup, aclSize=100"
sudo java -jar ./target/jvpp-benchmark*executable.jar -bm ss -f 100 -i 1 -wi 0

echo "100 JVP forks, 1 invocation each (single shot mode), no warmup, aclSize=1000"
sudo java -jar ./target/jvpp-benchmark*executable.jar -bm ss -f 100 -i 1 -wi 0 -p aclSize=1000

echo "100 JVP forks, 1 iteration each, no warmup, aclSize=100"
sudo java -jar ./target/jvpp-benchmark*executable.jar -f 100 -i 1 -wi 0

echo "100 JVP forks, 1 iteration each, no warmup, aclSize=1000"
sudo java -jar ./target/jvpp-benchmark*executable.jar -f 100 -i 1 -wi 0 -p aclSize=1000