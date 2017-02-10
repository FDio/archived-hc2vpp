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

package io.fd.hc2vpp.common.test.read;

import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;

/**
 * Generic test for classes implementing {@link InitializingReaderCustomizer} interface.
 *
 * @param <D> Specific DataObject derived type (Identifiable), that is handled by this customizer
 * @param <B> Specific Builder for handled type (D)
 */
public abstract class InitializingReaderCustomizerTest<D extends DataObject, B extends Builder<D>> extends
        ReaderCustomizerTest<D, B> implements InitializationTest<D> {

    protected InitializingReaderCustomizerTest(
        final Class<D> dataObjectClass,
        final Class<? extends Builder<? extends DataObject>> parentBuilderClass) {
        super(dataObjectClass, parentBuilderClass);
    }

    @Override
    protected InitializingReaderCustomizer<D, B> getCustomizer() {
        return InitializingReaderCustomizer.class.cast(super.getCustomizer());
    }

    protected void invokeInitTest(@Nonnull InstanceIdentifier<D> operationalPath,
                               @Nonnull D operationalData,
                               @Nonnull InstanceIdentifier<?> configPath,
                               @Nonnull Object configData) {
        invokeInit(getCustomizer(), ctx, operationalPath, operationalData, configPath, configData);
    }
}
