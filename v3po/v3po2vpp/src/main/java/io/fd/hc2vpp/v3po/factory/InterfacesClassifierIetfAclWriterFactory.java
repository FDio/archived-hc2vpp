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

package io.fd.hc2vpp.v3po.factory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfaces.acl.egress.EgressIetfAclWriter;
import io.fd.hc2vpp.v3po.interfaces.acl.ingress.IngressIetfAclWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.IetfAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.ietf.acl.Egress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.ietf.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classfier.acl.rev161214.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classfier.acl.rev161214.ietf.acl.base.attributes.access.lists.Acl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class InterfacesClassifierIetfAclWriterFactory implements WriterFactory {

    public static final InstanceIdentifier<Interface> IFC_ID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class);
    public static final InstanceIdentifier<VppInterfaceAugmentation> VPP_IFC_AUG_ID =
        IFC_ID.augmentation(VppInterfaceAugmentation.class);
    public static final InstanceIdentifier<IetfAcl> IETF_ACL_ID = VPP_IFC_AUG_ID.child(IetfAcl.class);
    public static final InstanceIdentifier<Ingress> INGRESS_IETF_ACL_ID = IETF_ACL_ID.child(Ingress.class);
    public static final InstanceIdentifier<Egress> EGRESS_IETF_ACL_ID = IETF_ACL_ID.child(Egress.class);

    private final IngressIetfAclWriter ingressAclWriter;
    private final EgressIetfAclWriter egressAclWriter;
    private final NamingContext ifcNamingContext;

    @Inject
    public InterfacesClassifierIetfAclWriterFactory(final IngressIetfAclWriter ingressAclWriter,
                                                    final EgressIetfAclWriter egressAclWriter,
                                                    @Named("interface-context") final NamingContext interfaceContextDependency) {
        this.ingressAclWriter = ingressAclWriter;
        this.egressAclWriter = egressAclWriter;
        this.ifcNamingContext = interfaceContextDependency;
    }

    @Override
    public void init(final ModifiableWriterRegistryBuilder registry) {
        // Ingress IETF-ACL, also handles AccessLists and Acl:
        final InstanceIdentifier<AccessLists> accessListsIdIngress =
            InstanceIdentifier.create(Ingress.class).child(AccessLists.class);
        final InstanceIdentifier<?> aclIdIngress = accessListsIdIngress.child(Acl.class);
        registry.subtreeAdd(
            Sets.newHashSet(accessListsIdIngress, aclIdIngress),
            new GenericWriter<>(INGRESS_IETF_ACL_ID,
                new io.fd.hc2vpp.v3po.interfaces.acl.ingress.IetfAclCustomizer(ingressAclWriter, ifcNamingContext)));

        // Ingress IETF-ACL, also handles AccessLists and Acl:
        final InstanceIdentifier<AccessLists> accessListsIdEgress =
            InstanceIdentifier.create(Egress.class).child(AccessLists.class);
        final InstanceIdentifier<?> aclIdEgress = accessListsIdEgress.child(Acl.class);
        registry.subtreeAdd(
            Sets.newHashSet(accessListsIdEgress, aclIdEgress),
            new GenericWriter<>(EGRESS_IETF_ACL_ID,
                new io.fd.hc2vpp.v3po.interfaces.acl.egress.IetfAclCustomizer(egressAclWriter, ifcNamingContext)));
    }
}
