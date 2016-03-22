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

package io.fd.honeycomb.v3po.impl.trans.util;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import io.fd.honeycomb.v3po.impl.trans.SubtreeManager;
import io.fd.honeycomb.v3po.impl.trans.r.ChildVppReader;
import java.util.Collection;
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

public final class VppRWUtils {

    private VppRWUtils() {}

    /**
     * Find next item in ID after provided type
     */
    @Nonnull
    public static InstanceIdentifier.PathArgument getNextId(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                                            @Nonnull final InstanceIdentifier<? extends DataObject> type) {
        // TODO this is inefficient(maybe, depending on actual Iterable type)
        final Iterable<InstanceIdentifier.PathArgument> pathArguments = id.getPathArguments();
        final int i = Iterables.indexOf(pathArguments, new Predicate<InstanceIdentifier.PathArgument>() {
            @Override
            public boolean apply(final InstanceIdentifier.PathArgument input) {
                return input.getType().isAssignableFrom(type.getTargetType());
            }
        });
        Preconditions.checkArgument(i >= 0, "Unable to find %s type in %s", type.getTargetType(), id);
        return Iterables.get(pathArguments, i + 1);
    }

    public static <T> List<ChildVppReader<? extends ChildOf<T>>> emptyChildReaderList() {
        return Collections.emptyList();
    }

    public static <T> List<ChildVppReader<? extends Augmentation<T>>> emptyAugReaderList() {
        return Collections.emptyList();
    }

    public static <T> List<ChildVppReader<? extends Augmentation<T>>> singletonAugReaderList(
        ChildVppReader<? extends Augmentation<T>> item) {
        return Collections.<ChildVppReader<? extends Augmentation<T>>>singletonList(item);
    }

    public static <T> List<ChildVppReader<? extends ChildOf<T>>> singletonChildReaderList(
        ChildVppReader<? extends ChildOf<T>> item) {
        return Collections.<ChildVppReader<? extends ChildOf<T>>>singletonList(item);
    }

    /**
     * Replace last item in ID with a provided IdentifiableItem of the same type
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public static <D extends DataObject & Identifiable<K>, K extends Identifier<D>> InstanceIdentifier<D> replaceLastInId(
        @Nonnull final InstanceIdentifier<D> id, final InstanceIdentifier.IdentifiableItem<D, K> currentBdItem) {

        final Iterable<InstanceIdentifier.PathArgument> pathArguments = id.getPathArguments();
        final Iterable<InstanceIdentifier.PathArgument> withoutCurrent =
            Iterables.limit(pathArguments, Iterables.size(pathArguments) - 1);
        final Iterable<InstanceIdentifier.PathArgument> concat =
            Iterables.concat(withoutCurrent, Collections.singleton(currentBdItem));
        return (InstanceIdentifier<D>) InstanceIdentifier.create(concat);
    }

    /**
     * Create IdentifiableItem from target type of provided ID with provided key
     */
    @Nonnull
    public static <D extends DataObject & Identifiable<K>, K extends Identifier<D>> InstanceIdentifier.IdentifiableItem<D, K> getCurrentIdItem(
        @Nonnull final InstanceIdentifier<D> id, final K key) {
        return new InstanceIdentifier.IdentifiableItem<>(id.getTargetType(), key);
    }

    /**
     * Trim InstanceIdentifier at indexOf(type)
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public static <D extends DataObject> InstanceIdentifier<D> cutId(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                                                     @Nonnull final InstanceIdentifier<D> type) {
        final Iterable<InstanceIdentifier.PathArgument> pathArguments = id.getPathArguments();
        final int i = Iterables.indexOf(pathArguments, new Predicate<InstanceIdentifier.PathArgument>() {
            @Override
            public boolean apply(final InstanceIdentifier.PathArgument input) {
                return input.getType().equals(type.getTargetType());
            }
        });
        Preconditions.checkArgument(i >= 0, "ID %s does not contain %s", id, type);
        return (InstanceIdentifier<D>) InstanceIdentifier.create(Iterables.limit(pathArguments, i + 1));
    }

    /**
     * Create a map from a collection, checking for duplicity in the process
     */
    @Nonnull
    public static <K, V> Map<K, V> uniqueLinkedIndex(@Nonnull final Collection<V> values, @Nonnull final Function<? super V, K> keyFunction) {
        final Map<K, V> objectObjectLinkedHashMap = Maps.newLinkedHashMap();
        for (V value : values) {
            final K key = keyFunction.apply(value);
            Preconditions.checkArgument(objectObjectLinkedHashMap.put(key, value) == null,
                "Duplicate key detected : %s", key);
        }
        return objectObjectLinkedHashMap;
    }

    public static final Function<SubtreeManager<? extends DataObject>, Class<? extends DataObject>>
        MANAGER_CLASS_FUNCTION = new Function<SubtreeManager<? extends DataObject>, Class<? extends DataObject>>() {
        @Override
        public Class<? extends DataObject> apply(final SubtreeManager<? extends DataObject> input) {
            return input.getManagedDataObjectType().getTargetType();
        }
    };

    public static final Function<SubtreeManager<? extends Augmentation<?>>, Class<? extends DataObject>>
        MANAGER_CLASS_AUG_FUNCTION = new Function<SubtreeManager<? extends Augmentation<?>>, Class<? extends DataObject>>() {

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends DataObject> apply(final SubtreeManager<? extends Augmentation<?>> input) {
            final Class<? extends Augmentation<?>> targetType = input.getManagedDataObjectType().getTargetType();
            Preconditions.checkArgument(DataObject.class.isAssignableFrom(targetType));
            return (Class<? extends DataObject>) targetType;
        }
    };

    @SuppressWarnings("unchecked")
    public static <D extends DataObject> InstanceIdentifier<D> appendTypeToId(
        final InstanceIdentifier<? extends DataObject> parentId, final InstanceIdentifier<D> type) {
        Preconditions.checkArgument(!parentId.contains(type),
            "Unexpected InstanceIdentifier %s, already contains %s", parentId, type);
        final InstanceIdentifier.PathArgument t = Iterables.getOnlyElement(type.getPathArguments());
        return (InstanceIdentifier<D>) InstanceIdentifier.create(Iterables.concat(
            parentId.getPathArguments(), Collections.singleton(t)));
    }
}
