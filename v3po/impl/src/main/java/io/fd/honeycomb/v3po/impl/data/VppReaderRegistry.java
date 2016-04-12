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

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import io.fd.honeycomb.v3po.vpp.facade.impl.read.CompositeChildVppReader;
import io.fd.honeycomb.v3po.vpp.facade.impl.read.CompositeListVppReader;
import io.fd.honeycomb.v3po.vpp.facade.impl.read.CompositeRootVppReader;
import io.fd.honeycomb.v3po.vpp.facade.impl.read.util.DelegatingReaderRegistry;
import io.fd.honeycomb.v3po.vpp.facade.impl.read.util.ReflexiveChildReaderCustomizer;
import io.fd.honeycomb.v3po.vpp.facade.impl.read.util.ReflexiveRootReaderCustomizer;
import io.fd.honeycomb.v3po.vpp.facade.impl.util.VppRWUtils;
import io.fd.honeycomb.v3po.vpp.facade.read.ChildVppReader;
import io.fd.honeycomb.v3po.vpp.facade.read.ReadContext;
import io.fd.honeycomb.v3po.vpp.facade.read.ReadFailedException;
import io.fd.honeycomb.v3po.vpp.facade.read.ReaderRegistry;
import io.fd.honeycomb.v3po.vpp.facade.read.VppReader;
import io.fd.honeycomb.v3po.vpp.facade.v3po.vppstate.BridgeDomainCustomizer;
import io.fd.honeycomb.v3po.vpp.facade.v3po.vppstate.VersionCustomizer;
import java.util.ArrayList;
import java.util.Collections;
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

// TODO use some DI framework instead of singleton
public class VppReaderRegistry implements ReaderRegistry {

    private static VppReaderRegistry instance;

    private final DelegatingReaderRegistry reader;

    private VppReaderRegistry(@Nonnull final vppApi vppApi) {
        final CompositeRootVppReader<VppState, VppStateBuilder> vppStateReader = initVppStateReader(vppApi);
        // TODO add more root readers
        reader = new DelegatingReaderRegistry(Collections.<VppReader<? extends DataObject>>singletonList(vppStateReader));
    }

    private static CompositeRootVppReader<VppState, VppStateBuilder> initVppStateReader(@Nonnull final vppApi vppApi) {

        final ChildVppReader<Version> versionReader = new CompositeChildVppReader<>(
                Version.class, new VersionCustomizer(vppApi));

        final CompositeListVppReader<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder>
                bridgeDomainReader = new CompositeListVppReader<>(
                BridgeDomain.class,
                new BridgeDomainCustomizer(vppApi));

        final ChildVppReader<BridgeDomains> bridgeDomainsReader = new CompositeChildVppReader<>(
                BridgeDomains.class,
                VppRWUtils.singletonChildReaderList(bridgeDomainReader),
                new ReflexiveChildReaderCustomizer<>(BridgeDomainsBuilder.class));

        final List<ChildVppReader<? extends ChildOf<VppState>>> childVppReaders = new ArrayList<>();
        childVppReaders.add(versionReader);
        childVppReaders.add(bridgeDomainsReader);

        return new CompositeRootVppReader<>(
                VppState.class,
                childVppReaders,
                VppRWUtils.<VppState>emptyAugReaderList(),
                new ReflexiveRootReaderCustomizer<>(VppStateBuilder.class));
    }

    public static synchronized VppReaderRegistry getInstance(@Nonnull final vppApi vppApi) {
        if (instance == null) {
            instance = new VppReaderRegistry(vppApi);
        }
        return instance;
    }

    @Nonnull
    @Override
    public Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> readAll(
        @Nonnull final ReadContext ctx) throws ReadFailedException {
        return reader.readAll(ctx);
    }

    @Nonnull
    @Override
    public Optional<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                               @Nonnull final ReadContext ctx) throws ReadFailedException {
        return reader.read(id, ctx);
    }

    @Nonnull
    @Override
    public InstanceIdentifier<DataObject> getManagedDataObjectType() {
        return reader.getManagedDataObjectType();
    }
}
