package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.japi.cfg.rev160406;

import java.io.IOException;
import org.openvpp.vppjapi.vppApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppJapiImplModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.japi.cfg.rev160406.AbstractVppJapiImplModule {

    private static final Logger LOG = LoggerFactory.getLogger(VppJapiImplModule.class);

    public VppJapiImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VppJapiImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.japi.cfg.rev160406.VppJapiImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        try {
            final vppApi vppApi = new vppApi(getName());
            LOG.info("VPP-INFO: VPP api client connection established");
            return vppApi;
        } catch (IOException e) {
            LOG.error("VPP-ERROR: VPP api client connection failed", e);
            throw new IllegalStateException("Unable to open vpp API", e);
        }
    }

}
