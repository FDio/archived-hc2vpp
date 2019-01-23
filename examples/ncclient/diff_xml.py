#!/usr/bin/env python
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

"""
Usage: {prog} [OPTION] FILE1 FILE2 xPath

Compare two XML files, while sorting elements.

xPath optional parameter
example:
    ./{{urn:ietf:params:xml:ns:yang:ietf-interfaces}}interfaces/
    {{urn:ietf:params:xml:ns:yang:ietf-interfaces}}interface[
    {{urn:ietf:params:xml:ns:yang:ietf-interfaces}}name='loop0']
"""
import os
import re
import subprocess
import sys
from tempfile import NamedTemporaryFile
from xml.etree import ElementTree
from xml.etree.ElementTree import Element

TAG_PATTERN = re.compile(r"""({(?P<namespace>[a-zA-Z0-9.:@\-/]+)}(?P<tag>[0-9a-zA-Z\-]+))|(?P<name>[0-9a-zA-Z\-]+)""",
                         re.VERBOSE)
PREFIX_PATTERN = re.compile(r"""(((urn)|(.+://))[a-zA-Z0-9.:@\-/]+[:/](?P<prefix>[0-9a-zA-Z\-]+))""", re.VERBOSE)

if sys.version_info < (3, 0):
    # Python 2
    import codecs

    def unicode_writer(fp):
        return codecs.getwriter('utf-8')(fp)
else:
    # Python 3
    def unicode_writer(fp):
        return fp


def print_usage(program):
    print(__doc__.format(prog=program).strip())


def parse_namespace(namespace):
    """
    Extracts module name fom namespace.
    example:
        urn:ietf:params:xml:ns:yang:ietf-nat -> ietf-nat
        http://fd.io/hc2vpp/yang/vpp-fib-table-management -> vpp-fib-table-management
        https://fd.io/hc2vpp/yang/vpp-fib-table-management -> vpp-fib-table-management
    :param namespace: namespace in urn or http format
    :return: module name from namespace
    """
    matcher = PREFIX_PATTERN.match(namespace)
    if matcher.group("prefix"):
        return matcher.group("prefix").lower()


def sort(xml_element):
    """
    Sorts elements in xml in alphabetical order.
    :param xml_element: root element of xml
    """
    if not isinstance(xml_element, Element):
        exit(-1)
    xml_element[:] = sorted(xml_element, key=lambda child: child.tag)

    mo = TAG_PATTERN.match(xml_element.tag)
    if mo.group("namespace") and mo.group("tag") and not mo.group("namespace").__contains__(
            "urn:ietf:params:xml:ns:netconf:base:1.0"):
        ElementTree.register_namespace(parse_namespace(mo.group("namespace")), mo.group("namespace"))

    for element in xml_element:
        sort(element)


def diff_xml(xml1, xml2, xpath):
    """
    Resolves differences between two xml files.
    :param xml1: input file left side (original)
    :param xml2: input file right side (to compare with)
    :param xpath: xpath of element to read. example:
                  ./{urn:ietf:params:xml:ns:yang:ietf-interfaces}interfaces/{ \
                  urn:ietf:params:xml:ns:yang:ietf-interfaces}interface[{ \
                  urn:ietf:params:xml:ns:yang:ietf-interfaces}name='loop0']
    :return: diff of input files
    """
    ElementTree.register_namespace("nc", "urn:ietf:params:xml:ns:netconf:base:1.0")
    tmp1 = unicode_writer(NamedTemporaryFile('w'))
    normalize_xml(tmp1, ElementTree.parse(xml1), xpath)

    tmp2 = unicode_writer(NamedTemporaryFile('w'))
    normalize_xml(tmp2, ElementTree.parse(xml2), xpath)

    return subprocess.call(["diff", "-u", "-s", "-d", "--label", xml1, "--label", xml2, tmp1.name, tmp2.name])


def normalize_xml(tmp, tree, xpath):
    root = tree.getroot() if (xpath == "*") else tree.getroot().find(xPath)
    sort(root)
    xml_str = ElementTree.tostring(root, encoding="utf-8", method="xml")
    tmp.write(xml_str.decode("utf-8"))
    tmp.flush()
    #   format and normalize output using xmllint
    #   cmd: xmllint --exc-c14n [file_name] -o [file_name]
    subprocess.call(["xmllint", "--format", tmp.name, "-o", tmp.name])


if __name__ == '__main__':
    args = sys.argv
    prog = os.path.basename(args.pop(0))

    if '-h' in args or '--help' in args:
        print_usage(prog)
        exit(0)

    if len(args) < 2:
        print_usage(prog)
        exit(1)
    args.reverse()
    file1 = args.pop(-1)
    file2 = args.pop(-1)
    if len(args) > 2:
        xPath = args.pop(-1)
    else:
        xPath = "*"

    exit(diff_xml(file1, file2, xPath))
