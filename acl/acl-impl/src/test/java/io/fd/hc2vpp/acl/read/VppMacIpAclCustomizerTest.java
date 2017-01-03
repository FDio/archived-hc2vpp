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

import static io.fd.hc2vpp.acl.read.AbstractVppAclCustomizerTest.getAclId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.test.read.InitializingReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.vpp.jvpp.acl.dto.MacipAclDetails;
import io.fd.vpp.jvpp.acl.dto.MacipAclDetailsReplyDump;
import io.fd.vpp.jvpp.acl.dto.MacipAclDump;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceGetReply;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.IngressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.macip.acls.base.attributes.VppMacipAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.macip.acls.base.attributes.VppMacipAclBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppMacIpAclCustomizerTest extends InitializingReaderCustomizerTest<VppMacipAcl, VppMacipAclBuilder> {

    protected static final String IF_NAME_NO_ACL = "eth2";
    protected static final int IF_ID_NO_ACL = 2;
    protected static final String IFC_CTX_NAME = "interface-context";
    private static final String IF_NAME = "eth1";
    private static final int IF_ID = 1;
    private static final String ACL_NAME = "acl-name";
    private static final int ACL_ID = 1;
    private static final Class<? extends AclBase> ACL_TYPE =
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.VppMacipAcl.class;
    @Mock
    protected FutureJVppAclFacade aclApi;
    protected NamingContext interfaceContext = new NamingContext("iface", IFC_CTX_NAME);
    @Mock
    protected AclContextManager macIpAclContext;

    public VppMacIpAclCustomizerTest() {
        super(VppMacipAcl.class, IngressBuilder.class);
    }

    @Override
    protected VppMacIpAclCustomizer initCustomizer() {
        return new VppMacIpAclCustomizer(aclApi, interfaceContext, macIpAclContext);
    }

    @Override
    protected void setUp() {
        defineMapping(mappingContext, IF_NAME, IF_ID, IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME_NO_ACL, IF_ID_NO_ACL, IFC_CTX_NAME);
        when(macIpAclContext.getAclName(ACL_ID, mappingContext)).thenReturn(ACL_NAME);
    }

    @Test
    public void testRead() throws ReadFailedException {
        final VppMacipAclBuilder builder = mock(VppMacipAclBuilder.class);

        final MacipAclInterfaceGetReply assignedAcls = new MacipAclInterfaceGetReply();
        assignedAcls.count = 2;
        assignedAcls.acls = new int[] {-1, ACL_ID};
        when(aclApi.macipAclInterfaceGet(any())).thenReturn(future(assignedAcls));

        final MacipAclDump request = new MacipAclDump();
        request.aclIndex = ACL_ID;
        final MacipAclDetailsReplyDump reply = new MacipAclDetailsReplyDump();
        final MacipAclDetails details = new MacipAclDetails();
        details.aclIndex = ACL_ID;
        reply.macipAclDetails.add(details);
        when(aclApi.macipAclDump(request)).thenReturn(future(reply));

        getCustomizer().readCurrentAttributes(getIid(IF_NAME), builder, ctx);
        verify(builder).setName(ACL_NAME);
        verify(builder).setType(ACL_TYPE);
    }

    @Test
    public void testReadNoAcls() throws ReadFailedException {
        final VppMacipAclBuilder builder = mock(VppMacipAclBuilder.class);
        when(aclApi.macipAclInterfaceGet(any())).thenReturn(future(new MacipAclInterfaceGetReply()));
        getCustomizer().readCurrentAttributes(getIid(IF_NAME_NO_ACL), builder, ctx);
        verifyZeroInteractions(builder);
    }

    @Test
    public void testInit() {
        final VppMacipAcl readValue = new VppMacipAclBuilder().build();
        final Initialized<? extends DataObject> cfgValue = getCustomizer().init(getIid(IF_NAME), readValue, ctx);
        assertEquals(cfgValue.getData(), readValue);
        assertNotNull(cfgValue.getId().firstKeyOf(Interface.class));
        assertEquals(cfgValue.getId().getTargetType(), VppMacipAcl.class);

    }

    protected InstanceIdentifier<VppMacipAcl> getIid(@Nonnull final String ifName) {
        return getAclId(ifName).child(Ingress.class).child(VppMacipAcl.class);
    }

}