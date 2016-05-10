package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;


import static io.fd.honeycomb.v3po.translate.util.RWUtils.emptyAugReaderList;
import static io.fd.honeycomb.v3po.translate.util.RWUtils.emptyChildReaderList;
import static io.fd.honeycomb.v3po.translate.util.RWUtils.singletonAugReaderList;
import static io.fd.honeycomb.v3po.translate.util.RWUtils.singletonChildReaderList;

import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeChildReader;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeListReader;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeRootReader;
import io.fd.honeycomb.v3po.translate.read.ChildReader;
import io.fd.honeycomb.v3po.translate.util.read.CloseableReader;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveAugmentReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveRootReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.EthernetCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.InterfaceCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.TapCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.VhostUserCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.VxlanCustomizer;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Vxlan;
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

        final List<ChildReader<? extends ChildOf<VppInterfaceStateAugmentation>>> childReaders = Lists.newArrayList();
        childReaders.add(ethernetReader);
        childReaders.add(tapReader);
        childReaders.add(vhostUserReader);
        childReaders.add(vxlanReader);

        final ChildReader<VppInterfaceStateAugmentation> vppInterfaceStateAugmentationChildReader =
            new CompositeChildReader<>(VppInterfaceStateAugmentation.class,
                childReaders,
                new ReflexiveAugmentReaderCustomizer<>(VppInterfaceStateAugmentationBuilder.class,
                    VppInterfaceStateAugmentation.class));

        final CompositeListReader<Interface, InterfaceKey, InterfaceBuilder> interfaceReader =
            new CompositeListReader<>(Interface.class,
                emptyChildReaderList(),
                singletonAugReaderList(vppInterfaceStateAugmentationChildReader),
                new InterfaceCustomizer(getVppJvppDependency(), getInterfaceContextIfcStateDependency()));

        return new CloseableReader<>(new CompositeRootReader<>(
            InterfacesState.class,
            singletonChildReaderList(interfaceReader),
            emptyAugReaderList(),
            new ReflexiveRootReaderCustomizer<>(InterfacesStateBuilder.class)));
    }
}
