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

package io.fd.hc2vpp.acl.read.factory;

import static io.fd.hc2vpp.acl.read.factory.InterfaceAclReaderFactory.ACL_EGRESS_IID;
import static io.fd.hc2vpp.acl.read.factory.InterfaceAclReaderFactory.ACL_INGRESS_IID;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.acl.AclModule;
import io.fd.hc2vpp.acl.read.AclCustomizer;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.acl.util.factory.AclFactory;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AclReaderFactory implements ReaderFactory, AclFactory {

    @Inject
    private FutureJVppAclFacade futureAclFacade;

    @Inject
    @Named(AclModule.STANDARD_ACL_CONTEXT_NAME)
    private AclContextManager standardAclContext;

    @Inject
    @Named(AclModule.MAC_IP_ACL_CONTEXT_NAME)
    private AclContextManager macIpAClContext;

    private static final InstanceIdentifier<AccessLists> ACLS_ID = InstanceIdentifier.create(AccessLists.class);
    private static final InstanceIdentifier<Acl> ACL_ID = ACLS_ID.child(Acl.class);

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        registry.addStructuralReader(ACLS_ID, AccessListsBuilder.class);

        // TODO(HONEYCOMB-331): initializer is not registered correctly when subtreeAddBefore is used,
        // enable after fixing infra issue:

//        registry.subtreeAddBefore(vppAclChildren(InstanceIdentifier.create(Acl.class)),
//            new GenericInitListReader<>(ACL_ID,
//                new AclCustomizer(futureAclFacade, standardAclContext, macIpAClContext)),
//            ImmutableSet.of(ACL_INGRESS_IID, ACL_EGRESS_IID));

        registry.addBefore(new GenericInitListReader<>(ACL_ID,
                new AclCustomizer(futureAclFacade, standardAclContext, macIpAClContext)),
            ImmutableSet.of(ACL_INGRESS_IID, ACL_EGRESS_IID));
    }
}
