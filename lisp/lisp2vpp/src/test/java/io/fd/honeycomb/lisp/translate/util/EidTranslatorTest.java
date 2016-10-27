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

package io.fd.honeycomb.lisp.translate.util;

import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV4;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV6;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.MAC;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;

public class EidTranslatorTest implements EidTranslator {

    @Test
    public void testGetEidType() {
        assertEquals(IPV4, getEidType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                        .setAddress(
                                new Ipv4Builder().setIpv4(
                                        new Ipv4Address("192.168.2.1"))
                                        .build())
                        .build()));

        assertEquals(IPV6, getEidType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                        .setAddress(
                                new Ipv6Builder().setIpv6(
                                        new Ipv6Address("2001:0db8:0a0b:12f0:0000:0000:0000:0001"))
                                        .build())
                        .build()));

        assertEquals(MAC, getEidType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                        .setAddress(
                                new MacBuilder().setMac(
                                        new MacAddress("aa:bb:cc:dd:ee:ff"))
                                        .build())
                        .build()));
    }
}
