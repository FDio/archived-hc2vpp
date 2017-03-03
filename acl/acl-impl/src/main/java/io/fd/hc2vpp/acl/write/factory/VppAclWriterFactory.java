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

package io.fd.hc2vpp.acl.write.factory;

import static io.fd.hc2vpp.acl.write.factory.InterfaceAclWriterFactory.ACL_IID;
import static io.fd.hc2vpp.acl.write.factory.InterfaceAclWriterFactory.aclHandledChildren;

import io.fd.hc2vpp.acl.util.factory.AclFactory;
import io.fd.hc2vpp.acl.write.VppAclCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppAclWriterFactory extends AbstractAclWriterFactory implements WriterFactory, AclFactory {

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        final InstanceIdentifier<AccessLists> rootNode = InstanceIdentifier.create(AccessLists.class);

        registry.subtreeAddBefore(vppAclChildren(InstanceIdentifier.create(Acl.class)),
            new GenericListWriter<>(rootNode.child(Acl.class),
                new VppAclCustomizer(futureAclFacade, standardAclContext, macIpAClContext)),
            aclHandledChildren(ACL_IID));
    }
}
