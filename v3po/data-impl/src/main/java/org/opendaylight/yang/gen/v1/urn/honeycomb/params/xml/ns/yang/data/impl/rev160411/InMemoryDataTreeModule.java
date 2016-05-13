package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.impl.rev160411;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.*;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryDataTreeModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.impl.rev160411.AbstractInMemoryDataTreeModule {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDataTreeModule.class);

    public InMemoryDataTreeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public InMemoryDataTreeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.data.impl.rev160411.InMemoryDataTreeModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.debug("InMemoryConfigDataTreeModule.createInstance()");
        return new CloseableConfigDataTree(getSchemaServiceDependency().getGlobalContext(), getType());
    }

    private static class CloseableConfigDataTree implements AutoCloseable,
        org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree {
        private final org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree dataTree;

        public CloseableConfigDataTree(final SchemaContext schemaContext, final DatatreeType type) {
            this.dataTree = InMemoryDataTreeFactory.getInstance().create(
                type == DatatreeType.Config ? TreeType.CONFIGURATION : TreeType.OPERATIONAL
            );
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
