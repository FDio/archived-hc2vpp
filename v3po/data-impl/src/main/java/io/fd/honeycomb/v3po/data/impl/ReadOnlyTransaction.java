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

package io.fd.honeycomb.v3po.data.impl;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.fd.honeycomb.v3po.data.ReadableDataTree;
import io.fd.honeycomb.v3po.data.DataTreeSnapshot;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ReadOnlyTransaction implements DOMDataReadOnlyTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(ReadOnlyTransaction.class);
    private volatile ReadableDataTree operationalData;
    private volatile DataTreeSnapshot configSnapshot;

    ReadOnlyTransaction(@Nonnull final ReadableDataTree operationalData,
                        @Nonnull final DataTreeSnapshot configSnapshot) {
        this.operationalData = Preconditions.checkNotNull(operationalData, "operationalData should not be null");
        this.configSnapshot = Preconditions.checkNotNull(configSnapshot, "config should not be null");
    }

    @Override
    public void close() {
        configSnapshot = null;
        operationalData = null;
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
            final LogicalDatastoreType store,
            final YangInstanceIdentifier path) {
        LOG.debug("ReadOnlyTransaction.read(), store={}, path={}", store, path);

        Preconditions.checkState(configSnapshot != null, "Transaction was closed");

        if (store == LogicalDatastoreType.OPERATIONAL) {
            return operationalData.read(path);
        } else {
            return configSnapshot.read(path);
        }
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final LogicalDatastoreType store,
                                                              final YangInstanceIdentifier path) {
        LOG.debug("ReadOnlyTransaction.exists() store={}, path={}", store, path);

        ListenableFuture<Boolean> listenableFuture = Futures.transform(read(store, path), IS_NODE_PRESENT);

        return Futures.makeChecked(listenableFuture, ANY_EX_TO_READ_FAILED_EXCEPTION_MAPPER);
    }

    @Nonnull
    @Override
    public Object getIdentifier() {
        return this;
    }


    private static final Function<? super Optional<NormalizedNode<?, ?>>, ? extends Boolean> IS_NODE_PRESENT =
            new Function<Optional<NormalizedNode<?, ?>>, Boolean>() {
                @Nullable
                @Override
                public Boolean apply(@Nullable final Optional<NormalizedNode<?, ?>> input) {
                    return input == null
                            ? Boolean.FALSE
                            : input.isPresent();
                }
            };

    private static final Function<? super Exception, ReadFailedException> ANY_EX_TO_READ_FAILED_EXCEPTION_MAPPER =
            new Function<Exception, ReadFailedException>() {
                @Override
                public ReadFailedException apply(@Nullable final Exception e) {
                    return new ReadFailedException("Exists failed", e);
                }
            };
}