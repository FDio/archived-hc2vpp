package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210;

import io.fd.honeycomb.v3po.translate.util.read.BindingBrokerReader;
import io.fd.honeycomb.v3po.translate.util.read.CloseableReader;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;

public class NetconfMonitoringReaderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210.AbstractNetconfMonitoringReaderModule {
    public NetconfMonitoringReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfMonitoringReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210.NetconfMonitoringReaderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new CloseableReader<>(new BindingBrokerReader<>(NetconfState.class, getNetconfMonitoringBindingBrokerDependency(),
            LogicalDatastoreType.OPERATIONAL));
    }

}
