/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.v3po.impl.data;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.fd.honeycomb.v3po.impl.LoggingFuturesCallBack;
import io.fd.honeycomb.v3po.impl.trans.r.ReaderRegistry;
import java.util.Collection;
import java.util.Collections;
import javassist.ClassPool;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates VppDataBroker which uses DataTree instead of DataStore internally in order to obtain better control over the
 * data processing in Honeycomb agent
 */
public final class VppDataBrokerInitializationProvider implements Provider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(VppDataBrokerInitializationProvider.class);

    private final TopologyId VPP_TOPOLOGY_ID = TopologyId.getDefaultInstance("vpp-topology");
    private final NodeId VPP_TOPOLOGY_NODE_ID = NodeId.getDefaultInstance("vpp");
    private final DataBroker bindingBroker;
    private final ReaderRegistry readerRegistry;
    private final InstanceIdentifier<Node> mountPointPath;
    private final VppWriterRegistry writerRegistry;
    private ObjectRegistration<DOMMountPoint> mountPointRegistration;
    private DOMDataBroker broker;

    public VppDataBrokerInitializationProvider(@Nonnull final DataBroker bindingBroker,
                                               final ReaderRegistry readerRegistry,
                                               final VppWriterRegistry writerRegistry) {
        this.bindingBroker = checkNotNull(bindingBroker, "bindingBroker should not be null");
        this.readerRegistry = checkNotNull(readerRegistry, "readerRegistry should not be null");
        this.writerRegistry = checkNotNull(writerRegistry, "writerRegistry should not be null");
        this.mountPointPath = getMountPointPath();
    }

    // TODO make configurable
    private InstanceIdentifier<Node> getMountPointPath() {
        final InstanceIdentifier<NetworkTopology> networkTopology =
                InstanceIdentifier.builder(NetworkTopology.class).build();
        final KeyedInstanceIdentifier<Topology, TopologyKey> topology =
                networkTopology.child(Topology.class, new TopologyKey(VPP_TOPOLOGY_ID));
        return topology.child(Node.class, new NodeKey(VPP_TOPOLOGY_NODE_ID));
    }

    @Override
    public void onSessionInitiated(final Broker.ProviderSession providerSession) {
        LOG.info("Session initialized, providerSession={}", providerSession);
        Preconditions.checkState(!isMountPointRegistered(), "Mount point is already registered");

        final DOMMountPointService mountPointService = providerSession.getService(DOMMountPointService.class);
        final SchemaService schemaService = providerSession.getService(SchemaService.class);

        final SchemaContext globalContext = schemaService.getGlobalContext();
        final BindingNormalizedNodeSerializer serializer = initSerializer(globalContext);
        final YangInstanceIdentifier path = serializer.toYangInstanceIdentifier(mountPointPath);

        final DOMMountPointService.DOMMountPointBuilder mountPointBuilder = mountPointService.createMountPoint(path);
        mountPointBuilder.addInitialSchemaContext(globalContext);

        broker = initVppDataBroker(globalContext, serializer);
        mountPointBuilder.addService(DOMDataBroker.class, broker);

        mountPointRegistration = mountPointBuilder.register();
        final DOMMountPoint mountPoint = mountPointRegistration.getInstance();
        LOG.debug("Created mountPoint: identifier={}, schemaContext={}", mountPoint.getIdentifier(),
                mountPoint.getSchemaContext());

        createMountPointPlaceholder();

        initialVppConfigSynchronization(broker);
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.EMPTY_LIST;
    }

    private boolean isMountPointRegistered() {
        final ReadOnlyTransaction readTx = bindingBroker.newReadOnlyTransaction();
        try {
            final Optional<Node> cfgPlaceholder =
                    readTx.read(LogicalDatastoreType.CONFIGURATION, mountPointPath).checkedGet();
            final Optional<Node> operPlaceholder =
                    readTx.read(LogicalDatastoreType.OPERATIONAL, mountPointPath).checkedGet();
            return cfgPlaceholder.isPresent() || operPlaceholder.isPresent();
        } catch (ReadFailedException e) {
            throw new IllegalStateException("Failed to read mountpoint placeholder data", e);
        }
    }

    private BindingNormalizedNodeSerializer initSerializer(final SchemaContext globalContext) {
        final JavassistUtils utils = JavassistUtils.forClassPool(ClassPool.getDefault());
        // TODO this produces ClassNotFoundException
        //final GeneratedClassLoadingStrategy loading = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();

        // FIXME get global class loader instance
        final GeneratedClassLoadingStrategy loadingStrategy =
                new GeneratedClassLoadingStrategy() {
                    @Override
                    public Class<?> loadClass(final String fullyQualifiedName)
                            throws ClassNotFoundException {
                        return Class.forName(fullyQualifiedName);
                    }
                };
        final DataObjectSerializerGenerator generator = StreamWriterGenerator.create(utils);

        // TODO make configurable:
        final BindingNormalizedNodeCodecRegistry serializer = new BindingNormalizedNodeCodecRegistry(generator);
        final BindingRuntimeContext context = BindingRuntimeContext.create(loadingStrategy, globalContext);
        serializer.onBindingRuntimeContextUpdated(context);
        return serializer;
    }

    private DOMDataBroker initVppDataBroker(final SchemaContext globalContext,
                                            final BindingNormalizedNodeSerializer serializer) {
        final ReadableVppDataTree operationalData =
                new VppOperationalDataTree(serializer, globalContext, readerRegistry); // TODO make configurable

        final DataTree dataTree =
                InMemoryDataTreeFactory.getInstance().create(TreeType.CONFIGURATION); // TODO make configurable
        dataTree.setSchemaContext(globalContext);

        final VppDataTree configDataProxy = new VppConfigDataTree(serializer, dataTree, writerRegistry); // TODO make configurable
        return new VppDataBroker(operationalData, configDataProxy);
    }

    /**
     * Writes placeholder data into MD-SAL's global datastore to indicate the presence of VPP mountpoint.
     */
    private void createMountPointPlaceholder() {
        final NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setKey(new NodeKey(VPP_TOPOLOGY_NODE_ID));
        final Node node = nodeBuilder.build();

        final WriteTransaction writeTx = bindingBroker.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, mountPointPath, node, true);
        writeTx.merge(LogicalDatastoreType.OPERATIONAL, mountPointPath, node, true);

        try {
            writeTx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            throw new IllegalStateException("Failed to create mountpoint placeholder", e);
        }
    }

    // TODO operational and config models are not 1-1
    // decide what part of operational data should be written to config during initialization
    private void initialVppConfigSynchronization(final DOMDataBroker broker) {
        // read from operational
        final DOMDataReadOnlyTransaction readTx = broker.newReadOnlyTransaction();

        final YangInstanceIdentifier
            id = YangInstanceIdentifier.builder().node(VppState.QNAME).node(BridgeDomains.QNAME).build();

        LOG.trace("initialVppStateSynchronization id: {}", id);

        final ListenableFuture<Void> writeFuture = Futures.transform(
                readTx.read(LogicalDatastoreType.OPERATIONAL, id),
                new AsyncFunction<Optional<NormalizedNode<?, ?>>, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(final Optional<NormalizedNode<?, ?>> readResult)
                            throws Exception {
                        if (readResult.isPresent()) {
                            final DOMDataWriteTransaction writeTx = broker.newWriteOnlyTransaction();
                            final NormalizedNode<?, ?> node = readResult.get();
                            LOG.trace("Read result: {}", node);

                            // FIXME
                            // this will fail because we are reading OPERATIONAL data and writing to CONFIGURATION
                            // we need to provide extensible way to register initializer that would
                            // translate between models

                            // writeTx.put(LogicalDatastoreType.CONFIGURATION, id, node);
                            return writeTx.submit();
                        } else {
                            return Futures
                                    .immediateFailedFuture(
                                            new IllegalStateException("Failed to read data from VPP."));
                        }
                    }
                });

        Futures.addCallback(writeFuture,
                new LoggingFuturesCallBack<Void>("Initializing VPP config DataTree failed", LOG));
    }

    public Optional<DOMDataBroker> getBroker() {
        return Optional.fromNullable(broker);
    }

    @Override
    public void close() throws Exception {
        if (mountPointRegistration != null) {
            mountPointRegistration.close();
        }

        if (broker != null) {
            broker = null;
        }

        // remove MD-SAL placeholder data for VPP mount point:
        final WriteTransaction rwTx = bindingBroker.newWriteOnlyTransaction();
        // does not fail if data is not present:
        rwTx.delete(LogicalDatastoreType.CONFIGURATION, mountPointPath);
        try {
            rwTx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            throw new IllegalStateException("Failed to remove mountpoint's placeholder from MD-SAL's global datastore",
                    e);
        }
    }
}
