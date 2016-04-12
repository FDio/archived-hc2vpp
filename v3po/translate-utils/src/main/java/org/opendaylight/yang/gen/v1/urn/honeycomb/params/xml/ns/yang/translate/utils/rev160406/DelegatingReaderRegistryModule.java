package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.read.Reader;
import io.fd.honeycomb.v3po.translate.read.ReaderRegistry;
import io.fd.honeycomb.v3po.translate.util.read.DelegatingReaderRegistry;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DelegatingReaderRegistryModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406.AbstractDelegatingReaderRegistryModule {
    public DelegatingReaderRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DelegatingReaderRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406.DelegatingReaderRegistryModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final List<Reader<? extends DataObject>> rootReadersDependency = Lists.transform(getRootReadersDependency(),
            new Function<Reader, Reader<? extends DataObject>>() {

                @SuppressWarnings("unchecked")
                @Override
                public Reader<? extends DataObject> apply(final Reader input) {
                    return input;
                }
            });
        return new CloseableReaderRegistry(new DelegatingReaderRegistry(rootReadersDependency));
    }

    // TODO move to translate-utils
    private static final class CloseableReaderRegistry implements ReaderRegistry, AutoCloseable {
        private final DelegatingReaderRegistry delegatingReaderRegistry;

        CloseableReaderRegistry(
            final DelegatingReaderRegistry delegatingReaderRegistry) {
            this.delegatingReaderRegistry = delegatingReaderRegistry;
        }

        @Override
        @Nonnull
        public Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> readAll(
            @Nonnull final ReadContext ctx) throws ReadFailedException {
            return delegatingReaderRegistry.readAll(ctx);
        }

        @Nonnull
        @Override
        public Optional<? extends DataObject> read(
            @Nonnull final InstanceIdentifier<? extends DataObject> id,
            @Nonnull final ReadContext ctx) throws ReadFailedException {
            return delegatingReaderRegistry.read(id, ctx);
        }

        @Nonnull
        @Override
        public InstanceIdentifier<DataObject> getManagedDataObjectType() {
            return delegatingReaderRegistry.getManagedDataObjectType();
        }

        @Override
        public void close() throws Exception {
            // NOOP
        }
    }
}
