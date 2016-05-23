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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.fd.honeycomb.v3po.data.ReadableDataManager;
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

    @Nullable
    private ReadableDataManager operationalData;
    @Nullable
    private ReadableDataManager configSnapshot;

    private boolean closed = false;

    /**
     * @param configData config data tree manager. Null if config reads are not to be supported
     * @param operationalData operational data tree manager. Null if operational reads are not to be supported
     */
    private ReadOnlyTransaction(@Nullable final ReadableDataManager configData,
                                @Nullable final ReadableDataManager operationalData) {
        this.configSnapshot = configData;
        this.operationalData = operationalData;
    }

    @Override
    public synchronized void close() {
        closed = true;
        configSnapshot = null;
        operationalData = null;
    }

    @Override
    public synchronized CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
            final LogicalDatastoreType store,
            final YangInstanceIdentifier path) {
        LOG.debug("ReadOnlyTransaction.read(), store={}, path={}", store, path);
        checkState(!closed, "Transaction has been closed");

        if (store == LogicalDatastoreType.OPERATIONAL) {
            checkArgument(operationalData != null, "{} reads not supported", store);
            return operationalData.read(path);
        } else {
            checkArgument(configSnapshot != null, "{} reads not supported", store);
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

    @Nonnull
    static ReadOnlyTransaction createOperationalOnly(@Nonnull final ReadableDataManager operationalData) {
        return new ReadOnlyTransaction(null, requireNonNull(operationalData));
    }

    @Nonnull
    static ReadOnlyTransaction createConfigOnly(@Nonnull final ReadableDataManager configData) {
        return new ReadOnlyTransaction(requireNonNull(configData), null);
    }

    @Nonnull
    static ReadOnlyTransaction create(@Nonnull final ReadableDataManager configData,
                                      @Nonnull final ReadableDataManager operationalData) {
        return new ReadOnlyTransaction(requireNonNull(configData), requireNonNull(operationalData));
    }

    private static final Function<? super Optional<NormalizedNode<?, ?>>, ? extends Boolean> IS_NODE_PRESENT =
        (Function<Optional<NormalizedNode<?, ?>>, Boolean>) input -> input == null ? Boolean.FALSE : input.isPresent();

    private static final Function<? super Exception, ReadFailedException> ANY_EX_TO_READ_FAILED_EXCEPTION_MAPPER =
        (Function<Exception, ReadFailedException>) e -> new ReadFailedException("Exists failed", e);
}