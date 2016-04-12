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

package io.fd.honeycomb.v3po.vpp.facade.impl.write.util;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import io.fd.honeycomb.v3po.vpp.facade.impl.util.ReflectionUtils;
import io.fd.honeycomb.v3po.vpp.facade.spi.write.ChildVppWriterCustomizer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Might be slow !
 */
public class ReflexiveChildWriterCustomizer<C extends DataObject> extends NoopWriterCustomizer<C> implements
    ChildVppWriterCustomizer<C> {

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Optional<C> extract(@Nonnull final InstanceIdentifier<C> currentId, @Nonnull final DataObject parentData) {
        final Class<C> currentType = currentId.getTargetType();
        final Optional<Method> method = ReflectionUtils.findMethodReflex(getParentType(currentId),
            "get" + currentType.getSimpleName(), Collections.<Class<?>>emptyList(), currentType);

        Preconditions.checkArgument(method.isPresent(), "Unable to get %s from %s", currentType, parentData);

        try {
            return method.isPresent()
                ? Optional.of((C) method.get().invoke(parentData))
                : Optional.<C>absent();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to get " + currentType + " from " + parentData, e);
        }
    }

    private Class<? extends DataObject> getParentType(final @Nonnull InstanceIdentifier<C> currentId) {
        return Iterables.get(currentId.getPathArguments(), Iterables.size(currentId.getPathArguments()) - 2).getType();
    }
}
