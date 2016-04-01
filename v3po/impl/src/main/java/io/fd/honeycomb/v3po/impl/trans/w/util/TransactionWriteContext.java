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
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class TransactionWriteContext implements WriteContext, AutoCloseable {

    private final DOMDataReadOnlyTransaction beforeTx;
    private final DOMDataReadOnlyTransaction afterTx;
    private final Context ctx;
    private final BindingNormalizedNodeSerializer serializer;

    public TransactionWriteContext(final BindingNormalizedNodeSerializer serializer,
                                   final DOMDataReadOnlyTransaction beforeTx,
                                   final DOMDataReadOnlyTransaction afterTx) {
        this.serializer = serializer;
        this.beforeTx = beforeTx;
        this.afterTx = afterTx;
        this.ctx = new Context();
    }

    @Override
    public List<? extends DataObject> readBefore(final InstanceIdentifier<? extends DataObject> currentId) {
        return read(currentId, beforeTx);
    }

    private List<? extends DataObject> read(final InstanceIdentifier<? extends DataObject> currentId,
                                            final DOMDataReadOnlyTransaction tx) {
        // FIXME how to read all for list (using wildcarded ID) ?

        final YangInstanceIdentifier path = serializer.toYangInstanceIdentifier(currentId);

        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read =
                tx.read(LogicalDatastoreType.CONFIGURATION, path);

        try {
            final Optional<NormalizedNode<?, ?>> optional = read.checkedGet();

            if (!optional.isPresent()) {
                return Collections.<DataObject>emptyList();
            }

            final NormalizedNode<?, ?> data = optional.get();
            final Map.Entry<InstanceIdentifier<?>, DataObject> entry =
                    serializer.fromNormalizedNode(path, data);

            return Collections.singletonList(entry.getValue());
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
