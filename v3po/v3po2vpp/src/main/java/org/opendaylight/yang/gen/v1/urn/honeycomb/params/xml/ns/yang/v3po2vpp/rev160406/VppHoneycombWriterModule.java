package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406;

import io.fd.honeycomb.v3po.translate.TranslationException;
import io.fd.honeycomb.v3po.translate.impl.write.CompositeChildWriter;
import io.fd.honeycomb.v3po.translate.impl.write.CompositeListWriter;
import io.fd.honeycomb.v3po.translate.impl.write.CompositeRootWriter;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.util.write.NoopWriterCustomizer;
import io.fd.honeycomb.v3po.translate.util.write.ReflexiveChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.vpp.BridgeDomainCustomizer;
import io.fd.honeycomb.v3po.translate.write.ChildWriter;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.Writer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppHoneycombWriterModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.AbstractVppHoneycombWriterModule {
    public VppHoneycombWriterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VppHoneycombWriterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.v3po2vpp.rev160406.VppHoneycombWriterModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final CompositeListWriter<BridgeDomain, BridgeDomainKey> bridgeDomainWriter = new CompositeListWriter<>(
            BridgeDomain.class,
            new BridgeDomainCustomizer(getVppJapiWriterDependency()));

        final ChildWriter<BridgeDomains> bridgeDomainsReader = new CompositeChildWriter<>(
            BridgeDomains.class,
            RWUtils.singletonChildWriterList(bridgeDomainWriter),
            new ReflexiveChildWriterCustomizer<BridgeDomains>());

        final List<ChildWriter<? extends ChildOf<Vpp>>> childWriters = new ArrayList<>();
        childWriters.add(bridgeDomainsReader);

        return new CloseableWriter(new CompositeRootWriter<>(
            Vpp.class,
            childWriters,
            new NoopWriterCustomizer<Vpp>()));
    }

    // TODO move to translate-utils
    private static final class CloseableWriter implements Writer<Vpp>, AutoCloseable {

        private CompositeRootWriter<Vpp> vppCompositeRootWriter;

        public CloseableWriter(
            final CompositeRootWriter<Vpp> vppCompositeRootWriter) {
            this.vppCompositeRootWriter = vppCompositeRootWriter;
        }

        @Override
        public void update(
            @Nonnull final InstanceIdentifier<? extends DataObject> id,
            @Nullable final DataObject dataBefore,
            @Nullable final DataObject dataAfter,
            @Nonnull final WriteContext ctx) throws TranslationException {
            vppCompositeRootWriter.update(id, dataBefore, dataAfter, ctx);
        }

        @Nonnull
        @Override
        public InstanceIdentifier<Vpp> getManagedDataObjectType() {
            return vppCompositeRootWriter.getManagedDataObjectType();
        }

        @Override
        public String toString() {
            return vppCompositeRootWriter.toString();
        }

        @Override
        public void close() throws Exception {

        }
    }

}
