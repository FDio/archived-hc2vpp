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

package io.fd.honeycomb.v3po.translate.util;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import io.fd.honeycomb.v3po.translate.SubtreeManager;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class RWUtils {

    private RWUtils() {}

    /**
     * Collector expecting only a single resulting item from a stream
     */
    public static<T> Collector<T,?,T> singleItemCollector() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException("Unexpected size of list: " + list + ". Single item expected");
                    }
                    return list.get(0);
                }
        );
    }

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
     * Create an ordered map from a collection, checking for duplicity in the process.
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

    /**
     * Transform a keyed instance identifier into a wildcarded one.
     * <p/>
     * ! This has to be called also for wildcarded List instance identifiers
     * due to weird behavior of equals in InstanceIdentifier !
     */
    @SuppressWarnings("unchecked")
    public static <D extends DataObject> InstanceIdentifier<D> makeIidWildcarded(final InstanceIdentifier<D> id) {
        final List<InstanceIdentifier.PathArgument> transformedPathArguments =
                StreamSupport.stream(id.getPathArguments().spliterator(), false)
                        .map(RWUtils::cleanPathArgumentFromKeys)
                        .collect(Collectors.toList());
        return (InstanceIdentifier<D>) InstanceIdentifier.create(transformedPathArguments);
    }

    /**
     * Transform a keyed instance identifier into a wildcarded one, keeping keys except the last item.
     */
    @SuppressWarnings("unchecked")
    public static <D extends DataObject> InstanceIdentifier<D> makeIidLastWildcarded(final InstanceIdentifier<D> id) {
        final InstanceIdentifier.Item<D> wildcardedItem = new InstanceIdentifier.Item<>(id.getTargetType());
        final Iterable<InstanceIdentifier.PathArgument> pathArguments = id.getPathArguments();
        return (InstanceIdentifier<D>) InstanceIdentifier.create(
                Iterables.concat(
                        Iterables.limit(pathArguments, Iterables.size(pathArguments) - 1),
                        Collections.singleton(wildcardedItem)));
    }

    private static InstanceIdentifier.PathArgument cleanPathArgumentFromKeys(final InstanceIdentifier.PathArgument pathArgument) {
        return pathArgument instanceof InstanceIdentifier.IdentifiableItem<?, ?>
                ? new InstanceIdentifier.Item<>(pathArgument.getType())
                : pathArgument;
    }
}
