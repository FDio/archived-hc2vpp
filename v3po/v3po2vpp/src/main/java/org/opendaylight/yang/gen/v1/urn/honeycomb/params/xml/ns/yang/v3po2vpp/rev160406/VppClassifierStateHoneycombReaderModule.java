package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.vppclassifier.ClassifySessionReader;
import io.fd.honeycomb.translate.v3po.vppclassifier.ClassifyTableReader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifierState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifierStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.state.ClassifyTable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.future.FutureJVpp;

public class VppClassifierStateHoneycombReaderModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.AbstractVppClassifierStateHoneycombReaderModule {
    public VppClassifierStateHoneycombReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VppClassifierStateHoneycombReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.VppClassifierStateHoneycombReaderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new VppClassifierReaderFactory(getVppJvppDependency(), getClassifyTableContextDependency());
    }

    private static final class VppClassifierReaderFactory implements ReaderFactory, AutoCloseable {

        private final FutureJVpp jvpp;
        private final NamingContext classifyCtx;

        VppClassifierReaderFactory(final FutureJVpp jvpp,
                                          final NamingContext classifyCtx) {
            this.jvpp = jvpp;
            this.classifyCtx = classifyCtx;
        }

        @Override
        public void init(final ModifiableReaderRegistryBuilder registry) {
            // VppClassifierState
            final InstanceIdentifier<VppClassifierState> vppStateId = InstanceIdentifier.create(VppClassifierState.class);
            registry.addStructuralReader(vppStateId, VppClassifierStateBuilder.class);
            //  ClassifyTable
            final InstanceIdentifier<ClassifyTable> classTblId = vppStateId.child(ClassifyTable.class);
            registry.add(new GenericListReader<>(classTblId, new ClassifyTableReader(jvpp, classifyCtx)));
            //   ClassifySession
            final InstanceIdentifier<ClassifySession> classSesId = classTblId.child(ClassifySession.class);
            registry.add(new GenericListReader<>(classSesId, new ClassifySessionReader(jvpp, classifyCtx)));
        }
    }
}
