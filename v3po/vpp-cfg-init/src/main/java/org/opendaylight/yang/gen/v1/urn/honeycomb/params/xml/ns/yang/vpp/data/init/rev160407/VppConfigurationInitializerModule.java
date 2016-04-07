package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.data.init.rev160407;

import io.fd.honeycomb.v3po.translate.read.ReaderRegistry;
import io.fd.honeycomb.v3po.vpp.data.init.InitializerRegistry;
import io.fd.honeycomb.v3po.vpp.data.init.InitializerRegistryImpl;

public class VppConfigurationInitializerModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.data.init.rev160407.AbstractVppConfigurationInitializerModule {
    public VppConfigurationInitializerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VppConfigurationInitializerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.data.init.rev160407.VppConfigurationInitializerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final ReaderRegistry readerRegistry = getReaderRegistryDependency();

        InitializerRegistry initializer = new InitializerRegistryImpl(readerRegistry);

        initializer.initialize();

        return initializer;
    }

}
