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
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import io.fd.honeycomb.v3po.translate.impl.TraversalType;
import io.fd.honeycomb.v3po.translate.util.ReflectionUtils;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.read.ChildReader;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.read.Reader;
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
abstract class AbstractCompositeReader<D extends DataObject, B extends Builder<D>> implements Reader<D> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCompositeReader.class);

    private final Map<Class<? extends DataObject>, ChildReader<? extends ChildOf<D>>> childReaders;
    private final Map<Class<? extends DataObject>, ChildReader<? extends Augmentation<D>>> augReaders;
    private final InstanceIdentifier<D> instanceIdentifier;
    private final TraversalType traversalType;

    AbstractCompositeReader(final Class<D> managedDataObjectType,
                            final List<ChildReader<? extends ChildOf<D>>> childReaders,
                            final List<ChildReader<? extends Augmentation<D>>> augReaders,
                            final TraversalType traversalType) {
        this.traversalType = traversalType;
        this.childReaders = RWUtils.uniqueLinkedIndex(childReaders, RWUtils.MANAGER_CLASS_FUNCTION);
        this.augReaders = RWUtils.uniqueLinkedIndex(augReaders, RWUtils.MANAGER_CLASS_AUG_FUNCTION);
        this.instanceIdentifier = InstanceIdentifier.create(managedDataObjectType);
    }

    @Nonnull
    @Override
    public final InstanceIdentifier<D> getManagedDataObjectType() {
        return instanceIdentifier;
    }

    /**
     * @param id {@link InstanceIdentifier} pointing to current node. In case of keyed list, key must be present.
     *
     */
    protected Optional<D> readCurrent(final InstanceIdentifier<D> id,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        LOG.debug("{}: Reading current: {}", this, id);
        final B builder = getBuilder(id);
        // Cache empty value to determine if anything has changed later TODO cache in a field
        final D emptyValue = builder.build();

        switch (traversalType) {
            case PREORDER: {
                LOG.trace("{}: Reading current attributes", this);
                readCurrentAttributes(id, builder, ctx);
                readChildren(id, ctx, builder);
            }
            case POSTORDER: {
                readChildren(id, ctx, builder);
                LOG.trace("{}: Reading current attributes", this);
                readCurrentAttributes(id, builder, ctx);
            }
        }

        // Need to check whether anything was filled in to determine if data is present or not.
        final D built = builder.build();
        final Optional<D> read = built.equals(emptyValue)
            ? Optional.<D>absent()
            : Optional.of(built);

        LOG.debug("{}: Current node read successfully. Result: {}", this, read);
        return read;
    }

    private void readChildren(final InstanceIdentifier<D> id, final @Nonnull ReadContext ctx, final B builder)
        throws ReadFailedException {
        // TODO expect exceptions from reader
        for (ChildReader<? extends ChildOf<D>> child : childReaders.values()) {
            LOG.debug("{}: Reading child from: {}", this, child);
            child.read(id, builder, ctx);
        }

        for (ChildReader<? extends Augmentation<D>> child : augReaders.values()) {
            LOG.debug("{}: Reading augment from: {}", this, child);
            child.read(id, builder, ctx);
        }
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Optional<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                               @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        LOG.trace("{}: Reading : {}", this, id);
        if (id.getTargetType().equals(getManagedDataObjectType().getTargetType())) {
            return readCurrent((InstanceIdentifier<D>) id, ctx);
        } else {
            return readSubtree(id, ctx);
        }
    }

    private Optional<? extends DataObject> readSubtree(final InstanceIdentifier<? extends DataObject> id,
                                                       @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        LOG.debug("{}: Reading subtree: {}", this, id);
        final Class<? extends DataObject> next = RWUtils.getNextId(id, getManagedDataObjectType()).getType();
        final ChildReader<? extends ChildOf<D>> reader = childReaders.get(next);

        if (reader != null) {
            LOG.debug("{}: Reading subtree: {} from: {}", this, id, reader);
            return reader.read(id, ctx);
        } else {
            LOG.debug("{}: Dedicated subtree reader missing for: {}. Reading current and filtering", this, next);
            // If there's no dedicated reader, use read current
            final InstanceIdentifier<D> currentId = RWUtils.cutId(id, getManagedDataObjectType());
            final Optional<D> current = readCurrent(currentId, ctx);
            // then perform post-reading filtering (return only requested sub-node)
            final Optional<? extends DataObject> readSubtree = current.isPresent()
                ? filterSubtree(current.get(), id, getManagedDataObjectType().getTargetType())
                : current;

            LOG.debug("{}: Subtree: {} read successfully. Result: {}", this, id, readSubtree);
            return readSubtree;
        }
    }

    /**
     * Fill in current node's attributes
     *
     * @param id {@link InstanceIdentifier} pointing to current node. In case of keyed list, key must be present.
     * @param builder Builder object for current node where the read attributes must be placed
     * @param ctx Current read context
     */
    protected abstract void readCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final B builder,
                                                  @Nonnull final ReadContext ctx) throws ReadFailedException;

    /**
     * Return new instance of a builder object for current node
     *
     * @param id {@link InstanceIdentifier} pointing to current node. In case of keyed list, key must be present.
     * @return Builder object for current node type
     */
    protected abstract B getBuilder(InstanceIdentifier<D> id);

    // TODO move filtering out of here into a dedicated Filter ifc
    @Nonnull
    private static Optional<? extends DataObject> filterSubtree(@Nonnull final DataObject parent,
                                                            @Nonnull final InstanceIdentifier<? extends DataObject> absolutPath,
                                                            @Nonnull final Class<?> managedType) {
        // TODO is there a better way than reflection ? e.g. convert into NN and filter out with a utility
        // FIXME this needs to be recursive. right now it expects only 1 additional element in ID + test

        final InstanceIdentifier.PathArgument nextId =
            RWUtils.getNextId(absolutPath, InstanceIdentifier.create(parent.getClass()));

        Optional<Method> method = ReflectionUtils.findMethodReflex(managedType, "get",
            Collections.<Class<?>>emptyList(), nextId.getType());

        if (method.isPresent()) {
            return Optional.fromNullable(filterSingle(parent, nextId, method.get()));
        } else {
            // List child nodes
            method = ReflectionUtils.findMethodReflex(managedType,
                "get" + nextId.getType().getSimpleName(), Collections.<Class<?>>emptyList(), List.class);

            if (method.isPresent()) {
                return filterList(parent, nextId, method.get());
            } else {
                throw new IllegalStateException(
                    "Unable to filter " + nextId + " from " + parent + " getters not found using reflexion");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Optional<? extends DataObject> filterList(final DataObject parent,
                                                             final InstanceIdentifier.PathArgument nextId,
                                                             final Method method) {
        final List<? extends DataObject> invoke = (List<? extends DataObject>) invoke(method, nextId, parent);

        checkArgument(nextId instanceof InstanceIdentifier.IdentifiableItem<?, ?>,
            "Unable to perform wildcarded read for %s", nextId);
        final Identifier key = ((InstanceIdentifier.IdentifiableItem) nextId).getKey();
        return Iterables.tryFind(invoke, new Predicate<DataObject>() {
            @Override
            public boolean apply(@Nullable final DataObject input) {
                final Optional<Method> keyGetter =
                    ReflectionUtils.findMethodReflex(nextId.getType(), "get",
                        Collections.<Class<?>>emptyList(), key.getClass());
                final Object actualKey;
                actualKey = invoke(keyGetter.get(), nextId, input);
                return key.equals(actualKey);
            }
        });
    }

    private static DataObject filterSingle(final DataObject parent,
                                           final InstanceIdentifier.PathArgument nextId, final Method method) {
        return nextId.getType().cast(invoke(method, nextId, parent));
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
