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

package io.fd.honeycomb.translate.v3po.interfaces.acl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.DenyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.openvpp.jvpp.core.dto.ClassifyAddDelSession;
import org.openvpp.jvpp.core.dto.ClassifyAddDelTable;
import org.openvpp.jvpp.core.dto.InputAclSetInterface;
import org.openvpp.jvpp.core.future.FutureJVppCore;

public class AceIp4WriterTest {
    @Mock
    private FutureJVppCore jvpp;
    private AceIp4Writer writer;
    private PacketHandling action;
    private AceIp aceIp;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        writer = new AceIp4Writer(jvpp);
        action = new DenyBuilder().setDeny(true).build();
        aceIp = new AceIpBuilder()
            .setProtocol((short) 4)
            .setDscp(new Dscp((short) 11))
            .setAceIpVersion(new AceIpv4Builder()
                .setSourceIpv4Network(new Ipv4Prefix("1.2.3.4/32"))
                .setDestinationIpv4Network(new Ipv4Prefix("1.2.4.5/24"))
                .build())
            .build();
    }

    @Test
    public void testGetClassifyAddDelTableRequest() throws Exception {
        final int nextTableIndex = 42;
        final ClassifyAddDelTable request = writer.getClassifyAddDelTableRequest(action, aceIp, nextTableIndex);

        assertEquals(1, request.isAdd);
        assertEquals(-1, request.tableIndex);
        assertEquals(1, request.nbuckets);
        assertEquals(-1, request.missNextIndex);
        assertEquals(nextTableIndex, request.nextTableIndex);
        assertEquals(0, request.skipNVectors);
        assertEquals(AceIp4Writer.MATCH_N_VECTORS, request.matchNVectors);
        assertEquals(AceIp4Writer.TABLE_MEM_SIZE, request.memorySize);

        byte[] expectedMask = new byte[] {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xf0, (byte) 0xfc,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1,
            -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };
        assertArrayEquals(expectedMask, request.mask);
    }

    @Test
    public void testGetClassifyAddDelSessionRequest() throws Exception {
        final int tableIndex = 123;
        final ClassifyAddDelSession request = writer.getClassifyAddDelSessionRequest(action, aceIp, tableIndex);

        assertEquals(1, request.isAdd);
        assertEquals(tableIndex, request.tableIndex);
        assertEquals(0, request.hitNextIndex);

        byte[] expectedMatch = new byte[] {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x40, (byte) 0x2c,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 1, 2,
            4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };
        assertArrayEquals(expectedMatch, request.match);
    }

    @Test
    public void testSetClassifyTable() throws Exception {
        final int tableIndex = 321;
        final InputAclSetInterface request = new InputAclSetInterface();
        writer.setClassifyTable(request, tableIndex);
        assertEquals(tableIndex, request.ip4TableIndex);
    }
}