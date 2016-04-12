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
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveRootReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.vppstate.BridgeDomainCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.vppstate.VersionCustomizer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.vppjapi.vppApi;

public class VppStateHoneycombReaderModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.AbstractVppStateHoneycombReaderModule {
    public VppStateHoneycombReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VppStateHoneycombReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.VppStateHoneycombReaderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final vppApi vppApi = getVppJapiDependency();

        final ChildReader<Version> versionReader = new CompositeChildReader<>(Version.class, new VersionCustomizer(vppApi));

        final CompositeListReader<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder> bridgeDomainReader =
            new CompositeListReader<>(BridgeDomain.class, new BridgeDomainCustomizer(vppApi));

        final ChildReader<BridgeDomains> bridgeDomainsReader = new CompositeChildReader<>(
            BridgeDomains.class,
            RWUtils.singletonChildReaderList(bridgeDomainReader),
            new ReflexiveChildReaderCustomizer<>(BridgeDomainsBuilder.class));

        final List<ChildReader<? extends ChildOf<VppState>>> childVppReaders = new ArrayList<>();
        childVppReaders.add(versionReader);
        childVppReaders.add(bridgeDomainsReader);

        return new CloseableReader(new CompositeRootReader<>(
            VppState.class,
            childVppReaders,
            RWUtils.<VppState>emptyAugReaderList(),
            new ReflexiveRootReaderCustomizer<>(VppStateBuilder.class)));
    }

    // TODO move to utils
    private static final class CloseableReader implements Reader<VppState>, AutoCloseable {

        private CompositeRootReader<VppState, VppStateBuilder> vppStateVppStateBuilderCompositeRootReader;

        public CloseableReader(
            final CompositeRootReader<VppState, VppStateBuilder> vppStateVppStateBuilderCompositeRootReader) {
            this.vppStateVppStateBuilderCompositeRootReader = vppStateVppStateBuilderCompositeRootReader;
        }

        @Nonnull
        @Override
        public Optional<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                                   @Nonnull final ReadContext ctx) throws ReadFailedException {
            return vppStateVppStateBuilderCompositeRootReader.read(id, ctx);
        }

        @Nonnull
        @Override
        public InstanceIdentifier<VppState> getManagedDataObjectType() {
            return vppStateVppStateBuilderCompositeRootReader.getManagedDataObjectType();
        }

        @Override
        public String toString() {
            return vppStateVppStateBuilderCompositeRootReader.toString();
        }

        @Override
        public void close() throws Exception {
            //NOOP
        }
    }
}
