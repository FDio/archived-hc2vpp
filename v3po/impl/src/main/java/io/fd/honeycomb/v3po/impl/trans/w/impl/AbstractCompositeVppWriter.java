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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.fd.honeycomb.v3po.impl.trans.util.VppRWUtils;
import io.fd.honeycomb.v3po.impl.trans.w.ChildVppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.VppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.WriteContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCompositeVppWriter<D extends DataObject> implements VppWriter<D> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCompositeVppWriter.class);

    public static final Function<DataObject, Object> INDEX_FUNCTION = new Function<DataObject, Object>() {
        @Override
        public Object apply(final DataObject input) {
            return input instanceof Identifiable<?>
                ? ((Identifiable<?>) input).getKey()
                : input;
        }
    };

    private final Map<Class<? extends DataObject>, ChildVppWriter<? extends ChildOf<D>>> childReaders;
    private final Map<Class<? extends DataObject>, ChildVppWriter<? extends Augmentation<D>>> augReaders;
    private final InstanceIdentifier<D> instanceIdentifier;

    public AbstractCompositeVppWriter(final Class<D> type,
                                      final List<ChildVppWriter<? extends ChildOf<D>>> childReaders,
                                      final List<ChildVppWriter<? extends Augmentation<D>>> augReaders) {
        this.instanceIdentifier = InstanceIdentifier.create(type);
        this.childReaders = VppRWUtils.uniqueLinkedIndex(childReaders, VppRWUtils.MANAGER_CLASS_FUNCTION);
        this.augReaders = VppRWUtils.uniqueLinkedIndex(augReaders, VppRWUtils.MANAGER_CLASS_AUG_FUNCTION);
    }

    protected void writeCurrent(final InstanceIdentifier<D> id, final D data, final WriteContext ctx) {
        LOG.debug("{}: Writing current: {} data: {}", this, id, data);

        LOG.trace("{}: Writing current attributes", this);
        writeCurrentAttributes(id, data, ctx);

        for (ChildVppWriter<? extends ChildOf<D>> child : childReaders.values()) {
            LOG.debug("{}: Writing child in: {}", this, child);
            child.writeChild(id, data, ctx);
        }

        for (ChildVppWriter<? extends Augmentation<D>> child : augReaders.values()) {
            LOG.debug("{}: Writing augment in: {}", this, child);
            child.writeChild(id, data, ctx);
        }

        LOG.debug("{}: Current node written successfully", this);
    }

    protected void updateCurrent(final InstanceIdentifier<D> id, final D dataBefore, final D dataAfter,
                                 final WriteContext ctx) {
        LOG.debug("{}: Updating current: {} dataBefore: {}, datAfter: {}", this, id, dataBefore, dataAfter);

        if(dataBefore.equals(dataAfter)) {
            LOG.debug("{}: Skipping current(no update): {}", this, id);
            // No change, ignore
            return;
        }

        LOG.trace("{}: Updating current attributes", this);
        updateCurrentAttributes(id, dataBefore, dataAfter, ctx);

        for (ChildVppWriter<? extends ChildOf<D>> child : childReaders.values()) {
            LOG.debug("{}: Updating child in: {}", this, child);
            child.updateChild(id, dataBefore, dataAfter, ctx);
        }

        for (ChildVppWriter<? extends Augmentation<D>> child : augReaders.values()) {
            LOG.debug("{}: Updating augment in: {}", this, child);
            child.updateChild(id, dataBefore, dataAfter, ctx);
        }

        LOG.debug("{}: Current node updated successfully", this);
    }

    protected void deleteCurrent(final InstanceIdentifier<D> id, final D dataBefore, final WriteContext ctx) {
        LOG.debug("{}: Deleting current: {} dataBefore: {}", this, id, dataBefore);

        // delete in reversed order
        for (ChildVppWriter<? extends Augmentation<D>> child : reverseCollection(augReaders.values())) {
            LOG.debug("{}: Deleting augment in: {}", this, child);
            child.deleteChild(id, dataBefore, ctx);
        }

        for (ChildVppWriter<? extends ChildOf<D>> child : reverseCollection(childReaders.values())) {
            LOG.debug("{}: Deleting child in: {}", this, child);
            child.deleteChild(id, dataBefore, ctx);
        }

        LOG.trace("{}: Deleting current attributes", this);
        deleteCurrentAttributes(id, dataBefore, ctx);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                       @Nonnull final List<? extends DataObject> dataBefore,
                       @Nonnull final List<? extends DataObject> dataAfter,
                       @Nonnull final WriteContext ctx) {
        LOG.debug("{}: Updating : {}", this, id);
        LOG.trace("{}: Updating : {}, from: {} to: {}", this, id, dataBefore, dataAfter);

        if (idPointsToCurrent(id)) {
            if(isWrite(dataBefore, dataAfter)) {
                writeAll((InstanceIdentifier<D>) id, dataAfter, ctx);
            } else if(isDelete(dataBefore, dataAfter)) {
                deleteAll((InstanceIdentifier<D>) id, dataBefore, ctx);
            } else {
                checkArgument(!dataBefore.isEmpty() && !dataAfter.isEmpty(), "No data to process");
                updateAll((InstanceIdentifier<D>) id, dataBefore, dataAfter, ctx);
            }
        } else {
            if (isWrite(dataBefore, dataAfter)) {
                writeSubtree(id, dataAfter, ctx);
            } else if (isDelete(dataBefore, dataAfter)) {
                deleteSubtree(id, dataBefore, ctx);
            } else {
                checkArgument(!dataBefore.isEmpty() && !dataAfter.isEmpty(), "No data to process");
                updateSubtree(id, dataBefore, dataAfter, ctx);
            }
        }
    }

    protected void updateAll(final @Nonnull InstanceIdentifier<D> id,
                             final @Nonnull List<? extends DataObject> dataBefore,
                             final @Nonnull List<? extends DataObject> dataAfter, final WriteContext ctx) {
        LOG.trace("{}: Updating all : {}", this, id);

        final Map<Object, ? extends DataObject> indexedAfter = indexData(dataAfter);
        final Map<Object, ? extends DataObject> indexedBefore = indexData(dataBefore);

        for (Map.Entry<Object, ? extends DataObject> after : indexedAfter.entrySet()) {
            final DataObject before = indexedBefore.get(after.getKey());
            if(before == null) {
                writeCurrent(id, castToManaged(after.getValue()), ctx);
            } else {
                updateCurrent(id, castToManaged(before), castToManaged(after.getValue()), ctx);
            }
        }

        // Delete the rest in dataBefore
        for (Object deletedNodeKey : Sets.difference(indexedBefore.keySet(), indexedAfter.keySet())) {
            final DataObject deleted = indexedBefore.get(deletedNodeKey);
            deleteCurrent(id, castToManaged(deleted), ctx);
        }
    }

    private static Map<Object, ? extends DataObject> indexData(final List<? extends DataObject> data) {
        return Maps.uniqueIndex(data, INDEX_FUNCTION);
    }

    protected void deleteAll(final @Nonnull InstanceIdentifier<D> id,
                             final @Nonnull List<? extends DataObject> dataBefore, final WriteContext ctx) {
        LOG.trace("{}: Deleting all : {}", this, id);
        for (DataObject singleValue : dataBefore) {
            checkArgument(getManagedDataObjectType().getTargetType().isAssignableFrom(singleValue.getClass()));
            deleteCurrent(id, castToManaged(singleValue), ctx);
        }
    }

    private D castToManaged(final DataObject data) {
        checkArgument(getManagedDataObjectType().getTargetType().isAssignableFrom(data.getClass()));
        return getManagedDataObjectType().getTargetType().cast(data);
    }

    protected void writeAll(final @Nonnull InstanceIdentifier<D> id,
                            final @Nonnull List<? extends DataObject> dataAfter, final WriteContext ctx) {
        LOG.trace("{}: Writing all : {}", this, id);
        for (DataObject singleValue : dataAfter) {
            checkArgument(getManagedDataObjectType().getTargetType().isAssignableFrom(singleValue.getClass()));
            writeCurrent(id, castToManaged(singleValue), ctx);
        }
    }

    private static boolean isWrite(final List<? extends DataObject> dataBefore,
                                    final List<? extends DataObject> dataAfter) {
        return dataBefore.isEmpty() && !dataAfter.isEmpty();
    }

    private static boolean isDelete(final List<? extends DataObject> dataBefore,
                                    final List<? extends DataObject> dataAfter) {
        return dataAfter.isEmpty() && !dataBefore.isEmpty();
    }

    private void writeSubtree(final InstanceIdentifier<? extends DataObject> id,
                              final List<? extends DataObject> dataAfter, final WriteContext ctx) {
        LOG.debug("{}: Writing subtree: {}", this, id);
        final VppWriter<? extends ChildOf<D>> vppWriter = getNextWriter(id);

        if (vppWriter != null) {
            LOG.debug("{}: Writing subtree: {} in: {}", this, id, vppWriter);
            vppWriter.update(id, Collections.<DataObject>emptyList(), dataAfter, ctx);
        } else {
            // If there's no dedicated writer, use write current
            // But we need current data after to do so
            final InstanceIdentifier<D> currentId = VppRWUtils.cutId(id, getManagedDataObjectType());
            List<? extends DataObject> currentDataAfter = ctx.readAfter(currentId);
            LOG.debug("{}: Dedicated subtree writer missing for: {}. Writing current.", this,
                VppRWUtils.getNextId(id, getManagedDataObjectType()).getType(), currentDataAfter);
            writeAll(currentId, currentDataAfter, ctx);
        }
    }

    private boolean idPointsToCurrent(final @Nonnull InstanceIdentifier<? extends DataObject> id) {
        return id.getTargetType().equals(getManagedDataObjectType().getTargetType());
    }

    @SuppressWarnings("unchecked")
    private void deleteSubtree(final InstanceIdentifier<? extends DataObject> id,
                               final List<? extends DataObject> dataBefore, final WriteContext ctx) {
        LOG.debug("{}: Deleting subtree: {}", this, id);
        final VppWriter<? extends ChildOf<D>> vppWriter = getNextWriter(id);

        if (vppWriter != null) {
            LOG.debug("{}: Deleting subtree: {} in: {}", this, id, vppWriter);
            vppWriter.update(id, dataBefore, Collections.<DataObject>emptyList(), ctx);
        } else {
            updateSubtreeFromCurrent(id, ctx);
        }
    }

    private void updateSubtreeFromCurrent(final InstanceIdentifier<? extends DataObject> id, final WriteContext ctx) {
        final InstanceIdentifier<D> currentId = VppRWUtils.cutId(id, getManagedDataObjectType());
        List<? extends DataObject> currentDataBefore = ctx.readBefore(currentId);
        List<? extends DataObject> currentDataAfter = ctx.readAfter(currentId);
        LOG.debug("{}: Dedicated subtree writer missing for: {}. Updating current without subtree", this,
            VppRWUtils.getNextId(id, getManagedDataObjectType()).getType(), currentDataAfter);
        updateAll((InstanceIdentifier<D>) id, currentDataBefore, currentDataAfter, ctx);
    }

    @SuppressWarnings("unchecked")
    private void updateSubtree(final InstanceIdentifier<? extends DataObject> id,
                               final List<? extends DataObject> dataBefore,
                               final List<? extends DataObject> dataAfter,
                               final WriteContext ctx) {
        LOG.debug("{}: Updating subtree: {}", this, id);
        final VppWriter<? extends ChildOf<D>> vppWriter = getNextWriter(id);

        if (vppWriter != null) {
            LOG.debug("{}: Updating subtree: {} in: {}", this, id, vppWriter);
            vppWriter.update(id, dataBefore, dataAfter, ctx);
        } else {
            updateSubtreeFromCurrent(id, ctx);
        }
    }

    private VppWriter<? extends ChildOf<D>> getNextWriter(final InstanceIdentifier<? extends DataObject> id) {
        final Class<? extends DataObject> next = VppRWUtils.getNextId(id, getManagedDataObjectType()).getType();
        return childReaders.get(next);
    }

    private static <T> List<T> reverseCollection(final Collection<T> original) {
        // TODO find a better reverse mechanism (probably a different collection for child readers is necessary)
        final ArrayList<T> list = Lists.newArrayList(original);
        Collections.reverse(list);
        return list;
    }

    protected abstract void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                                   @Nonnull final D data,
                                                   @Nonnull final WriteContext ctx);

    protected abstract void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                                    @Nonnull final D dataBefore,
                                                    @Nonnull final WriteContext ctx);

    protected abstract void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                                    @Nonnull final D dataBefore,
                                                    @Nonnull final D dataAfter,
                                                    @Nonnull final WriteContext ctx);

    @Nonnull
    @Override
    public InstanceIdentifier<D> getManagedDataObjectType() {
        return instanceIdentifier;
    }


    @Override
    public String toString() {
        return String.format("Writer[%s]", getManagedDataObjectType().getTargetType().getSimpleName());
    }
}
