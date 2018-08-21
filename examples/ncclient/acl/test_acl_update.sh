#!/bin/bash
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

DIR_NAME=$(dirname $0)

${DIR_NAME}/../test_copy_config.sh ${DIR_NAME}/config_acl.xml ${DIR_NAME}/expected_config_acl.xml

${DIR_NAME}/../test_copy_config.sh ${DIR_NAME}/config_acl_update.xml ${DIR_NAME}/expected_config_acl_update.xml
