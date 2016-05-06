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
import io.fd.honeycomb.v3po.translate.spi.read.ChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.ReflectionUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Might be slow !
 */
public class ReflexiveAugmentReaderCustomizer<C extends DataObject, B extends Builder<C>>
    extends ReflexiveRootReaderCustomizer<C, B>
    implements ChildReaderCustomizer<C,B> {

    private final Class<C> augType;

    public ReflexiveAugmentReaderCustomizer(final Class<B> builderClass, final Class<C> augType) {
        super(builderClass);
        this.augType = augType;
    }

    @Override
    public void merge(final Builder<? extends DataObject> parentBuilder, final C readValue) {
        final Optional<Method> method =
            ReflectionUtils.findMethodReflex(parentBuilder.getClass(), "addAugmentation",
                Lists.newArrayList(Class.class, Augmentation.class), parentBuilder.getClass());

        checkArgument(method.isPresent(), "Not possible to add augmentations to builder: %s", parentBuilder);
        try {
            method.get().invoke(parentBuilder, augType, readValue);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to set " + readValue + " to " + parentBuilder, e);
        }
    }

}
