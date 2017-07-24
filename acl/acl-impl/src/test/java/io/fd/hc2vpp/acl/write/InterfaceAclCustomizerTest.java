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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.acl.AclTestSchemaContext;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceSetAclList;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceSetAclListReply;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.IngressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAcls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAclsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceAclCustomizerTest extends WriterCustomizerTest implements AclTestSchemaContext {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IFACE_NAME = "eth0";
    private static final int IFACE_ID = 123;

    @Mock
    private FutureJVppAclFacade aclApi;
    @Mock
    private AclContextManager standardAclContext;

    private InterfaceAclCustomizer customizer;
    private NamingContext interfaceContext;
    private InstanceIdentifier<Acl> ACL_ID = InstanceIdentifier.create(Interfaces.class)
        .child(Interface.class, new InterfaceKey(IFACE_NAME)).augmentation(VppAclInterfaceAugmentation.class).child(Acl.class);

    @Override
    protected void setUpTest() throws Exception {
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        customizer = new InterfaceAclCustomizer(aclApi, interfaceContext, standardAclContext);
        when(aclApi.aclInterfaceSetAclList(any())).thenReturn(future(new AclInterfaceSetAclListReply()));
    }

    @Test
    public void testWrite() throws Exception {
        final Acl acl = new AclBuilder().build();
        customizer.writeCurrentAttributes(ACL_ID, acl, writeContext);
        final AclInterfaceSetAclList list = new AclInterfaceSetAclList();
        list.swIfIndex = IFACE_ID;
        list.acls = new int[]{};
        verify(aclApi).aclInterfaceSetAclList(list);
    }

    @Test
    public void testUpdate() throws Exception {
        final Acl acl = new AclBuilder().build();
        customizer.updateCurrentAttributes(ACL_ID, acl, acl, writeContext);
        final AclInterfaceSetAclList list = new AclInterfaceSetAclList();
        list.swIfIndex = IFACE_ID;
        list.acls = new int[]{};
        verify(aclApi).aclInterfaceSetAclList(list);
    }

    @Test
    public void testDelete() throws Exception {
        final VppAcls
            element = mock(VppAcls.class);
        final Acl acl = new AclBuilder()
            .setIngress(new IngressBuilder()
                .setVppAcls(Collections.singletonList(new VppAclsBuilder()
                    .setName("asd")
                    .build()))
                .build())
            .build();
        customizer.deleteCurrentAttributes(ACL_ID, acl, writeContext);
        final AclInterfaceSetAclList list = new AclInterfaceSetAclList();
        list.swIfIndex = IFACE_ID;
        list.acls = new int[]{};
        verify(aclApi).aclInterfaceSetAclList(list);
    }

}