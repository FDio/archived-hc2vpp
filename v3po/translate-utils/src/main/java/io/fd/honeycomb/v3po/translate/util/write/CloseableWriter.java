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

import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import io.fd.honeycomb.v3po.translate.write.Writer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Closeable wrapper for a writer
 */
public final class CloseableWriter<D extends DataObject> implements Writer<D>, AutoCloseable {

    private Writer<D> vppCompositeRootWriter;

    public CloseableWriter(
        final Writer<D> vppCompositeRootWriter) {
        this.vppCompositeRootWriter = vppCompositeRootWriter;
    }

    @Override
    public void update(
        @Nonnull final InstanceIdentifier<? extends DataObject> id,
        @Nullable final DataObject dataBefore,
        @Nullable final DataObject dataAfter,
        @Nonnull final WriteContext ctx) throws WriteFailedException {
        vppCompositeRootWriter.update(id, dataBefore, dataAfter, ctx);
    }

    @Nonnull
    @Override
    public InstanceIdentifier<D> getManagedDataObjectType() {
        return vppCompositeRootWriter.getManagedDataObjectType();
    }

    @Override
    public String toString() {
        return vppCompositeRootWriter.toString();
    }

    @Override
    public void close() throws Exception {

    }
}
