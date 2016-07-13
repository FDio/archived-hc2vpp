package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import static org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.InterfacesHoneycombWriterModule.ACL_ID;

import io.fd.honeycomb.v3po.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.vppclassifier.ClassifySessionWriter;
import io.fd.honeycomb.v3po.translate.v3po.vppclassifier.ClassifyTableWriter;
import io.fd.honeycomb.v3po.translate.write.WriterFactory;
import io.fd.honeycomb.v3po.translate.write.registry.ModifiableWriterRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.future.FutureJVpp;

public class VppClassifierHoneycombWriterModule extends
    org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.AbstractVppClassifierHoneycombWriterModule {


    public static final InstanceIdentifier<ClassifyTable> CLASSIFY_TABLE_ID =
        InstanceIdentifier.create(VppClassifier.class).child(ClassifyTable.class);

    public static final InstanceIdentifier<ClassifySession> CLASSIFY_SESSION_ID =
        CLASSIFY_TABLE_ID.child(ClassifySession.class);


    public VppClassifierHoneycombWriterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                              org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VppClassifierHoneycombWriterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                                              org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                              org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.VppClassifierHoneycombWriterModule oldModule,
                                              java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new VppClassifierHoneycombWriterFactory(
            getVppJvppDependency(),
            getClassifyTableContextDependency());
    }

    private static final class VppClassifierHoneycombWriterFactory implements WriterFactory, AutoCloseable {
        private final FutureJVpp jvpp;
        private final NamingContext classifyTableContext;

        public VppClassifierHoneycombWriterFactory(@Nonnull final FutureJVpp jvpp,
                                                   @Nonnull final NamingContext classifyTableContext) {
            this.jvpp = jvpp;
            this.classifyTableContext = classifyTableContext;
        }

        @Override
        public void close() throws Exception {
            // unregister is not supported in ModifiableWriterRegistry (not really needed though)
        }

        @Override
        public void init(final ModifiableWriterRegistryBuilder registry) {

            registry.addBefore(
                new GenericListWriter<>(CLASSIFY_TABLE_ID, new ClassifyTableWriter(jvpp, classifyTableContext)),
                ACL_ID);

            registry.addBefore(
                new GenericListWriter<>(CLASSIFY_SESSION_ID, new ClassifySessionWriter(jvpp, classifyTableContext)),
                CLASSIFY_TABLE_ID);
        }
    }
}
