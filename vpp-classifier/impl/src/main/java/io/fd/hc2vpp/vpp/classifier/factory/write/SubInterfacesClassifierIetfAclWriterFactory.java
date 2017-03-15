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

package io.fd.hc2vpp.vpp.classifier.factory.write;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.factory.InterfacesWriterFactory;
import io.fd.hc2vpp.vpp.classifier.write.acl.egress.EgressIetfAclWriter;
import io.fd.hc2vpp.vpp.classifier.write.acl.ingress.IngressIetfAclWriter;
import io.fd.hc2vpp.vpp.classifier.write.acl.ingress.SubInterfaceIetfAclCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev170315.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev170315.ietf.acl.base.attributes.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev170315.vpp.acl.attributes.IetfAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev170315.vpp.acl.attributes.ietf.acl.Egress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev170315.vpp.acl.attributes.ietf.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.subinterface.acl.rev170315.VppSubinterfaceAclAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170315.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170315.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170315.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class SubInterfacesClassifierIetfAclWriterFactory implements WriterFactory {

    public static final InstanceIdentifier<SubinterfaceAugmentation> SUB_IFC_AUG_ID =
        InterfacesWriterFactory.IFC_ID.augmentation(SubinterfaceAugmentation.class);
    public static final InstanceIdentifier<SubInterface> SUB_IFC_ID =
        SUB_IFC_AUG_ID.child(SubInterfaces.class).child(SubInterface.class);

    public static final InstanceIdentifier<VppSubinterfaceAclAugmentation> SUB_IFC_ACL_AUG_ID =
            SUB_IFC_ID.augmentation(VppSubinterfaceAclAugmentation.class);

    public static final InstanceIdentifier<IetfAcl> SUBIF_IETF_ACL_ID = SUB_IFC_ACL_AUG_ID.child(IetfAcl.class);
    public static final InstanceIdentifier<Ingress> SUBIF_INGRESS_IETF_ACL_ID = SUBIF_IETF_ACL_ID.child(Ingress.class);
    public static final InstanceIdentifier<Egress> SUBIF_EGRESS_IETF_ACL_ID = SUBIF_IETF_ACL_ID.child(Egress.class);

    private final IngressIetfAclWriter ingressAclWriter;
    private final EgressIetfAclWriter egressAclWriter;
    private final NamingContext ifcContext;

    @Inject
    public SubInterfacesClassifierIetfAclWriterFactory(final IngressIetfAclWriter ingressAclWriter,
                                                       final EgressIetfAclWriter egressAclWriter,
                                                       @Named("interface-context") final NamingContext ifcContext) {
        this.ingressAclWriter = ingressAclWriter;
        this.egressAclWriter = egressAclWriter;
        this.ifcContext = ifcContext;
    }

    @Override
    public void init(final ModifiableWriterRegistryBuilder registry) {
        // Ingress IETF-ACL, also handles AccessLists and Acl:
        final InstanceIdentifier<AccessLists> accessListsIdIngress =
            InstanceIdentifier.create(Ingress.class).child(AccessLists.class);
        final InstanceIdentifier<?> aclIdIngress = accessListsIdIngress.child(Acl.class);
        registry.subtreeAdd(
            Sets.newHashSet(accessListsIdIngress, aclIdIngress),
            new GenericWriter<>(SUBIF_INGRESS_IETF_ACL_ID,
                new SubInterfaceIetfAclCustomizer(ingressAclWriter, ifcContext)));

        // Egress IETF-ACL, also handles AccessLists and Acl:
        final InstanceIdentifier<AccessLists> accessListsIdEgress =
            InstanceIdentifier.create(Egress.class).child(AccessLists.class);
        final InstanceIdentifier<?> aclIdEgress = accessListsIdEgress.child(Acl.class);
        registry.subtreeAdd(
            Sets.newHashSet(accessListsIdEgress, aclIdEgress),
            new GenericWriter<>(SUBIF_EGRESS_IETF_ACL_ID,
                new io.fd.hc2vpp.vpp.classifier.write.acl.egress.SubInterfaceIetfAclCustomizer(
                    egressAclWriter, ifcContext)));
    }
}
