package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import io.fd.honeycomb.v3po.translate.impl.read.CompositeChildReader;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeListReader;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeRootReader;
import io.fd.honeycomb.v3po.translate.read.ChildReader;
import io.fd.honeycomb.v3po.translate.util.KeepaliveReaderWrapper;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.util.read.CloseableReader;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveRootReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.ReadTimeoutException;
import io.fd.honeycomb.v3po.translate.v3po.vppstate.BridgeDomainCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.vppstate.L2FibEntryCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.vppstate.VersionCustomizer;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.jvpp.cfg.rev160406.VppJvppImplModule;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.jvpp.cfg.rev160406.VppJvppImplModuleFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.L2FibTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.L2FibTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppStateHoneycombReaderModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.AbstractVppStateHoneycombReaderModule {

    private static final Logger LOG = LoggerFactory.getLogger(VppStateHoneycombReaderModule.class);

    public VppStateHoneycombReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VppStateHoneycombReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.VppStateHoneycombReaderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final FutureJVpp vppApi = getVppJvppDependency();

        ChildReader<Version> versionReader = new CompositeChildReader<>(Version.class, new VersionCustomizer(vppApi));
        // Wrap with keepalive reader to detect connection issues
        // TODO keepalive reader wrapper relies on VersionReaderCustomizer (to perform timeout on reads)
        // Once readers+customizers are asynchronous, pull the timeout to keepalive executor so that keepalive wrapper
        // is truly generic
        versionReader = new KeepaliveReaderWrapper<>(versionReader, getKeepaliveExecutorDependency().getExecutor(),
            ReadTimeoutException.class, 30, () -> reinitializeJVpp(reinitializationCounter));

        final CompositeListReader<L2FibEntry, L2FibEntryKey, L2FibEntryBuilder> l2FibEntryReader = new CompositeListReader<>(L2FibEntry.class,
                new L2FibEntryCustomizer(vppApi,
                        getBridgeDomainContextVppStateDependency(), getInterfaceContextVppStateDependency()));

        final ChildReader<L2FibTable> l2FibTableReader = new CompositeChildReader<>(
                L2FibTable.class,
                RWUtils.singletonChildReaderList(l2FibEntryReader),
                new ReflexiveChildReaderCustomizer<>(L2FibTableBuilder.class));

        final CompositeListReader<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder> bridgeDomainReader =
            new CompositeListReader<>(BridgeDomain.class,
                    RWUtils.singletonChildReaderList((ChildReader)l2FibTableReader),
                    new BridgeDomainCustomizer(vppApi,
                getBridgeDomainContextVppStateDependency()));

        final ChildReader<BridgeDomains> bridgeDomainsReader = new CompositeChildReader<>(
            BridgeDomains.class,
            RWUtils.singletonChildReaderList(bridgeDomainReader),
            new ReflexiveChildReaderCustomizer<>(BridgeDomainsBuilder.class));

        final List<ChildReader<? extends ChildOf<VppState>>> childVppReaders = new ArrayList<>();
        childVppReaders.add(versionReader);
        childVppReaders.add(bridgeDomainsReader);

        return new CloseableReader<>(new CompositeRootReader<>(
            VppState.class,
            childVppReaders,
            RWUtils.emptyAugReaderList(),
            new ReflexiveRootReaderCustomizer<>(VppStateBuilder.class)));
    }

    private static long reinitializationCounter;
    private static final long reinitializationLimit = 10;

    /**
     * In case we detect connection issues with VPP, reinitialize JVpp
     */
    private void reinitializeJVpp(final long currentAttempt) {
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
            throw new IllegalStateException("Unable to find jvpp instance in config subsystem. " +
                "Unable to reinitialize JVpp", e);
        } catch (ConflictingVersionException e) {
            LOG.debug("Conflict changes occurred, retrying", e);
            // Just retry until there's no conflicting change in progress
            reinitializeJVpp(nextAttempt);
        }

        reinitializationCounter = nextAttempt;
    }
}
