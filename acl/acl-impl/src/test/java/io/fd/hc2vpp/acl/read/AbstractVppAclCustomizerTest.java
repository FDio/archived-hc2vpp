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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.vpp.jvpp.acl.dto.AclDetails;
import io.fd.vpp.jvpp.acl.dto.AclDetailsReplyDump;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDetails;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDetailsReplyDump;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDump;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import java.util.ArrayList;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAcls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAclsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAclsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.VppAcl;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractVppAclCustomizerTest
    extends ListReaderCustomizerTest<VppAcls, VppAclsKey, VppAclsBuilder> {

    protected static final String IF_NAME = "eth1";
    protected static final int IF_ID = 1;

    protected static final String IF_NAME_NO_ACL = "eth2";
    protected static final int IF_ID_NO_ACL = 2;

    protected static final String IFC_CTX_NAME = "interface-context";
    protected static final String ACL_CTX_NAME = "standard-acl-context";

    @Mock
    protected FutureJVppAclFacade aclApi;

    protected NamingContext interfaceContext = new NamingContext("iface", IFC_CTX_NAME);
    protected NamingContext standardAclContext = new NamingContext("standardAcl", ACL_CTX_NAME);

    protected AbstractVppAclCustomizerTest(final Class<? extends Builder<? extends DataObject>> parentBuilderClass) {
        super(VppAcls.class, parentBuilderClass);
    }

    @Override
    protected void setUp() throws Exception {
        defineMapping(mappingContext, IF_NAME, IF_ID, IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME_NO_ACL, IF_ID_NO_ACL, IFC_CTX_NAME);
    }

    @Test
    public void testGetAllIdsNoAclConfigured() throws ReadFailedException {
        final AclInterfaceListDetailsReplyDump reply = aclInterfaceDump((byte) 0);
        when(aclApi.aclInterfaceListDump(any())).thenReturn(future(reply));
        assertTrue(getCustomizer().getAllIds(getWildcardedIid(IF_NAME), ctx).isEmpty());
    }

    @Test
    public void testRead() throws ReadFailedException {
        final String aclName = "acl-name";
        final Class<VppAcl> aclType = VppAcl.class;
        defineMapping(mappingContext, aclName, 1, ACL_CTX_NAME);

        final AclDetailsReplyDump reply = new AclDetailsReplyDump();
        reply.aclDetails = new ArrayList<>();
        final AclDetails detail = new AclDetails();
        detail.tag = new byte[0];
        reply.aclDetails.add(detail);
        when(aclApi.aclDump(any())).thenReturn(future(reply));

        final VppAclsBuilder builder = mock(VppAclsBuilder.class);
        getCustomizer().readCurrentAttributes(getIid(IF_NAME, new VppAclsKey(aclName, aclType)), builder, ctx);
        verify(builder).setName(aclName);
        verify(builder).setType(aclType);
    }

    @Test
    public void testReadAllTwoIfacesInOneTx() throws ReadFailedException {
        final AclInterfaceListDetailsReplyDump reply = aclInterfaceDump((byte) 2, "acl1", "acl2", "acl3");
        when(aclApi.aclInterfaceListDump(aclInterfaceRequest(IF_ID))).thenReturn(future(reply));

        when(aclApi.aclInterfaceListDump(aclInterfaceRequest(IF_ID_NO_ACL)))
            .thenReturn(future(aclInterfaceDump((byte) 0)));

        // read all for interface with defined ACLs:
        assertFalse(getCustomizer().getAllIds(getWildcardedIid(IF_NAME), ctx).isEmpty());
        // read all for interface without ACLs defined:
        assertEquals(0, getCustomizer().getAllIds(getWildcardedIid(IF_NAME_NO_ACL), ctx).size());
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
        details.nInput = nInput;
        reply.aclInterfaceListDetails.add(details);
        return reply;
    }

    protected InstanceIdentifier<Acl> getAclId(final String ifName) {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(ifName))
            .augmentation(VppAclInterfaceStateAugmentation.class).child(Acl.class);
    }

    protected abstract InstanceIdentifier<VppAcls> getWildcardedIid(@Nonnull final String ifName);

    protected abstract InstanceIdentifier<VppAcls> getIid(@Nonnull final String ifName, @Nonnull final VppAclsKey key);
}