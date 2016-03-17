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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import io.fd.honeycomb.v3po.impl.trans.ChildVppReader;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class VppReaderUtils {

    private VppReaderUtils() {}

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
    public static <D extends DataObject & Identifiable<K>, K extends Identifier<D>> InstanceIdentifier<D> getCurrentId(
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
     * Find a specific method using reflection
     */
    @Nonnull
    public static Optional<Method> findMethodReflex(@Nonnull final Class<?> managedType,
                                                    @Nonnull final String prefix,
                                                    @Nonnull final List<Class<?>> paramTypes,
                                                    @Nonnull final Class<?> retType) {
        top:
        for (Method method : managedType.getMethods()) {
            if (!method.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
                continue;
            }

            final Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != paramTypes.size()) {
                continue;
            }

            for (int i = 0; i < parameterTypes.length; i++) {
                if (!parameterTypes[i].isAssignableFrom(paramTypes.get(i))) {
                    continue top;
                }
            }

            if (!method.getReturnType().equals(retType)) {
                continue;
            }

            return Optional.of(method);
        }

        return Optional.absent();
    }
}
