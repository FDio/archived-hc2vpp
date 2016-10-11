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

import static io.fd.honeycomb.translate.v3po.interfaces.acl.ingress.AbstractAceWriter.VLAN_TAG_LEN;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import io.fd.vpp.jvpp.core.dto.InputAclSetInterface;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.DenyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6FlowLabel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;

public class AceIp6WriterTest {

    @Mock
    private FutureJVppCore jvpp;
    private AceIp6Writer writer;
    private PacketHandling action;
    private AceIp aceIp;

    @Before
    public void setUp() {
        initMocks(this);
        writer = new AceIp6Writer(jvpp);
        action = new DenyBuilder().setDeny(true).build();
        aceIp = new AceIpBuilder()
            .setProtocol((short) 132)
            .setDscp(new Dscp((short) 11))
            .setAceIpVersion(new AceIpv6Builder()
                .setFlowLabel(new Ipv6FlowLabel(123L))
                .setSourceIpv6Network(new Ipv6Prefix("2001:db8:85a3:8d3:1319:8a2e:370:7348/128"))
                .setDestinationIpv6Network(new Ipv6Prefix("fe80:1234:5678:abcd:ef01::/64"))
                .build())
            .build();
    }


    private static void verifyTableRequest(final ClassifyAddDelTable request, final int nextTableIndex,
                                           final int vlanTags, final boolean isL2) {
        assertEquals(1, request.isAdd);
        assertEquals(-1, request.tableIndex);
        assertEquals(1, request.nbuckets);
        assertEquals(nextTableIndex, request.nextTableIndex);
        assertEquals(0, request.skipNVectors);
        assertEquals(AceIp6Writer.MATCH_N_VECTORS, request.matchNVectors);
        assertEquals(AceIp6Writer.TABLE_MEM_SIZE, request.memorySize);

        byte[] expectedMask = new byte[] {
            // L2:
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // dscp, flow:
            (byte) 0x0f, (byte) 0xcf, (byte) 0xff, (byte) 0xff,
            // protocol:
            0, 0, (byte) 0xff, 0,
            // source address:
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            // destination address:
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            0, 0, 0, 0, 0, 0, 0, 0,
            // padding to multiple of 16B:
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };

        if (isL2) {
            expectedMask[12] = (byte) 0xff;
            expectedMask[13] = (byte) 0xff;
        }
        AceIpWriterTestUtils.assertArrayEqualsWithOffset(expectedMask, request.mask, vlanTags * VLAN_TAG_LEN);
    }

    private static void verifySessionRequest(final ClassifyAddDelSession request, final int tableIndex,
                                             final int vlanTags, final boolean isL2) {
        assertEquals(1, request.isAdd);
        assertEquals(tableIndex, request.tableIndex);
        assertEquals(0, request.hitNextIndex);

        byte[] expectedMatch = new byte[] {
            // L2:
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            // dscp(11), flow(123):
            (byte) 0x02, (byte) 0xc0, (byte) 0x00, (byte) 0x7b,
            // protocol (132):
            0, 0, (byte) 132, 0,
            // source address:
            (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8, (byte) 0x85, (byte) 0xa3, (byte) 0x08, (byte) 0xd3,
            (byte) 0x13, (byte) 0x19, (byte) 0x8a, (byte) 0x2e, (byte) 0x03, (byte) 0x70, (byte) 0x73, (byte) 0x48,
            // destination address:
            (byte) 0xfe, (byte) 0x80, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0xab, (byte) 0xcd,
            0, 0, 0, 0, 0, 0, 0, 0,
            // padding to multiple of 16B:
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };

        if (isL2) {
            expectedMatch[12] = (byte) 0x86;
            expectedMatch[13] = (byte) 0xdd;
        }
        AceIpWriterTestUtils.assertArrayEqualsWithOffset(expectedMatch, request.match, vlanTags * VLAN_TAG_LEN);

    }

    @Test
    public void testCreateClassifyTable() {
        final int nextTableIndex = 42;
        final ClassifyAddDelTable request =
            writer.createClassifyTable(aceIp, InterfaceMode.L3, nextTableIndex, 0);
        verifyTableRequest(request, nextTableIndex, 0, false);
    }

    @Test
    public void testCreateClassifyTableForL2Interface() {
        final int nextTableIndex = 42;
        final ClassifyAddDelTable request =
            writer.createClassifyTable(aceIp, InterfaceMode.L2, nextTableIndex, 0);
        verifyTableRequest(request, nextTableIndex, 0, true);
    }

    @Test
    public void testCreateClassifyTable1VlanTag() {
        final int nextTableIndex = 42;
        final int vlanTags = 1;
        final ClassifyAddDelTable request =
            writer.createClassifyTable(aceIp, InterfaceMode.L3, nextTableIndex, vlanTags);
        verifyTableRequest(request, nextTableIndex, vlanTags, false);
    }

    @Test
    public void testCreateClassifyTable2VlanTags() {
        final int nextTableIndex = 42;
        final int vlanTags = 2;
        final ClassifyAddDelTable request =
            writer.createClassifyTable(aceIp, InterfaceMode.L3, nextTableIndex, vlanTags);
        verifyTableRequest(request, nextTableIndex, vlanTags, false);
    }

    @Test
    public void testCreateClassifySession() {
        final int tableIndex = 123;
        final ClassifyAddDelSession request =
            writer.createClassifySession(action, aceIp, InterfaceMode.L3, tableIndex, 0);
        verifySessionRequest(request, tableIndex, 0, false);
    }

    @Test
    public void testCreateClassifySessionForL2Interface() {
        final int tableIndex = 123;
        final ClassifyAddDelSession request =
            writer.createClassifySession(action, aceIp, InterfaceMode.L2, tableIndex, 0);
        verifySessionRequest(request, tableIndex, 0, true);
    }

    @Test
    public void testCreateClassifySession1VlanTag() {
        final int tableIndex = 123;
        final int vlanTags = 1;
        final ClassifyAddDelSession request =
            writer.createClassifySession(action, aceIp, InterfaceMode.L3, tableIndex, vlanTags);
        verifySessionRequest(request, tableIndex, vlanTags, false);
    }

    @Test
    public void testCreateClassifySession2VlanTags() {
        final int tableIndex = 123;
        final int vlanTags = 2;
        final ClassifyAddDelSession request =
            writer.createClassifySession(action, aceIp, InterfaceMode.L3, tableIndex, vlanTags);
        verifySessionRequest(request, tableIndex, vlanTags, false);
    }

    @Test
    public void testSetClassifyTable() {
        final int tableIndex = 321;
        final InputAclSetInterface request = new InputAclSetInterface();
        writer.setClassifyTable(request, tableIndex);
        assertEquals(tableIndex, request.ip6TableIndex);
    }
}