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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Composite DOM transaction that delegates reads to a {@link DOMDataReadTransaction} delegate and writes to a {@link
 * DOMDataWriteTransaction} delegate.
 */
final class ReadWriteTransaction implements DOMDataReadWriteTransaction {

    private final DOMDataReadOnlyTransaction delegateReadTx;
    private final DOMDataWriteTransaction delegateWriteTx;

    ReadWriteTransaction(@Nonnull final DOMDataReadOnlyTransaction delegateReadTx,
                         @Nonnull final DOMDataWriteTransaction delegateWriteTx) {
        this.delegateReadTx = Preconditions.checkNotNull(delegateReadTx, "delegateReadTx should not be null");
        this.delegateWriteTx = Preconditions.checkNotNull(delegateWriteTx, "delegateWriteTx should not be null");
    }

    @Override
    public boolean cancel() {
        delegateReadTx.close();
        return delegateWriteTx.cancel();
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                    final NormalizedNode<?, ?> data) {
        delegateWriteTx.put(store, path, data);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                      final NormalizedNode<?, ?> data) {
        delegateWriteTx.merge(store, path, data);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        delegateWriteTx.delete(store, path);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        return delegateWriteTx.submit();
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return delegateWriteTx.commit();
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final LogicalDatastoreType store,
                                                                                   final YangInstanceIdentifier path) {
        return delegateReadTx.read(store, path);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final LogicalDatastoreType store,
                                                              final YangInstanceIdentifier path) {
        return delegateReadTx.exists(store, path);
    }

    @Override
    public Object getIdentifier() {
        return this;
    }
}

