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

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.impl.trans.r.ChildVppReader;
import io.fd.honeycomb.v3po.impl.trans.r.VppReader;
import io.fd.honeycomb.v3po.impl.trans.r.util.ReflectionUtils;
import io.fd.honeycomb.v3po.impl.trans.r.util.VppRWUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
abstract class AbstractCompositeVppReader<D extends DataObject, B extends Builder<D>> implements VppReader<D> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCompositeVppReader.class);

    private final Map<Class<? extends DataObject>, ChildVppReader<? extends ChildOf<D>>> childReaders;
    private final Map<Class<? extends DataObject>, ChildVppReader<? extends Augmentation<D>>> augReaders;
    private final InstanceIdentifier<D> instanceIdentifier;

    AbstractCompositeVppReader(final Class<D> managedDataObjectType,
            final List<ChildVppReader<? extends ChildOf<D>>> childReaders,
            final List<ChildVppReader<? extends Augmentation<D>>> augReaders) {
        this.childReaders = VppRWUtils.uniqueLinkedIndex(childReaders, VppRWUtils.MANAGER_CLASS_FUNCTION);
        this.augReaders = VppRWUtils.uniqueLinkedIndex(augReaders, VppRWUtils.MANAGER_CLASS_AUG_FUNCTION);
        this.instanceIdentifier = InstanceIdentifier.create(managedDataObjectType);
    }

    @Nonnull
    @Override
    public final InstanceIdentifier<D> getManagedDataObjectType() {
        return instanceIdentifier;
    }

    /**
     * @param id {@link InstanceIdentifier} pointing to current node. In case of keyed list, key must be present.
     */
    protected List<D> readCurrent(final InstanceIdentifier<D> id) {
        LOG.debug("{}: Reading current: {}", this, id);
        final B builder = getBuilder(id);
        // Cache empty value to determine if anything has changed later TODO cache in a field
        final D emptyValue = builder.build();

        LOG.trace("{}: Reading current attributes", this);
        readCurrentAttributes(id, builder);

        // TODO expect exceptions from reader
        for (ChildVppReader<? extends ChildOf<D>> child : childReaders.values()) {
            LOG.debug("{}: Reading child from: {}", this, child);
            child.read(id, builder);
        }

        for (ChildVppReader<? extends Augmentation<D>> child : augReaders.values()) {
            LOG.debug("{}: Reading augment from: {}", this, child);
            child.read(id, builder);
        }

        // Need to check whether anything was filled in to determine if data is present or not.
        final D built = builder.build();
        final List<D> read = built.equals(emptyValue)
            ? Collections.<D>emptyList()
            : Collections.singletonList(built);

        LOG.debug("{}: Current node read successfully. Result: {}", this, read);
        return read;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public List<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id) {
        LOG.trace("{}: Reading : {}", this, id);
        if (id.getTargetType().equals(getManagedDataObjectType().getTargetType())) {
            return readCurrent((InstanceIdentifier<D>) id);
        } else {
            return readSubtree(id);
        }
    }

    private List<? extends DataObject> readSubtree(final InstanceIdentifier<? extends DataObject> id) {
        LOG.debug("{}: Reading subtree: {}", this, id);
        final Class<? extends DataObject> next = VppRWUtils.getNextId(id, getManagedDataObjectType()).getType();
        final ChildVppReader<? extends ChildOf<D>> vppReader = childReaders.get(next);

        if (vppReader != null) {
            LOG.debug("{}: Reading subtree: {} from: {}", this, id, vppReader);
            return vppReader.read(id);
        } else {
            LOG.debug("{}: Dedicated subtree reader missing for: {}. Reading current and filtering", this, next);
            // If there's no dedicated reader, use read current
            final InstanceIdentifier<D> currentId = VppRWUtils.cutId(id, getManagedDataObjectType());
            final List<D> current = readCurrent(currentId);
            // then perform post-reading filtering (return only requested sub-node)
            final List<? extends DataObject> readSubtree = current.isEmpty()
                ? current
                : filterSubtree(current, id, getManagedDataObjectType().getTargetType());

            LOG.debug("{}: Subtree: {} read successfully. Result: {}", this, id, readSubtree);
            return readSubtree;
        }
    }

    /**
     * Fill in current node's attributes
     *
     * @param id {@link InstanceIdentifier} pointing to current node. In case of keyed list, key must be present.
     * @param builder Builder object for current node where the read attributes must be placed
     */
    protected abstract void readCurrentAttributes(final InstanceIdentifier<D> id, B builder);

    /**
     * Return new instance of a builder object for current node
     *
     * @param id {@link InstanceIdentifier} pointing to current node. In case of keyed list, key must be present.
     * @return Builder object for current node type
     */
    protected abstract B getBuilder(InstanceIdentifier<D> id);

    // TODO move filtering out of here into a dedicated Filter ifc
    @Nonnull
    private static List<? extends DataObject> filterSubtree(@Nonnull final List<? extends DataObject> built,
                                                            @Nonnull final InstanceIdentifier<? extends DataObject> absolutPath,
                                                            @Nonnull final Class<?> managedType) {
        // TODO is there a better way than reflection ? e.g. convert into NN and filter out with a utility
        // FIXME this needs to be recursive. right now it expects only 1 additional element in ID + test

        List<DataObject> filtered = Lists.newArrayList();
        for (DataObject parent : built) {
            final InstanceIdentifier.PathArgument nextId =
                VppRWUtils.getNextId(absolutPath, InstanceIdentifier.create(parent.getClass()));

            Optional<Method> method = ReflectionUtils.findMethodReflex(managedType, "get",
                Collections.<Class<?>>emptyList(), nextId.getType());

            if (method.isPresent()) {
                filterSingle(filtered, parent, nextId, method);
            } else {
                // List child nodes
                method = ReflectionUtils.findMethodReflex(managedType,
                    "get" + nextId.getType().getSimpleName(), Collections.<Class<?>>emptyList(), List.class);

                if (method.isPresent()) {
                    filterList(filtered, parent, nextId, method);
                } else {
                    throw new IllegalStateException(
                        "Unable to filter " + nextId + " from " + parent + " getters not found using reflexion");
                }
            }
        }

        return filtered;
    }

    @SuppressWarnings("unchecked")
    private static void filterList(final List<DataObject> filtered, final DataObject parent,
                                   final InstanceIdentifier.PathArgument nextId, final Optional<Method> method) {
        final List<? extends DataObject> invoke = (List<? extends DataObject>)invoke(method.get(), nextId, parent);

        if (nextId instanceof InstanceIdentifier.IdentifiableItem<?, ?>) {
            final Identifier key = ((InstanceIdentifier.IdentifiableItem) nextId).getKey();
            filtered.addAll(Collections2.filter(invoke, new Predicate<DataObject>() {
                @Override
                public boolean apply(@Nullable final DataObject input) {
                    final Optional<Method> keyGetter =
                        ReflectionUtils.findMethodReflex(nextId.getType(), "get",
                            Collections.<Class<?>>emptyList(), key.getClass());
                    final Object actualKey;
                    actualKey = invoke(keyGetter.get(), nextId, input);
                    return key.equals(actualKey);
                }
            }));
        } else {
            filtered.addAll(invoke);
        }
    }

    private static void filterSingle(final List<DataObject> filtered, final DataObject parent,
                                     final InstanceIdentifier.PathArgument nextId, final Optional<Method> method) {
        filtered.add(nextId.getType().cast(invoke(method.get(), nextId, parent)));
    }

    private static Object invoke(final Method method,
                                 final InstanceIdentifier.PathArgument nextId, final DataObject parent) {
        try {
            return method.invoke(parent);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to get " + nextId + " from " + parent, e);
        }
    }

    @Override
    public String toString() {
        return String.format("Reader[%s]", getManagedDataObjectType().getTargetType().getSimpleName());
    }
}
