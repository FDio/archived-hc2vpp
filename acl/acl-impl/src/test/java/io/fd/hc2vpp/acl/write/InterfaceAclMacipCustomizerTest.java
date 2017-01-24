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

package io.fd.hc2vpp.acl.write;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.acl.AclTestSchemaContext;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceAddDel;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceAddDelReply;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.macip.acls.base.attributes.VppMacipAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.macip.acls.base.attributes.VppMacipAclBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceAclMacipCustomizerTest extends WriterCustomizerTest implements AclTestSchemaContext {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IFACE_NAME = "eth0";
    private static final int IFACE_ID = 123;
    private static final String ACL_NAME = "macip_acl";
    private static final int ACL_ID = 111;

    @Mock
    private FutureJVppAclFacade aclApi;
    @Mock
    private AclContextManager macipAclContext;

    private InterfaceAclMacIpCustomizer customizer;
    private NamingContext interfaceContext;
    private InstanceIdentifier<VppMacipAcl> ACL_IID = InstanceIdentifier.create(Interfaces.class)
        .child(Interface.class, new InterfaceKey(IFACE_NAME)).augmentation(VppAclInterfaceAugmentation.class)
        .child(Acl.class).child(Ingress.class).child(VppMacipAcl.class);
    private VppMacipAcl acl;

    @Override
    protected void setUpTest() throws Exception {
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        customizer = new InterfaceAclMacIpCustomizer(aclApi, macipAclContext, interfaceContext);
        acl = new VppMacipAclBuilder().setName(ACL_NAME).build();
        when(macipAclContext.getAclIndex(ACL_NAME, mappingContext)).thenReturn(ACL_ID);
        when(aclApi.macipAclInterfaceAddDel(any())).thenReturn(future(new MacipAclInterfaceAddDelReply()));
    }

    @Test
    public void testWrite() throws Exception {
        customizer.writeCurrentAttributes(ACL_IID, acl, writeContext);
        final MacipAclInterfaceAddDel request = new MacipAclInterfaceAddDel();
        request.swIfIndex = IFACE_ID;
        request.isAdd = 1;
        request.aclIndex = ACL_ID;
        verify(aclApi).macipAclInterfaceAddDel(request);
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdate() throws Exception {
        customizer.updateCurrentAttributes(ACL_IID, acl, acl, writeContext);
    }

    @Test
    public void testDelete() throws Exception {
        customizer.deleteCurrentAttributes(ACL_IID, acl, writeContext);
        final MacipAclInterfaceAddDel request = new MacipAclInterfaceAddDel();
        request.swIfIndex = IFACE_ID;
        request.isAdd = 0;
        request.aclIndex = ACL_ID;
        verify(aclApi).macipAclInterfaceAddDel(request);
    }

}