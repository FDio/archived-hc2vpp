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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Empty registry that does not perform any changes. Can be used in data layer, if we want to disable passing data to
 * translation layer.
 */
public class NoopWriterRegistry implements WriterRegistry, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NoopWriterRegistry.class);

    @Override
    public void update(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                       @Nullable final DataObject dataBefore, @Nullable final DataObject dataAfter,
                       @Nonnull final WriteContext ctx) throws WriteFailedException {
        LOG.trace("NoopWriterRegistry.update id={}, dataBefore{}, dataAfter={], ctx={}", id, dataBefore, dataAfter,
                ctx);
        // NOOP
    }

    @Override
    public void update(@Nonnull final DataObjectUpdates updates,
                       @Nonnull final WriteContext ctx) throws TranslationException {
        // NOOP
    }

    @Nonnull
    @Override
    public InstanceIdentifier<DataObject> getManagedDataObjectType() {
        throw new UnsupportedOperationException("Noop registry has no type");
    }

    @Override
    public void close() throws Exception {
        // NOOP
    }
}
