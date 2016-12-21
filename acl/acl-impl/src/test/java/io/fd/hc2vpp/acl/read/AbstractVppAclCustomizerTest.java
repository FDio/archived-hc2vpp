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

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDetails;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDetailsReplyDump;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAcls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAclsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAclsKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractVppAclCustomizerTest extends ListReaderCustomizerTest<VppAcls, VppAclsKey, VppAclsBuilder> {

    protected static final String IF_NAME = "eth1";
    protected static final int IF_ID = 1;
    protected static final InstanceIdentifier<VppAcls> IID =
        InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
            .augmentation(VppAclInterfaceStateAugmentation.class).child(Acl.class).child(Ingress.class)
            .child(VppAcls.class);
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
    }

    @Test
    public void testGetAllIdsNoAclConfigured() throws ReadFailedException {
        AclInterfaceListDetailsReplyDump reply = new AclInterfaceListDetailsReplyDump();
        final AclInterfaceListDetails details = new AclInterfaceListDetails();
        details.acls = new int[0];
        reply.aclInterfaceListDetails.add(details);
        when(aclApi.aclInterfaceListDump(any())).thenReturn(future(reply));
        assertTrue(getCustomizer().getAllIds(IID, ctx).isEmpty());
    }
}