package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.MappingContext;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RealtimeMappingContextModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406.AbstractRealtimeMappingContextModule {
    public RealtimeMappingContextModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public RealtimeMappingContextModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406.RealtimeMappingContextModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // Provides real time CRUD on top of Context data broker
        return new MappingContext() {

            @Override
            public <T extends DataObject> Optional<T> read(@Nonnull final InstanceIdentifier<T> currentId) {
                try(ReadOnlyTransaction tx = getContextBindingBrokerDependency().newReadOnlyTransaction()) {
                    try {
                        return tx.read(LogicalDatastoreType.OPERATIONAL, currentId).checkedGet();
                    } catch (ReadFailedException e) {
                        throw new IllegalStateException("Unable to perform read of " + currentId, e);
                    }
                }
            }

            @Override
            public void delete(final InstanceIdentifier<?> path) {
                final WriteTransaction writeTx = getContextBindingBrokerDependency().newWriteOnlyTransaction();
                writeTx.delete(LogicalDatastoreType.OPERATIONAL, path);
                try {
                    writeTx.submit().checkedGet();
                } catch (TransactionCommitFailedException e) {
                    throw new IllegalStateException("Unable to perform delete of " + path, e);
                }
            }

            @Override
            public <T extends DataObject> void merge(final InstanceIdentifier<T> path, final T data) {
                final WriteTransaction writeTx = getContextBindingBrokerDependency().newWriteOnlyTransaction();
                writeTx.merge(LogicalDatastoreType.OPERATIONAL, path, data);
                try {
                    writeTx.submit().checkedGet();
                } catch (TransactionCommitFailedException e) {
                    throw new IllegalStateException("Unable to perform merge of " + path, e);
                }
            }

            @Override
            public <T extends DataObject> void put(final InstanceIdentifier<T> path, final T data) {
                final WriteTransaction writeTx = getContextBindingBrokerDependency().newWriteOnlyTransaction();
                writeTx.put(LogicalDatastoreType.OPERATIONAL, path, data);
                try {
                    writeTx.submit().checkedGet();
                } catch (TransactionCommitFailedException e) {
                    throw new IllegalStateException("Unable to perform put of " + path, e);
                }
            }

            @Override
            public void close() {
                // Noop
            }
        };
    }

}
