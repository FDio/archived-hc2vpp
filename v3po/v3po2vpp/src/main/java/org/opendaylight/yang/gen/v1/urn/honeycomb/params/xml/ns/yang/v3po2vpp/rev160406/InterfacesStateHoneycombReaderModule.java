package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import static io.fd.honeycomb.v3po.translate.util.RWUtils.emptyAugReaderList;
import static io.fd.honeycomb.v3po.translate.util.RWUtils.emptyChildReaderList;
import static io.fd.honeycomb.v3po.translate.util.RWUtils.singletonChildReaderList;

import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeChildReader;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeListReader;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeRootReader;
import io.fd.honeycomb.v3po.translate.read.ChildReader;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.util.read.CloseableReader;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveAugmentReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveRootReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.EthernetCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.InterfaceCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.L2Customizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.RewriteCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.SubInterfaceCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.SubInterfaceL2Customizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.TapCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.VhostUserCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.VxlanCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.VxlanGpeCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.ip.Ipv4AddressCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.ip.Ipv4Customizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.ip.Ipv6Customizer;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.SubInterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.l2.Rewrite;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;

public class InterfacesStateHoneycombReaderModule extends
        org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.AbstractInterfacesStateHoneycombReaderModule {
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
        final List<ChildReader<? extends Augmentation<Interface>>>
                interfaceAugReaders = new ArrayList<>();
        interfaceAugReaders.add(getVppInterfaceStateAugmentationReader());
        interfaceAugReaders.add(getInterface1AugmentationReader());
        interfaceAugReaders.add(getSubinterfaceStateAugmentationReader());

        final CompositeListReader<Interface, InterfaceKey, InterfaceBuilder> interfaceReader =
                new CompositeListReader<>(Interface.class,
                        emptyChildReaderList(),
                        interfaceAugReaders,
                        new InterfaceCustomizer(getVppJvppDependency(), getInterfaceContextIfcStateDependency()));

        return new CloseableReader<>(new CompositeRootReader<>(
                InterfacesState.class,
                singletonChildReaderList(interfaceReader),
                emptyAugReaderList(),
                new ReflexiveRootReaderCustomizer<>(InterfacesStateBuilder.class)));
    }

    private ChildReader<? extends Augmentation<Interface>> getInterface1AugmentationReader() {

        final ChildReader<Address> addressReader = new CompositeListReader<>(Address.class,
                new Ipv4AddressCustomizer(getVppJvppDependency()));

        final ChildReader<? extends ChildOf<Interface2>> ipv4Reader = new CompositeChildReader<>(Ipv4.class,
                RWUtils.singletonChildReaderList(addressReader),
                new Ipv4Customizer(getVppJvppDependency(), getInterfaceContextIfcStateDependency()));
        final ChildReader<? extends ChildOf<Interface2>> ipv6Reader = new CompositeChildReader<>(Ipv6.class,
                new Ipv6Customizer(getVppJvppDependency(), getInterfaceContextIfcStateDependency()));

        final List<ChildReader<? extends ChildOf<Interface2>>> interface1ChildWriters = Lists.newArrayList();
        interface1ChildWriters.add(ipv4Reader);
        interface1ChildWriters.add(ipv6Reader);

        return new CompositeChildReader<>(Interface2.class, interface1ChildWriters,
                new ReflexiveAugmentReaderCustomizer<>(Interface2Builder.class, Interface2.class));
    }


    private ChildReader<? extends Augmentation<Interface>> getVppInterfaceStateAugmentationReader() {

        final ChildReader<? extends ChildOf<VppInterfaceStateAugmentation>> ethernetReader =
                new CompositeChildReader<>(Ethernet.class,
                        new EthernetCustomizer(getVppJvppDependency(), getInterfaceContextIfcStateDependency()));

        final ChildReader<? extends ChildOf<VppInterfaceStateAugmentation>> tapReader =
                new CompositeChildReader<>(Tap.class,
                        new TapCustomizer(getVppJvppDependency(), getInterfaceContextIfcStateDependency()));

        final ChildReader<? extends ChildOf<VppInterfaceStateAugmentation>> vhostUserReader =
                new CompositeChildReader<>(VhostUser.class,
                        new VhostUserCustomizer(getVppJvppDependency(), getInterfaceContextIfcStateDependency()));

        final ChildReader<? extends ChildOf<VppInterfaceStateAugmentation>> vxlanReader =
                new CompositeChildReader<>(Vxlan.class,
                        new VxlanCustomizer(getVppJvppDependency(), getInterfaceContextIfcStateDependency()));

        final ChildReader<? extends ChildOf<VppInterfaceStateAugmentation>> vxlanGpeReader =
                new CompositeChildReader<>(VxlanGpe.class,
                        new VxlanGpeCustomizer(getVppJvppDependency(), getInterfaceContextIfcStateDependency()));

        final ChildReader<? extends ChildOf<VppInterfaceStateAugmentation>> l2Reader =
                new CompositeChildReader<>(L2.class,
                        new L2Customizer(getVppJvppDependency(), getInterfaceContextIfcStateDependency(), getBridgeDomainContextIfcStateDependency()));

        final List<ChildReader<? extends ChildOf<VppInterfaceStateAugmentation>>> childReaders = Lists.newArrayList();
        childReaders.add(ethernetReader);
        childReaders.add(tapReader);
        childReaders.add(vhostUserReader);
        childReaders.add(vxlanReader);
        childReaders.add(vxlanGpeReader);
        childReaders.add(l2Reader);

        final ChildReader<VppInterfaceStateAugmentation> vppInterfaceStateAugmentationChildReader =
                new CompositeChildReader<>(VppInterfaceStateAugmentation.class,
                        childReaders,
                        new ReflexiveAugmentReaderCustomizer<>(VppInterfaceStateAugmentationBuilder.class,
                                VppInterfaceStateAugmentation.class));
        return vppInterfaceStateAugmentationChildReader;
    }

    private ChildReader<SubinterfaceStateAugmentation> getSubinterfaceStateAugmentationReader() {

        final ChildReader<Rewrite> rewriteReader =
        new CompositeChildReader<>(Rewrite.class,
                new RewriteCustomizer(getVppJvppDependency(), getInterfaceContextIfcStateDependency()));

        final ChildReader<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.L2> l2Reader =
                new CompositeChildReader<>(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.L2.class,
                        singletonChildReaderList(rewriteReader),
                        new SubInterfaceL2Customizer(getVppJvppDependency(), getInterfaceContextIfcStateDependency(), getBridgeDomainContextIfcStateDependency()));

        List<ChildReader<? extends ChildOf<SubInterface>>> childReaders = new ArrayList<>();
        childReaders.add((ChildReader) l2Reader); // TODO can get rid of that cast?

        final CompositeListReader<SubInterface, SubInterfaceKey, SubInterfaceBuilder> subInterfaceReader =
                new CompositeListReader<>(SubInterface.class, childReaders, new SubInterfaceCustomizer(getVppJvppDependency(),
                        getInterfaceContextIfcStateDependency()));

        final ChildReader<SubInterfaces> subInterfacesReader = new CompositeChildReader<>(
                SubInterfaces.class,
                RWUtils.singletonChildReaderList(subInterfaceReader),
                new ReflexiveChildReaderCustomizer<>(SubInterfacesBuilder.class));

        final ChildReader<SubinterfaceStateAugmentation> subinterfaceStateAugmentationReader =
                new CompositeChildReader<>(SubinterfaceStateAugmentation.class,
                        singletonChildReaderList(subInterfacesReader),
                        new ReflexiveAugmentReaderCustomizer<>(SubinterfaceStateAugmentationBuilder.class,
                                SubinterfaceStateAugmentation.class));
        return subinterfaceStateAugmentationReader;
    }
}
