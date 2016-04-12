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

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.read.Reader;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Closeable wrapper for a reader
 */
public final class CloseableReader<D extends DataObject> implements Reader<D>, AutoCloseable {

    private Reader<D> compositeRootReader;

    public CloseableReader(@Nonnull final Reader<D> compositeRootReader) {
        this.compositeRootReader = compositeRootReader;
    }

    @Nonnull
    @Override
    public Optional<? extends DataObject> read(@Nonnull InstanceIdentifier<? extends DataObject> id,
                                               @Nonnull ReadContext ctx) throws ReadFailedException {
        return compositeRootReader.read(id, ctx);
    }

    @Nonnull
    @Override
    public InstanceIdentifier<D> getManagedDataObjectType() {
        return compositeRootReader.getManagedDataObjectType();
    }

    @Override
    public String toString() {
        return compositeRootReader.toString();
    }

    @Override
    public void close() throws Exception {
        //NOOP
    }
}
