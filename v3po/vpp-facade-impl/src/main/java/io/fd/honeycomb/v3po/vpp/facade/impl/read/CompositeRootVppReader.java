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

package io.fd.honeycomb.v3po.vpp.facade.impl.read;

import com.google.common.annotations.Beta;
import io.fd.honeycomb.v3po.vpp.facade.impl.util.VppRWUtils;
import io.fd.honeycomb.v3po.vpp.facade.read.ChildVppReader;
import io.fd.honeycomb.v3po.vpp.facade.read.ReadContext;
import io.fd.honeycomb.v3po.vpp.facade.read.ReadFailedException;
import io.fd.honeycomb.v3po.vpp.facade.read.VppReader;
import io.fd.honeycomb.v3po.vpp.facade.spi.read.RootVppReaderCustomizer;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Composite implementation of {@link io.fd.honeycomb.v3po.vpp.facade.read.VppReader}
 */
@Beta
@ThreadSafe
public final class CompositeRootVppReader<C extends DataObject, B extends Builder<C>> extends AbstractCompositeVppReader<C, B>
    implements VppReader<C> {

    private final RootVppReaderCustomizer<C, B> customizer;

    /**
     * Create new {@link CompositeRootVppReader}
     *
     * @param managedDataObjectType Class object for managed data type
     * @param childReaders Child nodes(container, list) readers
     * @param augReaders Child augmentations readers
     * @param customizer Customizer instance to customize this generic reader
     *
     */
    public CompositeRootVppReader(@Nonnull final Class<C> managedDataObjectType,
                                  @Nonnull final List<ChildVppReader<? extends ChildOf<C>>> childReaders,
                                  @Nonnull final List<ChildVppReader<? extends Augmentation<C>>> augReaders,
                                  @Nonnull final RootVppReaderCustomizer<C, B> customizer) {
        super(managedDataObjectType, childReaders, augReaders);
        this.customizer = customizer;
    }

    /**
     * @see {@link CompositeRootVppReader#CompositeRootVppReader(Class, List, List, RootVppReaderCustomizer)}
     */
    public CompositeRootVppReader(@Nonnull final Class<C> managedDataObjectType,
                                  @Nonnull final List<ChildVppReader<? extends ChildOf<C>>> childReaders,
                                  @Nonnull final RootVppReaderCustomizer<C, B> customizer) {
        this(managedDataObjectType, childReaders, VppRWUtils.<C>emptyAugReaderList(), customizer);
    }

    /**
     * @see {@link CompositeRootVppReader#CompositeRootVppReader(Class, List, List, RootVppReaderCustomizer)}
     */
    public CompositeRootVppReader(@Nonnull final Class<C> managedDataObjectType,
                                  @Nonnull final RootVppReaderCustomizer<C, B> customizer) {
        this(managedDataObjectType, VppRWUtils.<C>emptyChildReaderList(), VppRWUtils.<C>emptyAugReaderList(),
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
