package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.impl.TraversalType;
import io.fd.honeycomb.v3po.translate.impl.write.CompositeChildWriter;
import io.fd.honeycomb.v3po.translate.impl.write.CompositeListWriter;
import io.fd.honeycomb.v3po.translate.impl.write.CompositeRootWriter;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.util.write.CloseableWriter;
import io.fd.honeycomb.v3po.translate.util.write.NoopWriterCustomizer;
import io.fd.honeycomb.v3po.translate.util.write.ReflexiveAugmentWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.EthernetCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.InterfaceCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.L2Customizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.RoutingCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.SubInterfaceCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.TapCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.VhostUserCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.VlanTagRewriteCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.VxlanCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.ip.Ipv4Customizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.ip.Ipv6Customizer;
import io.fd.honeycomb.v3po.translate.write.ChildWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.l2.VlanTagRewrite;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;

public class InterfacesHoneycombWriterModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.AbstractInterfacesHoneycombWriterModule {
    public InterfacesHoneycombWriterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public InterfacesHoneycombWriterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.InterfacesHoneycombWriterModule oldModule, java.lang.AutoCloseable oldInstance) {
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
        final ChildWriter<Ipv4> ipv4Writer = new CompositeChildWriter<>(Ipv4.class,
            new Ipv4Customizer(getVppJvppIfcDependency(), getInterfaceContextDependency()));
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

        final ChildWriter<VhostUser> vhostUserWriter = new CompositeChildWriter<>(VhostUser.class,
                new VhostUserCustomizer(getVppJvppIfcDependency(), getInterfaceContextDependency()));

        final ChildWriter<Tap> tapWriter = new CompositeChildWriter<>(Tap.class,
                new TapCustomizer(getVppJvppIfcDependency(), getInterfaceContextDependency()));

        final ChildWriter<SubInterface> subIfWriter = new CompositeChildWriter<>(SubInterface.class,
                new SubInterfaceCustomizer(getVppJvppIfcDependency(), getInterfaceContextDependency()));

        final ChildWriter<VlanTagRewrite> vlanTagWriter = new CompositeChildWriter<>(VlanTagRewrite.class,
                new VlanTagRewriteCustomizer(getVppJvppIfcDependency(), getInterfaceContextDependency()));

        final List<ChildWriter<? extends ChildOf<L2>>> l2ChildWriters = Collections.singletonList(vlanTagWriter);
        final ChildWriter<L2> l2Writer = new CompositeChildWriter<>(L2.class,
                l2ChildWriters,
                RWUtils.emptyAugWriterList(),
                new L2Customizer(getVppJvppIfcDependency(), getInterfaceContextDependency(), getBridgeDomainContextDependency())
                );

        final List<ChildWriter<? extends ChildOf<VppInterfaceAugmentation>>> vppIfcChildWriters = Lists.newArrayList();
        vppIfcChildWriters.add(vhostUserWriter);
        vppIfcChildWriters.add(vxlanWriter);
        vppIfcChildWriters.add(tapWriter);
        vppIfcChildWriters.add(ethernetWriter);
        vppIfcChildWriters.add(subIfWriter);
        vppIfcChildWriters.add(l2Writer);
        vppIfcChildWriters.add(routingWriter);

        return new CompositeChildWriter<>(VppInterfaceAugmentation.class,
            vppIfcChildWriters,
            RWUtils.emptyAugWriterList(),
            new ReflexiveAugmentWriterCustomizer<>());
    }
}
