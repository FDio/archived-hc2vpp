package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeChildReader;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeListReader;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeRootReader;
import io.fd.honeycomb.v3po.translate.read.ChildReader;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.read.Reader;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveRootReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.InterfaceCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.VppInterfaceStateCustomizer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.vppjapi.vppApi;

import javax.annotation.Nonnull;

public class InterfacesStateHoneycombReaderModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.AbstractInterfacesStateHoneycombReaderModule {
    public InterfacesStateHoneycombReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public InterfacesStateHoneycombReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.InterfacesStateHoneycombReaderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final vppApi vppApi = getVppJapiDependency();

        final ChildReader<VppInterfaceStateAugmentation> vppInterfaceStateAugmentationChildReader =
                new CompositeChildReader<>(VppInterfaceStateAugmentation.class,
                        new VppInterfaceStateCustomizer(vppApi));

        final CompositeListReader<Interface, InterfaceKey, InterfaceBuilder> interfaceReader =
                new CompositeListReader<>(Interface.class,
                        RWUtils.<Interface>emptyChildReaderList(),
                        RWUtils.<Interface>singletonAugReaderList(vppInterfaceStateAugmentationChildReader),
                        new InterfaceCustomizer(vppApi));

        return new CloseableReader(new CompositeRootReader<>(
                InterfacesState.class,
                RWUtils.singletonChildReaderList(interfaceReader),
                RWUtils.<InterfacesState>emptyAugReaderList(),
                new ReflexiveRootReaderCustomizer<>(InterfacesStateBuilder.class)));
    }


    private static final class CloseableReader implements Reader<InterfacesState>, AutoCloseable {

        private CompositeRootReader<InterfacesState, InterfacesStateBuilder> compositeRootReader;

        public CloseableReader(
                final CompositeRootReader<InterfacesState, InterfacesStateBuilder> compositeRootReader) {
            this.compositeRootReader = compositeRootReader;
        }

        @Nonnull
        @Override
        public Optional<? extends DataObject> read(@Nonnull InstanceIdentifier<? extends DataObject> id,
                                                   @Nonnull ReadContext ctx) throws ReadFailedException {
            return compositeRootReader.read(id, ctx);
        }

        @Nonnull
        @Override
        public InstanceIdentifier<InterfacesState> getManagedDataObjectType() {
            return compositeRootReader.getManagedDataObjectType();
        }

        @Override
        public String toString() {
            return compositeRootReader.toString();
        }

        @Override
        public void close() throws Exception {
            //NOOP
        }
    }
}
