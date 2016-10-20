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

package io.fd.honeycomb.translate.v3po.vppclassifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.BindingBrokerReader;
import java.util.List;
import java.util.stream.Collector;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.VppNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.VppClassifierContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.VppClassifierContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.vpp.classifier.context.ClassifyTableContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.vpp.classifier.context.ClassifyTableContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.vpp.classifier.context.ClassifyTableContextKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.vpp.classifier.context.classify.table.context.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.vpp.classifier.context.classify.table.context.NodeContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.vpp.classifier.context.classify.table.context.NodeContextKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Facade on top of {@link MappingContext} that manages {@link ClassifyTableContext}.
 */
public final class VppClassifierContextManagerImpl implements VppClassifierContextManager {
    private static final Collector<ClassifyTableContext, ?, ClassifyTableContext> SINGLE_ITEM_COLLECTOR =
        RWUtils.singleItemCollector();

    @VisibleForTesting
    static final InstanceIdentifier<VppClassifierContext>
        VPP_CLASSIFIER_CONTEXT_IID = KeyedInstanceIdentifier.create(VppClassifierContext.class);
    private final String artificialNamePrefix;

    /**
     * Creates new VppClassifierContextManagerImpl.
     *
     * @param artificialNamePrefix artificial name to be used to generate names for classify tables without existing
     *                             metadata
     */
    public VppClassifierContextManagerImpl(@Nonnull final String artificialNamePrefix) {
        this.artificialNamePrefix =
            Preconditions.checkNotNull(artificialNamePrefix, "artificialNamePrefix should not be null");
    }

    private KeyedInstanceIdentifier<ClassifyTableContext, ClassifyTableContextKey> getMappingIid(final String name) {
        return VPP_CLASSIFIER_CONTEXT_IID.child(ClassifyTableContext.class, new ClassifyTableContextKey(name));
    }

    @Override
    public void addTable(final int id, @Nonnull final String name, @Nullable final VppNodeName classifierNode,
                         @Nonnull final MappingContext ctx) {
        final KeyedInstanceIdentifier<ClassifyTableContext, ClassifyTableContextKey> mappingIid = getMappingIid(name);
        final ClassifyTableContextBuilder tableCtx = new ClassifyTableContextBuilder().setIndex(id).setName(name);
        if (classifierNode != null) {
            tableCtx.setClassifierNodeName(classifierNode.getValue());
        }
        ctx.put(mappingIid, tableCtx.build());
    }

    @Override
    public boolean containsTable(@Nonnull final String name, @Nonnull final MappingContext ctx) {
        final Optional<ClassifyTableContext> read = ctx.read(getMappingIid(name));
        return read.isPresent();
    }

    @Override
    public int getTableIndex(@Nonnull final String name, @Nonnull final MappingContext ctx) {
        final Optional<ClassifyTableContext> read = ctx.read(getMappingIid(name));
        checkArgument(read.isPresent(), "No mapping stored for name: %s", name);
        return read.get().getIndex();
    }

    @Override
    public String getTableName(final int id, @Nonnull final MappingContext ctx) {
        if (!containsName(id, ctx)) {
            final String artificialName = getArtificialName(id);
            addTable(id, artificialName, null, ctx);
        }

        final Optional<VppClassifierContext> read = ctx.read(VPP_CLASSIFIER_CONTEXT_IID);
        checkState(read.isPresent(), "VppClassifierContext for index: %s is not present. But should be", id);

        return read.get().getClassifyTableContext().stream()
            .filter(t -> t.getIndex().equals(id))
            .collect(SINGLE_ITEM_COLLECTOR).getName();
    }

    private boolean containsName(final int index, @Nonnull final MappingContext mappingContext) {
        final Optional<VppClassifierContext> read = mappingContext.read(VPP_CLASSIFIER_CONTEXT_IID);
        return read.isPresent()
            ? read.get().getClassifyTableContext().stream().anyMatch(t -> t.getIndex().equals(index))
            : false;
    }

    @Override
    public Optional<String> getTableBaseNode(@Nonnull final String name, @Nonnull final MappingContext ctx) {
        final Optional<ClassifyTableContext> read = ctx.read(getMappingIid(name));
        if (read.isPresent()) {
            return Optional.fromNullable(read.get().getClassifierNodeName());
        }
        return Optional.absent();
    }

    @Override
    public void removeTable(@Nonnull final String name, @Nonnull final MappingContext ctx) {
        ctx.delete(getMappingIid(name));
    }

    @Override
    public void addNodeName(@Nonnull final String tableName, final int nodeIndex,
                            @Nonnull final String nodeName,
                            @Nonnull final MappingContext ctx) {
        final KeyedInstanceIdentifier<NodeContext, NodeContextKey> iid =
            getMappingIid(tableName).child(NodeContext.class, new NodeContextKey(nodeName));
        ctx.put(iid, new NodeContextBuilder().setName(nodeName).setIndex(nodeIndex).build());
    }

    @Override
    public Optional<String> getNodeName(final int tableIndex, final int nodeIndex, @Nonnull final MappingContext ctx) {
        if (!containsName(tableIndex, ctx)) {
            return Optional.absent();
        }
        final String tableName = getTableName(tableIndex, ctx);
        final Optional<ClassifyTableContext> tableCtx = ctx.read(getMappingIid(tableName));
        final List<NodeContext> nodeContext = tableCtx.get().getNodeContext();
        if (nodeContext == null) {
            return Optional.absent();
        }
        return Optional.fromNullable(nodeContext.stream()
            .filter(n -> n.getIndex().equals(nodeIndex))
            .findFirst()
            .map(nodes -> nodes.getName())
            .orElse(null));
    }

    private String getArtificialName(final int index) {
        return artificialNamePrefix + index;
    }

    public static final class ContextsReaderFactory implements ReaderFactory {

        @Inject
        @Named("honeycomb-context")
        private DataBroker contextBindingBrokerDependency;

        @Override
        public void init(final ModifiableReaderRegistryBuilder registry) {
            registry.add(new BindingBrokerReader<>(VPP_CLASSIFIER_CONTEXT_IID,
                contextBindingBrokerDependency,
                LogicalDatastoreType.OPERATIONAL, VppClassifierContextBuilder.class));
        }
    }
}
