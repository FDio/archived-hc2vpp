package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import io.fd.honeycomb.translate.v3po.VppStateHoneycombReaderFactory;
import java.lang.management.ManagementFactory;
import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.jvpp.cfg.rev160406.VppJvppImplModule;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.jvpp.cfg.rev160406.VppJvppImplModuleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppStateHoneycombReaderModule extends
    org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.AbstractVppStateHoneycombReaderModule {

    private static final Logger LOG = LoggerFactory.getLogger(VppStateHoneycombReaderModule.class);

    public VppStateHoneycombReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                         org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VppStateHoneycombReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                         org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                         org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.VppStateHoneycombReaderModule oldModule,
                                         java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new VppStateHoneycombReaderFactory(getVppJvppDependency(),
                getInterfaceContextVppStateDependency(),
                getBridgeDomainContextVppStateDependency(),
                getKeepaliveExecutorDependency());
    }

    private static long reinitializationCounter;
    private static final long reinitializationLimit = 10;

    /**
     * In case we detect connection issues with VPP, reinitialize JVpp.
     */
    private static void reinitializeJVpp(final long currentAttempt) {
        // FIXME https://jira.fd.io/browse/HONEYCOMB-78 This code correctly re-initializes all the components
        // starting with jvpp, but jvpp reconnect fails. Test in a JVpp test and then from C
        LOG.info("Reinitializing JVpp, attempt: {}", currentAttempt);

        final long nextAttempt = currentAttempt + 1;
        if (nextAttempt - reinitializationCounter > reinitializationLimit) {
            LOG.error("Too many JVpp reinitialization attempts. Unable to reinitialize JVpp in {} attempts. Giving up",
                reinitializationLimit);
            throw new IllegalStateException("Too many JVpp reinitialization attempts. Unable to reinitialize JVpp in "
                + reinitializationLimit + " attempts. Giving up");
        }

        final ConfigRegistryJMXClient cfgRegistryClient =
            ConfigRegistryJMXClient.createWithoutNotifications(ManagementFactory.getPlatformMBeanServer());

        final ObjectName objectName = cfgRegistryClient.beginConfig();
        final ConfigTransactionJMXClient txClient = cfgRegistryClient.getConfigTransactionClient(objectName);

        final ObjectName jvppOn;
        try {
            final String attributeName = VppJvppImplModule.descriptionJmxAttribute.getAttributeName();
            final String factoryName = VppJvppImplModuleFactory.NAME;
            jvppOn = txClient.lookupConfigBean(factoryName, "vpp-jvpp");

            // Change configuration attribute of JVpp to trigger full reinitialization here using config subsystem
            // TODO improve this when switching from karaf in planned minimal distribution
            txClient.setAttribute(jvppOn, attributeName, new Attribute(attributeName,
                Long.toString(nextAttempt)));

            txClient.validateConfig();
            cfgRegistryClient.commitConfig(txClient.getObjectName());
            LOG.info("JVpp reinitialized successfully");
        } catch (InstanceNotFoundException | ValidationException e) {
            LOG.error("Unable to reinitialize JVpp. Honeycomb will not work properly from now on.", e);
            throw new IllegalStateException("Unable to find jvpp instance in config subsystem. Unable to reinitialize JVpp", e);
        } catch (ConflictingVersionException e) {
            LOG.debug("Conflict changes occurred, retrying", e);
            // Just retry until there's no conflicting change in progress
            reinitializeJVpp(nextAttempt);
        }

        reinitializationCounter = nextAttempt;
    }


}
