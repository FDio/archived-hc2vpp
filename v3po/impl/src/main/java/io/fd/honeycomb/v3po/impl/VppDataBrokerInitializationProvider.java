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

package io.fd.honeycomb.v3po.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
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
    private final org.opendaylight.controller.md.sal.binding.api.DataBroker bindingBroker;
    private final InstanceIdentifier<Node> mountPointPath;
    private final BindingNormalizedNodeSerializer serializer;
    private ObjectRegistration<DOMMountPoint> mountPointRegistration;
    private final DOMDataBroker domDataBroker;

    public VppDataBrokerInitializationProvider(
            @Nonnull final org.opendaylight.controller.md.sal.binding.api.DataBroker bindingBroker,
            @Nonnull final BindingNormalizedNodeSerializer serializer,
            @Nonnull final DOMDataBroker domDataBroker) {
        this.bindingBroker = checkNotNull(bindingBroker, "bindingBroker should not be null");
        this.serializer = checkNotNull(serializer, "serializer should not be null");
        this.domDataBroker = checkNotNull(domDataBroker, "domDataBroker should not be null");
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
        final YangInstanceIdentifier path = serializer.toYangInstanceIdentifier(mountPointPath);

        final DOMMountPointService.DOMMountPointBuilder mountPointBuilder = mountPointService.createMountPoint(path);
        mountPointBuilder.addInitialSchemaContext(globalContext);

        mountPointBuilder.addService(DOMDataBroker.class, domDataBroker);

        mountPointRegistration = mountPointBuilder.register();
        final DOMMountPoint mountPoint = mountPointRegistration.getInstance();
        LOG.debug("Created mountPoint: identifier={}, schemaContext={}", mountPoint.getIdentifier(),
                mountPoint.getSchemaContext());

        createMountPointPlaceholder();
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

    public Optional<DOMDataBroker> getBroker() {
        return Optional.fromNullable(domDataBroker);
    }

    @Override
    public void close() throws Exception {
        if (mountPointRegistration != null) {
            mountPointRegistration.close();
        }

        // remove MD-SAL placeholder data for VPP mount point:
        final WriteTransaction rwTx = bindingBroker.newWriteOnlyTransaction();
        // does not fail if data is not present:
        rwTx.delete(LogicalDatastoreType.CONFIGURATION, mountPointPath);
        rwTx.delete(LogicalDatastoreType.OPERATIONAL, mountPointPath);
        try {
            rwTx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            throw new IllegalStateException("Failed to remove mountpoint's placeholder from MD-SAL's global datastore",
                    e);
        }
    }
}
