package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.jvpp.cfg.rev160406;

import java.io.IOException;
import org.openvpp.jvpp.JVppImpl;
import org.openvpp.jvpp.VppJNIConnection;
import org.openvpp.jvpp.future.FutureJVppFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppJvppImplModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.jvpp.cfg.rev160406.AbstractVppJvppImplModule {

    private static final Logger LOG = LoggerFactory.getLogger(VppJvppImplModule.class);

    public VppJvppImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VppJvppImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.jvpp.cfg.rev160406.VppJvppImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        try {
            final JVppImpl jVpp = new JVppImpl(new VppJNIConnection(getName()));
            LOG.info("JVpp connection opened successfully as: {}", getName());
            return new FutureJVppFacade(jVpp) {
                @Override
                public void close() throws Exception {
                    super.close();
                    LOG.info("Closing JVpp connection: {}", getName());
                    jVpp.close();
                }
            };
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open VPP management connection", e);
        }
    }

}
