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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEthBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.openvpp.jvpp.core.dto.ClassifyAddDelSession;
import org.openvpp.jvpp.core.dto.ClassifyAddDelTable;
import org.openvpp.jvpp.core.dto.InputAclSetInterface;
import org.openvpp.jvpp.core.future.FutureJVppCore;

public class AceEthWriterTest {

    @Mock
    private FutureJVppCore jvpp;
    private AceEthWriter writer;
    private PacketHandling action;
    private AceEth aceEth;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        writer = new AceEthWriter(jvpp);
        action = new DenyBuilder().setDeny(true).build();
        aceEth = new AceEthBuilder()
            .setDestinationMacAddress(new MacAddress("11:22:33:44:55:66"))
            .setDestinationMacAddressMask(new MacAddress("ff:ff:ff:ff:ff:ff"))
            .setSourceMacAddress(new MacAddress("aa:bb:cc:dd:ee:ff"))
            .setSourceMacAddressMask(new MacAddress("ff:ff:ff:00:00:00"))
            .build();
    }

    @Test
    public void testGetClassifyAddDelTableRequest() throws Exception {
        final int nextTableIndex = 42;
        final ClassifyAddDelTable request = writer.createClassifyTable(action, aceEth, nextTableIndex, 0);

        assertEquals(1, request.isAdd);
        assertEquals(-1, request.tableIndex);
        assertEquals(1, request.nbuckets);
        assertEquals(-1, request.missNextIndex);
        assertEquals(nextTableIndex, request.nextTableIndex);
        assertEquals(0, request.skipNVectors);
        assertEquals(AceEthWriter.MATCH_N_VECTORS, request.matchNVectors);
        assertEquals(AceEthWriter.TABLE_MEM_SIZE, request.memorySize);

        byte[] expectedMask = new byte[] {
            // destination MAC:
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            // source MAC:
            (byte) 0xff, (byte) 0xff, (byte) 0xff, 0, 0, 0,
            0, 0, 0, 0
        };
        assertArrayEquals(expectedMask, request.mask);
    }

    @Test
    public void testGetClassifyAddDelSessionRequest() throws Exception {
        final int tableIndex = 123;
        final ClassifyAddDelSession request = writer.createClassifySession(action, aceEth, tableIndex, 0);

        assertEquals(1, request.isAdd);
        assertEquals(tableIndex, request.tableIndex);
        assertEquals(0, request.hitNextIndex);

        byte[] expectedMatch = new byte[] {
            // destination MAC:
            (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66,
            // source MAC:
            (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff,
            0, 0, 0, 0
        };
        assertArrayEquals(expectedMatch, request.match);
    }

    @Test
    public void testSetClassifyTable() throws Exception {
        final int tableIndex = 321;
        final InputAclSetInterface request = new InputAclSetInterface();
        writer.setClassifyTable(request, tableIndex);
        assertEquals(tableIndex, request.l2TableIndex);
    }
}