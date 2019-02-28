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

package io.fd.hc2vpp.acl.read;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.acl.AclIIds;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.test.read.InitializingListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.jvpp.acl.dto.AclDetails;
import io.fd.jvpp.acl.dto.AclDetailsReplyDump;
import io.fd.jvpp.acl.dto.AclInterfaceListDetails;
import io.fd.jvpp.acl.dto.AclInterfaceListDetailsReplyDump;
import io.fd.jvpp.acl.dto.AclInterfaceListDump;
import io.fd.jvpp.acl.dto.MacipAclInterfaceGetReply;
import io.fd.jvpp.acl.dto.MacipAclInterfaceListDetails;
import io.fd.jvpp.acl.dto.MacipAclInterfaceListDetailsReplyDump;
import io.fd.jvpp.acl.future.FutureJVppAclFacade;
import java.util.ArrayList;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSetBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSetKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public abstract class AbstractAclCustomizerTest
        extends InitializingListReaderCustomizerTest<AclSet, AclSetKey, AclSetBuilder> {

    protected static final String IF_NAME = "eth1";
    protected static final int IF_ID = 1;
    protected static final int ACL_ID = 1;
    protected static final int ACL_MAC_ID = 2;
    private static final String ACL_NAME = "acl-name";
    private static final String ACL_MAC_NAME = "acl-mac-name";

    protected static final String IF_NAME_NO_ACL = "eth2";
    protected static final int IF_ID_NO_ACL = 2;

    protected static final String IFC_CTX_NAME = "interface-context";
    protected static final String ACL_CTX_NAME = "standard-acl-context";

    @Mock
    protected FutureJVppAclFacade aclApi;

    protected NamingContext interfaceContext = new NamingContext("iface", IFC_CTX_NAME);

    @Mock
    protected AclContextManager standardAclContext;

    @Mock
    protected AclContextManager macIpAclContext;

    protected AbstractAclCustomizerTest(final Class<? extends Builder<? extends DataObject>> parentBuilderClass) {
        super(AclSet.class, parentBuilderClass);
    }

    protected static KeyedInstanceIdentifier<Interface, InterfaceKey> getAclId(final String ifName) {
        return AclIIds.ACLS_AP.child(Interface.class, new InterfaceKey(ifName));
    }

    @Override
    protected void setUp() throws Exception {
        defineMapping(mappingContext, IF_NAME, IF_ID, IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME_NO_ACL, IF_ID_NO_ACL, IFC_CTX_NAME);
        when(macIpAclContext.getAclName(ACL_MAC_ID, mappingContext)).thenReturn(ACL_MAC_NAME);
        when(standardAclContext.getAclName(ACL_ID, mappingContext)).thenReturn(ACL_NAME);
        when(macIpAclContext.containsAcl(ACL_MAC_NAME, mappingContext)).thenReturn(true);
        when(standardAclContext.containsAcl(ACL_NAME, mappingContext)).thenReturn(true);
        final MacipAclInterfaceListDetailsReplyDump macReply = macAaclInterfaceDump(0);
        when(aclApi.macipAclInterfaceListDump(any())).thenReturn(future(macReply));
        final AclInterfaceListDetailsReplyDump reply = aclInterfaceDump((byte) 0);
        when(aclApi.aclInterfaceListDump(any())).thenReturn(future(reply));
    }

    @Test
    public void testGetAllIdsNoAclConfigured() throws ReadFailedException {
        assertTrue(getCustomizer().getAllIds(getWildcardedIid(IF_NAME), ctx).isEmpty());
    }

    @Test
    public void testRead() throws ReadFailedException {
        final String aclName = "acl-name";
        defineMapping(mappingContext, aclName, 1, ACL_CTX_NAME);

        final AclDetailsReplyDump reply = new AclDetailsReplyDump();
        reply.aclDetails = new ArrayList<>();
        final AclDetails detail = new AclDetails();
        detail.tag = new byte[0];
        reply.aclDetails.add(detail);
        when(aclApi.aclDump(any())).thenReturn(future(reply));

        final AclSetBuilder builder = mock(AclSetBuilder.class);
        getCustomizer().readCurrentAttributes(getIid(IF_NAME, new AclSetKey(aclName)), builder, ctx);
        verify(builder).setName(aclName);
    }

    @Test
    public void testReadAllTwoIfacesInOneTx() throws ReadFailedException {
        final AclInterfaceListDetailsReplyDump reply = aclInterfaceDump((byte) 2, "acl1", "acl2", "acl3");
        final MacipAclInterfaceListDetailsReplyDump macReply = macAaclInterfaceDump(0);
        final MacipAclInterfaceListDetailsReplyDump macReply2 = macAaclInterfaceDump(1);
        final MacipAclInterfaceGetReply interfaceGet = macipAclInterfaceGetReply();


        when(aclApi.aclInterfaceListDump(aclInterfaceRequest(IF_ID))).thenReturn(future(reply));
        when(aclApi.macipAclInterfaceListDump(any())).thenReturn(future(macReply));
        when(aclApi.macipAclInterfaceGet(any())).thenReturn(future(interfaceGet));

        when(aclApi.aclInterfaceListDump(aclInterfaceRequest(IF_ID_NO_ACL)))
                .thenReturn(future(aclInterfaceDump((byte) 0)));

        // read all for interface with defined ACLs:
        assertFalse(getCustomizer().getAllIds(getWildcardedIid(IF_NAME), ctx).isEmpty());
        // read all for interface without ACLs defined:
        assertEquals(0, getCustomizer().getAllIds(getWildcardedIid(IF_NAME_NO_ACL), ctx).size());
    }

    protected MacipAclInterfaceGetReply macipAclInterfaceGetReply(final String... aclNames) {
        final MacipAclInterfaceGetReply reply = new MacipAclInterfaceGetReply();
        reply.acls = new int[aclNames.length];
        for (int i = 0; i < aclNames.length; ++i) {
            defineMapping(mappingContext, aclNames[i], i, ACL_CTX_NAME);
            reply.acls[i] = i;
        }
        reply.count = (byte) aclNames.length;
        return reply;
    }

    @Test
    public void testInit() {
        final String aclName = "acl-name";
        defineMapping(mappingContext, aclName, 1, ACL_CTX_NAME);

        final AclSet readValue = new AclSetBuilder().build();
        final Initialized<? extends DataObject> cfgValue =
                getCustomizer().init(getIid(IF_NAME, new AclSetKey(aclName)), readValue, ctx);
        assertEquals(readValue, cfgValue.getData());
        assertNotNull(cfgValue.getId().firstKeyOf(Interface.class));
        assertEquals(cfgValue.getId().getTargetType(), AclSet.class);
    }

    protected AclInterfaceListDump aclInterfaceRequest(final int swIfIndex) {
        final AclInterfaceListDump request = new AclInterfaceListDump();
        request.swIfIndex = swIfIndex;
        return request;
    }

    protected AclInterfaceListDetailsReplyDump aclInterfaceDump(final byte nInput, final String... aclNames) {
        final AclInterfaceListDetailsReplyDump reply = new AclInterfaceListDetailsReplyDump();
        final AclInterfaceListDetails details = new AclInterfaceListDetails();
        details.acls = new int[aclNames.length];
        for (int i = 0; i < aclNames.length; ++i) {
            defineMapping(mappingContext, aclNames[i], i, ACL_CTX_NAME);
            details.acls[i] = i;
        }
        details.count = (byte) aclNames.length;
        details.nInput = nInput;
        reply.aclInterfaceListDetails.add(details);
        return reply;
    }

    protected MacipAclInterfaceListDetailsReplyDump macAaclInterfaceDump(int swIfIndex, final String... aclNames) {
        final MacipAclInterfaceListDetailsReplyDump assignedAcls = new MacipAclInterfaceListDetailsReplyDump();

        MacipAclInterfaceListDetails details = new MacipAclInterfaceListDetails();
        details.swIfIndex = swIfIndex;
        details.count = (byte) aclNames.length;
        details.acls = new int[aclNames.length];
        for (int i = 0; i < aclNames.length; ++i) {
            defineMapping(mappingContext, aclNames[i], i, ACL_CTX_NAME);
            details.acls[i] = i;
        }

        assignedAcls.macipAclInterfaceListDetails.add(details);
        assignedAcls.macipAclInterfaceListDetails = Collections.singletonList(details);

        return assignedAcls;
    }

    protected abstract InstanceIdentifier<AclSet> getWildcardedIid(@Nonnull final String ifName);

    protected abstract InstanceIdentifier<AclSet> getIid(@Nonnull final String ifName, @Nonnull final AclSetKey key);
}