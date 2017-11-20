/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.hc2vpp.acl.util.ace.extractor;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;

abstract class AceDataExtractorTestCase {

    protected static final byte[] DEFAULT_MAC_ADDRESS_BYTES = new byte[]{0, 0, 0, 0, 0, 0};
    protected static final MacAddress DEFAULT_MAC_ADDRESS = new MacAddress("00:00:00:00:00:00");
    protected static final MacAddress DEFAULT_MAC_MASK_ADDRESS = new MacAddress("00:00:00:00:00:00");
    protected static final MacAddress MAC_ADDRESS = new MacAddress("00:11:11:00:11:11");
    protected static final MacAddress MAC_ADDRESS_MASK = new MacAddress("00:11:00:11:11:11");
    protected static final byte[] MAC_ADDRESS_BYTES = {0, 17, 17, 0, 17, 17};
    protected static final byte[] MAC_ADDRESS_MASK_BYTES = {0, 17, 0, 17, 17, 17};

    protected static final byte[] DEFAULT_IPV4_ADDRESS_BYTES = new byte[4];
    protected static final Ipv4Prefix IPV4_PREFIX = new Ipv4Prefix("192.168.2.1/32");
    protected static final Ipv4Prefix IPV4_2_PREFIX = new Ipv4Prefix("192.168.2.2/24");
    protected static final byte[] IPV4_PREFIX_BYTES = {-64, -88, 2, 1};
    protected static final byte[] IPV4_2_PREFIX_BYTES = {-64, -88, 2, 2};
    protected static final byte IPV4_PREFIX_VALUE = (byte) 32;
    protected static final byte IPV4_2_PREFIX_VALUE = (byte) 24;
    protected static final int DEFAULT_IPV4_PREFIX_VALUE = 0;

    protected static final byte[] DEFAULT_IPV6_ADDRESS_BYTES = new byte[16];
    protected static final Ipv6Prefix IPV6_PREFIX = new Ipv6Prefix("2001:db8:a0b:12f0::1/64");
    protected static final Ipv6Prefix IPV6_2_PREFIX = new Ipv6Prefix("2001:db8:a0b:12f0::2/48");
    protected static final byte[] IPV6_PREFIX_BYTES = {32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 0, 1};
    protected static final byte[] IPV6_2_PREFIX_BYTES = {32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 0, 2};
    protected static final byte IPV6_PREFIX_VALUE = (byte) 64;
    protected static final byte IPV6_2_PREFIX_VALUE = (byte) 48;
    protected static final int DEFAULT_IPV6_PREFIX_VALUE = 0;
}
