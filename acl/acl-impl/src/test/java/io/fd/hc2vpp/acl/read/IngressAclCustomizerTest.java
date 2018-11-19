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

import static io.fd.vpp.jvpp.Assertions.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDetailsReplyDump;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.Ingress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.AclSets;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.AclSetsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSetKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IngressAclCustomizerTest extends AbstractAclCustomizerTest {

    public IngressAclCustomizerTest() {
        super(AclSetsBuilder.class);
    }

    @Override
    protected IngressAclCustomizer initCustomizer() {
        return new IngressAclCustomizer(aclApi, interfaceContext, standardAclContext, macIpAclContext);
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
    protected InstanceIdentifier<AclSet> getWildcardedIid(@Nonnull final String ifName) {
        return getAclId(ifName).child(Ingress.class).child(AclSets.class).child(AclSet.class);
    }

    @Override
    protected InstanceIdentifier<AclSet> getIid(@Nonnull final String ifName, @Nonnull final AclSetKey key) {
        return getAclId(ifName).child(Ingress.class).child(AclSets.class).child(AclSet.class, key);
    }
}