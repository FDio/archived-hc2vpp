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
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.impl.trans.ChildVppReader;
import io.fd.honeycomb.v3po.impl.trans.VppReader;
import io.fd.honeycomb.v3po.impl.trans.util.VppReaderUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
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

@Beta
abstract class AbstractCompositeVppReader<D extends DataObject, B extends Builder<D>> implements VppReader<D> {

    // TODO add debug + trace logs here and there

    private final Map<Class<? extends DataObject>, ChildVppReader<? extends ChildOf<D>>> childReaders;
    private final Map<Class<? extends DataObject>, ChildVppReader<? extends Augmentation<D>>> augReaders;
    private final InstanceIdentifier<D> instanceIdentifier;

    public AbstractCompositeVppReader(
            final Class<D> managedDataObjectType,
            final List<ChildVppReader<? extends ChildOf<D>>> childReaders,
            final List<ChildVppReader<? extends Augmentation<D>>> augReaders) {
        this.childReaders = childReadersToMap(childReaders);
        this.augReaders = augReadersToMap(augReaders);
        this.instanceIdentifier = InstanceIdentifier.create(managedDataObjectType);
    }

    protected final Map<Class<? extends DataObject>, ChildVppReader<? extends ChildOf<D>>> childReadersToMap(
            final List<ChildVppReader<? extends ChildOf<D>>> childReaders) {
        final LinkedHashMap<Class<? extends DataObject>, ChildVppReader<? extends ChildOf<D>>>
                classVppReaderLinkedHashMap = new LinkedHashMap<>();

        for (ChildVppReader<? extends ChildOf<D>> childReader : childReaders) {
            Preconditions.checkArgument(
                    classVppReaderLinkedHashMap.put(childReader.getManagedDataObjectType().getTargetType(), childReader) == null,
                    "Duplicate (%s) child readers detected under: %s", childReader.getManagedDataObjectType(),
                    getManagedDataObjectType());
        }

        return classVppReaderLinkedHashMap;
    }

    // FIXME add child/augReaders to one list and unify toMap helper methods + move to utils
    protected final Map<Class<? extends DataObject>, ChildVppReader<? extends Augmentation<D>>> augReadersToMap(
            final List<ChildVppReader<? extends Augmentation<D>>> childReaders) {
        final LinkedHashMap<Class<? extends DataObject>, ChildVppReader<? extends Augmentation<D>>>
                classVppReaderLinkedHashMap = new LinkedHashMap<>();

        for (ChildVppReader<? extends DataObject> childReader : childReaders) {
            Preconditions.checkArgument(
                    classVppReaderLinkedHashMap.put(childReader.getManagedDataObjectType().getTargetType(),
                            (ChildVppReader<? extends Augmentation<D>>) childReader) == null,
                    "Duplicate (%s) child readers detected under: %s", childReader.getManagedDataObjectType(),
                    getManagedDataObjectType());
        }
        return classVppReaderLinkedHashMap;
    }

    @Nonnull
    @Override
    public final InstanceIdentifier<D> getManagedDataObjectType() {
        return instanceIdentifier;
    }

    protected List<D> readCurrent(final InstanceIdentifier<D> id) {
        final B builder = getBuilder(id);
        // Cache empty value to determine if anything has changed later TODO cache in a field
        final D emptyValue = builder.build();

        readCurrentAttributes(id, builder);

        for (ChildVppReader<? extends ChildOf<D>> child : childReaders.values()) {
            child.read(id, builder);
        }

        for (ChildVppReader<? extends Augmentation<D>> child : augReaders.values()) {
            child.read(id, builder);
        }

        // Need to check whether anything was filled in to determine if data is present or not.
        final D built = builder.build();
        return built.equals(emptyValue) ? Collections.<D>emptyList() : Collections.singletonList(built);
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public List<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id) {
        // This is read for one of children, we need to read and then filter, not parent)

        // If this is target, just read
        if (id.getTargetType().equals(getManagedDataObjectType().getTargetType())) {
            return readCurrent((InstanceIdentifier<D>) id);
        } else {
            return readSubtree(id);
        }
    }

    private List<? extends DataObject> readSubtree(final InstanceIdentifier<? extends DataObject> id) {
        // Read only specific subtree
        final Class<? extends DataObject> next = VppReaderUtils.getNextId(id, getManagedDataObjectType()).getType();
        final ChildVppReader<? extends ChildOf<D>> vppReader = childReaders.get(next);

        if (vppReader != null) {
            return vppReader.read(id);
        } else {
            // If there's no dedicated reader, use read current
            final InstanceIdentifier<D> currentId = VppReaderUtils.cutId(id, getManagedDataObjectType());
            final List<D> current = readCurrent(currentId);
            // then perform post-reading filtering (return only requested sub-node)
            return current.isEmpty() ? current : filterSubtree(current, id, getManagedDataObjectType().getTargetType()) ;
        }
    }

    @SuppressWarnings("unchecked")
    protected InstanceIdentifier<D> getCurrentId(final InstanceIdentifier<? extends DataObject> parentId) {
        Preconditions.checkArgument(!parentId.contains(getManagedDataObjectType()),
            "Unexpected InstanceIdentifier %s, already contains %s", parentId, getManagedDataObjectType());
        final InstanceIdentifier.PathArgument t = Iterables.getOnlyElement(getManagedDataObjectType().getPathArguments());
        return (InstanceIdentifier<D>) InstanceIdentifier.create(Iterables.concat(
            parentId.getPathArguments(), Collections.singleton(t)));
    }

    protected abstract void readCurrentAttributes(final InstanceIdentifier<D> id, B builder);

    protected abstract B getBuilder(InstanceIdentifier<? extends DataObject> id);

    // TODO move filtering out of here into a dedicated Filter ifc
    @Nonnull
    private static List<? extends DataObject> filterSubtree(@Nonnull final List<? extends DataObject> built,
                                                            @Nonnull final InstanceIdentifier<? extends DataObject> absolutPath,
                                                            @Nonnull final Class<?> managedType) {
        // TODO is there a better way than reflection ?

        List<DataObject> filtered = Lists.newArrayList();
        for (DataObject parent : built) {
            final InstanceIdentifier.PathArgument nextId =
                VppReaderUtils.getNextId(absolutPath, InstanceIdentifier.create(parent.getClass()));

            Optional<Method> method = VppReaderUtils.findMethodReflex(managedType, "get",
                Collections.<Class<?>>emptyList(), nextId.getType());

            if (method.isPresent()) {
                filterSingle(filtered, parent, nextId, method);
            } else {
                // List child nodes
                method = VppReaderUtils.findMethodReflex(managedType,
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
                        VppReaderUtils.findMethodReflex(nextId.getType(), "get",
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

}
