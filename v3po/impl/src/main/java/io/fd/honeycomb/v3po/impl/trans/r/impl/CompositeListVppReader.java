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

package io.fd.honeycomb.v3po.impl.trans.r.impl;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.impl.trans.ReadFailedException;
import io.fd.honeycomb.v3po.impl.trans.r.ChildVppReader;
import io.fd.honeycomb.v3po.impl.trans.r.ListVppReader;
import io.fd.honeycomb.v3po.impl.trans.r.impl.spi.ListVppReaderCustomizer;
import io.fd.honeycomb.v3po.impl.trans.util.VppRWUtils;
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
 * Composite implementation of {@link ChildVppReader} able to place the read result into parent builder object intended
 * for list node type.
 *
 * This reader checks if the IDs are wildcarded in which case it performs read of all list entries. In case the ID has a
 * key, it reads only the specified value.
 */
@Beta
@ThreadSafe
public final class CompositeListVppReader<C extends DataObject & Identifiable<K>, K extends Identifier<C>, B extends Builder<C>>
        extends AbstractCompositeVppReader<C, B> implements ChildVppReader<C>, ListVppReader<C, K> {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeListVppReader.class);

    private final ListVppReaderCustomizer<C, K, B> customizer;

    /**
     * Create new {@link CompositeListVppReader}
     *
     * @param managedDataObjectType Class object for managed data type. Must come from a list node type.
     * @param childReaders          Child nodes(container, list) readers
     * @param augReaders            Child augmentations readers
     * @param customizer            Customizer instance to customize this generic reader
     */
    public CompositeListVppReader(@Nonnull final Class<C> managedDataObjectType,
                                  @Nonnull final List<ChildVppReader<? extends ChildOf<C>>> childReaders,
                                  @Nonnull final List<ChildVppReader<? extends Augmentation<C>>> augReaders,
                                  @Nonnull final ListVppReaderCustomizer<C, K, B> customizer) {
        super(managedDataObjectType, childReaders, augReaders);
        this.customizer = customizer;
    }

    /**
     * @see {@link CompositeListVppReader#CompositeListVppReader(Class, List, List, ListVppReaderCustomizer)}
     */
    public CompositeListVppReader(@Nonnull final Class<C> managedDataObjectType,
                                  @Nonnull final List<ChildVppReader<? extends ChildOf<C>>> childReaders,
                                  @Nonnull final ListVppReaderCustomizer<C, K, B> customizer) {
        this(managedDataObjectType, childReaders, VppRWUtils.<C>emptyAugReaderList(), customizer);
    }

    /**
     * @see {@link CompositeListVppReader#CompositeListVppReader(Class, List, List, ListVppReaderCustomizer)}
     */
    public CompositeListVppReader(@Nonnull final Class<C> managedDataObjectType,
                                  @Nonnull final ListVppReaderCustomizer<C, K, B> customizer) {
        this(managedDataObjectType, VppRWUtils.<C>emptyChildReaderList(), VppRWUtils.<C>emptyAugReaderList(),
                customizer);
    }

    @Override
    public void read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                     @Nonnull final Builder<? extends DataObject> parentBuilder) throws ReadFailedException {
        // Create ID pointing to current node
        final InstanceIdentifier<C> currentId = VppRWUtils.appendTypeToId(id, getManagedDataObjectType());
        // Read all, since current ID is definitely wildcarded
        final List<C> ifcs = readList(currentId);
        customizer.merge(parentBuilder, ifcs);
    }

    @Override
    @Nonnull
    public List<C> readList(@Nonnull final InstanceIdentifier<C> id) throws ReadFailedException {
        LOG.trace("{}: Reading all list entries", this);
        final List<K> allIds = customizer.getAllIds(id);
        LOG.debug("{}: Reading list entries for: {}", this, allIds);

        final ArrayList<C> allEntries = new ArrayList<>(allIds.size());
        for (K key : allIds) {
            final InstanceIdentifier.IdentifiableItem<C, K> currentBdItem =
                    VppRWUtils.getCurrentIdItem(id, key);
            final InstanceIdentifier<C> keyedId = VppRWUtils.replaceLastInId(id, currentBdItem);
            final Optional<C> read = readCurrent(keyedId);
            final DataObject singleItem = read.get();
            checkArgument(getManagedDataObjectType().getTargetType().isAssignableFrom(singleItem.getClass()));
            allEntries.add(getManagedDataObjectType().getTargetType().cast(singleItem));
        }
        return allEntries;
    }

    @Override
    protected void readCurrentAttributes(@Nonnull final InstanceIdentifier<C> id, @Nonnull final B builder)
            throws ReadFailedException {
        customizer.readCurrentAttributes(id, builder);
    }

    @Override
    protected B getBuilder(@Nonnull final InstanceIdentifier<C> id) {
        return customizer.getBuilder(id);
    }

}
