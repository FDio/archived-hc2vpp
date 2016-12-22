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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDetailsReplyDump;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.IngressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAcls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAclsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IngressVppAclCustomizerTest extends AbstractVppAclCustomizerTest {

    public IngressVppAclCustomizerTest() {
        super(IngressBuilder.class);
    }

    @Override
    protected IngressVppAclCustomizer initCustomizer() {
        return new IngressVppAclCustomizer(aclApi, interfaceContext, standardAclContext);
    }

    @Test
    public void testGetAllIdsNoInputAclConfigured() throws ReadFailedException {
        final AclInterfaceListDetailsReplyDump reply = aclInterfaceDump((byte) 0, "acl1");
        when(aclApi.aclInterfaceListDump(aclInterfaceRequest(IF_ID))).thenReturn(future(reply));
        assertTrue(getCustomizer().getAllIds(getWildcardedIid(IF_NAME), ctx).isEmpty());
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {
        final byte nInput = 2;
        final AclInterfaceListDetailsReplyDump reply = aclInterfaceDump(nInput, "acl1", "acl2", "acl3");
        when(aclApi.aclInterfaceListDump(aclInterfaceRequest(IF_ID))).thenReturn(future(reply));
        assertEquals(nInput, getCustomizer().getAllIds(getWildcardedIid(IF_NAME), ctx).size());
    }

    @Override
    protected InstanceIdentifier<VppAcls> getWildcardedIid(@Nonnull final String ifName) {
        return getAclId(ifName).child(Ingress.class).child(VppAcls.class);
    }

    @Override
    protected InstanceIdentifier<VppAcls> getIid(@Nonnull final String ifName, @Nonnull final VppAclsKey key) {
        return getAclId(ifName).child(Ingress.class).child(VppAcls.class, key);
    }
}