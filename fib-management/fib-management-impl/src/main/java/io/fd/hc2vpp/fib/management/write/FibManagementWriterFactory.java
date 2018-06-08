/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.fib.management.write;

import com.google.inject.Inject;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;

/**
 * Factory producing writers for FIB table management plugin's data.
 */
public final class FibManagementWriterFactory implements WriterFactory {

    @Inject
    private FutureJVppCore vppApi;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        registry.add(new GenericListWriter<>(FibManagementIIds.FM_FTBLS_TABLE, new FibTableCustomizer(vppApi)));
    }
}
