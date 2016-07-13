package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406;

import io.fd.honeycomb.v3po.translate.util.read.registry.CompositeReaderRegistryBuilder;

public class DelegatingReaderRegistryModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406.AbstractDelegatingReaderRegistryModule {
    public DelegatingReaderRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DelegatingReaderRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406.DelegatingReaderRegistryModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final CompositeReaderRegistryBuilder flatReaderRegistryBuilder = new CompositeReaderRegistryBuilder();
        getReaderFactoryDependency().forEach(readerFactory -> readerFactory.init(flatReaderRegistryBuilder));
        return flatReaderRegistryBuilder;
    }
}
