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

package io.fd.honeycomb.v3po.translate.util.read;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.util.ReflectionUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Might be slow !
 */
final class ReflexiveReaderCustomizer<C extends DataObject, B extends Builder<C>> extends NoopReaderCustomizer<C, B> {

    private final Class<C> typeClass;
    private final Class<B> builderClass;

    public ReflexiveReaderCustomizer(final Class<C> typeClass, final Class<B> builderClass) {
        this.typeClass = typeClass;
        this.builderClass = builderClass;
    }

    protected Class<C> getTypeClass() {
        return typeClass;
    }

    protected Class<B> getBuilderClass() {
        return builderClass;
    }

    @Nonnull
    @Override
    public B getBuilder(@Nonnull InstanceIdentifier<C> id) {
        try {
            return builderClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to instantiate " + builderClass, e);
        }
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final C readValue) {
        if (Augmentation.class.isAssignableFrom(typeClass)) {
            mergeAugmentation(parentBuilder, (Class<? extends Augmentation<?>>) typeClass, readValue);
        } else {
            mergeRegular(parentBuilder, readValue);
        }
    }

    private static void mergeRegular(@Nonnull final Builder<? extends DataObject> parentBuilder,
                                     @Nonnull final DataObject readValue) {
        final Optional<Method> method =
                ReflectionUtils.findMethodReflex(parentBuilder.getClass(), "set",
                        Collections.singletonList(readValue.getClass()), parentBuilder.getClass());

        checkArgument(method.isPresent(), "Unable to set %s to %s", readValue, parentBuilder);

        try {
            method.get().invoke(parentBuilder, readValue);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to set " + readValue + " to " + parentBuilder, e);
        }
    }

    private static void mergeAugmentation(@Nonnull final Builder<? extends DataObject> parentBuilder,
                                          @Nonnull final Class<? extends Augmentation<?>> typeClass,
                                          @Nonnull final DataObject readValue) {
        final Optional<Method> method =
                ReflectionUtils.findMethodReflex(parentBuilder.getClass(), "addAugmentation",
                        Lists.newArrayList(Class.class, Augmentation.class), parentBuilder.getClass());

        checkArgument(method.isPresent(), "Not possible to add augmentations to builder: %s", parentBuilder);
        try {
            method.get().invoke(parentBuilder, typeClass, readValue);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to set " + readValue + " to " + parentBuilder, e);
        }
    }

}
