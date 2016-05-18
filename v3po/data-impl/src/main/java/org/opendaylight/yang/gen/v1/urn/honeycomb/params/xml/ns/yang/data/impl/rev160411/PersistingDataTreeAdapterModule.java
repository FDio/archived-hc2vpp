package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.impl.rev160411;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;

public class PersistingDataTreeAdapterModule extends
    org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.impl.rev160411.AbstractPersistingDataTreeAdapterModule {
    public PersistingDataTreeAdapterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                           org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public PersistingDataTreeAdapterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                           org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                           org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.impl.rev160411.PersistingDataTreeAdapterModule oldModule,
                                           java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        try {
            Paths.get(getPersistFilePath());
        } catch (InvalidPathException e) {
            throw new JmxAttributeValidationException("Invalid persist path", e, persistFilePathJmxAttribute);
        }
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new io.fd.honeycomb.v3po.data.impl.PersistingDataTreeAdapter(
            getDelegateDependency(),
            getSchemaServiceDependency(),
            Paths.get(getPersistFilePath()));
    }

}
