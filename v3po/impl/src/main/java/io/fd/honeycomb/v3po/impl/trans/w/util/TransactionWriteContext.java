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

package io.fd.honeycomb.v3po.impl.trans.w.util;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.impl.trans.util.Context;
import io.fd.honeycomb.v3po.impl.trans.w.WriteContext;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TransactionWriteContext implements WriteContext, AutoCloseable {

    private final ReadOnlyTransaction beforeTx;
    private final ReadOnlyTransaction afterTx;
    private final Context ctx;

    public TransactionWriteContext(final ReadOnlyTransaction beforeTx, final ReadOnlyTransaction afterTx,
                                   final Context ctx) {
        super();
        this.beforeTx = beforeTx;
        this.afterTx = afterTx;
        this.ctx = ctx;
    }

    public TransactionWriteContext(final ReadOnlyTransaction beforeTx,
                                   final ReadOnlyTransaction afterTx) {
        this(beforeTx, afterTx, new Context());
    }

    @Override
    public List<? extends DataObject> readBefore(final InstanceIdentifier<? extends DataObject> currentId) {
        return read(currentId, beforeTx);
    }

    private List<? extends DataObject> read(final InstanceIdentifier<? extends DataObject> currentId,
                                            final ReadOnlyTransaction tx) {
        // FIXME how to read all for list (using wildcarded ID) ?

        final CheckedFuture<? extends Optional<? extends DataObject>, ReadFailedException> read =
            tx.read(LogicalDatastoreType.CONFIGURATION, currentId);
        try {
            final Optional<? extends DataObject> optional = read.checkedGet();
            return optional.isPresent()
                ? Collections.singletonList(optional.get())
                : Collections.<DataObject>emptyList();
        } catch (ReadFailedException e) {
            throw new IllegalStateException("Unable to perform read", e);
        }
    }

    @Override
    public List<? extends DataObject> readAfter(final InstanceIdentifier<? extends DataObject> currentId) {
        return read(currentId, afterTx);
    }

    @Override
    public Context getContext() {
        return ctx;
    }

    /**
     * Does not close the transactions
     */
    @Override
    public void close() throws Exception {
        ctx.close();
    }
}
