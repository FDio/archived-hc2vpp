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

import static io.fd.honeycomb.translate.v3po.interfaces.acl.AbstractAceWriter.VLAN_TAG_LEN;
import static io.fd.honeycomb.translate.v3po.interfaces.acl.AceIpWriterTestUtils.assertArrayEqualsWithOffset;
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
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import io.fd.vpp.jvpp.core.dto.InputAclSetInterface;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;

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

    private static void verifyTableRequest(final ClassifyAddDelTable request, final int nextTableIndex,
                                           final int vlanTags) {
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
        assertArrayEqualsWithOffset(expectedMask, request.mask, vlanTags * VLAN_TAG_LEN);

    }

    private static void verifySessionRequest(final ClassifyAddDelSession request, final int tableIndex,
                                             final int vlanTags) {
        assertEquals(1, request.isAdd);
        assertEquals(tableIndex, request.tableIndex);
        assertEquals(0, request.hitNextIndex);

        byte[] expectedMatch = new byte[] {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x40, (byte) 0x2c,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 1, 2,
            4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };
        assertArrayEqualsWithOffset(expectedMatch, request.match, vlanTags * VLAN_TAG_LEN);

    }

    @Test
    public void testGetClassifyAddDelTableRequest() throws Exception {
        final int nextTableIndex = 42;
        final ClassifyAddDelTable request = writer.createClassifyTable(action, aceIp, nextTableIndex, 0);
        verifyTableRequest(request, nextTableIndex, 0);
    }

    @Test
    public void testGetClassifyAddDelTableRequest1VlanTag() throws Exception {
        final int nextTableIndex = 42;
        final int vlanTags = 1;
        final ClassifyAddDelTable request = writer.createClassifyTable(action, aceIp, nextTableIndex, vlanTags);
        verifyTableRequest(request, nextTableIndex, vlanTags);
    }

    @Test
    public void testGetClassifyAddDelTableRequest2VlanTags() throws Exception {
        final int nextTableIndex = 42;
        final int vlanTags = 2;
        final ClassifyAddDelTable request = writer.createClassifyTable(action, aceIp, nextTableIndex, vlanTags);
        verifyTableRequest(request, nextTableIndex, vlanTags);
    }

    @Test
    public void testGetClassifyAddDelSessionRequest() throws Exception {
        final int tableIndex = 123;
        final ClassifyAddDelSession request = writer.createClassifySession(action, aceIp, tableIndex, 0);
        verifySessionRequest(request, tableIndex, 0);
    }

    @Test
    public void testGetClassifyAddDelSessionRequest1VlanTag() throws Exception {
        final int tableIndex = 123;
        final int vlanTags = 1;
        final ClassifyAddDelSession request = writer.createClassifySession(action, aceIp, tableIndex, vlanTags);
        verifySessionRequest(request, tableIndex, vlanTags);
    }

    @Test
    public void testGetClassifyAddDelSessionRequest2VlanTags() throws Exception {
        final int tableIndex = 123;
        final int vlanTags = 2;
        final ClassifyAddDelSession request = writer.createClassifySession(action, aceIp, tableIndex, vlanTags);

        verifySessionRequest(request, tableIndex, vlanTags);
    }

    @Test
    public void testSetClassifyTable() throws Exception {
        final int tableIndex = 321;
        final InputAclSetInterface request = new InputAclSetInterface();
        writer.setClassifyTable(request, tableIndex);
        assertEquals(tableIndex, request.ip4TableIndex);
    }
}