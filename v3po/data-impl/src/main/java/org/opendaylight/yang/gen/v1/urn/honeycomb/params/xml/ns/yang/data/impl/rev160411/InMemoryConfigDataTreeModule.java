package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.impl.rev160411;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryConfigDataTreeModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.impl.rev160411.AbstractInMemoryConfigDataTreeModule {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryConfigDataTreeModule.class);

    public InMemoryConfigDataTreeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public InMemoryConfigDataTreeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.impl.rev160411.InMemoryConfigDataTreeModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.debug("InMemoryConfigDataTreeModule.createInstance()");
        return new CloseableConfigDataTree(getSchemaServiceDependency().getGlobalContext());
    }

    private static class CloseableConfigDataTree implements AutoCloseable, DataTree {
        private final DataTree dataTree;

        public CloseableConfigDataTree(final SchemaContext schemaContext) {
            this.dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.CONFIGURATION);
            dataTree.setSchemaContext(schemaContext);
        }

        @Override
        public void close() throws Exception {
            // NOP
        }

        @Override
        public DataTreeSnapshot takeSnapshot() {
            return dataTree.takeSnapshot();
        }

        @Override
        public void setSchemaContext(final SchemaContext newSchemaContext) {
            dataTree.setSchemaContext(newSchemaContext);
        }

        @Override
        public void commit(final DataTreeCandidate candidate) {
            dataTree.commit(candidate);
        }

        @Override
        public YangInstanceIdentifier getRootPath() {
            return dataTree.getRootPath();
        }

        @Override
        public void validate(final DataTreeModification modification) throws DataValidationFailedException {
            dataTree.validate(modification);
        }

        @Override
        public DataTreeCandidate prepare(final DataTreeModification modification) {
            return dataTree.prepare(modification);
        }
    }
}
