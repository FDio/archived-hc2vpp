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

package io.fd.honeycomb.v3po.translate.util;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.translate.MappingContext;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Binding Transaction backed mapping context.
 */
public class TransactionMappingContext implements MappingContext {

    private final ReadWriteTransaction readWriteTransaction;

    // TODO make async

    public TransactionMappingContext(final ReadWriteTransaction readWriteTransaction) {
        this.readWriteTransaction = readWriteTransaction;
    }

    @Override
    public <T extends DataObject> Optional<T> read(@Nonnull final InstanceIdentifier<T> currentId) {
        try {
            return readWriteTransaction.read(LogicalDatastoreType.OPERATIONAL, currentId).checkedGet();
        } catch (ReadFailedException e) {
            throw new IllegalStateException("Unable to perform read", e);
        }
    }

    @Override
    public void delete(final InstanceIdentifier<?> path) {
        readWriteTransaction.delete(LogicalDatastoreType.OPERATIONAL, path);
    }

    @Override
    public <T extends DataObject> void merge(final InstanceIdentifier<T> path, T data) {
        readWriteTransaction.merge(LogicalDatastoreType.OPERATIONAL, path, data, true);
    }

    @Override
    public <T extends DataObject> void put(final InstanceIdentifier<T> path, T data) {
        readWriteTransaction.put(LogicalDatastoreType.OPERATIONAL, path, data, true);
    }

    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        return readWriteTransaction.submit();
    }

    @Override
    public void close() {
        readWriteTransaction.cancel();
    }
}
