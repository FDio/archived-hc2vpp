/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.honeycomb.translate.vpp.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;

public class AddressTranslatorTest implements AddressTranslator {

    @Test
    public void testRevertAddress() {
        assertEquals("1.2.168.192",
                reverseAddress(new IpAddress(new Ipv4Address("192.168.2.1"))).getIpv4Address().getValue());
        assertEquals("3473:7003:2e8a::a385:b80d:120",
                reverseAddress(new IpAddress(new Ipv6Address("2001:db8:85a3:0:0:8a2e:370:7334"))).getIpv6Address()
                        .getValue());
    }
}