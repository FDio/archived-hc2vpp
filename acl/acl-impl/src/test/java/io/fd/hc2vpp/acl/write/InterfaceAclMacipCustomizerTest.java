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

import io.fd.hc2vpp.acl.AclIIds;
import io.fd.hc2vpp.acl.AclTestSchemaContext;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.jvpp.acl.dto.AclInterfaceSetAclListReply;
import io.fd.jvpp.acl.dto.MacipAclInterfaceAddDel;
import io.fd.jvpp.acl.dto.MacipAclInterfaceAddDelReply;
import io.fd.jvpp.acl.future.FutureJVppAclFacade;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.IngressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.AclSetsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSetBuilder;
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
    @Mock
    private AclContextManager standardAclContext;

    private InterfaceAclCustomizer customizer;
    private NamingContext interfaceContext;
    private InstanceIdentifier<Interface> IFC_IID =
            AclIIds.ACLS_AP.child(Interface.class, new InterfaceKey(IFACE_NAME));
    private Interface ifcAcl;

    @Override
    protected void setUpTest() throws Exception {
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        customizer = new InterfaceAclCustomizer(aclApi, interfaceContext, standardAclContext, macipAclContext);
        ifcAcl = new InterfaceBuilder().setIngress(new IngressBuilder().setAclSets(new AclSetsBuilder().setAclSet(
                Collections.singletonList(new AclSetBuilder().setName(ACL_NAME).build())).build()).build()).build();
        when(macipAclContext.getAclIndex(ACL_NAME, mappingContext)).thenReturn(ACL_ID);
        when(macipAclContext.containsAcl(ACL_NAME, mappingContext)).thenReturn(true);
        when(aclApi.macipAclInterfaceAddDel(any())).thenReturn(future(new MacipAclInterfaceAddDelReply()));
        when(aclApi.aclInterfaceSetAclList(any())).thenReturn(future(new AclInterfaceSetAclListReply()));
    }

    @Test
    public void testWrite() throws Exception {
        customizer.writeCurrentAttributes(IFC_IID, ifcAcl, writeContext);
        final MacipAclInterfaceAddDel request = new MacipAclInterfaceAddDel();
        request.swIfIndex = IFACE_ID;
        request.isAdd = 1;
        request.aclIndex = ACL_ID;
        verify(aclApi).macipAclInterfaceAddDel(request);
    }

    @Test
    public void testUpdate() throws Exception {
        customizer.updateCurrentAttributes(IFC_IID, ifcAcl, ifcAcl, writeContext);
        verify(aclApi).aclInterfaceSetAclList(any());
    }

    @Test
    public void testDelete() throws Exception {
        customizer.deleteCurrentAttributes(IFC_IID, ifcAcl, writeContext);
        final MacipAclInterfaceAddDel request = new MacipAclInterfaceAddDel();
        request.swIfIndex = IFACE_ID;
        request.isAdd = 0;
        request.aclIndex = ACL_ID;
        verify(aclApi).macipAclInterfaceAddDel(request);
    }
}