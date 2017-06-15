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

package io.fd.hc2vpp.acl.read;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.test.read.InitializingListReaderCustomizerTest;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.vpp.jvpp.acl.dto.AclDetails;
import io.fd.vpp.jvpp.acl.dto.AclDetailsReplyDump;
import io.fd.vpp.jvpp.acl.dto.MacipAclDetails;
import io.fd.vpp.jvpp.acl.dto.MacipAclDetailsReplyDump;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import io.fd.vpp.jvpp.acl.types.AclRule;
import io.fd.vpp.jvpp.acl.types.MacipAclRule;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Deny;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppMacipAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.VppAceNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.acl.ip.protocol.header.fields.ip.protocol.Other;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class AclCustomizerTest extends InitializingListReaderCustomizerTest<Acl, AclKey, AclBuilder> {

    private static final String ACL_NAME = "vpp-acl";
    private static final String ACE_NAME = "vpp-ace";
    private static final int ACL_INDEX = 123;
    private static final String MACIP_ACL_NAME = "vpp-macip-acl";
    private static final String MACIP_ACE_NAME = "vpp-macip-ace";
    private static final int MACIP_ACL_INDEX = 456;
    private static final short PROTOCOL = 2;
    @Mock
    private FutureJVppAclFacade aclApi;
    @Mock
    private AclContextManager standardAclContext;
    @Mock
    private AclContextManager macipAclContext;
    private KeyedInstanceIdentifier<Acl, AclKey> ACL_IID =
        InstanceIdentifier.create(AccessLists.class).child(Acl.class, new AclKey(
        ACL_NAME, VppAcl.class));
    private KeyedInstanceIdentifier<Acl, AclKey> MACIP_ACL_IID =
        InstanceIdentifier.create(AccessLists.class).child(Acl.class, new AclKey(MACIP_ACL_NAME, VppMacipAcl.class));

    public AclCustomizerTest() {
        super(Acl.class, AccessListsBuilder.class);
    }

    @Override
    protected AclCustomizer initCustomizer() {
        return new AclCustomizer(aclApi, standardAclContext, macipAclContext);
    }

    @Override
    protected void setUp() throws Exception {
        final AclDetailsReplyDump aclDump = new AclDetailsReplyDump();
        aclDump.aclDetails = new ArrayList<>();
        final AclDetails acl1 = new AclDetails();
        acl1.r = new AclRule[1];
        acl1.r[0] = new AclRule();
        acl1.r[0].proto = PROTOCOL;
        acl1.aclIndex = ACL_INDEX;
        aclDump.aclDetails.add(acl1);
        when(aclApi.aclDump(any())).thenReturn(future(aclDump));

        final MacipAclDetailsReplyDump macipAclDump = new MacipAclDetailsReplyDump();
        macipAclDump.macipAclDetails = new ArrayList<>();
        final MacipAclDetails macipAcl1 = new MacipAclDetails();
        macipAcl1.r = new MacipAclRule[]{new MacipAclRule()};
        macipAcl1.aclIndex = MACIP_ACL_INDEX;
        macipAclDump.macipAclDetails.add(macipAcl1);
        when(aclApi.macipAclDump(any())).thenReturn(future(macipAclDump));

        when(standardAclContext.getAclName(ACL_INDEX, mappingContext)).thenReturn(ACL_NAME);
        when(standardAclContext.getAclIndex(ACL_NAME, mappingContext)).thenReturn(ACL_INDEX);
        when(standardAclContext.getAceName(ACL_NAME, 0, mappingContext)).thenReturn(ACE_NAME);

        when(macipAclContext.getAclName(MACIP_ACL_INDEX, mappingContext)).thenReturn(MACIP_ACL_NAME);
        when(macipAclContext.getAclIndex(MACIP_ACL_NAME, mappingContext)).thenReturn(MACIP_ACL_INDEX);
        when(macipAclContext.getAceName(MACIP_ACL_NAME, 0, mappingContext)).thenReturn(MACIP_ACE_NAME);
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {
        final List<AclKey> allIds = getCustomizer().getAllIds(InstanceIdentifier.create(AccessLists.class).child(Acl.class), ctx);
        assertEquals(2, allIds.size());
        assertEquals(ACL_IID.getKey(), allIds.get(0));
        assertEquals(MACIP_ACL_IID.getKey(), allIds.get(1));
    }

    @Test
    public void testReadStandardAcl() throws ReadFailedException {
        final AclBuilder builder = new AclBuilder();
        getCustomizer().readCurrentAttributes(ACL_IID, builder, ctx);
        assertEquals(ACL_IID.getKey(), builder.getKey());
        final List<Ace> aces = builder.getAccessListEntries().getAce();
        assertEquals(1, aces.size());
        final Ace ace = aces.get(0);
        assertEquals(ACE_NAME, ace.getKey().getRuleName());
        assertTrue(ace.getActions().getPacketHandling() instanceof Deny);
        final VppAceNodes nodes = ((VppAce) (ace.getMatches().getAceType())).getVppAceNodes();
        assertEquals(PROTOCOL, ((Other) nodes.getIpProtocol()).getOtherNodes().getProtocol().shortValue());

    }

    @Test
    public void testReadMacipAcl() throws ReadFailedException {
        final AclBuilder builder = new AclBuilder();
        getCustomizer().readCurrentAttributes(MACIP_ACL_IID, builder, ctx);
        assertEquals(MACIP_ACL_IID.getKey(), builder.getKey());
        final List<Ace> aces = builder.getAccessListEntries().getAce();
        assertEquals(1, aces.size());
        final Ace ace = aces.get(0);
        assertEquals(MACIP_ACE_NAME, ace.getKey().getRuleName());
        assertTrue(ace.getActions().getPacketHandling() instanceof Deny);
    }
}