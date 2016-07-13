package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import static org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.VppClassifierHoneycombWriterModule.CLASSIFY_SESSION_ID;
import static org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.VppClassifierHoneycombWriterModule.CLASSIFY_TABLE_ID;

import com.google.common.collect.Sets;
import io.fd.honeycomb.v3po.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.v3po.translate.impl.write.GenericWriter;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.AclCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.EthernetCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.InterfaceCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.L2Customizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.RoutingCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.TapCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.VhostUserCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.VxlanCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.VxlanGpeCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.ip.Ipv4AddressCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.ip.Ipv4Customizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.ip.Ipv4NeighbourCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.ip.Ipv6Customizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.honeycomb.v3po.translate.write.WriterFactory;
import java.util.Set;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip6Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.L2Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VxlanGpe;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.future.FutureJVpp;

public class InterfacesHoneycombWriterModule extends
    org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.AbstractInterfacesHoneycombWriterModule {

    // TODO split configuration and translation code into 2 or more bundles

    public static final InstanceIdentifier<Interface> IFC_ID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class);
    public static final InstanceIdentifier<VppInterfaceAugmentation> VPP_IFC_AUG_ID =
        IFC_ID.augmentation(VppInterfaceAugmentation.class);
    public static final InstanceIdentifier<L2> L2_ID = VPP_IFC_AUG_ID.child(L2.class);
    public static final InstanceIdentifier<Acl> ACL_ID = VPP_IFC_AUG_ID.child(Acl.class);

    public InterfacesHoneycombWriterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                           org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public InterfacesHoneycombWriterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                           org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                           org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.InterfacesHoneycombWriterModule oldModule,
                                           java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new InterfacesWriterFactory(getVppJvppIfcDependency(),
            getBridgeDomainContextDependency(),
            getInterfaceContextDependency(),
            getClassifyTableContextDependency());
    }


    private static class InterfacesWriterFactory implements WriterFactory, AutoCloseable {

        private final FutureJVpp jvpp;
        private final NamingContext bdContext;
        private final NamingContext ifcContext;
        private final NamingContext classifyTableContext;

        InterfacesWriterFactory(final FutureJVpp vppJvppIfcDependency,
                                final NamingContext bridgeDomainContextDependency,
                                final NamingContext interfaceContextDependency,
                                final NamingContext classifyTableContextDependency) {
            this.jvpp = vppJvppIfcDependency;
            this.bdContext = bridgeDomainContextDependency;
            this.ifcContext = interfaceContextDependency;
            this.classifyTableContext = classifyTableContextDependency;
        }

        @Override
        public void close() throws Exception {
            // unregister is not supported in ModifiableWriterRegistry (not really needed though)
        }

        @Override
        public void init(final ModifiableWriterRegistryBuilder registry) {
            // Interfaces
            //  Interface =
            registry.add(new GenericListWriter<>(IFC_ID, new InterfaceCustomizer(jvpp, ifcContext)));
            //   VppInterfaceAugmentation
            addVppInterfaceAgmentationWriters(IFC_ID, registry);
            //   Interface1 (ietf-ip augmentation)
            addInterface1AugmentationWriters(IFC_ID, registry);
            //   SubinterfaceAugmentation TODO make dedicated module for subIfc writer factory
            new SubinterfaceAugmentationWriterFactory(jvpp, ifcContext, bdContext, classifyTableContext).init(registry);
        }

        private void addInterface1AugmentationWriters(final InstanceIdentifier<Interface> ifcId,
                                                      final ModifiableWriterRegistryBuilder registry) {
            final InstanceIdentifier<Interface1> ifc1AugId = ifcId.augmentation(Interface1.class);
            // Ipv6(after interface) TODO unfinished customizer =
            registry.addAfter(new GenericWriter<>(ifc1AugId.child(Ipv6.class), new Ipv6Customizer(jvpp)),
                    ifcId);
            // Ipv4(after interface)
            final InstanceIdentifier<Ipv4> ipv4Id = ifc1AugId.child(Ipv4.class);
            registry.addAfter(new GenericWriter<>(ipv4Id, new Ipv4Customizer(jvpp, ifcContext)),
                    ifcId);
            //  Address(after Ipv4) =
            final InstanceIdentifier<Address> ipv4AddressId = ipv4Id.child(Address.class);
            registry.addAfter(new GenericListWriter<>(ipv4AddressId, new Ipv4AddressCustomizer(jvpp, ifcContext)),
                    ipv4Id);
            //  Neighbor(after ipv4Address)
            registry.addAfter(new GenericListWriter<>(ipv4Id.child(Neighbor.class), new Ipv4NeighbourCustomizer(jvpp, ifcContext)),
                    ipv4AddressId);
        }

        private void addVppInterfaceAgmentationWriters(final InstanceIdentifier<Interface> ifcId,
                                                       final ModifiableWriterRegistryBuilder registry) {
            // VhostUser(Needs to be executed before Interface customizer) =
            final InstanceIdentifier<VhostUser> vhostId = VPP_IFC_AUG_ID.child(VhostUser.class);
            registry.addBefore(new GenericWriter<>(vhostId, new VhostUserCustomizer(jvpp, ifcContext)),
                    ifcId);
            // Vxlan(Needs to be executed before Interface customizer) =
            final InstanceIdentifier<Vxlan> vxlanId = VPP_IFC_AUG_ID.child(Vxlan.class);
            registry.addBefore(new GenericWriter<>(vxlanId, new VxlanCustomizer(jvpp, ifcContext)),
                    ifcId);
            // VxlanGpe(Needs to be executed before Interface customizer) =
            final InstanceIdentifier<VxlanGpe> vxlanGpeId = VPP_IFC_AUG_ID.child(VxlanGpe.class);
            registry.addBefore(new GenericWriter<>(vxlanGpeId, new VxlanGpeCustomizer(jvpp, ifcContext)),
                    ifcId);
            // Tap(Needs to be executed before Interface customizer) =
            final InstanceIdentifier<Tap> tapId = VPP_IFC_AUG_ID.child(Tap.class);
            registry.addBefore(new GenericWriter<>(tapId, new TapCustomizer(jvpp, ifcContext)),
                    ifcId);

            final Set<InstanceIdentifier<?>> specificIfcTypes = Sets.newHashSet(vhostId, vxlanGpeId, vxlanGpeId, tapId);

            // Ethernet(No dependency, customizer not finished TODO) =
            registry.add(new GenericWriter<>(VPP_IFC_AUG_ID.child(Ethernet.class), new EthernetCustomizer(jvpp)));
            // Routing(Execute only after specific interface customizers) =
            registry.addAfter(
                    new GenericWriter<>(VPP_IFC_AUG_ID.child(Routing.class), new RoutingCustomizer(jvpp, ifcContext)),
                    specificIfcTypes);
            // Routing(Execute only after specific interface customizers) =
            registry.addAfter(new GenericWriter<>(L2_ID, new L2Customizer(jvpp, ifcContext, bdContext)),
                    specificIfcTypes);

            // ACL (execute after classify table and session writers)
            // also handles L2Acl, Ip4Acl and Ip6Acl:
            final InstanceIdentifier<Acl> aclId = InstanceIdentifier.create(Acl.class);
            registry
                .subtreeAddAfter(
                    Sets.newHashSet(aclId.child(L2Acl.class), aclId.child(Ip4Acl.class), aclId.child(Ip6Acl.class)),
                    new GenericWriter<>(ACL_ID, new AclCustomizer(jvpp, ifcContext, classifyTableContext)),
                    Sets.newHashSet(CLASSIFY_TABLE_ID, CLASSIFY_SESSION_ID));
        }

    }

}
