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

package io.fd.honeycomb.v3po.translate.impl.write;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.fd.honeycomb.v3po.translate.impl.TraversalType;
import io.fd.honeycomb.v3po.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.write.ChildWriter;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class CompositeListWriter<D extends DataObject & Identifiable<K>, K extends Identifier<D>> extends
    AbstractCompositeWriter<D>
    implements ChildWriter<D> {

    private static final Function<DataObject, Object> INDEX_FUNCTION = input -> input instanceof Identifiable<?>
        ? ((Identifiable<?>) input).getKey()
        : input;


    private final ListWriterCustomizer<D, K> customizer;

    public CompositeListWriter(@Nonnull final Class<D> type,
                               @Nonnull final List<ChildWriter<? extends ChildOf<D>>> childWriters,
                               @Nonnull final List<ChildWriter<? extends Augmentation<D>>> augWriters,
                               @Nonnull final ListWriterCustomizer<D, K> customizer) {
        this(type, childWriters, augWriters, customizer, TraversalType.PREORDER);
    }

    public CompositeListWriter(@Nonnull final Class<D> type,
                               @Nonnull final List<ChildWriter<? extends ChildOf<D>>> childWriters,
                               @Nonnull final List<ChildWriter<? extends Augmentation<D>>> augWriters,
                               @Nonnull final ListWriterCustomizer<D, K> customizer,
                               @Nonnull final TraversalType traversalType) {
        super(type, childWriters, augWriters, traversalType);
        this.customizer = customizer;
    }

    public CompositeListWriter(@Nonnull final Class<D> type,
                               @Nonnull final List<ChildWriter<? extends ChildOf<D>>> childWriters,
                               @Nonnull final ListWriterCustomizer<D, K> customizer) {
        this(type, childWriters, RWUtils.<D>emptyAugWriterList(), customizer);
    }

    public CompositeListWriter(@Nonnull final Class<D> type,
                               @Nonnull final ListWriterCustomizer<D, K> customizer) {
        this(type, RWUtils.<D>emptyChildWriterList(), RWUtils.<D>emptyAugWriterList(), customizer);

    }

    @Override
    protected void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D data,
                                          @Nonnull final WriteContext ctx) throws WriteFailedException {
        customizer.writeCurrentAttributes(id, data, ctx);
    }

    @Override
    protected void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                           @Nonnull final WriteContext ctx) throws WriteFailedException {
        customizer.deleteCurrentAttributes(id, dataBefore, ctx);
    }

    @Override
    protected void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                           @Nonnull final D dataAfter, @Nonnull final WriteContext ctx)
        throws WriteFailedException {
        customizer.updateCurrentAttributes(id, dataBefore, dataAfter, ctx);
    }

    @Override
    public void writeChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                           @Nonnull final DataObject parentData,
                           @Nonnull final WriteContext ctx) throws WriteFailedException {
        final InstanceIdentifier<D> currentId = RWUtils.appendTypeToId(parentId, getManagedDataObjectType());
        final Optional<List<D>> currentData = customizer.extract(currentId, parentData);
        if (currentData.isPresent()) {
            for (D entry : currentData.get()) {
                writeCurrent(currentId, entry, ctx);
            }
        }
    }

    @Override
    public void deleteChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                            @Nonnull final DataObject parentDataBefore,
                            @Nonnull final WriteContext ctx) throws WriteFailedException {
        final InstanceIdentifier<D> currentId = RWUtils.appendTypeToId(parentId, getManagedDataObjectType());
        final Optional<List<D>> dataBefore = customizer.extract(currentId, parentDataBefore);
        if (dataBefore.isPresent()) {
            for (D entry : dataBefore.get()) {
                deleteCurrent(currentId, entry, ctx);
            }
        }
    }

    private Map<Object, D> listOfIdentifiableToMap(Optional<List<D>> list) {
        if (list.isPresent()) {
            return Maps.uniqueIndex(list.get(), INDEX_FUNCTION);
        } else {
            return Collections.emptyMap();
        }

    }

    @Override
    public void updateChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                            @Nonnull final DataObject parentDataBefore, @Nonnull final DataObject parentDataAfter,
                            @Nonnull final WriteContext ctx) throws WriteFailedException {
        final InstanceIdentifier<D> currentId = RWUtils.appendTypeToId(parentId, getManagedDataObjectType());
        final Map<Object, D> dataBefore = listOfIdentifiableToMap(customizer.extract(currentId, parentDataBefore));
        final Map<Object, D> dataAfter = listOfIdentifiableToMap(customizer.extract(currentId, parentDataAfter));

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
    protected void writeCurrent(final InstanceIdentifier<D> id, final D data, final WriteContext ctx)
        throws WriteFailedException {
        // Make sure the key is present
        if(isWildcarded(id)) {
            super.writeCurrent(getSpecificId(id, data), data, ctx);
        } else {
            super.writeCurrent(id, data, ctx);
        }
    }

    @Override
    protected void updateCurrent(final InstanceIdentifier<D> id, final D dataBefore, final D dataAfter,
                                 final WriteContext ctx) throws WriteFailedException {
        // Make sure the key is present
        if(isWildcarded(id)) {
            super.updateCurrent(getSpecificId(id, dataBefore), dataBefore, dataAfter, ctx);
        } else {
            super.updateCurrent(id, dataBefore, dataAfter, ctx);
        }
    }

    @Override
    protected void deleteCurrent(final InstanceIdentifier<D> id, final D dataBefore, final WriteContext ctx)
        throws WriteFailedException {
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
        return RWUtils.replaceLastInId(currentId,
            new InstanceIdentifier.IdentifiableItem<>(currentId.getTargetType(), current.getKey()));
    }
}
