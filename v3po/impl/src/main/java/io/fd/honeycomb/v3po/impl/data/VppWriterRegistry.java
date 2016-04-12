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

package io.fd.honeycomb.v3po.impl.data;

import io.fd.honeycomb.v3po.impl.trans.VppException;
import io.fd.honeycomb.v3po.impl.trans.util.VppRWUtils;
import io.fd.honeycomb.v3po.impl.trans.w.ChildVppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.VppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.WriteContext;
import io.fd.honeycomb.v3po.impl.trans.w.WriterRegistry;
import io.fd.honeycomb.v3po.impl.trans.w.impl.CompositeChildVppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.impl.CompositeListVppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.impl.CompositeRootVppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.util.DelegatingWriterRegistry;
import io.fd.honeycomb.v3po.impl.trans.w.util.NoopWriterCustomizer;
import io.fd.honeycomb.v3po.impl.trans.w.util.ReflexiveChildWriterCustomizer;
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
public class VppWriterRegistry implements WriterRegistry {

    private static VppWriterRegistry instance;

    private final DelegatingWriterRegistry writer;

    private VppWriterRegistry(@Nonnull final vppApi vppApi) {
        final CompositeRootVppWriter<Vpp> vppWriter = initVppStateWriter(vppApi);
        writer = new DelegatingWriterRegistry(Collections.<VppWriter<? extends DataObject>>singletonList(vppWriter));
    }

    private static CompositeRootVppWriter<Vpp> initVppStateWriter(@Nonnull final vppApi vppApi) {
        final CompositeListVppWriter<BridgeDomain, BridgeDomainKey> bridgeDomainWriter = new CompositeListVppWriter<>(
            BridgeDomain.class,
            new io.fd.honeycomb.v3po.impl.vpp.BridgeDomainCustomizer(vppApi));

        final ChildVppWriter<BridgeDomains> bridgeDomainsWriter = new CompositeChildVppWriter<>(
            BridgeDomains.class,
            VppRWUtils.singletonChildWriterList(bridgeDomainWriter),
            new ReflexiveChildWriterCustomizer<BridgeDomains>());

        final List<ChildVppWriter<? extends ChildOf<Vpp>>> childWriters = new ArrayList<>();
        childWriters.add(bridgeDomainsWriter);

        return new CompositeRootVppWriter<>(
            Vpp.class,
            childWriters,
            new NoopWriterCustomizer<Vpp>());
    }

    public static synchronized VppWriterRegistry getInstance(@Nonnull final vppApi vppApi) {
        if (instance == null) {
            instance = new VppWriterRegistry(vppApi);
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
                       @Nullable final DataObject data, @Nonnull final WriteContext ctx) throws VppException {
        writer.update(id, dataBefore, data, ctx);
    }

    @Override
    public void update(@Nonnull final Map<InstanceIdentifier<?>, DataObject> dataBefore,
                       @Nonnull final Map<InstanceIdentifier<?>, DataObject> dataAfter,
                       @Nonnull final WriteContext ctx)
        throws VppException {
        writer.update(dataBefore, dataAfter, ctx);
    }
}
