package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.context.impl.rev141210;

import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.BindingBrokerReader;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.ContextsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
* A reader to provide naming context related data
*/
public class ContextReaderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.context.impl.rev141210.AbstractContextReaderModule {
    public ContextReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ContextReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.context.impl.rev141210.ContextReaderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new ReaderFactory(getContextBindingBrokerDependency());
    }

    private static final class ReaderFactory implements AutoCloseable, io.fd.honeycomb.translate.read.ReaderFactory {

        private final DataBroker contextBindingBrokerDependency;

        public ReaderFactory(final DataBroker contextBindingBrokerDependency) {
            this.contextBindingBrokerDependency = contextBindingBrokerDependency;
        }

        @Override
        public void init(final ModifiableReaderRegistryBuilder registry) {
            registry.add(new BindingBrokerReader<>(InstanceIdentifier.create(Contexts.class),
                    contextBindingBrokerDependency,
                    LogicalDatastoreType.OPERATIONAL, ContextsBuilder.class));
        }
    }

}
