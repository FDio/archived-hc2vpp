package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.impl.rev160411;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.data.DataModification;
import io.fd.honeycomb.v3po.data.ModifiableDataManager;
import io.fd.honeycomb.v3po.data.impl.ModifiableDataTreeDelegator;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigDataTreeModule extends
        org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.impl.rev160411.AbstractConfigDataTreeModule {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigDataTreeModule.class);

    public ConfigDataTreeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ConfigDataTreeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.impl.rev160411.ConfigDataTreeModule oldModule,
                                java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.debug("ConfigDataTreeModule.createInstance()");
        return new CloseableConfigDataTree(
                new ModifiableDataTreeDelegator(getSerializerDependency(), getDataTreeDependency(),
                        getWriterRegistryBuilderDependency().build(), getContextBindingBrokerDependency()));
    }

    private static final class CloseableConfigDataTree implements ModifiableDataManager, AutoCloseable {

        private final ModifiableDataTreeDelegator delegate;

        CloseableConfigDataTree(final ModifiableDataTreeDelegator delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() throws Exception {
            LOG.debug("CloseableConfigDataTree.close()");
            // NOP
        }

        @Override
        public DataModification newModification() {
            LOG.trace("CloseableConfigDataTree.newModification");
            return delegate.newModification();
        }

        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
            @Nonnull final YangInstanceIdentifier path) {
            return delegate.read(path);
        }
    }
}
