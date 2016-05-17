package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.util.rev160406;

import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;

public class NamingContextImplModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.util.rev160406.AbstractNamingContextImplModule {
    public NamingContextImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NamingContextImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.util.rev160406.NamingContextImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new NamingContext(
            getArtificialNamePrefix(),
            getIdentifier().getInstanceName());
    }

}
