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

package io.fd.honeycomb.vpp.test.read;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import java.lang.reflect.Method;
import java.util.List;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;

/**
 * Generic test for classes implementing {@link ListReaderCustomizer} interface.
 *
 * @param <D> Specific DataObject derived type (Identifiable), that is handled by this customizer
 * @param <K> Specific Identifier for handled type (D)
 * @param <B> Specific Builder for handled type (D)
 */
public abstract class ListReaderCustomizerTest<D extends DataObject & Identifiable<K>, K extends Identifier<D>, B extends Builder<D>> extends
    ReaderCustomizerTest<D, B> {

    protected ListReaderCustomizerTest(
        final Class<D> dataObjectClass,
        final Class<? extends Builder<? extends DataObject>> parentBuilderClass) {
        super(dataObjectClass, parentBuilderClass);
    }

    @Override
    protected ListReaderCustomizer<D, K, B> getCustomizer() {
        return ListReaderCustomizer.class.cast(super.getCustomizer());
    }

    @Override
    public void testMerge() throws Exception {
        final Builder<? extends DataObject> parentBuilder = mock(parentBuilderClass);
        final List<D> value = (List<D>) mock(List.class);
        getCustomizer().merge(parentBuilder, value);

        final Method method = parentBuilderClass.getMethod("set" + dataObjectClass.getSimpleName(), List.class);
        method.invoke(verify(parentBuilder), value);
    }
}
