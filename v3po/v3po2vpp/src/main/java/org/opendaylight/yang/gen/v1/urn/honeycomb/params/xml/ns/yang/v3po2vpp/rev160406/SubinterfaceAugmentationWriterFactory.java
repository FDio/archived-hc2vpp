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

package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import com.google.common.collect.Sets;
import io.fd.honeycomb.v3po.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.v3po.translate.impl.write.GenericWriter;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.RewriteCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.SubInterfaceCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.SubInterfaceL2Customizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.ip.SubInterfaceIpv4AddressCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.write.ModifiableWriterRegistry;
import io.fd.honeycomb.v3po.translate.write.WriterFactory;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.or.any.Dot1qTag;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.match.attributes.match.type.vlan.tagged.VlanTagged;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.Tags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.l2.Rewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.tags.Tag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.tag.rewrite.PushTags;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.future.FutureJVpp;

final class SubinterfaceAugmentationWriterFactory implements WriterFactory {

    private final InstanceIdentifier<Interface> ifcId;
    private final FutureJVpp jvpp;
    private final NamingContext ifcContext;
    private final NamingContext bdContext;

    public SubinterfaceAugmentationWriterFactory(
            final InstanceIdentifier<Interface> ifcId, final FutureJVpp jvpp,
            final NamingContext ifcContext, final NamingContext bdContext) {
        this.ifcId = ifcId;
        this.jvpp = jvpp;
        this.ifcContext = ifcContext;
        this.bdContext = bdContext;
    }

    @Override
    public void init(final ModifiableWriterRegistry registry) {
        final InstanceIdentifier<SubinterfaceAugmentation> subIfcAugId =
                ifcId.augmentation(SubinterfaceAugmentation.class);
        // Subinterfaces
        //  Subinterface(Handle only after all interface related stuff gets processed) =
        final InstanceIdentifier<SubInterface> subIfcId = subIfcAugId.child(SubInterfaces.class).child(SubInterface.class);
        registry.addSubtreeWriterAfter(
                // TODO this customizer covers quite a lot of complex child nodes (maybe refactor ?)
                Sets.newHashSet(
                        InstanceIdentifier.create(SubInterface.class).child(Tags.class),
                        InstanceIdentifier.create(SubInterface.class).child(Tags.class).child(Tag.class),
                        InstanceIdentifier.create(SubInterface.class).child(Tags.class).child(Tag.class).child(
                                Dot1qTag.class),
                        InstanceIdentifier.create(SubInterface.class).child(Match.class),
                        InstanceIdentifier.create(SubInterface.class).child(Match.class).child(VlanTagged.class)),
                new GenericListWriter<>(subIfcId, new SubInterfaceCustomizer(jvpp, ifcContext)),
                ifcId);
        //   L2 =
        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.L2>
                l2Id = subIfcId.child(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.L2.class);
        registry.addWriterAfter(new GenericWriter<>(l2Id, new SubInterfaceL2Customizer(jvpp, ifcContext, bdContext)),
                subIfcId);
        //    Rewrite(also handles pushTags + pushTags/dot1qtag) =
        final InstanceIdentifier<Rewrite> rewriteId = l2Id.child(Rewrite.class);
        registry.addSubtreeWriterAfter(
                Sets.newHashSet(
                        InstanceIdentifier.create(Rewrite.class).child(PushTags.class),
                        InstanceIdentifier.create(Rewrite.class).child(PushTags.class)
                                .child(org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.Dot1qTag.class)),
                new GenericWriter<>(rewriteId, new RewriteCustomizer(jvpp, ifcContext)),
                l2Id);
        //   Ipv4(handled after L2 and L2/rewrite is done) =
        final InstanceIdentifier<Address> ipv4SubifcAddressId = subIfcId.child(Ipv4.class).child(Address.class);
        registry.addWriterAfter(new GenericListWriter<>(ipv4SubifcAddressId,
                new SubInterfaceIpv4AddressCustomizer(jvpp, ifcContext)),
                rewriteId);

    }
}
