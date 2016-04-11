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

package io.fd.honeycomb.v3po.translate.util.write;

import io.fd.honeycomb.v3po.translate.TranslationException;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import io.fd.honeycomb.v3po.translate.write.WriterRegistry;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * WriterRegistry wrapper providing AutoCloseable interface.
 */
public final class CloseableWriterRegistry implements WriterRegistry, AutoCloseable {
    private final WriterRegistry writerRegistry;

    public CloseableWriterRegistry( final WriterRegistry writerRegistry) {
        this.writerRegistry = writerRegistry;
    }

    @Override
    public void update(
            @Nonnull final Map<InstanceIdentifier<?>, DataObject> nodesBefore,
            @Nonnull final Map<InstanceIdentifier<?>, DataObject> nodesAfter,
            @Nonnull final WriteContext ctx) throws TranslationException {
        writerRegistry.update(nodesBefore, nodesAfter, ctx);
    }

    @Override
    public void update(
            @Nonnull final InstanceIdentifier<? extends DataObject> id,
            @Nullable final DataObject dataBefore, @Nullable final DataObject dataAfter,
            @Nonnull final WriteContext ctx) throws WriteFailedException {
        writerRegistry.update(id, dataBefore, dataAfter, ctx);
    }

    @Nonnull
    @Override
    public InstanceIdentifier<DataObject> getManagedDataObjectType() {
        return writerRegistry.getManagedDataObjectType();
    }

    @Override
    public void close() throws Exception {
        // NOOP
    }
}