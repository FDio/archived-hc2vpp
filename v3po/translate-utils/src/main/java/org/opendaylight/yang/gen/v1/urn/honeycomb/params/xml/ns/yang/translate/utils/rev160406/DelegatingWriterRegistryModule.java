package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.TranslationException;
import io.fd.honeycomb.v3po.translate.util.write.DelegatingWriterRegistry;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.Writer;
import io.fd.honeycomb.v3po.translate.write.WriterRegistry;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DelegatingWriterRegistryModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406.AbstractDelegatingWriterRegistryModule {
    public DelegatingWriterRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DelegatingWriterRegistryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406.DelegatingWriterRegistryModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final List<Writer<? extends DataObject>> rootReadersDependency = Lists.transform(getRootWritersDependency(),
            new Function<Writer, Writer<? extends DataObject>>() {

                @SuppressWarnings("unchecked")
                @Override
                public Writer<? extends DataObject> apply(final Writer input) {
                    return input;
                }
            });
        return new CloseableWriterRegistry(new DelegatingWriterRegistry(rootReadersDependency));
    }

    // TODO move to translate-utils
    private static final class CloseableWriterRegistry implements WriterRegistry, AutoCloseable {
        private final DelegatingWriterRegistry delegatingWriterRegistry;

        CloseableWriterRegistry(
            final DelegatingWriterRegistry delegatingWriterRegistry) {
            this.delegatingWriterRegistry = delegatingWriterRegistry;
        }

        @Override
        public void update(
            @Nonnull final Map<InstanceIdentifier<?>, DataObject> nodesBefore,
            @Nonnull final Map<InstanceIdentifier<?>, DataObject> nodesAfter,
            @Nonnull final WriteContext ctx) throws TranslationException {
            delegatingWriterRegistry.update(nodesBefore, nodesAfter, ctx);
        }

        @Override
        public void update(
            @Nonnull final InstanceIdentifier<? extends DataObject> id,
            @Nullable final DataObject dataBefore, @Nullable final DataObject dataAfter,
            @Nonnull final WriteContext ctx) throws TranslationException {
            delegatingWriterRegistry.update(id, dataBefore, dataAfter, ctx);
        }

        @Nonnull
        @Override
        public InstanceIdentifier<DataObject> getManagedDataObjectType() {
            return delegatingWriterRegistry.getManagedDataObjectType();
        }

        @Override
        public void close() throws Exception {
            // NOOP
        }
    }

}
