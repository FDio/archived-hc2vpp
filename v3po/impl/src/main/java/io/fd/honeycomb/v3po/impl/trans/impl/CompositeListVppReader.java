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

package io.fd.honeycomb.v3po.impl.trans.impl;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.impl.trans.ChildVppReader;
import io.fd.honeycomb.v3po.impl.trans.impl.spi.ListVppReaderCustomizer;
import io.fd.honeycomb.v3po.impl.trans.util.VppReaderUtils;
import java.util.Collections;
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

@Beta
@ThreadSafe
public final class CompositeListVppReader<C extends DataObject & Identifiable<K>, K extends Identifier<C>, B extends Builder<C>>
    extends AbstractCompositeVppReader<C, B> implements ChildVppReader<C> {

    private ListVppReaderCustomizer<C, K, B> customizer;

    public CompositeListVppReader(@Nonnull final Class<C> managedDataObjectType,
                                  @Nonnull final List<ChildVppReader<? extends ChildOf<C>>> childReaders,
                                  @Nonnull final List<ChildVppReader<? extends Augmentation<C>>> augReaders,
                                  @Nonnull final ListVppReaderCustomizer<C, K, B> customizer) {
        super(managedDataObjectType, childReaders, augReaders);
        this.customizer = customizer;
    }

    public CompositeListVppReader(@Nonnull final Class<C> managedDataObjectType,
                                  @Nonnull final List<ChildVppReader<? extends ChildOf<C>>> childReaders,
                                  @Nonnull final ListVppReaderCustomizer<C, K, B> customizer) {
        this(managedDataObjectType, childReaders, VppReaderUtils.<C>emptyAugReaderList(), customizer);
    }

    public CompositeListVppReader(@Nonnull final Class<C> managedDataObjectType,
                                  @Nonnull final ListVppReaderCustomizer<C, K, B> customizer) {
        this(managedDataObjectType, VppReaderUtils.<C>emptyChildReaderList(), VppReaderUtils.<C>emptyAugReaderList(),
            customizer);
    }

    @Override
    protected List<C> readCurrent(@Nonnull final InstanceIdentifier<C> id) {
        if(shouldReadAll(id)) {
            return readList(id);
        }
        return super.readCurrent(id);
    }

    @Override
    public void read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                     @Nonnull final Builder<? extends DataObject> parentBuilder) {
        final InstanceIdentifier<C> currentId = getCurrentId(id);

        final List<C> ifcs;
        if (shouldReadAll(id)) {
            ifcs = readList(currentId);
        } else {
            final Optional<? extends DataObject> readSingle = Optional.fromNullable(read(id).get(0));
            final Optional<C> read = readSingle.transform(new Function<DataObject, C>() {
                @Override
                public C apply(final DataObject input) {
                    Preconditions.checkArgument(getManagedDataObjectType().getTargetType().isAssignableFrom(input.getClass()));
                    return getManagedDataObjectType().getTargetType().cast(input);
                }
            });
            ifcs = read.isPresent() ? Collections.singletonList(read.get()) : Collections.<C>emptyList();
        }

        customizer.merge(parentBuilder, ifcs);
    }

    private List<C> readList(@Nonnull final InstanceIdentifier<C> id) {
        Preconditions.checkArgument(id.getTargetType().equals(getManagedDataObjectType().getTargetType()),
            "Id %s does not contain expected type %s", id, getManagedDataObjectType());

        return Lists.transform(customizer.getAllIds(id), new Function<InstanceIdentifier<? extends DataObject>, C>() {
                @Override
                public C apply(final InstanceIdentifier<? extends DataObject> input) {
                    final List<? extends DataObject> read = read(input);
                    Preconditions.checkState(read.size() == 1);
                    Preconditions.checkArgument(getManagedDataObjectType().getTargetType().isAssignableFrom(read.get(0).getClass()));
                    return getManagedDataObjectType().getTargetType().cast(read.get(0));
                }
            });
    }

     private boolean shouldReadAll(@Nonnull final InstanceIdentifier<? extends DataObject> id) {
        final InstanceIdentifier instanceIdentifier = id.firstIdentifierOf(getManagedDataObjectType().getTargetType());
        return instanceIdentifier == null || instanceIdentifier.isWildcarded();
    }

    @Override
    protected void readCurrentAttributes(@Nonnull final InstanceIdentifier<C> id, @Nonnull final B builder) {
        customizer.readCurrentAttributes(id, builder);
    }

    @Override
    protected B getBuilder(@Nonnull final InstanceIdentifier<? extends DataObject> id) {
        return customizer.getBuilder(id.firstKeyOf(getManagedDataObjectType().getTargetType()));
    }

}
