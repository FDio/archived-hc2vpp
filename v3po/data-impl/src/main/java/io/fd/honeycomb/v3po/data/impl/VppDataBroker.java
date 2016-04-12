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
import io.fd.honeycomb.v3po.data.ReadableVppDataTree;
import io.fd.honeycomb.v3po.data.VppDataTree;
import io.fd.honeycomb.v3po.data.VppDataTreeSnapshot;
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
 * Data Broker which provides data transaction functionality for VPP using {@link NormalizedNode} data format.
 */
public class VppDataBroker implements DOMDataBroker {

    private final ReadableVppDataTree operationalData;
    private final VppDataTree configDataTree;

    /**
     * Creates VppDataBroker instance.
     *
     * @param operationalData VPP operational data
     * @param configDataTree  VPP configuration data
     */
    public VppDataBroker(@Nonnull final ReadableVppDataTree operationalData,
                         @Nonnull final VppDataTree configDataTree) {
        this.operationalData = Preconditions.checkNotNull(operationalData, "operationalData should not be null");
        this.configDataTree = Preconditions.checkNotNull(configDataTree, "configDataProxy should not be null");
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return new VppReadOnlyTransaction(operationalData, configDataTree.takeSnapshot());
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        // todo use the same snapshot
        final VppDataTreeSnapshot configSnapshot = configDataTree.takeSnapshot();
        final DOMDataReadOnlyTransaction readOnlyTx = new VppReadOnlyTransaction(operationalData, configSnapshot);
        final DOMDataWriteTransaction writeOnlyTx = new VppWriteTransaction(configDataTree, configSnapshot);
        return new ReadWriteTransaction(readOnlyTx, writeOnlyTx);
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return new VppWriteTransaction(configDataTree);
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


