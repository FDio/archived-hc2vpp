package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.data.init.rev160407;

import io.fd.honeycomb.v3po.data.ModifiableDataTree;
import io.fd.honeycomb.v3po.translate.read.ReaderRegistry;
import io.fd.honeycomb.v3po.vpp.data.init.DataTreeInitializer;
import io.fd.honeycomb.v3po.vpp.data.init.InitializerRegistry;
import io.fd.honeycomb.v3po.vpp.data.init.InitializerRegistryImpl;
import io.fd.honeycomb.v3po.vpp.data.init.VppInitializer;
import java.util.Collections;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppConfigurationInitializerModule extends
        org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.data.init.rev160407.AbstractVppConfigurationInitializerModule {

    private static final Logger LOG = LoggerFactory.getLogger(VppConfigurationInitializerModule.class);

    public VppConfigurationInitializerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                             org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VppConfigurationInitializerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                             org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                             org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.data.init.rev160407.VppConfigurationInitializerModule oldModule,
                                             java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("VppConfigurationInitializerModule.createInstance()");
        final ReaderRegistry readerRegistry = getReaderRegistryDependency();
        final ModifiableDataTree configDataTree = getConfigDataTreeDependency();
        final BindingNormalizedNodeSerializer serializer = getSerializerDependency();

        // TODO make configurable
        final VppInitializer vppInitializer =
                new VppInitializer(readerRegistry, configDataTree, serializer);

        final InitializerRegistry initializer =
                new InitializerRegistryImpl(Collections.<DataTreeInitializer>singletonList(vppInitializer));

        try {
            initializer.initialize();
        } catch (Exception e) {
            LOG.warn("Failed to initialize config", e);
        }

        return initializer;
    }

}
