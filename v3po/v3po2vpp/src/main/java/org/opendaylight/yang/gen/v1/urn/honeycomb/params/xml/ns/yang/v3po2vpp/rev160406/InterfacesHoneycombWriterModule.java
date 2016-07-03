package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.impl.TraversalType;
import io.fd.honeycomb.v3po.translate.impl.write.CompositeChildWriter;
import io.fd.honeycomb.v3po.translate.impl.write.CompositeListWriter;
import io.fd.honeycomb.v3po.translate.impl.write.CompositeRootWriter;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.util.write.CloseableWriter;
import io.fd.honeycomb.v3po.translate.util.write.NoopWriterCustomizer;
import io.fd.honeycomb.v3po.translate.util.write.ReflexiveAugmentWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.*;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.ip.Ipv4AddressCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.ip.Ipv4Customizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.ip.Ipv4NeighbourCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.ip.Ipv6Customizer;
import io.fd.honeycomb.v3po.translate.write.ChildWriter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.*;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;

import java.util.ArrayList;
import java.util.List;

public class InterfacesHoneycombWriterModule extends
    org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.AbstractInterfacesHoneycombWriterModule {
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

        final List<ChildWriter<? extends Augmentation<Interface>>> ifcAugmentations = Lists.newArrayList();
        ifcAugmentations.add(getVppIfcAugmentationWriter());
        ifcAugmentations.add(getInterface1AugmentationWriter());
        ifcAugmentations.add(
            SubinterfaceAugmentationWriterFactory.createInstance(getVppJvppIfcDependency(), getInterfaceContextDependency(),
                getBridgeDomainContextDependency()));

        final ChildWriter<Interface> interfaceWriter = new CompositeListWriter<>(Interface.class,
            RWUtils.emptyChildWriterList(),
            ifcAugmentations,
            new InterfaceCustomizer(getVppJvppIfcDependency(), getInterfaceContextDependency()),
            // It's important that this customizer is handled in a postorder way, because you first have to handle child nodes
            // e.g. Vxlan before setting other interface or vppInterfaceAugmentation leaves
            TraversalType.POSTORDER);

        final List<ChildWriter<? extends ChildOf<Interfaces>>> childWriters = new ArrayList<>();
        childWriters.add(interfaceWriter);

        // FIXME if we just return the root writer and cfg subsystem takes care to set it into reader registry,
        // we loose the ordering information for root writers
        // Or can we rely to the order in which readers are configured ?
        return new CloseableWriter<>(new CompositeRootWriter<>(Interfaces.class,
            childWriters, new NoopWriterCustomizer<>()));
    }

    private ChildWriter<? extends Augmentation<Interface>> getInterface1AugmentationWriter() {

        final ChildWriter<Neighbor> neighborWriter = new CompositeListWriter<>(Neighbor.class,
                new Ipv4NeighbourCustomizer(getVppJvppIfcDependency(),getInterfaceContextDependency()));

        final ChildWriter<Address> addressWriter = new CompositeListWriter<>(Address.class,
            new Ipv4AddressCustomizer(getVppJvppIfcDependency(), getInterfaceContextDependency()));

        final ChildWriter<Ipv4> ipv4Writer = new CompositeChildWriter<>(Ipv4.class,
                ImmutableList.of(neighborWriter,addressWriter),
                new Ipv4Customizer(getVppJvppIfcDependency(),getInterfaceContextDependency()));
        final ChildWriter<Ipv6> ipv6Writer = new CompositeChildWriter<>(Ipv6.class,
            new Ipv6Customizer(getVppJvppIfcDependency()));

        final List<ChildWriter<? extends ChildOf<Interface1>>> interface1ChildWriters = Lists.newArrayList();
        interface1ChildWriters.add(ipv4Writer);
        interface1ChildWriters.add(ipv6Writer);

        return new CompositeChildWriter<>(Interface1.class,
            interface1ChildWriters, new ReflexiveAugmentWriterCustomizer<>());
    }

    private ChildWriter<VppInterfaceAugmentation> getVppIfcAugmentationWriter() {

        final ChildWriter<Ethernet> ethernetWriter = new CompositeChildWriter<>(Ethernet.class,
            new EthernetCustomizer(getVppJvppIfcDependency()));

        final ChildWriter<Routing> routingWriter = new CompositeChildWriter<>(Routing.class,
            new RoutingCustomizer(getVppJvppIfcDependency(), getInterfaceContextDependency()));

        final ChildWriter<Vxlan> vxlanWriter = new CompositeChildWriter<>(Vxlan.class,
            new VxlanCustomizer(getVppJvppIfcDependency(), getInterfaceContextDependency()));

        final ChildWriter<VxlanGpe> vxlanGpeWriter = new CompositeChildWriter<>(VxlanGpe.class,
            new VxlanGpeCustomizer(getVppJvppIfcDependency(), getInterfaceContextDependency()));

        final ChildWriter<VhostUser> vhostUserWriter = new CompositeChildWriter<>(VhostUser.class,
            new VhostUserCustomizer(getVppJvppIfcDependency(), getInterfaceContextDependency()));

        final ChildWriter<Tap> tapWriter = new CompositeChildWriter<>(Tap.class,
            new TapCustomizer(getVppJvppIfcDependency(), getInterfaceContextDependency()));

        final ChildWriter<L2> l2Writer = new CompositeChildWriter<>(L2.class,
            new L2Customizer(getVppJvppIfcDependency(), getInterfaceContextDependency(),
                getBridgeDomainContextDependency())
        );

        final List<ChildWriter<? extends ChildOf<VppInterfaceAugmentation>>> vppIfcChildWriters = Lists.newArrayList();
        vppIfcChildWriters.add(vhostUserWriter);
        vppIfcChildWriters.add(vxlanWriter);
        vppIfcChildWriters.add(vxlanGpeWriter);
        vppIfcChildWriters.add(tapWriter);
        vppIfcChildWriters.add(ethernetWriter);
        vppIfcChildWriters.add(l2Writer);
        vppIfcChildWriters.add(routingWriter);

        return new CompositeChildWriter<>(VppInterfaceAugmentation.class,
            vppIfcChildWriters,
            RWUtils.emptyAugWriterList(),
            new ReflexiveAugmentWriterCustomizer<>());
    }
}
