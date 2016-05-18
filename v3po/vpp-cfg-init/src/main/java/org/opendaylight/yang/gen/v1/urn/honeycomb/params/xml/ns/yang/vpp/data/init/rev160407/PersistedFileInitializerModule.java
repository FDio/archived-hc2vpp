package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.data.init.rev160407;

import io.fd.honeycomb.v3po.vpp.data.init.RestoringInitializer;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.api.rev160411.DatatreeType;

/**
* Initializer restoring data from a persisted file
*/
public class PersistedFileInitializerModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.data.init.rev160407.AbstractPersistedFileInitializerModule {
    public PersistedFileInitializerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public PersistedFileInitializerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.data.init.rev160407.PersistedFileInitializerModule oldModule, java.lang.AutoCloseable oldInstance) {
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
        return new RestoringInitializer(getSchemaServiceDependency(), Paths.get(getPersistFilePath()),
            getDomDataBrokerDependency(), getRestorationType(),
            getDatastoreType() == DatatreeType.Config ? LogicalDatastoreType.CONFIGURATION : LogicalDatastoreType.OPERATIONAL);
    }

}
