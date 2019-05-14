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
import io.fd.hc2vpp.v3po.DisabledInterfacesManager;
import io.fd.hc2vpp.v3po.interfaces.AfPacketCustomizer;
import io.fd.hc2vpp.v3po.interfaces.AfPacketValidator;
import io.fd.hc2vpp.v3po.interfaces.EthernetCustomizer;
import io.fd.hc2vpp.v3po.interfaces.EthernetValidator;
import io.fd.hc2vpp.v3po.interfaces.GreCustomizer;
import io.fd.hc2vpp.v3po.interfaces.GreValidator;
import io.fd.hc2vpp.v3po.interfaces.InterfaceCustomizer;
import io.fd.hc2vpp.v3po.interfaces.InterfaceRoutingCustomizer;
import io.fd.hc2vpp.v3po.interfaces.InterfaceRoutingValidator;
import io.fd.hc2vpp.v3po.interfaces.InterfaceUnnumberedCustomizer;
import io.fd.hc2vpp.v3po.interfaces.InterfaceUnnumberedValidator;
import io.fd.hc2vpp.v3po.interfaces.InterfaceValidator;
import io.fd.hc2vpp.v3po.interfaces.InterfacesStatisticsCustomizer;
import io.fd.hc2vpp.v3po.interfaces.InterfacesStatisticsValidator;
import io.fd.hc2vpp.v3po.interfaces.L2Customizer;
import io.fd.hc2vpp.v3po.interfaces.L2Validator;
import io.fd.hc2vpp.v3po.interfaces.LoopbackCustomizer;
import io.fd.hc2vpp.v3po.interfaces.LoopbackValidator;
import io.fd.hc2vpp.v3po.interfaces.TapV2Customizer;
import io.fd.hc2vpp.v3po.interfaces.TapV2Validator;
import io.fd.hc2vpp.v3po.interfaces.VhostUserCustomizer;
import io.fd.hc2vpp.v3po.interfaces.VhostUserValidator;
import io.fd.hc2vpp.v3po.interfaces.VxlanCustomizer;
import io.fd.hc2vpp.v3po.interfaces.VxlanGpeCustomizer;
import io.fd.hc2vpp.v3po.interfaces.VxlanGpeValidator;
import io.fd.hc2vpp.v3po.interfaces.VxlanValidator;
import io.fd.hc2vpp.v3po.interfaces.pbb.PbbRewriteCustomizer;
import io.fd.hc2vpp.v3po.interfaces.pbb.PbbRewriteValidator;
import io.fd.hc2vpp.v3po.interfaces.span.MirroredInterfaceCustomizer;
import io.fd.hc2vpp.v3po.interfaces.span.MirroredInterfaceValidator;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceStatisticsManager;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.unnumbered.interfaces.rev180103.InterfaceUnnumberedAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.unnumbered.interfaces.rev180103.unnumbered.config.attributes.Unnumbered;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfacesStatsAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces.Statistics;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.AfPacket;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Ethernet;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Gre;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Loopback;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Span;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.TapV2;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.VhostUser;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Vxlan;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.VxlanGpe;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.span.attributes.MirroredInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.span.attributes.mirrored.interfaces.MirroredInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev161214.PbbRewriteInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev161214.interfaces._interface.PbbRewrite;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class InterfacesWriterFactory implements WriterFactory {

    public static final InstanceIdentifier<Interfaces> IFCS_ID = InstanceIdentifier.create(Interfaces.class);
    public static final InstanceIdentifier<Interface> IFC_ID = IFCS_ID.child(Interface.class);
    public static final InstanceIdentifier<VppInterfaceAugmentation> VPP_IFC_AUG_ID =
            IFC_ID.augmentation(VppInterfaceAugmentation.class);
    public static final InstanceIdentifier<L2> L2_ID = VPP_IFC_AUG_ID.child(L2.class);

    private final FutureJVppCore jvpp;
    private final NamingContext bdNamingContext;
    private final NamingContext ifcNamingContext;
    private final DisabledInterfacesManager ifcDisableContext;
    private final InterfaceStatisticsManager statisticsManager;

    @Inject
    public InterfacesWriterFactory(final FutureJVppCore vppJvppIfcDependency,
                                   @Named("bridge-domain-context") final NamingContext bridgeDomainContextDependency,
                                   @Named("interface-context") final NamingContext interfaceContextDependency,
                                   final DisabledInterfacesManager ifcDisableContext,
                                   final InterfaceStatisticsManager statisticsManager) {
        this.jvpp = vppJvppIfcDependency;
        this.bdNamingContext = bridgeDomainContextDependency;
        this.ifcNamingContext = interfaceContextDependency;
        this.ifcDisableContext = ifcDisableContext;
        this.statisticsManager = statisticsManager;
    }

    @Override
    public void init(final ModifiableWriterRegistryBuilder registry) {
        // Interfaces
        registry.add(new GenericWriter<>(IFCS_ID.augmentation(VppInterfacesStatsAugmentation.class)
                .child(Statistics.class), new InterfacesStatisticsCustomizer(statisticsManager),
                new InterfacesStatisticsValidator(statisticsManager)));
        //  Interface =
        registry.add(new GenericListWriter<>(IFC_ID, new InterfaceCustomizer(jvpp, ifcNamingContext),
                new InterfaceValidator(ifcNamingContext)));
        //   VppInterfaceAugmentation
        addVppInterfaceAgmentationWriters(IFC_ID, registry);

        addPbbAugmentationWriters(IFC_ID, registry);
    }

    private void addVppInterfaceAgmentationWriters(final InstanceIdentifier<Interface> ifcId,
                                                   final ModifiableWriterRegistryBuilder registry) {
        // VhostUser(Needs to be executed before Interface customizer) =
        final InstanceIdentifier<VhostUser> vhostId = VPP_IFC_AUG_ID.child(VhostUser.class);
        registry.addBefore(new GenericWriter<>(vhostId, new VhostUserCustomizer(jvpp, ifcNamingContext),
                new VhostUserValidator(ifcNamingContext)), ifcId);
        // AfPacket(Needs to be executed before Interface customizer) =
        final InstanceIdentifier<AfPacket> afpacketId = VPP_IFC_AUG_ID.child(AfPacket.class);
        registry.addBefore(new GenericWriter<>(afpacketId, new AfPacketCustomizer(jvpp, ifcNamingContext),
                new AfPacketValidator(ifcNamingContext)), ifcId);
        // Vxlan(Needs to be executed before Interface customizer) =
        final InstanceIdentifier<Vxlan> vxlanId = VPP_IFC_AUG_ID.child(Vxlan.class);
        registry.addBefore(new GenericWriter<>(vxlanId, new VxlanCustomizer(jvpp, ifcNamingContext, ifcDisableContext),
                        new VxlanValidator(ifcNamingContext, ifcDisableContext)),
                ifcId);
        // VxlanGpe(Needs to be executed before Interface customizer) =
        final InstanceIdentifier<VxlanGpe> vxlanGpeId = VPP_IFC_AUG_ID.child(VxlanGpe.class);
        registry.addBefore(new GenericWriter<>(vxlanGpeId,
                new VxlanGpeCustomizer(jvpp, ifcNamingContext, ifcDisableContext),
                new VxlanGpeValidator(ifcNamingContext, ifcDisableContext)), ifcId);
        // TapV2(Needs to be executed before Interface customizer) =
        final InstanceIdentifier<TapV2> tapV2Id = VPP_IFC_AUG_ID.child(TapV2.class);
        registry.addBefore(new GenericWriter<>(tapV2Id, new TapV2Customizer(jvpp, ifcNamingContext),
                new TapV2Validator(ifcNamingContext)), ifcId);
        // Loopback(Needs to be executed before Interface customizer) =
        final InstanceIdentifier<Loopback> loopbackId = VPP_IFC_AUG_ID.child(Loopback.class);
        registry.addBefore(new GenericWriter<>(loopbackId, new LoopbackCustomizer(jvpp, ifcNamingContext),
                new LoopbackValidator(ifcNamingContext)), ifcId);

        // Gre(Needs to be executed before Interface customizer) =
        final InstanceIdentifier<Gre> greId = VPP_IFC_AUG_ID.child(Gre.class);
        registry.addBefore(new GenericWriter<>(greId, new GreCustomizer(jvpp, ifcNamingContext),
                new GreValidator(ifcNamingContext)), ifcId);

        final Set<InstanceIdentifier<?>> specificIfcTypes =
                Sets.newHashSet(vhostId, afpacketId, vxlanId, vxlanGpeId, tapV2Id, loopbackId);

        // Ethernet =
        registry.add(new GenericWriter<>(VPP_IFC_AUG_ID.child(Ethernet.class),
                new EthernetCustomizer(jvpp, ifcNamingContext), new EthernetValidator(ifcNamingContext)));
        // Routing(Execute only after specific interface customizers) =
        registry.addAfter(
                new GenericWriter<>(VPP_IFC_AUG_ID.child(Routing.class),
                        new InterfaceRoutingCustomizer(jvpp, ifcNamingContext),
                        new InterfaceRoutingValidator(ifcNamingContext)),
                specificIfcTypes);
        // L2(Execute only after subinterface (and all other ifc types) =
        registry.addAfter(new GenericWriter<>(L2_ID, new L2Customizer(jvpp, ifcNamingContext, bdNamingContext),
                        new L2Validator(ifcNamingContext, bdNamingContext)),
                SubinterfaceAugmentationWriterFactory.SUB_IFC_ID);

        // Span writers
        //  Mirrored interfaces
        final InstanceIdentifier<MirroredInterface> mirroredIfcId = VPP_IFC_AUG_ID
                .child(Span.class)
                .child(MirroredInterfaces.class)
                .child(MirroredInterface.class);
        registry.addAfter(new GenericWriter<>(mirroredIfcId, new MirroredInterfaceCustomizer(jvpp, ifcNamingContext,
                        id -> id.firstKeyOf(Interface.class).getName()),
                        new MirroredInterfaceValidator(ifcNamingContext, id -> id.firstKeyOf(Interface.class).getName())),
                ifcId);

        // Unnumbered =
        final InstanceIdentifier<Unnumbered> unnumberedId =
                IFC_ID.augmentation(InterfaceUnnumberedAugmentation.class).child(Unnumbered.class);
        registry.addAfter(new GenericWriter<>(unnumberedId, new InterfaceUnnumberedCustomizer(jvpp, ifcNamingContext),
                new InterfaceUnnumberedValidator(ifcNamingContext)), ifcId);
    }

    private void addPbbAugmentationWriters(final InstanceIdentifier<Interface> ifcId,
                                           final ModifiableWriterRegistryBuilder registry) {
        final InstanceIdentifier<PbbRewrite> pbbRewriteId =
                ifcId.augmentation(PbbRewriteInterfaceAugmentation.class).child(PbbRewrite.class);

        registry.add(new GenericWriter<>(pbbRewriteId, new PbbRewriteCustomizer(jvpp, ifcNamingContext),
                new PbbRewriteValidator(ifcNamingContext)));
    }
}
