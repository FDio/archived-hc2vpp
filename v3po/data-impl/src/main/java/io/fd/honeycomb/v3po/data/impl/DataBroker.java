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

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.v3po.data.DataModification;
import io.fd.honeycomb.v3po.data.ModifiableDataManager;
import io.fd.honeycomb.v3po.data.ReadableDataManager;
import java.io.Closeable;
import java.io.IOException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Broker which provides data transaction functionality for YANG capable data provider using {@link NormalizedNode}
 * data format.
 */
public class DataBroker implements DOMDataBroker, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(DataBroker.class);
    private final TransactionFactory transactionFactory;

    /**
     * Creates DataBroker instance.
     *
     * @param transactionFactory transaction producing factory
     */
    public DataBroker(final TransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        LOG.trace("DataBroker({}).newReadOnlyTransaction()", this);
        return transactionFactory.newReadOnlyTransaction();
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        LOG.trace("DataBroker({}).newReadWriteTransaction()", this);
        return transactionFactory.newReadWriteTransaction();
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        LOG.trace("DataBroker({}).newWriteOnlyTransaction()", this);
        return transactionFactory.newWriteOnlyTransaction();
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

    /**
     * Create DataBroker for a modifiable config DT, but only readable Operational
     */
    @Nonnull
    public static DataBroker create(@Nonnull final ModifiableDataManager configDataTree,
                             @Nonnull final ReadableDataManager operationalDataTree) {
        checkNotNull(operationalDataTree, "operationalDataTree should not be null");
        checkNotNull(configDataTree, "configDataTree should not be null");
        return new DataBroker(new MainPipelineTxFactory(configDataTree, operationalDataTree));
    }

    /**
     * Create DataBroker for modifiable operational DT, but no support for config
     */
    @Nonnull
    public static DataBroker create(@Nonnull final ModifiableDataManager operationalDataTree) {
        checkNotNull(operationalDataTree, "operationalDataTree should not be null");
        return new DataBroker(new ContextPipelineTxFactory(operationalDataTree));
    }

    @Override
    public void close() throws IOException {
        // NOOP
    }

    /**
     * Transaction provider factory to be used by {@link DataBroker}
     */
    public interface TransactionFactory {

        DOMDataReadOnlyTransaction newReadOnlyTransaction();

        DOMDataReadWriteTransaction newReadWriteTransaction();

        DOMDataWriteTransaction newWriteOnlyTransaction();
    }

    /**
     * Transaction factory specific for Honeycomb's main pipeline (config: read+write, operational: read-only)
     */
    private static class MainPipelineTxFactory implements TransactionFactory {
        private final ReadableDataManager operationalDataTree;
        private final ModifiableDataManager configDataTree;

        MainPipelineTxFactory(@Nonnull final ModifiableDataManager configDataTree,
                              @Nonnull final ReadableDataManager operationalDataTree) {
            this.operationalDataTree = operationalDataTree;
            this.configDataTree = configDataTree;
        }

        @Override
        public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
            return ReadOnlyTransaction.create(configDataTree.newModification(), operationalDataTree);
        }

        @Override
        public DOMDataReadWriteTransaction newReadWriteTransaction() {
            final DataModification configModification = configDataTree.newModification();
            return new ReadWriteTransaction(
                ReadOnlyTransaction.create(configModification, operationalDataTree),
                WriteTransaction.createConfigOnly(configModification));
        }

        @Override
        public DOMDataWriteTransaction newWriteOnlyTransaction() {
            return WriteTransaction.createConfigOnly(configDataTree.newModification());
        }
    }

    /**
     * Transaction factory specific for Honeycomb's context pipeline (config: none, operational: read+write)
     */
    private static class ContextPipelineTxFactory implements TransactionFactory {
        private final ModifiableDataManager operationalDataTree;

        ContextPipelineTxFactory(@Nonnull final ModifiableDataManager operationalDataTree) {
            this.operationalDataTree = operationalDataTree;
        }

        @Override
        public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
            return ReadOnlyTransaction.createOperationalOnly(operationalDataTree);
        }

        @Override
        public DOMDataReadWriteTransaction newReadWriteTransaction() {
            final DataModification dataModification = operationalDataTree.newModification();
            return new ReadWriteTransaction(
                ReadOnlyTransaction.createOperationalOnly(dataModification),
                WriteTransaction.createOperationalOnly(dataModification));
        }

        @Override
        public DOMDataWriteTransaction newWriteOnlyTransaction() {
            return WriteTransaction.createOperationalOnly(operationalDataTree.newModification());
        }
    }
}


