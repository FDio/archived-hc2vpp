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

import static io.fd.hc2vpp.acl.read.IngressAclCustomizer.ACL_NOT_ASSIGNED;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDetailsReplyDump;
import io.fd.vpp.jvpp.acl.dto.MacipAclDetails;
import io.fd.vpp.jvpp.acl.dto.MacipAclDetailsReplyDump;
import io.fd.vpp.jvpp.acl.dto.MacipAclDump;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceGetReply;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceListDetailsReplyDump;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import javax.annotation.Nonnull;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.Ingress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.AclSets;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.AclSetsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSetBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSetKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MacIpAclCustomizerTest extends AbstractAclCustomizerTest {

    protected static final String IF_NAME_NO_ACL = "eth2";
    protected static final int IF_ID_NO_ACL = 1;
    protected static final String IFC_CTX_NAME = "interface-context";
    private static final String IF_NAME = "eth1";
    private static final int IF_ID = 1;
    private static final String ACL_NAME = "acl-name";
    private static final int ACL_ID = 1;
    @Mock
    protected FutureJVppAclFacade aclApi;
    protected NamingContext interfaceContext = new NamingContext("iface", IFC_CTX_NAME);

    public MacIpAclCustomizerTest() {
        super(AclSetsBuilder.class);
    }

    @Override
    protected IngressAclCustomizer initCustomizer() {
        return new IngressAclCustomizer(aclApi, interfaceContext, standardAclContext, macIpAclContext);
    }

    @Override
    protected void setUp() {
        defineMapping(mappingContext, IF_NAME, IF_ID, IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME_NO_ACL, IF_ID_NO_ACL, IFC_CTX_NAME);
        when(macIpAclContext.getAclName(ACL_ID, mappingContext)).thenReturn(ACL_NAME);
        when(macIpAclContext.containsAcl(ACL_NAME, mappingContext)).thenReturn(true);
        when(standardAclContext.containsAcl(ACL_NAME, mappingContext)).thenReturn(false);
        final AclInterfaceListDetailsReplyDump reply = aclInterfaceDump((byte) 0);
        when(aclApi.aclInterfaceListDump(any())).thenReturn(future(reply));
        final MacipAclInterfaceListDetailsReplyDump macReply = macAaclInterfaceDump(1, "acl-name");
        when(aclApi.macipAclInterfaceListDump(any())).thenReturn(future(macReply));
    }


    @Test
    public void testRead() throws ReadFailedException {
        final AclSetBuilder builder = mock(AclSetBuilder.class);

        final MacipAclInterfaceGetReply assignedAcls = new MacipAclInterfaceGetReply();
        assignedAcls.count = 2;
        assignedAcls.acls = new int[]{ACL_NOT_ASSIGNED, ACL_ID};
        when(aclApi.macipAclInterfaceGet(any())).thenReturn(future(assignedAcls));

        final MacipAclDump request = new MacipAclDump();
        request.aclIndex = ACL_ID;
        final MacipAclDetailsReplyDump reply = new MacipAclDetailsReplyDump();
        final MacipAclDetails details = new MacipAclDetails();
        details.aclIndex = ACL_ID;
        reply.macipAclDetails.add(details);
        when(aclApi.macipAclDump(request)).thenReturn(future(reply));

        getCustomizer().readCurrentAttributes(getIid(IF_NAME_NO_ACL, new AclSetKey(ACL_NAME)), builder, ctx);
        verify(builder).setName(ACL_NAME);
    }

    @Test
    public void testReadNotAssigned() throws ReadFailedException {
        final AclSetBuilder builder = mock(AclSetBuilder.class);

        final MacipAclInterfaceGetReply assignedAcls = new MacipAclInterfaceGetReply();
        // pretending we have 3 interfaces, IF_NAME does not have MacipAcl assigned
        assignedAcls.count = 3;
        assignedAcls.acls = new int[]{ACL_NOT_ASSIGNED, ACL_NOT_ASSIGNED, ACL_NOT_ASSIGNED};
        when(aclApi.macipAclInterfaceGet(any())).thenReturn(future(assignedAcls));

        final MacipAclDump request = new MacipAclDump();
        request.aclIndex = ACL_ID;
        final MacipAclDetailsReplyDump reply = new MacipAclDetailsReplyDump();
        final MacipAclDetails details = new MacipAclDetails();
        details.aclIndex = ACL_ID;
        reply.macipAclDetails.add(details);
        when(aclApi.macipAclDump(request)).thenReturn(future(reply));

        getCustomizer().readCurrentAttributes(getIid(IF_NAME_NO_ACL, new AclSetKey(ACL_NAME)), builder, ctx);
        verifyZeroInteractions(builder);
    }

    @Test
    public void testReadNoAcls() throws ReadFailedException {
        final AclSetBuilder builder = mock(AclSetBuilder.class);
        final MacipAclInterfaceGetReply assignedAcls = new MacipAclInterfaceGetReply();
        assignedAcls.count = 0;
        assignedAcls.acls = new int[0];
        when(aclApi.macipAclInterfaceGet(any())).thenReturn(future(assignedAcls));
        getCustomizer().readCurrentAttributes(getIid(IF_NAME_NO_ACL, new AclSetKey(ACL_NAME)), builder, ctx);
        verifyZeroInteractions(builder);
    }

    @Test
    public void testGetAllIdsNoAclConfigured() throws ReadFailedException {
        final MacipAclInterfaceListDetailsReplyDump macReply = macAaclInterfaceDump(1);
        when(aclApi.macipAclInterfaceListDump(any())).thenReturn(future(macReply));
        assertTrue(getCustomizer().getAllIds(getWildcardedIid(IF_NAME_NO_ACL), ctx).isEmpty());
    }

    @Test
    public void testReadAllTwoIfacesInOneTx() throws ReadFailedException {
        final MacipAclInterfaceListDetailsReplyDump macReply = macAaclInterfaceDump(1);
        final MacipAclInterfaceGetReply interfaceGet = macipAclInterfaceGetReply();
        // read all for interface with defined ACLs:
        assertFalse(getCustomizer().getAllIds(getWildcardedIid(IF_NAME), ctx).isEmpty());
        // read all for interface without ACLs defined:
        when(aclApi.macipAclInterfaceListDump(any())).thenReturn(future(macReply));
        when(aclApi.macipAclInterfaceGet(any())).thenReturn(future(interfaceGet));
        Assert.assertEquals(0, getCustomizer().getAllIds(getWildcardedIid(IF_NAME_NO_ACL), ctx).size());
    }

    @Test
    public void testInit() {
        final AclSet readValue = new AclSetBuilder().build();
        final Initialized<? extends DataObject>
                cfgValue = getCustomizer().init(getWildcardedIid(IF_NAME), readValue, ctx);
        assertEquals(cfgValue.getData(), readValue);
        assertNotNull(cfgValue.getId().firstKeyOf(Interface.class));
        assertEquals(cfgValue.getId().getTargetType(), AclSet.class);

    }

    @Override
    protected InstanceIdentifier<AclSet> getWildcardedIid(@Nonnull final String ifName) {
        return getAclId(ifName).child(Ingress.class).child(AclSets.class).child(AclSet.class);
    }

    @Override
    protected InstanceIdentifier<AclSet> getIid(@Nonnull final String ifName, @Nonnull final AclSetKey key) {
        return getAclId(ifName).child(Ingress.class).child(AclSets.class).child(AclSet.class, key);
    }
}