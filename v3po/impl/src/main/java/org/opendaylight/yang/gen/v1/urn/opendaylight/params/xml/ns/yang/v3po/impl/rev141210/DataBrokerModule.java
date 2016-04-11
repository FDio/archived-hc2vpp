package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210;

import io.fd.honeycomb.v3po.data.impl.DataBroker;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataBrokerModule extends
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210.AbstractDataBrokerModule {

    private static final Logger LOG = LoggerFactory.getLogger(DataBrokerModule.class);

    public DataBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DataBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210.DataBrokerModule oldModule,
                            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("DataBrokerModule.createInstance()");
        return new CloseableDataBroker(
                new DataBroker(getOperationalDataTreeDependency(), getConfigDataTreeDependency()));
    }

    private static final class CloseableDataBroker implements AutoCloseable, DOMDataBroker {

        private final DataBroker delegate;

        CloseableDataBroker(final DataBroker delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() throws Exception {
            LOG.info("CloseableDataBroker.close()");
            // NOP
        }

        @Override
        public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
            LOG.trace("CloseableDataBroker.newReadOnlyTransaction()");
            return delegate.newReadOnlyTransaction();
        }

        @Override
        public DOMDataReadWriteTransaction newReadWriteTransaction() {
            LOG.trace("CloseableDataBroker.newReadWriteTransaction()");
            return delegate.newReadWriteTransaction();
        }

        @Override
        public DOMDataWriteTransaction newWriteOnlyTransaction() {
            LOG.trace("CloseableDataBroker.newWriteOnlyTransaction()");
            return delegate.newWriteOnlyTransaction();
        }

        @Override
        public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store,
                                                                                      final YangInstanceIdentifier path,
                                                                                      final DOMDataChangeListener listener,
                                                                                      final DataChangeScope triggeringScope) {
            LOG.trace("CloseableDataBroker.createTransactionChain store={}, path={}, listener={}, triggeringScope={}",
                    store, path, listener, triggeringScope);
            return delegate.registerDataChangeListener(store, path, listener, triggeringScope);
        }

        @Override
        public DOMTransactionChain createTransactionChain(final TransactionChainListener listener) {
            LOG.trace("CloseableDataBroker.createTransactionChain listener={}", listener);
            return delegate.createTransactionChain(listener);
        }

        @Nonnull
        @Override
        public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
            LOG.trace("CloseableDataBroker.getSupportedExtensions()");
            return delegate.getSupportedExtensions();
        }
    }
}
