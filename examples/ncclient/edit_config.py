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


def _edit_config(config_filename, host='localhost', port=2831, username='admin', password='admin',
                 validate=False, commit=False):
    with manager.connect(host=host, port=port, username=username, password=password, hostkey_verify=False) as m:
        logger.info("Connected to HC")
        with open(config_filename, 'r') as f:
            ret = m.edit_config(config=f.read())
            logger.info("<edit-config> successful:\n%s" % ret)
            validate = m.validate()
            logger.info("<validate> successful:\n%s" % validate)
            commit = m.commit()
            logger.info("<commit> successful:\n%s" % commit)

if __name__ == '__main__':
    argparser = argparse.ArgumentParser(description="Configures VPP using <edit-config> RPC")
    argparser.add_argument('config_filename', help="name of XML file with <config> element")
    argparser.add_argument('-v', '--validate', help="sends <validate> RPC is <edit-config> was successful",
                           action="store_true")
    argparser.add_argument('-c', '--commit', help="commits candidate configuration",
                           action="store_true")
    argparser.add_argument('--verbose', help="increase output verbosity", action="store_true")
    args = argparser.parse_args()

    logger = logging.getLogger("hc2vpp.examples.edit_config")
    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    _edit_config(args.config_filename, validate=args.validate, commit=args.commit)
