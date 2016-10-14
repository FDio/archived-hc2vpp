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

package io.fd.honeycomb.translate.v3po;

import static io.fd.honeycomb.translate.v3po.VppClassifierHoneycombWriterFactory.CLASSIFY_SESSION_ID;
import static io.fd.honeycomb.translate.v3po.VppClassifierHoneycombWriterFactory.CLASSIFY_TABLE_ID;

import com.google.common.collect.Sets;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.v3po.interfaces.RewriteCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.SubInterfaceCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.SubInterfaceL2Customizer;
import io.fd.honeycomb.translate.v3po.interfaces.acl.egress.EgressIetfAclWriter;
import io.fd.honeycomb.translate.v3po.interfaces.acl.ingress.IngressIetfAclWriter;
import io.fd.honeycomb.translate.v3po.interfaces.acl.ingress.SubInterfaceAclCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.acl.ingress.SubInterfaceIetfAclCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.ip.SubInterfaceIpv4AddressCustomizer;
import io.fd.honeycomb.translate.v3po.vppclassifier.VppClassifierContextManager;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.or.any.Dot1qTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.base.attributes.Ip6Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.base.attributes.L2Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.match.attributes.match.type.vlan.tagged.VlanTagged;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.IetfAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.Tags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.l2.Rewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.tags.Tag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.tag.rewrite.PushTags;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class SubinterfaceAugmentationWriterFactory implements WriterFactory {

    private final FutureJVppCore jvpp;
    private final IngressIetfAclWriter ingressAclWriter;
    private final EgressIetfAclWriter egressAclWriter;
    private final NamingContext ifcContext;
    private final NamingContext bdContext;
    private final VppClassifierContextManager classifyTableContext;

    public static final InstanceIdentifier<SubinterfaceAugmentation> SUB_IFC_AUG_ID =
            InterfacesWriterFactory.IFC_ID.augmentation(SubinterfaceAugmentation.class);
    public static final InstanceIdentifier<SubInterface> SUB_IFC_ID =
            SUB_IFC_AUG_ID.child(SubInterfaces.class).child(SubInterface.class);
    public static final InstanceIdentifier<L2> L2_ID = SUB_IFC_ID.child(
            L2.class);
    public static final InstanceIdentifier<Acl> SUBIF_ACL_ID = SUB_IFC_ID.child(Acl.class);
    public static final InstanceIdentifier<Ingress> SUBIF_INGRESS_ACL_ID = SUBIF_ACL_ID.child(Ingress.class);
    public static final InstanceIdentifier<IetfAcl> SUBIF_IETF_ACL_ID = SUB_IFC_ID.child(IetfAcl.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.ietf.acl.Ingress> SUBIF_INGRESS_IETF_ACL_ID = SUBIF_IETF_ACL_ID.child(
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.ietf.acl.Ingress.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.ietf.acl.Egress> SUBIF_EGRESS_IETF_ACL_ID = SUBIF_IETF_ACL_ID.child(
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.ietf.acl.Egress.class);

    public SubinterfaceAugmentationWriterFactory(final FutureJVppCore jvpp,
                                                 final IngressIetfAclWriter ingressAclWriter,
                                                 final EgressIetfAclWriter egressAclWriter,
            final NamingContext ifcContext, final NamingContext bdContext, final VppClassifierContextManager classifyTableContext) {
        this.jvpp = jvpp;
        this.ingressAclWriter = ingressAclWriter;
        this.egressAclWriter = egressAclWriter;
        this.ifcContext = ifcContext;
        this.bdContext = bdContext;
        this.classifyTableContext = classifyTableContext;
    }

    @Override
    public void init(final ModifiableWriterRegistryBuilder registry) {
        // Subinterfaces
        //  Subinterface(Handle only after all interface related stuff gets processed) =
        registry.subtreeAddAfter(
                // TODO HONEYCOMB-188 this customizer covers quite a lot of complex child nodes (maybe refactor ?)
                Sets.newHashSet(
                        InstanceIdentifier.create(SubInterface.class).child(Tags.class),
                        InstanceIdentifier.create(SubInterface.class).child(Tags.class).child(Tag.class),
                        InstanceIdentifier.create(SubInterface.class).child(Tags.class).child(Tag.class).child(
                                Dot1qTag.class),
                        InstanceIdentifier.create(SubInterface.class).child(Match.class),
                        InstanceIdentifier.create(SubInterface.class).child(Match.class).child(VlanTagged.class)),
                new GenericListWriter<>(SUB_IFC_ID, new SubInterfaceCustomizer(jvpp, ifcContext)),
                InterfacesWriterFactory.IFC_ID);
        //   L2 =
        registry.addAfter(new GenericWriter<>(L2_ID, new SubInterfaceL2Customizer(jvpp, ifcContext, bdContext)),
                SUB_IFC_ID);
        //    Rewrite(also handles pushTags + pushTags/dot1qtag) =
        final InstanceIdentifier<Rewrite> rewriteId = L2_ID.child(Rewrite.class);
        registry.subtreeAddAfter(
                Sets.newHashSet(
                        InstanceIdentifier.create(Rewrite.class).child(PushTags.class),
                        InstanceIdentifier.create(Rewrite.class).child(PushTags.class)
                                .child(org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.Dot1qTag.class)),
                new GenericWriter<>(rewriteId, new RewriteCustomizer(jvpp, ifcContext)),
                L2_ID);
        //   Ipv4(handled after L2 and L2/rewrite is done) =
        final InstanceIdentifier<Address> ipv4SubifcAddressId = SUB_IFC_ID.child(Ipv4.class).child(Address.class);
        registry.addAfter(new GenericListWriter<>(ipv4SubifcAddressId,
                new SubInterfaceIpv4AddressCustomizer(jvpp, ifcContext)),
                rewriteId);

        // Ingress (execute after classify table and session writers)
        // also handles L2Acl, Ip4Acl and Ip6Acl:
        final InstanceIdentifier<Ingress> aclId = InstanceIdentifier.create(Ingress.class);
        registry
            .subtreeAddAfter(
                Sets.newHashSet(aclId.child(L2Acl.class), aclId.child(Ip4Acl.class), aclId.child(Ip6Acl.class)),
                new GenericWriter<>(SUBIF_INGRESS_ACL_ID, new SubInterfaceAclCustomizer(jvpp, ifcContext, classifyTableContext)),
                Sets.newHashSet(CLASSIFY_TABLE_ID, CLASSIFY_SESSION_ID));

        // Ingress IETF-ACL, also handles AccessLists and Acl:
        final InstanceIdentifier<AccessLists> accessListsIdIngress = InstanceIdentifier.create(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.ietf.acl.Ingress.class)
            .child(AccessLists.class);
        final InstanceIdentifier<?> aclIdIngress = accessListsIdIngress.child(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.access.lists.Acl.class);
        registry.subtreeAdd(
            Sets.newHashSet(accessListsIdIngress, aclIdIngress),
            new GenericWriter<>(SUBIF_INGRESS_IETF_ACL_ID, new SubInterfaceIetfAclCustomizer(ingressAclWriter, ifcContext)));

        // Egress IETF-ACL, also handles AccessLists and Acl:
        final InstanceIdentifier<AccessLists> accessListsIdEgress = InstanceIdentifier.create(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.ietf.acl.Egress.class)
            .child(AccessLists.class);
        final InstanceIdentifier<?> aclIdEgress = accessListsIdEgress.child(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.access.lists.Acl.class);
        registry.subtreeAdd(
            Sets.newHashSet(accessListsIdEgress, aclIdEgress),
            new GenericWriter<>(SUBIF_EGRESS_IETF_ACL_ID, new io.fd.honeycomb.translate.v3po.interfaces.acl.egress.SubInterfaceIetfAclCustomizer(
                egressAclWriter, ifcContext)));
    }
}
