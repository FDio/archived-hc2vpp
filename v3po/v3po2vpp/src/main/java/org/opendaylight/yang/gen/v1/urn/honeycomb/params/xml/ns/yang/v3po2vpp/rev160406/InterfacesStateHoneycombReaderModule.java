package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import com.google.common.collect.Sets;
import io.fd.honeycomb.v3po.translate.impl.read.GenericListReader;
import io.fd.honeycomb.v3po.translate.impl.read.GenericReader;
import io.fd.honeycomb.v3po.translate.read.ReaderFactory;
import io.fd.honeycomb.v3po.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.AclCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.EthernetCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.InterfaceCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.L2Customizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.TapCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.VhostUserCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.VxlanCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.VxlanGpeCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.ip.Ipv4AddressCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.ip.Ipv4Customizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.ip.Ipv4NeighbourCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.ip.Ipv6Customizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VxlanGpe;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.future.FutureJVpp;

public class InterfacesStateHoneycombReaderModule extends
        org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.AbstractInterfacesStateHoneycombReaderModule {

    public static final InstanceIdentifier<InterfacesState> IFC_STATE_ID = InstanceIdentifier.create(InterfacesState.class);
    static final InstanceIdentifier<Interface> IFC_ID = IFC_STATE_ID.child(Interface.class);

    public InterfacesStateHoneycombReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                                org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public InterfacesStateHoneycombReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                                org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                                org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.InterfacesStateHoneycombReaderModule oldModule,
                                                java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new VppStateReaderFactory(getVppJvppDependency(),
                getInterfaceContextIfcStateDependency(),
                getBridgeDomainContextIfcStateDependency(),
                getClassifyTableContextDependency());
    }

    private static final class VppStateReaderFactory implements ReaderFactory, AutoCloseable {

        private NamingContext ifcCtx;
        private NamingContext bdCtx;
        private NamingContext classifyCtx;
        private FutureJVpp jvpp;

        VppStateReaderFactory(final FutureJVpp jvpp,
                              final NamingContext ifcCtx,
                              final NamingContext bdCtx,
                              final NamingContext classifyCtx) {
            this.jvpp = jvpp;
            this.ifcCtx = ifcCtx;
            this.bdCtx = bdCtx;
            this.classifyCtx = classifyCtx;
        }

        @Override
        public void init(final ModifiableReaderRegistryBuilder registry) {
            // InterfacesState(Structural)
            registry.addStructuralReader(IFC_STATE_ID, InterfacesStateBuilder.class);
            //  Interface
            registry.add(new GenericListReader<>(IFC_ID, new InterfaceCustomizer(jvpp, ifcCtx)));

            // v3po.yang
            initVppIfcAugmentationReaders(registry, IFC_ID);
            // ietf-ip.yang
            initInterface2AugmentationReaders(registry, IFC_ID);
            // vpp-vlan.yang
            new SubinterfaceStateAugmentationReaderFactory(jvpp, ifcCtx, bdCtx, classifyCtx).init(registry);
        }

        private void initInterface2AugmentationReaders(final ModifiableReaderRegistryBuilder registry,
                                                       final InstanceIdentifier<Interface> ifcId) {
            //   Interface2Augmentation(Structural)
            final InstanceIdentifier<Interface2> ifc2AugId = ifcId.augmentation(Interface2.class);
            registry.addStructuralReader(ifc2AugId, Interface2Builder.class);
            //    Ipv4
            // TODO unfinished customizer
            final InstanceIdentifier<Ipv4> ipv4Id = ifc2AugId.child(Ipv4.class);
            registry.add(new GenericReader<>(ipv4Id, new Ipv4Customizer(jvpp)));
            //     Address
            final InstanceIdentifier<Address> ipv4AddrId = ipv4Id.child(Address.class);
            registry.add(new GenericListReader<>(ipv4AddrId, new Ipv4AddressCustomizer(jvpp, ifcCtx)));
            //     Neighbor
            final InstanceIdentifier<Neighbor> neighborId = ipv4Id.child(Neighbor.class);
            registry.add(new GenericListReader<>(neighborId, new Ipv4NeighbourCustomizer(jvpp)));
            //    Ipv6
            // TODO unfinished customizer
            final InstanceIdentifier<Ipv6> ipv6Id = ifc2AugId.child(Ipv6.class);
            registry.add(new GenericReader<>(ipv6Id, new Ipv6Customizer(jvpp, ifcCtx)));
        }

        private void initVppIfcAugmentationReaders(final ModifiableReaderRegistryBuilder registry,
                                                   final InstanceIdentifier<Interface> ifcId) {
            //   VppInterfaceStateAugmentation
            final InstanceIdentifier<VppInterfaceStateAugmentation> vppIfcAugId = ifcId.augmentation(VppInterfaceStateAugmentation.class);
            registry.addStructuralReader(vppIfcAugId, VppInterfaceStateAugmentationBuilder.class);
            //    Ethernet
            registry.add(new GenericReader<>(vppIfcAugId.child(Ethernet.class), new EthernetCustomizer(jvpp, ifcCtx)));
            //    Tap
            registry.add(new GenericReader<>(vppIfcAugId.child(Tap.class), new TapCustomizer(jvpp, ifcCtx)));
            //    VhostUser
            registry.add(new GenericReader<>(vppIfcAugId.child(VhostUser.class), new VhostUserCustomizer(jvpp, ifcCtx)));
            //    Vxlan
            registry.add(new GenericReader<>(vppIfcAugId.child(Vxlan.class), new VxlanCustomizer(jvpp, ifcCtx)));
            //    VxlanGpe
            registry.add(new GenericReader<>(vppIfcAugId.child(VxlanGpe.class), new VxlanGpeCustomizer(jvpp, ifcCtx)));
            //    L2
            registry.add(new GenericReader<>(vppIfcAugId.child(L2.class), new L2Customizer(jvpp, ifcCtx, bdCtx)));
            //    Acl(Subtree)
            final InstanceIdentifier<Acl> aclIdRelative = InstanceIdentifier.create(Acl.class);
            registry.subtreeAdd(
                    Sets.newHashSet(aclIdRelative.child(L2Acl.class), aclIdRelative.child(Ip4Acl.class), aclIdRelative.child(Ip6Acl.class)),
                    new GenericReader<>(vppIfcAugId.child(Acl.class), new AclCustomizer(jvpp, ifcCtx, classifyCtx)));

        }

        @Override
        public void close() throws Exception {
            // unregister not supported
        }
    }
}
