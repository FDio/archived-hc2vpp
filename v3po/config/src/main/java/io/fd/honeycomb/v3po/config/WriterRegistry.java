/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.v3po.config;

import io.fd.honeycomb.v3po.translate.TranslationException;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.impl.write.CompositeChildWriter;
import io.fd.honeycomb.v3po.translate.impl.write.CompositeListWriter;
import io.fd.honeycomb.v3po.translate.impl.write.CompositeRootWriter;
import io.fd.honeycomb.v3po.translate.util.write.DelegatingWriterRegistry;
import io.fd.honeycomb.v3po.translate.util.write.NoopWriterCustomizer;
import io.fd.honeycomb.v3po.translate.util.write.ReflexiveChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.vpp.BridgeDomainCustomizer;
import io.fd.honeycomb.v3po.translate.write.ChildWriter;
import io.fd.honeycomb.v3po.translate.write.Writer;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.vppjapi.vppApi;

// TODO use some DI framework instead of singleton
public class WriterRegistry implements io.fd.honeycomb.v3po.translate.write.WriterRegistry {

    private static WriterRegistry instance;

    private final DelegatingWriterRegistry writer;

    private WriterRegistry(@Nonnull final vppApi vppApi) {
        final CompositeRootWriter<Vpp> vppWriter = initVppStateWriter(vppApi);
        writer = new DelegatingWriterRegistry(Collections.<Writer<? extends DataObject>>singletonList(vppWriter));
    }

    private static CompositeRootWriter<Vpp> initVppStateWriter(@Nonnull final vppApi vppApi) {
        final CompositeListWriter<BridgeDomain, BridgeDomainKey> bridgeDomainWriter = new CompositeListWriter<>(
            BridgeDomain.class,
            new BridgeDomainCustomizer(vppApi));

        final ChildWriter<BridgeDomains> bridgeDomainsWriter = new CompositeChildWriter<>(
            BridgeDomains.class,
            RWUtils.singletonChildWriterList(bridgeDomainWriter),
            new ReflexiveChildWriterCustomizer<BridgeDomains>());

        final List<ChildWriter<? extends ChildOf<Vpp>>> childWriters = new ArrayList<>();
        childWriters.add(bridgeDomainsWriter);

        return new CompositeRootWriter<>(
            Vpp.class,
            childWriters,
            new NoopWriterCustomizer<Vpp>());
    }

    public static synchronized WriterRegistry getInstance(@Nonnull final vppApi vppApi) {
        if (instance == null) {
            instance = new WriterRegistry(vppApi);
        }
        return instance;
    }

    @Nonnull
    @Override
    public InstanceIdentifier<DataObject> getManagedDataObjectType() {
        return writer.getManagedDataObjectType();
    }

    @Override
    public void update(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                       @Nullable final DataObject dataBefore,
                       @Nullable final DataObject data, @Nonnull final WriteContext ctx) throws TranslationException {
        writer.update(id, dataBefore, data, ctx);
    }

    @Override
    public void update(@Nonnull final Map<InstanceIdentifier<?>, DataObject> dataBefore,
                       @Nonnull final Map<InstanceIdentifier<?>, DataObject> dataAfter,
                       @Nonnull final WriteContext ctx)
        throws TranslationException {
        writer.update(dataBefore, dataAfter, ctx);
    }
}
