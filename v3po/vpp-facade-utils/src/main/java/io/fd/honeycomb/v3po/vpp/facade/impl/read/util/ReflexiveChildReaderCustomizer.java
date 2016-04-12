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

package io.fd.honeycomb.v3po.vpp.facade.impl.read.util;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.fd.honeycomb.v3po.vpp.facade.impl.util.ReflectionUtils;
import io.fd.honeycomb.v3po.vpp.facade.spi.read.ChildVppReaderCustomizer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Might be slow !
 */
public class ReflexiveChildReaderCustomizer<C extends DataObject, B extends Builder<C>>
    extends ReflexiveRootReaderCustomizer<C, B>
    implements ChildVppReaderCustomizer<C,B> {

    public ReflexiveChildReaderCustomizer(final Class<B> builderClass) {
        super(builderClass);
    }

    // TODO Could be just a default implementation in interface (making this a mixin)

    @Override
    public void merge(final Builder<? extends DataObject> parentBuilder, final C readValue) {
        final Optional<Method> method =
            ReflectionUtils.findMethodReflex(parentBuilder.getClass(), "set",
                Collections.<Class<?>>singletonList(readValue.getClass()), parentBuilder.getClass());

        Preconditions.checkArgument(method.isPresent(), "Unable to set %s to %s", readValue, parentBuilder);

        try {
            method.get().invoke(parentBuilder, readValue);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to set " + readValue + " to " + parentBuilder, e);
        }
    }

}
