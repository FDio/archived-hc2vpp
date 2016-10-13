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

package io.fd.honeycomb.translate.v3po.interfaces.acl.ingress;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.DenyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.DestinationPortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.SourcePortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpAndEth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpAndEthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.and.eth.ace.ip.version.AceIpv4Builder;

public class AceIpAndEthWriterTest {

    private AceIpAndEthWriter writer;
    private PacketHandling action;
    private AceIpAndEth ace;

    @Before
    public void setUp() {
        initMocks(this);
        writer = new AceIpAndEthWriter();
        action = new DenyBuilder().setDeny(true).build();
        ace = new AceIpAndEthBuilder()
            .setDestinationMacAddress(new MacAddress("11:22:33:44:55:66"))
            .setSourceMacAddress(new MacAddress("aa:bb:cc:dd:ee:ff"))
            .setAceIpVersion(new AceIpv4Builder()
                .setSourceIpv4Network(new Ipv4Prefix("1.2.3.4/32")).build())
            .setSourcePortRange(new SourcePortRangeBuilder().setLowerPort(new PortNumber(0x1111)).build())
            .setDestinationPortRange(new DestinationPortRangeBuilder().setLowerPort(new PortNumber(0x2222)).build())
            .build();
    }

    @Test
    public void testCreateTable() {
        final int nextTableIndex = 42;
        final ClassifyAddDelTable request = writer.createTable(ace, InterfaceMode.L2, nextTableIndex, 0);

        assertEquals(1, request.isAdd);
        assertEquals(-1, request.tableIndex);
        assertEquals(1, request.nbuckets);
        assertEquals(nextTableIndex, request.nextTableIndex);
        assertEquals(0, request.skipNVectors);
        assertEquals(3, request.matchNVectors);
        assertEquals(AceEthWriter.TABLE_MEM_SIZE, request.memorySize);

        byte[] expectedMask = new byte[] {
            // destination MAC:
            -1, -1, -1, -1, -1, -1,
            // source MAC:
            -1, -1, -1, -1, -1, -1,
            // ether type:
            -1, -1,
            // IP header
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // source address:
            -1, -1, -1, -1,
            // destination address:
            0, 0, 0, 0,
            // source and destination port:
            -1, -1, -1, -1,
            // padding:
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };
        assertArrayEquals(expectedMask, request.mask);
    }

    @Test
    public void testCreateClassifySession() {
        final int tableIndex = 123;
        final ClassifyAddDelSession request = writer.createSession(action, ace, InterfaceMode.L2, tableIndex, 0).get(0);

        assertEquals(1, request.isAdd);
        assertEquals(tableIndex, request.tableIndex);
        assertEquals(0, request.hitNextIndex);

        byte[] expectedMatch = new byte[] {
            // destination MAC:
            (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66,
            // source MAC:
            (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff,
            // ether type (IP4):
            0x08, 0,
            // IP header
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // source address:
            1, 2, 3, 4,
            // destination address:
            0, 0, 0, 0,
            // source and destination port:
            0x11, 0x11, 0x22, 0x22,
            // padding:
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };
        assertArrayEquals(expectedMatch, request.match);
    }
}