/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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


import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingCustomizer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

interface InitializationTest<D extends DataObject> {

    /**
     * Invokes initialization test
     *
     * @param customizer      initialization capable customizer
     * @param ctx             read context
     * @param operationalPath paths passed to initialization
     * @param operationalData data passed to initialization
     * @param configPath      expected data path
     * @param configData      expected data
     */
    default void invokeInit(@Nonnull final InitializingCustomizer<D> customizer,
                            @Nonnull final ReadContext ctx,
                            @Nonnull final InstanceIdentifier<D> operationalPath,
                            @Nonnull final D operationalData,
                            @Nonnull final InstanceIdentifier<?> configPath,
                            @Nonnull final Object configData) {
        final Initialized<? extends DataObject> init = customizer.init(operationalPath, operationalData, ctx);
        assertNotNull(init);
        assertEquals(configPath, init.getId());
        assertEquals(configData, init.getData());
    }
}
