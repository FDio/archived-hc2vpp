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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.DisabledInterfacesManager;
import io.fd.hc2vpp.v3po.read.AfPacketCustomizer;
import io.fd.hc2vpp.v3po.read.EthernetCustomizer;
import io.fd.hc2vpp.v3po.read.GreCustomizer;
import io.fd.hc2vpp.v3po.read.InterfaceCustomizer;
import io.fd.hc2vpp.v3po.read.InterfaceRoutingCustomizer;
import io.fd.hc2vpp.v3po.read.InterfaceStatisticsCustomizer;
import io.fd.hc2vpp.v3po.read.L2Customizer;
import io.fd.hc2vpp.v3po.read.LoopbackCustomizer;
import io.fd.hc2vpp.v3po.read.TapV2Customizer;
import io.fd.hc2vpp.v3po.read.VhostUserCustomizer;
import io.fd.hc2vpp.v3po.read.VxlanCustomizer;
import io.fd.hc2vpp.v3po.read.VxlanGpeCustomizer;
import io.fd.hc2vpp.v3po.read.cache.InterfaceCacheDumpManager;
import io.fd.hc2vpp.v3po.read.cache.InterfaceStatisticsManager;
import io.fd.hc2vpp.v3po.read.pbb.PbbRewriteCustomizer;
import io.fd.hc2vpp.v3po.read.span.InterfaceMirroredInterfacesCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.impl.read.GenericInitReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import io.fd.jvpp.stats.future.FutureJVppStatsFacade;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.AfPacket;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Ethernet;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Gre;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Loopback;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Span;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.SpanBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.TapV2;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.VhostUser;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Vxlan;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.VxlanGpe;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.span.attributes.MirroredInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.span.attributes.mirrored.interfaces.MirroredInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev190527.PbbRewriteInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev190527.PbbRewriteInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev190527.interfaces._interface.PbbRewrite;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces._interface.Statistics;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class InterfacesReaderFactory implements ReaderFactory {

    private final FutureJVppStatsFacade jvppStats;
    private final NamingContext ifcNamingCtx;
    private final NamingContext bdNamingCtx;
    private final DisabledInterfacesManager ifcDisableContext;
    private final InterfaceCacheDumpManager ifaceDumpManager;
    private final FutureJVppCore jvpp;
    private final InterfaceStatisticsManager statisticsManager;

    static final InstanceIdentifier<Interfaces> IFC_STATE_ID =
            InstanceIdentifier.create(Interfaces.class);
    static final InstanceIdentifier<Interface> IFC_ID = IFC_STATE_ID.child(Interface.class);

    @Inject
    public InterfacesReaderFactory(final FutureJVppCore jvpp,
                                   final FutureJVppStatsFacade jvppStats,
                                   @Named("interface-context") final NamingContext ifcNamingCtx,
                                   @Named("bridge-domain-context") final NamingContext bdNamingCtx,
                                   final DisabledInterfacesManager ifcDisableContext,
                                   final InterfaceCacheDumpManager ifaceDumpManager,
                                   final InterfaceStatisticsManager statisticsManager) {
        this.jvpp = jvpp;
        this.jvppStats = jvppStats;
        this.ifcNamingCtx = ifcNamingCtx;
        this.bdNamingCtx = bdNamingCtx;
        this.ifcDisableContext = ifcDisableContext;
        this.ifaceDumpManager = ifaceDumpManager;
        this.statisticsManager = statisticsManager;
    }

    @Override
    public void init(final ModifiableReaderRegistryBuilder registry) {
        // Interfaces(Structural)
        registry.addStructuralReader(IFC_STATE_ID, InterfacesBuilder.class);
        //  Interface
        registry.add(new GenericInitListReader<>(IFC_ID,
                new InterfaceCustomizer(ifcNamingCtx, ifcDisableContext, ifaceDumpManager)));

        // Interface Statistics
        registry.add(new GenericReader<>(IFC_ID.child(Statistics.class),
                new InterfaceStatisticsCustomizer(ifcNamingCtx, jvppStats, statisticsManager)));
        // v3po.yang
        initVppIfcAugmentationReaders(registry, IFC_ID);

        //vpp-pbb.yang
        initPbbRewriteAugmentation(registry, IFC_ID);
    }

    private void initVppIfcAugmentationReaders(final ModifiableReaderRegistryBuilder registry,
                                               final InstanceIdentifier<Interface> ifcId) {
        //   VppInterfaceAugmentation
        final InstanceIdentifier<VppInterfaceAugmentation> vppIfcAugId =
                ifcId.augmentation(VppInterfaceAugmentation.class);
        registry.addStructuralReader(vppIfcAugId, VppInterfaceAugmentationBuilder.class);
        //    Ethernet
        registry.add(new GenericInitReader<>(vppIfcAugId.child(Ethernet.class),
                new EthernetCustomizer(ifaceDumpManager)));
        //    Loopback
        registry.add(new GenericInitReader<>(vppIfcAugId.child(Loopback.class),
                new LoopbackCustomizer(ifaceDumpManager)));
        //    Routing
        registry.add(new GenericInitReader<>(vppIfcAugId.child(Routing.class),
                new InterfaceRoutingCustomizer(jvpp, ifcNamingCtx)));
        //    TapV2
        registry.add(new GenericInitReader<>(vppIfcAugId.child(TapV2.class),
                new TapV2Customizer(jvpp, ifcNamingCtx, ifaceDumpManager)));
        //    VhostUser
        registry.add(new GenericInitReader<>(vppIfcAugId.child(VhostUser.class),
                new VhostUserCustomizer(jvpp, ifcNamingCtx, ifaceDumpManager)));
        //    AfPacket
        registry.add(new GenericInitReader<>(vppIfcAugId.child(AfPacket.class),
                new AfPacketCustomizer(jvpp, ifcNamingCtx, ifaceDumpManager)));
        //    Vxlan
        registry.add(new GenericInitReader<>(vppIfcAugId.child(Vxlan.class),
                new VxlanCustomizer(jvpp, ifcNamingCtx, ifaceDumpManager)));
        //    VxlanGpe
        registry.add(new GenericInitReader<>(vppIfcAugId.child(VxlanGpe.class),
                new VxlanGpeCustomizer(jvpp, ifcNamingCtx, ifaceDumpManager)));
        //    Gre
        registry.add(new GenericInitReader<>(vppIfcAugId.child(Gre.class),
                new GreCustomizer(jvpp, ifcNamingCtx, ifaceDumpManager)));
        //    L2
        registry.add(new GenericInitReader<>(vppIfcAugId.child(L2.class),
                new L2Customizer(jvpp, ifcNamingCtx, bdNamingCtx, ifaceDumpManager)));
        // Span
        final InstanceIdentifier<Span> spanId = vppIfcAugId.child(Span.class);
        registry.addStructuralReader(spanId, SpanBuilder.class);
        //  MirroredInterfaces
        registry.subtreeAdd(
                ImmutableSet.of(InstanceIdentifier.create(MirroredInterfaces.class).child(MirroredInterface.class)),
                new GenericInitReader<>(spanId.child(MirroredInterfaces.class),
                        new InterfaceMirroredInterfacesCustomizer(jvpp, ifcNamingCtx)));
    }

    private void initPbbRewriteAugmentation(final ModifiableReaderRegistryBuilder registry,
                                            final InstanceIdentifier<Interface> ifcId) {
        registry.addStructuralReader(ifcId.augmentation(PbbRewriteInterfaceAugmentation.class),
                PbbRewriteInterfaceAugmentationBuilder.class);

        registry.add(new GenericReader<>(ifcId.augmentation(PbbRewriteInterfaceAugmentation.class).child(
                PbbRewrite.class), new PbbRewriteCustomizer(jvpp)));
    }
}
