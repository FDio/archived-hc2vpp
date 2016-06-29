package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406;

import io.fd.honeycomb.v3po.translate.util.write.registry.FlatWriterRegistryBuilder;

public class DelegatingWriterRegistryModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406.AbstractDelegatingWriterRegistryModule {
    public DelegatingWriterRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DelegatingWriterRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406.DelegatingWriterRegistryModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final FlatWriterRegistryBuilder flatWriterRegistryBuilder = new FlatWriterRegistryBuilder();
        getWriterFactoryDependency().forEach(writerFactory -> writerFactory.init(flatWriterRegistryBuilder));
        return flatWriterRegistryBuilder;
    }

}
