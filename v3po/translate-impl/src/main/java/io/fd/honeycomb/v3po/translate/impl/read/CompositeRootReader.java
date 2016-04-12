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

import com.google.common.annotations.Beta;
import io.fd.honeycomb.v3po.translate.impl.TraversalType;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.read.ChildReader;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.Reader;
import io.fd.honeycomb.v3po.translate.spi.read.RootReaderCustomizer;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Composite implementation of {@link Reader}
 */
@Beta
@ThreadSafe
public final class CompositeRootReader<C extends DataObject, B extends Builder<C>> extends AbstractCompositeReader<C, B>
    implements Reader<C> {

    private final RootReaderCustomizer<C, B> customizer;

    /**
     * Create new {@link CompositeRootReader}
     *
     * @param managedDataObjectType Class object for managed data type
     * @param childReaders Child nodes(container, list) readers
     * @param augReaders Child augmentations readers
     * @param customizer Customizer instance to customize this generic reader
     *
     */
    public CompositeRootReader(@Nonnull final Class<C> managedDataObjectType,
                               @Nonnull final List<ChildReader<? extends ChildOf<C>>> childReaders,
                               @Nonnull final List<ChildReader<? extends Augmentation<C>>> augReaders,
                               @Nonnull final RootReaderCustomizer<C, B> customizer) {
        this(managedDataObjectType, childReaders, augReaders, customizer, TraversalType.PREORDER);
    }

    /**
     * Create new {@link CompositeRootReader}
     *
     * @param managedDataObjectType Class object for managed data type
     * @param childReaders Child nodes(container, list) readers
     * @param augReaders Child augmentations readers
     * @param customizer Customizer instance to customize this generic reader
     * @param traversalType Type of traversal to use in the tree of readers
     *
     */
    public CompositeRootReader(@Nonnull final Class<C> managedDataObjectType,
                               @Nonnull final List<ChildReader<? extends ChildOf<C>>> childReaders,
                               @Nonnull final List<ChildReader<? extends Augmentation<C>>> augReaders,
                               @Nonnull final RootReaderCustomizer<C, B> customizer,
                               @Nonnull final TraversalType traversalType) {
        super(managedDataObjectType, childReaders, augReaders, traversalType);
        this.customizer = customizer;
    }

    /**
     * @see {@link CompositeRootReader#CompositeRootReader(Class, List, List, RootReaderCustomizer)}
     */
    public CompositeRootReader(@Nonnull final Class<C> managedDataObjectType,
                               @Nonnull final List<ChildReader<? extends ChildOf<C>>> childReaders,
                               @Nonnull final RootReaderCustomizer<C, B> customizer) {
        this(managedDataObjectType, childReaders, RWUtils.<C>emptyAugReaderList(), customizer);
    }

    /**
     * @see {@link CompositeRootReader#CompositeRootReader(Class, List, List, RootReaderCustomizer)}
     */
    public CompositeRootReader(@Nonnull final Class<C> managedDataObjectType,
                               @Nonnull final RootReaderCustomizer<C, B> customizer) {
        this(managedDataObjectType, RWUtils.<C>emptyChildReaderList(), RWUtils.<C>emptyAugReaderList(),
            customizer);
    }

    @Override
    protected void readCurrentAttributes(@Nonnull final InstanceIdentifier<C> id, @Nonnull final B builder,
                                         @Nonnull final ReadContext ctx) throws ReadFailedException {
        customizer.readCurrentAttributes(id, builder, ctx.getContext());
    }

    @Override
    protected B getBuilder(@Nonnull final InstanceIdentifier<C> id) {
        return customizer.getBuilder(id);
    }

}
