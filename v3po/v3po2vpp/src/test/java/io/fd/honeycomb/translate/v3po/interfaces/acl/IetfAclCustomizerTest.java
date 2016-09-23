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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import java.util.Collections;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.EthAcl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.DenyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.ietf.acl.base.attributes.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.ietf.acl.base.attributes.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.IetfAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.IetfAclBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.ClassifyAddDelSessionReply;
import org.openvpp.jvpp.core.dto.ClassifyAddDelTable;
import org.openvpp.jvpp.core.dto.ClassifyAddDelTableReply;
import org.openvpp.jvpp.core.dto.ClassifyTableByInterface;
import org.openvpp.jvpp.core.dto.ClassifyTableByInterfaceReply;
import org.openvpp.jvpp.core.dto.InputAclSetInterface;
import org.openvpp.jvpp.core.dto.InputAclSetInterfaceReply;

public class IetfAclCustomizerTest extends WriterCustomizerTest {

    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final InstanceIdentifier<IetfAcl> IID = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME)).augmentation(
        VppInterfaceAugmentation.class).child(IetfAcl.class);
    private static final String ACL_NAME = "acl1";
    private static final Class<? extends AclBase> ACL_TYPE = EthAcl.class;

    private IetfAclCustomizer customizer;
    private IetfAcl acl;

    @Override
    protected void setUp() {
        customizer = new IetfAclCustomizer(new IetfAClWriter(api), new NamingContext("prefix", IFC_TEST_INSTANCE));
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_TEST_INSTANCE);
        acl = new IetfAclBuilder().setAccessLists(
            new AccessListsBuilder().setAcl(
                Collections.singletonList(new AclBuilder()
                    .setName(ACL_NAME)
                    .setType(ACL_TYPE)
                    .build())
            ).build()
        ).build();
    }

    @Test
    public void testWrite() throws WriteFailedException {
        when(api.classifyAddDelTable(any())).thenReturn(future(new ClassifyAddDelTableReply()));
        when(api.classifyAddDelSession(any())).thenReturn(future(new ClassifyAddDelSessionReply()));

        when(writeContext.readAfter(any())).thenReturn(Optional.of(
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclBuilder()
                .setAccessListEntries(
                    new AccessListEntriesBuilder().setAce(Collections.singletonList(
                        new AceBuilder()
                            .setMatches(new MatchesBuilder().setAceType(
                                new AceIpBuilder()
                                    .setAceIpVersion(new AceIpv6Builder().build())
                                    .setProtocol((short)1)
                                    .build()
                            ).build())
                            .setActions(new ActionsBuilder().setPacketHandling(new DenyBuilder().build()).build())
                            .build()
                    )).build()
                ).build()

        ));
        when(api.inputAclSetInterface(any())).thenReturn(future(new InputAclSetInterfaceReply()));

        customizer.writeCurrentAttributes(IID, acl, writeContext);

        verify(api).classifyAddDelTable(any());
        verify(api).classifyAddDelSession(any());
        verify(api).inputAclSetInterface(inputAclSetInterfaceWriteRequest());
    }

    @Test
    public void testDelete() throws WriteFailedException {
        when(api.classifyTableByInterface(any())).thenReturn(future(classifyTableByInterfaceReply()));
        when(api.inputAclSetInterface(any())).thenReturn(future(new InputAclSetInterfaceReply()));
        when(api.classifyAddDelTable(any())).thenReturn(future(new ClassifyAddDelTableReply()));

        customizer.deleteCurrentAttributes(IID, acl, writeContext);

        final ClassifyTableByInterface expectedRequest = new ClassifyTableByInterface();
        expectedRequest.swIfIndex = IF_INDEX;
        verify(api).classifyTableByInterface(expectedRequest);
        verify(api).inputAclSetInterface(inputAclSetInterfaceDeleteRequest());
        verify(api).classifyAddDelTable(classifyAddDelTable(1));
        verify(api).classifyAddDelTable(classifyAddDelTable(2));
        verify(api).classifyAddDelTable(classifyAddDelTable(3));
    }

    private static InputAclSetInterface inputAclSetInterfaceDeleteRequest() {
        final InputAclSetInterface request = new InputAclSetInterface();
        request.l2TableIndex = 1;
        request.ip4TableIndex = 2;
        request.ip6TableIndex = 3;
        return request;
    }

    private static ClassifyAddDelTable classifyAddDelTable(final int tableIndex) {
        final ClassifyAddDelTable reply = new ClassifyAddDelTable();
        reply.tableIndex = tableIndex;
        return reply;
    }

    private static ClassifyTableByInterfaceReply classifyTableByInterfaceReply() {
        final ClassifyTableByInterfaceReply reply = new ClassifyTableByInterfaceReply();
        reply.l2TableId = 1;
        reply.ip4TableId = 2;
        reply.ip6TableId = 3;
        return reply;
    }

    private static InputAclSetInterface inputAclSetInterfaceWriteRequest() {
        final InputAclSetInterface request = new InputAclSetInterface();
        request.swIfIndex = IF_INDEX;
        request.isAdd = 1;
        request.l2TableIndex = -1;
        request.ip4TableIndex = -1;
        request.ip6TableIndex = 0;
        return request;
    }
}