package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210;

import io.fd.honeycomb.v3po.data.impl.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataBrokerModule extends
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210.AbstractDataBrokerModule {

    private static final Logger LOG = LoggerFactory.getLogger(DataBrokerModule.class);

    public DataBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DataBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210.DataBrokerModule oldModule,
                            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.debug("DataBrokerModule.createInstance()");
        return DataBroker.create(getConfigDataTreeDependency(), getOperationalDataTreeDependency());
    }
}
