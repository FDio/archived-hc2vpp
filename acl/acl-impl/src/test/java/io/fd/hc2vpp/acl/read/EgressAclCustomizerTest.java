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
import io.fd.jvpp.acl.dto.AclInterfaceListDetailsReplyDump;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.Egress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.AclSets;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.AclSetsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSetKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class EgressAclCustomizerTest extends AbstractAclCustomizerTest {

    public EgressAclCustomizerTest() {
        super(AclSetsBuilder.class);
    }

    @Override
    protected EgressAclCustomizer initCustomizer() {
        return new EgressAclCustomizer(aclApi, interfaceContext, standardAclContext);
    }

    @Test
    public void testGetAllIdsNoOutputAclConfigured() throws ReadFailedException {
        final AclInterfaceListDetailsReplyDump reply = aclInterfaceDump((byte) 1, "acl1");
        when(aclApi.aclInterfaceListDump(any())).thenReturn(future(reply));
        assertTrue(getCustomizer().getAllIds(getWildcardedIid(IF_NAME), ctx).isEmpty());
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {
        final AclInterfaceListDetailsReplyDump reply = aclInterfaceDump((byte) 2, "acl1", "acl2", "acl3");
        when(aclApi.aclInterfaceListDump(any())).thenReturn(future(reply));
        assertEquals(1, getCustomizer().getAllIds(getWildcardedIid(IF_NAME), ctx).size());
    }

    @Override
    protected InstanceIdentifier<AclSet> getWildcardedIid(@Nonnull final String ifName) {
        return getAclId(ifName).child(Egress.class).child(AclSets.class).child(AclSet.class);
    }

    protected KeyedInstanceIdentifier<AclSet, AclSetKey> getIid(@Nonnull final String ifName,
                                                                @Nonnull final AclSetKey key) {
        return getAclId(ifName).child(Egress.class).child(AclSets.class).child(AclSet.class, key);
    }
}