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
import io.fd.hc2vpp.v3po.interfacesstate.RewriteCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.SubInterfaceCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.SubInterfaceL2Customizer;
import io.fd.hc2vpp.v3po.interfacesstate.SubInterfaceRoutingCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.hc2vpp.v3po.interfacesstate.span.SubInterfaceMirroredInterfacesCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.impl.read.GenericInitReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.subinterface.span.rev170607.VppSubinterfaceSpanStateAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.subinterface.span.rev170607.VppSubinterfaceSpanStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.subinterface.span.rev170607.interfaces.state._interface.sub.interfaces.sub._interface.SpanState;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.subinterface.span.rev170607.interfaces.state._interface.sub.interfaces.sub._interface.SpanStateBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.span.state.attributes.MirroredInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.SubinterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.SubInterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.match.attributes.match.type.vlan.tagged.VlanTagged;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.rewrite.attributes.Rewrite;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.base.attributes.Match;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.base.attributes.Tags;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.base.attributes.tags.Tag;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.l2.state.attributes.L2;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.routing.attributes.Routing;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.tag.rewrite.PushTags;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.or.any.Dot1qTag;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class SubinterfaceStateAugmentationReaderFactory implements ReaderFactory {

    private final FutureJVppCore jvpp;
    private final NamingContext ifcCtx;
    private final NamingContext bdCtx;
    private final InterfaceCacheDumpManager ifaceDumpManager;

    @Inject
    public SubinterfaceStateAugmentationReaderFactory(final FutureJVppCore jvpp,
                                                      @Named("interface-context") final NamingContext ifcCtx,
                                                      @Named("bridge-domain-context") final NamingContext bdCtx,
                                                      final InterfaceCacheDumpManager ifaceDumpManager) {
        this.jvpp = jvpp;
        this.ifcCtx = ifcCtx;
        this.bdCtx = bdCtx;
        this.ifaceDumpManager = ifaceDumpManager;
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
                new GenericInitListReader<>(subIfcId,
                        new SubInterfaceCustomizer(jvpp, ifcCtx, ifaceDumpManager)));
        //    L2
        final InstanceIdentifier<L2> l2Id = subIfcId.child(L2.class);
        registry.add(new GenericInitReader<>(l2Id,
                new SubInterfaceL2Customizer(jvpp, ifcCtx, bdCtx, ifaceDumpManager)));
        //     Rewrite(Subtree)
        registry.subtreeAdd(Sets.newHashSet(
                InstanceIdentifier.create(Rewrite.class).child(PushTags.class),
                InstanceIdentifier.create(Rewrite.class).child(PushTags.class)
                        .child(
                                org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.Dot1qTag.class)),
                new GenericReader<>(l2Id.child(Rewrite.class),
                        new RewriteCustomizer(ifaceDumpManager)));
        final InstanceIdentifier<Routing> routingId = subIfcId.child(Routing.class);
        registry.add(new GenericReader<>(routingId, new SubInterfaceRoutingCustomizer(jvpp, ifcCtx)));

        final InstanceIdentifier<VppSubinterfaceSpanStateAugmentation> spanStateAugId =
                subIfcId.augmentation(VppSubinterfaceSpanStateAugmentation.class);
        registry.addStructuralReader(spanStateAugId, VppSubinterfaceSpanStateAugmentationBuilder.class);

        final InstanceIdentifier<SpanState> spanStateId = spanStateAugId
                .child(SpanState.class);
        registry.addStructuralReader(spanStateId, SpanStateBuilder.class);

        final InstanceIdentifier<MirroredInterfaces> mirroredInterfacesId = spanStateId.child(MirroredInterfaces.class);
        registry.add(new GenericInitReader<>(mirroredInterfacesId,
                new SubInterfaceMirroredInterfacesCustomizer(jvpp, ifcCtx)));
    }
}
