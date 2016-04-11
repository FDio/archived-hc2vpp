package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.util.write.CloseableWriterRegistry;
import io.fd.honeycomb.v3po.translate.util.write.DelegatingWriterRegistry;
import io.fd.honeycomb.v3po.translate.write.Writer;
import java.util.List;
import org.opendaylight.yangtools.yang.binding.DataObject;

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

}
