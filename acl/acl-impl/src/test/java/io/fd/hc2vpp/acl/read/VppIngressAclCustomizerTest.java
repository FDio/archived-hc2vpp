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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDetails;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDetailsReplyDump;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.IngressBuilder;

public class VppIngressAclCustomizerTest extends AbstractVppAclCustomizerTest {

    public VppIngressAclCustomizerTest() {
        super(IngressBuilder.class);
    }

    @Override
    protected VppAclCustomizer initCustomizer() {
        return new VppAclCustomizer(aclApi, interfaceContext, standardAclContext, true);
    }

    @Test
    public void testGetAllIdsNoInputAclConfigured() throws ReadFailedException {
        AclInterfaceListDetailsReplyDump reply = new AclInterfaceListDetailsReplyDump();
        final AclInterfaceListDetails details = new AclInterfaceListDetails();
        details.acls = new int[1];
        details.nInput = 0;
        reply.aclInterfaceListDetails.add(details);
        when(aclApi.aclInterfaceListDump(any())).thenReturn(future(reply));
        assertTrue(getCustomizer().getAllIds(IID, ctx).isEmpty());
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {
        AclInterfaceListDetailsReplyDump reply = new AclInterfaceListDetailsReplyDump();
        final AclInterfaceListDetails details = new AclInterfaceListDetails();
        defineMapping(mappingContext, "acl1", 1, ACL_CTX_NAME);
        defineMapping(mappingContext, "acl2", 2, ACL_CTX_NAME);
        details.acls = new int[]{1,2,3};
        details.nInput = 2;
        reply.aclInterfaceListDetails.add(details);
        when(aclApi.aclInterfaceListDump(any())).thenReturn(future(reply));
        assertEquals(details.nInput, getCustomizer().getAllIds(IID, ctx).size());
    }
}