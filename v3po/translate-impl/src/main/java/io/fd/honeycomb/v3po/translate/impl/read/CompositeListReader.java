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

package io.fd.honeycomb.v3po.translate.impl.read;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.impl.TraversalType;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.read.ChildReader;
import io.fd.honeycomb.v3po.translate.read.ListReader;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.spi.read.ListReaderCustomizer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Composite implementation of {@link ChildReader} able to place the read result into parent builder object intended
 * for list node type.
 *
 * This reader checks if the IDs are wildcarded in which case it performs read of all list entries. In case the ID has a
 * key, it reads only the specified value.
 */
@Beta
@ThreadSafe
public final class CompositeListReader<C extends DataObject & Identifiable<K>, K extends Identifier<C>, B extends Builder<C>>
        extends AbstractCompositeReader<C, B> implements ChildReader<C>, ListReader<C, K> {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeListReader.class);

    private final ListReaderCustomizer<C, K, B> customizer;

    /**
     * Create new {@link CompositeListReader}
     *
     * @param managedDataObjectType Class object for managed data type. Must come from a list node type.
     * @param childReaders          Child nodes(container, list) readers
     * @param augReaders            Child augmentations readers
     * @param customizer            Customizer instance to customize this generic reader
     */
    public CompositeListReader(@Nonnull final Class<C> managedDataObjectType,
                               @Nonnull final List<ChildReader<? extends ChildOf<C>>> childReaders,
                               @Nonnull final List<ChildReader<? extends Augmentation<C>>> augReaders,
                               @Nonnull final ListReaderCustomizer<C, K, B> customizer) {
        this(managedDataObjectType, childReaders, augReaders, customizer, TraversalType.PREORDER);
    }

    /**
     * Create new {@link CompositeListReader}
     *
     * @param managedDataObjectType Class object for managed data type. Must come from a list node type.
     * @param childReaders          Child nodes(container, list) readers
     * @param augReaders            Child augmentations readers
     * @param customizer            Customizer instance to customize this generic reader
     * @param traversalType Type of traversal to use in the tree of readers
     */
    public CompositeListReader(@Nonnull final Class<C> managedDataObjectType,
                               @Nonnull final List<ChildReader<? extends ChildOf<C>>> childReaders,
                               @Nonnull final List<ChildReader<? extends Augmentation<C>>> augReaders,
                               @Nonnull final ListReaderCustomizer<C, K, B> customizer,
                               @Nonnull final TraversalType traversalType) {
        super(managedDataObjectType, childReaders, augReaders, traversalType);
        this.customizer = customizer;
    }

    /**
     * @see {@link CompositeListReader#CompositeListReader(Class, List, List, ListReaderCustomizer)}
     */
    public CompositeListReader(@Nonnull final Class<C> managedDataObjectType,
                               @Nonnull final List<ChildReader<? extends ChildOf<C>>> childReaders,
                               @Nonnull final ListReaderCustomizer<C, K, B> customizer) {
        this(managedDataObjectType, childReaders, RWUtils.<C>emptyAugReaderList(), customizer);
    }

    /**
     * @see {@link CompositeListReader#CompositeListReader(Class, List, List, ListReaderCustomizer)}
     */
    public CompositeListReader(@Nonnull final Class<C> managedDataObjectType,
                               @Nonnull final ListReaderCustomizer<C, K, B> customizer) {
        this(managedDataObjectType, RWUtils.<C>emptyChildReaderList(), RWUtils.<C>emptyAugReaderList(),
                customizer);
    }

    @Override
    public void read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                     @Nonnull final Builder<? extends DataObject> parentBuilder,
                     @Nonnull final ReadContext ctx) throws ReadFailedException {
        // Create ID pointing to current node
        final InstanceIdentifier<C> currentId = RWUtils.appendTypeToId(id, getManagedDataObjectType());
        // Read all, since current ID is definitely wildcarded
        final List<C> ifcs = readList(currentId, ctx);
        customizer.merge(parentBuilder, ifcs);
    }

    @Override
    @Nonnull
    public List<C> readList(@Nonnull final InstanceIdentifier<C> id,
                            @Nonnull final ReadContext ctx) throws ReadFailedException {
        LOG.trace("{}: Reading all list entries", this);
        final List<K> allIds = customizer.getAllIds(id, ctx);
        LOG.debug("{}: Reading list entries for: {}", this, allIds);

        final ArrayList<C> allEntries = new ArrayList<>(allIds.size());
        for (K key : allIds) {
            final InstanceIdentifier.IdentifiableItem<C, K> currentBdItem =
                    RWUtils.getCurrentIdItem(id, key);
            final InstanceIdentifier<C> keyedId = RWUtils.replaceLastInId(id, currentBdItem);
            final Optional<C> read = readCurrent(keyedId, ctx);
            if(read.isPresent()) {
                final DataObject singleItem = read.get();
                checkArgument(getManagedDataObjectType().getTargetType().isAssignableFrom(singleItem.getClass()));
                allEntries.add(getManagedDataObjectType().getTargetType().cast(singleItem));
            }
        }
        return allEntries;
    }

    @Override
    protected void readCurrentAttributes(@Nonnull final InstanceIdentifier<C> id, @Nonnull final B builder,
                                         @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        customizer.readCurrentAttributes(id, builder, ctx);
    }

    @Override
    protected B getBuilder(@Nonnull final InstanceIdentifier<C> id) {
        return customizer.getBuilder(id);
    }

}
