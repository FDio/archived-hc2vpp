#!/bin/bash
#
# Copyright (c) 2019 Cisco and/or its affiliates.
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
#
# $1 candidate config element to diff against actual config
# $2 xPath to verify config against

DIR_NAME=$(dirname $0)

${DIR_NAME}/get_config.py --reply_filename _actual_config.xml --simple

echo "Differences in running and candidate config:"

${DIR_NAME}/diff_xml.py $1 _actual_config.xml $2
ret_code=$?

if [[ ${ret_code} == 0 ]]; then
    exit 0
fi
rm _actual_config.xml
echo "finished."
exit 0
