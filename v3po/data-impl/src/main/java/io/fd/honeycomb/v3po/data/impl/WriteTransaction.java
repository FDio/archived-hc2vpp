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
import static java.util.Objects.requireNonNull;
import static org.opendaylight.controller.md.sal.common.api.TransactionStatus.CANCELED;
import static org.opendaylight.controller.md.sal.common.api.TransactionStatus.COMMITED;
import static org.opendaylight.controller.md.sal.common.api.TransactionStatus.FAILED;
import static org.opendaylight.controller.md.sal.common.api.TransactionStatus.NEW;
import static org.opendaylight.controller.md.sal.common.api.TransactionStatus.SUBMITED;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.fd.honeycomb.v3po.data.DataModification;
import io.fd.honeycomb.v3po.translate.TranslationException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
final class WriteTransaction implements DOMDataWriteTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(WriteTransaction.class);

    @Nullable
    private DataModification operationalModification;
    @Nullable
    private DataModification configModification;
    private TransactionStatus status = NEW;

    private WriteTransaction(@Nullable final DataModification configModification,
                             @Nullable final DataModification operationalModification) {
        this.operationalModification = operationalModification;
        this.configModification = configModification;
    }

    private void checkIsNew() {
        Preconditions.checkState(status == NEW, "Transaction was submitted or canceled");
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                    final NormalizedNode<?, ?> data) {
        LOG.debug("WriteTransaction.put() store={}, path={}, data={}", store, path, data);
        checkIsNew();
        handleOperation(store, (modification) -> modification.write(path, data));
    }

    private void handleOperation(final LogicalDatastoreType store,
                                 final java.util.function.Consumer<DataModification> r) {
        switch (store) {
            case CONFIGURATION:
                checkArgument(configModification != null, "Modification of %s is not supported", store);
                r.accept(configModification);
                break;
            case OPERATIONAL:
                checkArgument(operationalModification != null, "Modification of %s is not supported", store);
                r.accept(operationalModification);
                break;
        }
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                      final NormalizedNode<?, ?> data) {
        LOG.debug("WriteTransaction.merge() store={}, path={}, data={}", store, path, data);
        checkIsNew();
        handleOperation(store, (modification) -> modification.merge(path, data));
    }

    @Override
    public boolean cancel() {
        if (status != NEW) {
            // only NEW transactions can be cancelled
            return false;
        } else {
            status = CANCELED;
            return true;
        }
    }

    @Override
    public void delete(LogicalDatastoreType store, final YangInstanceIdentifier path) {
        LOG.debug("WriteTransaction.delete() store={}, path={}", store, path);
        checkIsNew();
        handleOperation(store, (modification) -> modification.delete(path));
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        LOG.trace("WriteTransaction.submit()");
        checkIsNew();

        try {
            status = SUBMITED;

            // Validate first to catch any issues before attempting commit
            if (configModification != null) {
                configModification.validate();
            }
            if (operationalModification != null) {
                operationalModification.validate();
            }

            if(configModification != null) {
                configModification.commit();
            }
            if(operationalModification != null) {
                operationalModification.commit();
            }

            status = COMMITED;
        } catch (DataValidationFailedException | TranslationException e) {
            status = FAILED;
            LOG.error("Failed modify data tree", e);
            return Futures.immediateFailedCheckedFuture(
                new TransactionCommitFailedException("Failed to validate DataTreeModification", e));
        }
        return Futures.immediateCheckedFuture(null);
    }

    @Override
    @Deprecated
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        throw new UnsupportedOperationException("deprecated");
    }

    @Override
    public Object getIdentifier() {
        return this;
    }


    @Nonnull
    static WriteTransaction createOperationalOnly(@Nonnull final DataModification operationalData) {
        return new WriteTransaction(null, requireNonNull(operationalData));
    }

    @Nonnull
    static WriteTransaction createConfigOnly(@Nonnull final DataModification configData) {
        return new WriteTransaction(requireNonNull(configData), null);
    }

    @Nonnull
    static WriteTransaction create(@Nonnull final DataModification configData,
                            @Nonnull final DataModification operationalData) {
        return new WriteTransaction(requireNonNull(configData), requireNonNull(operationalData));
    }
}
