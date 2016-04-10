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

package io.fd.honeycomb.v3po.data.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import io.fd.honeycomb.v3po.data.ReadableDataTree;
import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.read.ReaderRegistry;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ReadableDataTree implementation for operational data.
 */
public final class OperationalDataTree implements ReadableDataTree {
    private static final Logger LOG = LoggerFactory.getLogger(OperationalDataTree.class);

    private final BindingNormalizedNodeSerializer serializer;
    private final ReaderRegistry readerRegistry;
    private final SchemaContext globalContext;

    /**
     * Creates operational data tree instance.
     *
     * @param serializer     service for serialization between Java Binding Data representation and NormalizedNode
     *                       representation.
     * @param globalContext  service for obtaining top level context data from all yang modules.
     * @param readerRegistry service responsible for translation between DataObjects and data provider.
     */
    public OperationalDataTree(@Nonnull BindingNormalizedNodeSerializer serializer,
                               @Nonnull final SchemaContext globalContext, @Nonnull ReaderRegistry readerRegistry) {
        this.globalContext = checkNotNull(globalContext, "globalContext should not be null");
        this.serializer = checkNotNull(serializer, "serializer should not be null");
        this.readerRegistry = checkNotNull(readerRegistry, "reader should not be null");
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>,
            org.opendaylight.controller.md.sal.common.api.data.ReadFailedException> read(
            @Nonnull final YangInstanceIdentifier yangInstanceIdentifier) {

        try(ReadContext ctx = new ReadContextImpl()) {
            if (checkNotNull(yangInstanceIdentifier).equals(YangInstanceIdentifier.EMPTY)) {
                return Futures.immediateCheckedFuture(readRoot(ctx));
            } else {
                return Futures.immediateCheckedFuture(readNode(yangInstanceIdentifier, ctx));
            }
        } catch (ReadFailedException e) {
            return Futures.immediateFailedCheckedFuture(
                    new org.opendaylight.controller.md.sal.common.api.data.ReadFailedException(
                            "Failed to read VPP data", e));
        }
    }

    private Optional<NormalizedNode<?, ?>> readNode(final YangInstanceIdentifier yangInstanceIdentifier,
                                                    final ReadContext ctx)
            throws ReadFailedException {
        LOG.debug("OperationalDataTree.readNode(), yangInstanceIdentifier={}", yangInstanceIdentifier);
        final InstanceIdentifier<?> path = serializer.fromYangInstanceIdentifier(yangInstanceIdentifier);
        checkNotNull(path, "Invalid instance identifier %s. Cannot create BA equivalent.", yangInstanceIdentifier);
        LOG.debug("OperationalDataTree.readNode(), path={}", path);

        final Optional<? extends DataObject> dataObject;

        dataObject = readerRegistry.read(path, ctx);
        if (dataObject.isPresent()) {
            final NormalizedNode<?, ?> value = toNormalizedNodeFunction(path).apply(dataObject.get());
            return Optional.<NormalizedNode<?, ?>>fromNullable(value);
        } else {
            return Optional.absent();
        }
    }

    private Optional<NormalizedNode<?, ?>> readRoot(final ReadContext ctx) throws ReadFailedException {
        LOG.debug("OperationalDataTree.readRoot()");

        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> dataNodeBuilder =
                Builders.containerBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(SchemaContext.NAME));

        final Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> dataObjects =
                readerRegistry.readAll(ctx);

        for (final InstanceIdentifier<? extends DataObject> instanceIdentifier : dataObjects.keySet()) {
            final YangInstanceIdentifier rootElementId = serializer.toYangInstanceIdentifier(instanceIdentifier);
            final NormalizedNode<?, ?> node =
                    wrapDataObjects(rootElementId, instanceIdentifier, dataObjects.get(instanceIdentifier));
            dataNodeBuilder.withChild((DataContainerChild<?, ?>) node);
        }

        return Optional.<NormalizedNode<?, ?>>of(dataNodeBuilder.build());
    }

    private NormalizedNode<?, ?> wrapDataObjects(final YangInstanceIdentifier yangInstanceIdentifier,
                                                 final InstanceIdentifier<? extends DataObject> instanceIdentifier,
                                                 final Collection<? extends DataObject> dataObjects) {
        final Collection<NormalizedNode<?, ?>> normalizedRootElements = Collections2
                .transform(dataObjects, toNormalizedNodeFunction(instanceIdentifier));

        final DataSchemaNode schemaNode =
                globalContext.getDataChildByName(yangInstanceIdentifier.getLastPathArgument().getNodeType());
        if (schemaNode instanceof ListSchemaNode) {
            // In case of a list, wrap all the values in a Mixin parent node
            final ListSchemaNode listSchema = (ListSchemaNode) schemaNode;
            return wrapListIntoMixinNode(normalizedRootElements, listSchema);
        } else {
            Preconditions.checkState(dataObjects.size() == 1, "Singleton list was expected");
            return getOnlyElement(normalizedRootElements);
        }
    }

    private static DataContainerChild<?, ?> wrapListIntoMixinNode(
            final Collection<NormalizedNode<?, ?>> normalizedRootElements, final ListSchemaNode listSchema) {
        if (listSchema.getKeyDefinition().isEmpty()) {
            final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> listBuilder =
                    Builders.unkeyedListBuilder();
            for (NormalizedNode<?, ?> normalizedRootElement : normalizedRootElements) {
                listBuilder.withChild((UnkeyedListEntryNode) normalizedRootElement);
            }
            return listBuilder.build();
        } else {
            final CollectionNodeBuilder<MapEntryNode, ? extends MapNode> listBuilder =
                    listSchema.isUserOrdered()
                            ? Builders.orderedMapBuilder()
                            : Builders.mapBuilder();

            for (NormalizedNode<?, ?> normalizedRootElement : normalizedRootElements) {
                listBuilder.withChild((MapEntryNode) normalizedRootElement);
            }
            return listBuilder.build();
        }
    }

    @SuppressWarnings("unchecked")
    private Function<DataObject, NormalizedNode<?, ?>> toNormalizedNodeFunction(final InstanceIdentifier path) {
        return new Function<DataObject, NormalizedNode<?, ?>>() {
            @Override
            public NormalizedNode<?, ?> apply(@Nullable final DataObject dataObject) {
                LOG.trace("OperationalDataTree.toNormalizedNode(), path={}, dataObject={}", path, dataObject);
                final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry =
                        serializer.toNormalizedNode(path, dataObject);

                LOG.trace("OperationalDataTree.toNormalizedNode(), normalizedNodeEntry={}", entry);
                return entry.getValue();
            }
        };
    }

    private static final class ReadContextImpl implements ReadContext {
        public final Context ctx = new Context();

        @Nonnull
        @Override
        public Context getContext() {
            return ctx;
        }

        @Override
        public void close() {
            // Make sure to clear the storage in case some customizer stored it  to prevent memory leaks
            ctx.close();
        }
    }
}
