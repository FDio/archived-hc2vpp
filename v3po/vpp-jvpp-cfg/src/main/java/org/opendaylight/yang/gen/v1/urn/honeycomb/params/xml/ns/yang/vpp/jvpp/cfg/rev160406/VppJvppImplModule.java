package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.jvpp.cfg.rev160406;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.openvpp.jvpp.JVppImpl;
import org.openvpp.jvpp.VppJNIConnection;
import org.openvpp.jvpp.dto.JVppReply;
import org.openvpp.jvpp.future.FutureJVppFacade;
import org.openvpp.jvpp.future.FutureJVppFacadeCallback;

public class VppJvppImplModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.jvpp.cfg.rev160406.AbstractVppJvppImplModule {
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
            final Map<Integer, CompletableFuture<? extends JVppReply<?>>> map = new HashMap<>();
            final JVppImpl jVpp =
                new JVppImpl(VppJNIConnection.create(getName(), new FutureJVppFacadeCallback(map)));
            return new FutureJVppFacade(jVpp, map) {
                @Override
                public void close() throws Exception {
                    super.close();
                    jVpp.close();
                }
            };
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open VPP management connection", e);
        }
    }

}
