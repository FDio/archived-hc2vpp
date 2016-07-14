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
import io.fd.honeycomb.v3po.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.ReflectionUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;

/**
 * Might be slow !
 */
public abstract class ReflexiveListReaderCustomizer<C extends DataObject & Identifiable<K>, K extends Identifier<C>, B extends Builder<C>>
        extends ReflexiveReaderCustomizer<C, B>
        implements ListReaderCustomizer<C, K, B> {


    public ReflexiveListReaderCustomizer(final Class<C> typeClass, final Class<B> builderClass) {
        super(typeClass, builderClass);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final C readValue) {
        merge(parentBuilder, Collections.singletonList(readValue));
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final List<C> readData) {
        final Optional<Method> method =
                ReflectionUtils.findMethodReflex(parentBuilder.getClass(), "set" + getTypeClass().getSimpleName(),
                        Collections.singletonList(List.class), parentBuilder.getClass());

        checkArgument(method.isPresent(), "Unable to set %s to %s", readData, parentBuilder);

        try {
            method.get().invoke(parentBuilder, readData);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to set " + readData + " to " + parentBuilder, e);
        }
    }
}
