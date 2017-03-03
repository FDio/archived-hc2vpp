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

import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.acl.write.InterfaceAclCustomizer;
import io.fd.hc2vpp.acl.write.InterfaceAclMacIpCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.Egress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAcls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.macip.acls.base.attributes.VppMacipAcl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceAclWriterFactory extends AbstractAclWriterFactory implements WriterFactory {

    static final InstanceIdentifier<Acl> ACL_IID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class)
            .augmentation(VppAclInterfaceAugmentation.class).child(Acl.class);
    private static final InstanceIdentifier<Interface> IFC_ID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class);


    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        registry.subtreeAddAfter(aclHandledChildren(InstanceIdentifier.create(Acl.class)),
            new GenericWriter<>(ACL_IID,
                new InterfaceAclCustomizer(futureAclFacade, interfaceContext, standardAclContext)), IFC_ID);

        registry.addAfter(new GenericWriter<>(ACL_IID.child(Ingress.class).child(VppMacipAcl.class),
            new InterfaceAclMacIpCustomizer(futureAclFacade, macIpAClContext, interfaceContext)), IFC_ID);
    }

    static Set<InstanceIdentifier<?>> aclHandledChildren(final InstanceIdentifier<Acl> parentId) {
        return ImmutableSet.of(parentId.child(Ingress.class),
            parentId.child(Ingress.class).child(VppAcls.class),
            parentId.child(Egress.class),
            parentId.child(Egress.class).child(VppAcls.class));
    }
}
