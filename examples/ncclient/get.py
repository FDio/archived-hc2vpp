#!/usr/bin/env python2
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

import argparse
import logging
from ncclient import manager


def _get(reply_filename=None, host='localhost', port=2831, username='admin', password='admin'):
    with manager.connect(host=host, port=port, username=username, password=password, hostkey_verify=False) as m:
        logger.info("Connected to HC")
        state = m.get()
        logger.info("<get> successful:\n%s" % state)
        if reply_filename:
            with open(reply_filename, 'w') as f:
                f.write(state.data_xml)
        else:
            print state.data_xml


if __name__ == '__main__':
    argparser = argparse.ArgumentParser(description="Obtains VPP state data using <get> RPC")
    argparser.add_argument('--reply_filename', help="name of XML file to store received state data")
    argparser.add_argument('--verbose', help="increase output verbosity", action="store_true")
    args = argparser.parse_args()

    logger = logging.getLogger("hc2vpp.examples.get")
    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    _get(args.reply_filename)
