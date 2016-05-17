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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.translate.MappingContext;
import io.fd.honeycomb.v3po.translate.ModificationCache;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Transaction based WriteContext
 */
public final class TransactionWriteContext implements WriteContext {

    private final DOMDataReadOnlyTransaction beforeTx;
    private final DOMDataReadOnlyTransaction afterTx;
    private final ModificationCache ctx;
    private final BindingNormalizedNodeSerializer serializer;
    private MappingContext mappingContext;

    public TransactionWriteContext(final BindingNormalizedNodeSerializer serializer,
                                   final DOMDataReadOnlyTransaction beforeTx,
                                   final DOMDataReadOnlyTransaction afterTx,
                                   final MappingContext mappingContext) {
        this.serializer = serializer;
        // TODO do we have a BA transaction adapter ? If so, use it here and don't pass serializer
        this.beforeTx = beforeTx;
        this.afterTx = afterTx;
        this.mappingContext = mappingContext;
        this.ctx = new ModificationCache();
    }

    // TODO make this asynchronous

    @Override
    public <T extends DataObject> Optional<T> readBefore(@Nonnull final InstanceIdentifier<T> currentId) {
        return read(currentId, beforeTx);
    }

    @Override
    public <T extends DataObject> Optional<T> readAfter(@Nonnull final InstanceIdentifier<T> currentId) {
        return read(currentId, afterTx);
    }


    private <T extends DataObject> Optional<T> read(final InstanceIdentifier<T> currentId,
                                                    final DOMDataReadOnlyTransaction tx) {
        final YangInstanceIdentifier path = serializer.toYangInstanceIdentifier(currentId);

        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read =
                tx.read(LogicalDatastoreType.CONFIGURATION, path);

        try {
            // TODO once the APIs are asynchronous use just Futures.transform
            final Optional<NormalizedNode<?, ?>> optional = read.checkedGet();

            if (!optional.isPresent()) {
                return Optional.absent();
            }

            final NormalizedNode<?, ?> data = optional.get();
            final Map.Entry<InstanceIdentifier<?>, DataObject> entry = serializer.fromNormalizedNode(path, data);

            final Class<T> targetType = currentId.getTargetType();
            checkState(targetType.isAssignableFrom(entry.getValue().getClass()),
                "Unexpected data object type, should be: %s, but was: %s", targetType, entry.getValue().getClass());
            return Optional.of(targetType.cast(entry.getValue()));
        } catch (ReadFailedException e) {
            throw new IllegalStateException("Unable to perform read", e);
        }
    }

    @Nonnull
    @Override
    public ModificationCache getModificationCache() {
        return ctx;
    }

    @Nonnull
    @Override
    public MappingContext getMappingContext() {
        return mappingContext;
    }

    /**
     * Does not close the transactions
     */
    @Override
    public void close() {
        ctx.close();
        beforeTx.close();
        afterTx.close();
    }
}
