package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.data.init.rev160407;

import io.fd.honeycomb.v3po.vpp.data.init.InitializerRegistry;
import io.fd.honeycomb.v3po.vpp.data.init.InitializerRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Initializer registry, delegating initialization to a list of initializers
*/
public class ConfigurationInitializerRegistryModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.data.init.rev160407.AbstractConfigurationInitializerRegistryModule {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationInitializerRegistryModule.class);

    public ConfigurationInitializerRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ConfigurationInitializerRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.data.init.rev160407.ConfigurationInitializerRegistryModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("VppConfigurationInitializerModule.createInstance(): initialization started");

        final InitializerRegistry initializer = new InitializerRegistryImpl(getInitializersDependency());

        try {
            // Initialize contexts first so that other initializers can find any relevant mapping before initializing
            // configuration to what is already in VPP
            getPersistedContextInitializerDependency().initialize();
            LOG.info("Persisted context restored successfully");
            // Initialize all registered initializers
            initializer.initialize();
            LOG.info("VPP configuration initialized successfully from VPP");
            // Initialize stored configuration on top
            getPersistedConfigInitializerDependency().initialize();
            LOG.info("Persisted configuration restored successfully");
        } catch (Exception e) {
            LOG.warn("Failed to initialize config", e);
        }

        LOG.info("Honeycomb initialized");

        return initializer;
    }

}
