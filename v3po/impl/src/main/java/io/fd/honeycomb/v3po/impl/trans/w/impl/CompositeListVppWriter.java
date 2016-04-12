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

package io.fd.honeycomb.v3po.impl.trans.w.impl;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.fd.honeycomb.v3po.impl.trans.util.VppRWUtils;
import io.fd.honeycomb.v3po.impl.trans.w.ChildVppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.WriteContext;
import io.fd.honeycomb.v3po.impl.trans.w.impl.spi.ListVppWriterCustomizer;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class CompositeListVppWriter<D extends DataObject & Identifiable<K>, K extends Identifier<D>> extends AbstractCompositeVppWriter<D>
    implements ChildVppWriter<D> {

    public static final Function<DataObject, Object> INDEX_FUNCTION = new Function<DataObject, Object>() {
        @Override
        public Object apply(final DataObject input) {
            return input instanceof Identifiable<?>
                ? ((Identifiable<?>) input).getKey()
                : input;
        }
    };


    private final ListVppWriterCustomizer<D, K> customizer;

    public CompositeListVppWriter(@Nonnull final Class<D> type,
                                  @Nonnull final List<ChildVppWriter<? extends ChildOf<D>>> childWriters,
                                  @Nonnull final List<ChildVppWriter<? extends Augmentation<D>>> augWriters,
                                  @Nonnull final ListVppWriterCustomizer<D, K> customizer) {
        super(type, childWriters, augWriters);
        this.customizer = customizer;
    }

    public CompositeListVppWriter(@Nonnull final Class<D> type,
                                  @Nonnull final List<ChildVppWriter<? extends ChildOf<D>>> childWriters,
                                  @Nonnull final ListVppWriterCustomizer<D, K> customizer) {
        this(type, childWriters, VppRWUtils.<D>emptyAugWriterList(), customizer);
    }

    public CompositeListVppWriter(@Nonnull final Class<D> type,
                                  @Nonnull final ListVppWriterCustomizer<D, K> customizer) {
        this(type, VppRWUtils.<D>emptyChildWriterList(), VppRWUtils.<D>emptyAugWriterList(), customizer);

    }

    @Override
    protected void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D data,
                                          @Nonnull final WriteContext ctx) {
        customizer.writeCurrentAttributes(id, data, ctx.getContext());
    }

    @Override
    protected void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                           @Nonnull final WriteContext ctx) {
        customizer.deleteCurrentAttributes(id, dataBefore, ctx.getContext());
    }

    @Override
    protected void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                           @Nonnull final D dataAfter, @Nonnull final WriteContext ctx) {
        customizer.updateCurrentAttributes(id, dataBefore, dataAfter, ctx.getContext());
    }

    @Override
    public void writeChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                           @Nonnull final DataObject parentData,
                           @Nonnull final WriteContext ctx) {
        final InstanceIdentifier<D> currentId = VppRWUtils.appendTypeToId(parentId, getManagedDataObjectType());
        final List<D> currentData = customizer.extract(currentId, parentData);
        for (D entry : currentData) {
            writeCurrent(currentId, entry, ctx);
        }
    }

    @Override
    public void deleteChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                            @Nonnull final DataObject parentDataBefore,
                            @Nonnull final WriteContext ctx) {
        final InstanceIdentifier<D> currentId = VppRWUtils.appendTypeToId(parentId, getManagedDataObjectType());
        final List<D> dataBefore = customizer.extract(currentId, parentDataBefore);
        for (D entry : dataBefore) {
            deleteCurrent(currentId, entry, ctx);
        }
    }

    @Override
    public void updateChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                            @Nonnull final DataObject parentDataBefore, @Nonnull final DataObject parentDataAfter,
                            @Nonnull final WriteContext ctx) {
        final InstanceIdentifier<D> currentId = VppRWUtils.appendTypeToId(parentId, getManagedDataObjectType());
        final ImmutableMap<Object, D>
            dataBefore = Maps.uniqueIndex(customizer.extract(currentId, parentDataBefore), INDEX_FUNCTION);
        final ImmutableMap<Object, D>
            dataAfter = Maps.uniqueIndex(customizer.extract(currentId, parentDataAfter), INDEX_FUNCTION);

        for (Map.Entry<Object, D> after : dataAfter.entrySet()) {
            final D before = dataBefore.get(after.getKey());
            if(before == null) {
                writeCurrent(currentId, after.getValue(), ctx);
            } else {
                updateCurrent(currentId, before, after.getValue(), ctx);
            }
        }

        // Delete the rest in dataBefore
        for (Object deletedNodeKey : Sets.difference(dataBefore.keySet(), dataAfter.keySet())) {
            final D deleted = dataBefore.get(deletedNodeKey);
            deleteCurrent(currentId, deleted, ctx);
        }

    }

    @Override
    protected void writeCurrent(final InstanceIdentifier<D> id, final D data, final WriteContext ctx) {
        // Make sure the key is present
        if(isWildcarded(id)) {
            super.writeCurrent(getSpecificId(id, data), data, ctx);
        } else {
            super.writeCurrent(id, data, ctx);
        }
    }

    @Override
    protected void updateCurrent(final InstanceIdentifier<D> id, final D dataBefore, final D dataAfter,
                                 final WriteContext ctx) {
        // Make sure the key is present
        if(isWildcarded(id)) {
            super.updateCurrent(getSpecificId(id, dataBefore), dataBefore, dataAfter, ctx);
        } else {
            super.updateCurrent(id, dataBefore, dataAfter, ctx);
        }
    }

    @Override
    protected void deleteCurrent(final InstanceIdentifier<D> id, final D dataBefore, final WriteContext ctx) {
        // Make sure the key is present
        if(isWildcarded(id)) {
            super.deleteCurrent(getSpecificId(id, dataBefore), dataBefore, ctx);
        } else {
            super.deleteCurrent(id, dataBefore, ctx);
        }
    }

    private boolean isWildcarded(final InstanceIdentifier<D> id) {
        return id.firstIdentifierOf(getManagedDataObjectType().getTargetType()).isWildcarded();
    }

    private InstanceIdentifier<D> getSpecificId(final InstanceIdentifier<D> currentId, final D current) {
        return VppRWUtils.replaceLastInId(currentId,
            new InstanceIdentifier.IdentifiableItem<>(currentId.getTargetType(), current.getKey()));
    }
}
