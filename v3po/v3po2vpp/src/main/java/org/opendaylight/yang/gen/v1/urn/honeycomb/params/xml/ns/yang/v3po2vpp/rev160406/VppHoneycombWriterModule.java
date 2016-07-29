package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import com.google.common.collect.Sets;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.vpp.BridgeDomainCustomizer;
import io.fd.honeycomb.translate.v3po.vpp.L2FibEntryCustomizer;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.honeycomb.translate.write.WriterFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.L2FibTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.future.FutureJVpp;

public class VppHoneycombWriterModule extends
    org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.AbstractVppHoneycombWriterModule {
    public VppHoneycombWriterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                    org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VppHoneycombWriterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                    org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                    org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.VppHoneycombWriterModule oldModule,
                                    java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new VppHoneycombWriterFactory(
                getVppJvppWriterDependency(),
                getBridgeDomainContextVppDependency(),
                getInterfaceContextVppDependency());
    }

    private static final class VppHoneycombWriterFactory implements WriterFactory, AutoCloseable {

        private final FutureJVpp jvpp;
        private final NamingContext bdContext;
        private final NamingContext ifcContext;

        VppHoneycombWriterFactory(final FutureJVpp vppJvppWriterDependency,
                                  final NamingContext bridgeDomainContextVppDependency,
                                  final NamingContext interfaceContextVppDependency) {
            this.jvpp = vppJvppWriterDependency;
            this.bdContext = bridgeDomainContextVppDependency;
            this.ifcContext = interfaceContextVppDependency;
        }

        @Override
        public void init(final ModifiableWriterRegistryBuilder registry) {
            // Vpp has no handlers
            //  BridgeDomains has no handlers
            //   BridgeDomain =
            final InstanceIdentifier<BridgeDomain> bdId =
                    InstanceIdentifier.create(Vpp.class).child(BridgeDomains.class).child(BridgeDomain.class);
            registry.add(new GenericListWriter<>(bdId, new BridgeDomainCustomizer(jvpp, bdContext)));
            //    L2FibTable has no handlers
            //     L2FibEntry(handled after BridgeDomain and L2 of ifc and subifc) =
            final InstanceIdentifier<L2FibEntry> l2FibEntryId = bdId.child(L2FibTable.class).child(L2FibEntry.class);
            registry.addAfter(
                    new GenericListWriter<>(l2FibEntryId, new L2FibEntryCustomizer(jvpp, bdContext, ifcContext)),
                    Sets.newHashSet(
                            bdId,
                            InterfacesHoneycombWriterModule.L2_ID,
                            SubinterfaceAugmentationWriterFactory.L2_ID));
        }
    }
}
