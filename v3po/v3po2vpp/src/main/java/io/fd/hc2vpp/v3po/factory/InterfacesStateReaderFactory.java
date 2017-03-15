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
import io.fd.hc2vpp.v3po.interfacesstate.EthernetCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.GreCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.InterfaceCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.L2Customizer;
import io.fd.hc2vpp.v3po.interfacesstate.RoutingCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.TapCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.VhostUserCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.VxlanCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.VxlanGpeCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.pbb.PbbRewriteStateCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.span.MirroredInterfacesCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.impl.read.GenericInitReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces.state._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces.state._interface.Gre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces.state._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces.state._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces.state._interface.Span;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces.state._interface.SpanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces.state._interface.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces.state._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces.state._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces.state._interface.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.span.attributes.MirroredInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.span.attributes.mirrored.interfaces.MirroredInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.pbb.rev161214.PbbRewriteStateInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.pbb.rev161214.PbbRewriteStateInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.pbb.rev161214.interfaces.state._interface.PbbRewriteState;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class InterfacesStateReaderFactory implements ReaderFactory {

    private final NamingContext ifcNamingCtx;
    private final NamingContext bdNamingCtx;
    private final DisabledInterfacesManager ifcDisableContext;
    private final FutureJVppCore jvpp;

    static final InstanceIdentifier<InterfacesState> IFC_STATE_ID =
            InstanceIdentifier.create(InterfacesState.class);
    static final InstanceIdentifier<Interface> IFC_ID = IFC_STATE_ID.child(Interface.class);

    @Inject
    public InterfacesStateReaderFactory(final FutureJVppCore jvpp,
                                        @Named("interface-context") final NamingContext ifcNamingCtx,
                                        @Named("bridge-domain-context") final NamingContext bdNamingCtx,
                                        final DisabledInterfacesManager ifcDisableContext) {
        this.jvpp = jvpp;
        this.ifcNamingCtx = ifcNamingCtx;
        this.bdNamingCtx = bdNamingCtx;
        this.ifcDisableContext = ifcDisableContext;
    }

    @Override
    public void init(final ModifiableReaderRegistryBuilder registry) {
        // InterfacesState(Structural)
        registry.addStructuralReader(IFC_STATE_ID, InterfacesStateBuilder.class);
        //  Interface
        registry.add(new GenericInitListReader<>(IFC_ID, new InterfaceCustomizer(jvpp, ifcNamingCtx, ifcDisableContext)));

        // v3po.yang
        initVppIfcAugmentationReaders(registry, IFC_ID);

        //vpp-pbb.yang
        initPbbRewriteAugmentation(registry, IFC_ID);
    }

    private void initVppIfcAugmentationReaders(final ModifiableReaderRegistryBuilder registry,
                                               final InstanceIdentifier<Interface> ifcId) {
        //   VppInterfaceStateAugmentation
        final InstanceIdentifier<VppInterfaceStateAugmentation> vppIfcAugId =
                ifcId.augmentation(VppInterfaceStateAugmentation.class);
        registry.addStructuralReader(vppIfcAugId, VppInterfaceStateAugmentationBuilder.class);
        //    Ethernet
        registry.add(new GenericInitReader<>(vppIfcAugId.child(Ethernet.class),
            new EthernetCustomizer(jvpp, ifcNamingCtx)));
        //    Routing
        registry.add(new GenericInitReader<>(vppIfcAugId.child(Routing.class),
            new RoutingCustomizer(jvpp, ifcNamingCtx)));
        //    Tap
        registry.add(new GenericInitReader<>(vppIfcAugId.child(Tap.class), new TapCustomizer(jvpp, ifcNamingCtx)));
        //    VhostUser
        registry.add(new GenericInitReader<>(vppIfcAugId.child(VhostUser.class),
                        new VhostUserCustomizer(jvpp, ifcNamingCtx)));
        //    Vxlan
        registry.add(new GenericInitReader<>(vppIfcAugId.child(Vxlan.class), new VxlanCustomizer(jvpp, ifcNamingCtx)));
        //    VxlanGpe
        registry.add(new GenericInitReader<>(vppIfcAugId.child(VxlanGpe.class),
                        new VxlanGpeCustomizer(jvpp, ifcNamingCtx)));
        //    Gre
        registry.add(new GenericInitReader<>(vppIfcAugId.child(Gre.class), new GreCustomizer(jvpp, ifcNamingCtx)));
        //    L2
        registry.add(new GenericInitReader<>(vppIfcAugId.child(L2.class),
                        new L2Customizer(jvpp, ifcNamingCtx, bdNamingCtx)));
        // Span
        final InstanceIdentifier<Span> spanId = vppIfcAugId.child(Span.class);
        registry.addStructuralReader(spanId, SpanBuilder.class);
        //  MirroredInterfaces
        registry.subtreeAdd(
                ImmutableSet.of(InstanceIdentifier.create(MirroredInterfaces.class).child(MirroredInterface.class)),
                new GenericInitReader<>(spanId.child(MirroredInterfaces.class),
                        new MirroredInterfacesCustomizer(jvpp, ifcNamingCtx)));
    }

    private void initPbbRewriteAugmentation(final ModifiableReaderRegistryBuilder registry,
                                            final InstanceIdentifier<Interface> ifcId) {
        registry.addStructuralReader(ifcId.augmentation(PbbRewriteStateInterfaceAugmentation.class),
                PbbRewriteStateInterfaceAugmentationBuilder.class);

        registry.add(new GenericReader<>(ifcId.augmentation(PbbRewriteStateInterfaceAugmentation.class).child(
                PbbRewriteState.class), new PbbRewriteStateCustomizer(jvpp)));
    }

}
