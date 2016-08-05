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

import com.google.common.collect.Sets;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.v3po.interfacesstate.RewriteCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.SubInterfaceAclCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.SubInterfaceCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.SubInterfaceL2Customizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.ip.SubInterfaceIpv4AddressCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.or.any.Dot1qTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip6Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.L2Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.SubInterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.match.attributes.match.type.vlan.tagged.VlanTagged;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.Tags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.l2.Rewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.tags.Tag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.tag.rewrite.PushTags;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.future.FutureJVppCore;

final class SubinterfaceStateAugmentationReaderFactory implements ReaderFactory {

    private final FutureJVppCore jvpp;
    private final NamingContext ifcCtx;
    private final NamingContext bdCtx;
    private final NamingContext classifyCtx;

    SubinterfaceStateAugmentationReaderFactory(final FutureJVppCore jvpp, final NamingContext ifcCtx,
                                               final NamingContext bdCtx, final NamingContext classifyCtx) {
        this.jvpp = jvpp;
        this.ifcCtx = ifcCtx;
        this.bdCtx = bdCtx;
        this.classifyCtx = classifyCtx;
    }

    @Override
    public void init(final ModifiableReaderRegistryBuilder registry) {
        // SubinterfaceStateAugmentation(Structural)
        final InstanceIdentifier<SubinterfaceStateAugmentation> subIfcAugId =
                InterfacesStateReaderFactory.IFC_ID.augmentation(SubinterfaceStateAugmentation.class);
        registry.addStructuralReader(subIfcAugId, SubinterfaceStateAugmentationBuilder.class);
        //  SubInterfaces(Structural)
        final InstanceIdentifier<SubInterfaces> subIfcsId = subIfcAugId.child(SubInterfaces.class);
        registry.addStructuralReader(subIfcsId, SubInterfacesBuilder.class);
        //   SubInterface(Subtree)
        final InstanceIdentifier<SubInterface> subIfcId = subIfcsId.child(SubInterface.class);
        registry.subtreeAdd(Sets.newHashSet(
                InstanceIdentifier.create(SubInterface.class).child(Tags.class),
                InstanceIdentifier.create(SubInterface.class).child(Tags.class).child(Tag.class),
                InstanceIdentifier.create(SubInterface.class).child(Tags.class).child(Tag.class).child(Dot1qTag.class),
                InstanceIdentifier.create(SubInterface.class).child(Match.class),
                InstanceIdentifier.create(SubInterface.class).child(Match.class).child(VlanTagged.class)),
                new GenericListReader<>(subIfcId, new SubInterfaceCustomizer(jvpp, ifcCtx)));
        //    L2
        final InstanceIdentifier<L2> l2Id = subIfcId.child(L2.class);
        registry.add(new GenericReader<>(l2Id, new SubInterfaceL2Customizer(jvpp, ifcCtx, bdCtx)));
        //     Rewrite(Subtree)
        registry.subtreeAdd(Sets.newHashSet(
                InstanceIdentifier.create(Rewrite.class).child(PushTags.class),
                InstanceIdentifier.create(Rewrite.class).child(PushTags.class)
                        .child(org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.Dot1qTag.class)),
                new GenericReader<>(l2Id.child(Rewrite.class), new RewriteCustomizer(jvpp, ifcCtx)));
        //    Ipv4(Structural)
        final InstanceIdentifier<Ipv4> ipv4Id = subIfcId.child(Ipv4.class);
        registry.addStructuralReader(ipv4Id, Ipv4Builder.class);
        //     Address
        registry.add(new GenericListReader<>(ipv4Id.child(Address.class), new SubInterfaceIpv4AddressCustomizer(jvpp, ifcCtx)));
        //    Acl(Subtree)
        final InstanceIdentifier<Acl> aclIdRelative = InstanceIdentifier.create(Acl.class);
        registry.subtreeAdd(
                Sets.newHashSet(aclIdRelative.child(L2Acl.class), aclIdRelative.child(Ip4Acl.class), aclIdRelative.child(Ip6Acl.class)),
                new GenericReader<>(subIfcId.child(Acl.class), new SubInterfaceAclCustomizer(jvpp, ifcCtx, classifyCtx)));
    }
}
