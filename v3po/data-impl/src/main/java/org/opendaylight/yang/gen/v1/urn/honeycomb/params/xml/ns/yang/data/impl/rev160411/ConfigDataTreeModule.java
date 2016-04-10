package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.impl.rev160411;

import io.fd.honeycomb.v3po.data.DataTreeSnapshot;
import io.fd.honeycomb.v3po.data.ModifiableDataTree;
import io.fd.honeycomb.v3po.data.impl.ConfigDataTree;
import io.fd.honeycomb.v3po.translate.TranslationException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;

public class ConfigDataTreeModule extends
        org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.impl.rev160411.AbstractConfigDataTreeModule {
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
        final DataTree dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.CONFIGURATION);
        dataTree.setSchemaContext(getSchemaServiceDependency().getGlobalContext());
        return new CloseableConfigDataTree(
                new ConfigDataTree(getSerializerDependency(), dataTree, getWriterRegistryDependency()));
    }

    private static final class CloseableConfigDataTree implements ModifiableDataTree, AutoCloseable {

        private final ConfigDataTree delegate;

        CloseableConfigDataTree(final ConfigDataTree delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() throws Exception {
            // NOP
        }

        @Override
        public void modify(final DataTreeModification modification)
                throws DataValidationFailedException, TranslationException {
            delegate.modify(modification);
        }

        @Override
        public DataTreeSnapshot takeSnapshot() {
            return delegate.takeSnapshot();
        }
    }
}
