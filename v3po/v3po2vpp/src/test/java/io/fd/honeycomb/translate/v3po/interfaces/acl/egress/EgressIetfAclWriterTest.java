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

package io.fd.honeycomb.translate.v3po.interfaces.acl.egress;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.v3po.interfaces.acl.common.AclTableContextManager;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSessionReply;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTableReply;
import io.fd.vpp.jvpp.core.dto.ClassifySetInterfaceL2Tables;
import io.fd.vpp.jvpp.core.dto.ClassifySetInterfaceL2TablesReply;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.EthAcl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.PermitBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.ietf.acl.Egress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.ietf.acl.EgressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.acl.mapping.entry.context.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.access.lists.AclBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EgressIetfAclWriterTest extends WriterCustomizerTest {

    private static final int IF_INDEX = 1;
    private static final String ACL_NAME = "acl1";
    private static final Class<? extends AclBase> ACL_TYPE = EthAcl.class;

    private EgressIetfAclWriter writer;
    @Mock
    private AclTableContextManager aclCtx;
    @Mock
    private InstanceIdentifier<?> id;

    @Override
    protected void setUp() throws Exception {
        writer = new EgressIetfAclWriter(api, aclCtx);
    }

    private static ClassifyAddDelTable classifyAddDelTable(final int tableIndex) {
        final ClassifyAddDelTable reply = new ClassifyAddDelTable();
        reply.tableIndex = tableIndex;
        return reply;
    }

    private ClassifySetInterfaceL2Tables classifySetInterfaceL2TablesRequest() {
        final ClassifySetInterfaceL2Tables request = new ClassifySetInterfaceL2Tables();
        request.isInput = 0;
        request.ip4TableIndex = -1;
        request.ip6TableIndex = -1;
        request.otherTableIndex = -1;
        request.swIfIndex = IF_INDEX;
        return request;
    }

    @Test
    public void testDeleteAcl() throws Exception {
        when(api.classifyAddDelTable(any())).thenReturn(future(new ClassifyAddDelTableReply()));
        when(api.classifySetInterfaceL2Tables(any())).thenReturn(future(new ClassifySetInterfaceL2TablesReply()));
        when(aclCtx.getEntry(IF_INDEX, mappingContext)).thenReturn(Optional.of(
            new MappingEntryBuilder()
                .setIndex(IF_INDEX)
                .setL2TableId(1)
                .setIp4TableId(2)
                .setIp6TableId(3)
                .build()));

        writer.deleteAcl(id, IF_INDEX, mappingContext);

        verify(api).classifySetInterfaceL2Tables(classifySetInterfaceL2TablesRequest());
        verify(api).classifyAddDelTable(classifyAddDelTable(1));
        verify(api).classifyAddDelTable(classifyAddDelTable(2));
        verify(api).classifyAddDelTable(classifyAddDelTable(3));
        verify(aclCtx).removeEntry(IF_INDEX, mappingContext);
    }

    @Test
    public void testWrite() throws Exception {
        when(api.classifyAddDelTable(any())).thenReturn(future(new ClassifyAddDelTableReply()));
        when(api.classifyAddDelSession(any())).thenReturn(future(new ClassifyAddDelSessionReply()));
        when(api.classifySetInterfaceL2Tables(any())).thenReturn(future(new ClassifySetInterfaceL2TablesReply()));

        final Egress
            acl = new EgressBuilder().setAccessLists(
            new AccessListsBuilder().setAcl(
                Collections.singletonList(new AclBuilder()
                    .setName(ACL_NAME)
                    .setType(ACL_TYPE)
                    .build())
            ).setMode(InterfaceMode.L2).build()
        ).build();

        final AccessLists accessLists = acl.getAccessLists();

        when(writeContext.readAfter(any())).thenReturn(Optional.of(
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclBuilder()
                .setAccessListEntries(
                    new AccessListEntriesBuilder().setAce(Collections.singletonList(new AceBuilder()
                        .setMatches(new MatchesBuilder().setAceType(
                            new AceIpBuilder()
                                .setAceIpVersion(new AceIpv4Builder().build())
                                .setProtocol((short) 1)
                                .build()
                        ).build())
                        .setActions(new ActionsBuilder().setPacketHandling(new PermitBuilder().build()).build())
                        .build())).build()
                ).build()

        ));

        writer.write(id, IF_INDEX, accessLists.getAcl(), accessLists.getDefaultAction(), accessLists.getMode(),
            writeContext, mappingContext);

        final ClassifySetInterfaceL2Tables request = new ClassifySetInterfaceL2Tables();
        request.isInput = 0;
        request.swIfIndex = IF_INDEX;
        request.otherTableIndex = -1;
        request.ip4TableIndex = 0;
        request.ip6TableIndex = -1;
        verify(api).classifySetInterfaceL2Tables(request);
    }
}