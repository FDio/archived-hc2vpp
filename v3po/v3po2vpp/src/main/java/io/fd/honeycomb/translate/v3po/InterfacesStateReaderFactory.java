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
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.v3po.interfacesstate.EthernetCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.GreCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.acl.ingress.AclCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.InterfaceCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.L2Customizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.ProxyArpCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.TapCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.VhostUserCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.VxlanCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.VxlanGpeCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.ip.Ipv4AddressCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.ip.Ipv4Customizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.ip.Ipv4NeighbourCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.ip.Ipv6Customizer;
import io.fd.honeycomb.translate.v3po.vppclassifier.VppClassifierContextManager;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip6Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.L2Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Gre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.ProxyArp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.acl.Ingress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class InterfacesStateReaderFactory implements ReaderFactory {

    private final NamingContext ifcNamingCtx;
    private final NamingContext bdNamingCtx;
    private final VppClassifierContextManager classifyContext;
    private final DisabledInterfacesManager ifcDisableContext;
    private final FutureJVppCore jvpp;

    static final InstanceIdentifier<InterfacesState> IFC_STATE_ID =
        InstanceIdentifier.create(InterfacesState.class);
    static final InstanceIdentifier<Interface> IFC_ID = IFC_STATE_ID.child(Interface.class);

    @Inject
    public InterfacesStateReaderFactory(final FutureJVppCore jvpp,
                                        @Named("interface-context") final NamingContext ifcNamingCtx,
                                        @Named("bridge-domain-context") final NamingContext bdNamingCtx,
                                        @Named("classify-table-context") final VppClassifierContextManager classifyContext,
                                        final DisabledInterfacesManager ifcDisableContext) {
        this.jvpp = jvpp;
        this.ifcNamingCtx = ifcNamingCtx;
        this.bdNamingCtx = bdNamingCtx;
        this.classifyContext = classifyContext;
        this.ifcDisableContext = ifcDisableContext;
    }

    @Override
    public void init(final ModifiableReaderRegistryBuilder registry) {
        // InterfacesState(Structural)
        registry.addStructuralReader(IFC_STATE_ID, InterfacesStateBuilder.class);
        //  Interface
        registry.add(new GenericListReader<>(IFC_ID, new InterfaceCustomizer(jvpp, ifcNamingCtx, ifcDisableContext)));

        // v3po.yang
        initVppIfcAugmentationReaders(registry, IFC_ID);
        // ietf-ip.yang
        initInterface2AugmentationReaders(registry, IFC_ID);
        // vpp-vlan.yang
        new SubinterfaceStateAugmentationReaderFactory(jvpp, ifcNamingCtx, bdNamingCtx, classifyContext).init(registry);
    }

    private void initInterface2AugmentationReaders(final ModifiableReaderRegistryBuilder registry,
                                                   final InstanceIdentifier<Interface> ifcId) {
        //   Interface2Augmentation(Structural)
        final InstanceIdentifier<Interface2> ifc2AugId = ifcId.augmentation(Interface2.class);
        registry.addStructuralReader(ifc2AugId, Interface2Builder.class);
        //    Ipv4
        final InstanceIdentifier<Ipv4> ipv4Id = ifc2AugId.child(Ipv4.class);
        registry.add(new GenericReader<>(ipv4Id, new Ipv4Customizer(jvpp)));
        //     Address
        final InstanceIdentifier<Address> ipv4AddrId = ipv4Id.child(Address.class);
        registry.add(new GenericListReader<>(ipv4AddrId, new Ipv4AddressCustomizer(jvpp, ifcNamingCtx)));
        //     Neighbor
        final InstanceIdentifier<Neighbor> neighborId = ipv4Id.child(Neighbor.class);
        registry.add(new GenericListReader<>(neighborId, new Ipv4NeighbourCustomizer(jvpp)));
        //    Ipv6
        final InstanceIdentifier<Ipv6> ipv6Id = ifc2AugId.child(Ipv6.class);
        registry.add(new GenericReader<>(ipv6Id, new Ipv6Customizer(jvpp, ifcNamingCtx)));
    }

    private void initVppIfcAugmentationReaders(final ModifiableReaderRegistryBuilder registry,
                                               final InstanceIdentifier<Interface> ifcId) {
        //   VppInterfaceStateAugmentation
        final InstanceIdentifier<VppInterfaceStateAugmentation> vppIfcAugId =
            ifcId.augmentation(VppInterfaceStateAugmentation.class);
        registry.addStructuralReader(vppIfcAugId, VppInterfaceStateAugmentationBuilder.class);
        //    Ethernet
        registry
            .add(new GenericReader<>(vppIfcAugId.child(Ethernet.class), new EthernetCustomizer(jvpp, ifcNamingCtx)));
        //    Tap
        registry.add(new GenericReader<>(vppIfcAugId.child(Tap.class), new TapCustomizer(jvpp, ifcNamingCtx)));
        //    VhostUser
        registry
            .add(new GenericReader<>(vppIfcAugId.child(VhostUser.class), new VhostUserCustomizer(jvpp, ifcNamingCtx)));
        //    Vxlan
        registry.add(new GenericReader<>(vppIfcAugId.child(Vxlan.class), new VxlanCustomizer(jvpp, ifcNamingCtx)));
        //    VxlanGpe
        registry
            .add(new GenericReader<>(vppIfcAugId.child(VxlanGpe.class), new VxlanGpeCustomizer(jvpp, ifcNamingCtx)));
        //    Gre
        registry.add(new GenericReader<>(vppIfcAugId.child(Gre.class), new GreCustomizer(jvpp, ifcNamingCtx)));
        //    L2
        registry
            .add(new GenericReader<>(vppIfcAugId.child(L2.class), new L2Customizer(jvpp, ifcNamingCtx, bdNamingCtx)));
        //    Acl(Structural)
        final InstanceIdentifier<Acl> aclIid = vppIfcAugId.child(Acl.class);
        registry.addStructuralReader(aclIid, AclBuilder.class);
        //    Ingress(Subtree)
        final InstanceIdentifier<Ingress> ingressIdRelative = InstanceIdentifier.create(Ingress.class);
        registry.subtreeAdd(
            Sets.newHashSet(ingressIdRelative.child(L2Acl.class), ingressIdRelative.child(Ip4Acl.class),
                ingressIdRelative.child(Ip6Acl.class)),
            new GenericReader<>(aclIid.child(Ingress.class),
                new AclCustomizer(jvpp, ifcNamingCtx,
                    classifyContext)));
        //   Proxy ARP
        registry.add(new GenericReader<>(vppIfcAugId.child(ProxyArp.class), new ProxyArpCustomizer(jvpp,
            ifcNamingCtx)));
    }
}
