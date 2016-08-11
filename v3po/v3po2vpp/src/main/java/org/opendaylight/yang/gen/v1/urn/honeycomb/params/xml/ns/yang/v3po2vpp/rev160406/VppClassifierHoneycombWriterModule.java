package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import io.fd.honeycomb.translate.v3po.VppClassifierHoneycombWriterFactory;

public class VppClassifierHoneycombWriterModule extends
    org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.AbstractVppClassifierHoneycombWriterModule {


    public VppClassifierHoneycombWriterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                              org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VppClassifierHoneycombWriterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                              org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                              org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.VppClassifierHoneycombWriterModule oldModule,
                                              java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new VppClassifierHoneycombWriterFactory(
            getVppJvppDependency(),
            getClassifyTableContextDependency());
    }

}
