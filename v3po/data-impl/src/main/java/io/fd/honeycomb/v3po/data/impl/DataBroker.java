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

import com.google.common.base.Preconditions;
import io.fd.honeycomb.v3po.data.ReadableDataTree;
import io.fd.honeycomb.v3po.data.ModifiableDataTree;
import io.fd.honeycomb.v3po.data.DataTreeSnapshot;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Data Broker which provides data transaction functionality for YANG capable data provider
 * using {@link NormalizedNode} data format.
 */
public class DataBroker implements DOMDataBroker {

    private final ReadableDataTree operationalData;
    private final ModifiableDataTree configDataTree;

    /**
     * Creates DataBroker instance.
     *
     * @param operationalData operational data
     * @param configDataTree  configuration data
     */
    public DataBroker(@Nonnull final ReadableDataTree operationalData,
                      @Nonnull final ModifiableDataTree configDataTree) {
        this.operationalData = Preconditions.checkNotNull(operationalData, "operationalData should not be null");
        this.configDataTree = Preconditions.checkNotNull(configDataTree, "configDataProxy should not be null");
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return new ReadOnlyTransaction(operationalData, configDataTree.takeSnapshot());
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        // todo use the same snapshot
        final DataTreeSnapshot configSnapshot = configDataTree.takeSnapshot();
        final DOMDataReadOnlyTransaction readOnlyTx = new ReadOnlyTransaction(operationalData, configSnapshot);
        final DOMDataWriteTransaction writeOnlyTx = new WriteTransaction(configDataTree, configSnapshot);
        return new ReadWriteTransaction(readOnlyTx, writeOnlyTx);
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return new WriteTransaction(configDataTree);
    }

    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store,
                                                                                  final YangInstanceIdentifier path,
                                                                                  final DOMDataChangeListener listener,
                                                                                  final DataChangeScope triggeringScope) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public DOMTransactionChain createTransactionChain(final TransactionChainListener listener) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Nonnull
    @Override
    public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
        return Collections.emptyMap();
    }
}


