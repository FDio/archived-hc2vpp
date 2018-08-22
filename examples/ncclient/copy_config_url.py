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


def _copy_config_url(source, target, host='localhost', port=2831, username='admin', password='admin'):
    with manager.connect(host=host, port=port, username=username, password=password, hostkey_verify=False) as m:
        logger.info("Connected to HC")
        ret = m.copy_config(source=source, target=target)
        logger.info("<copy-config> successful:\n%s" % ret)
        ret = m.commit()
        logger.info("<commit> successful:\n%s", ret)


if __name__ == '__main__':
    logger = logging.getLogger("hc2vpp.examples.copy_config_url")
    argparser = argparse.ArgumentParser(description="Configures VPP using <copy-config> RPC")
    argparser.add_argument('source', help="source datastore name or URI of XML file with <config> element")
    argparser.add_argument('target', help="target datastore name or URI of XML file with <config> element")
    argparser.add_argument('--verbose', help="increase output verbosity", action="store_true")
    args = argparser.parse_args()

    logger = logging.getLogger("hc2vpp.examples.copy_config")
    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    _copy_config_url(args.source, args.target)
