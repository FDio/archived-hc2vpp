/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.lisp.translate.write.factory;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.translate.write.AdjacencyCustomizer;
import io.fd.honeycomb.lisp.translate.write.BridgeDomainSubtableCustomizer;
import io.fd.honeycomb.lisp.translate.write.LocalMappingCustomizer;
import io.fd.honeycomb.lisp.translate.write.RemoteMappingCustomizer;
import io.fd.honeycomb.lisp.translate.write.VniTableCustomizer;
import io.fd.honeycomb.lisp.translate.write.VrfSubtableCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.Adjacencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.adjacency.LocalEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.adjacency.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.negative.mapping.MapReply;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.Rlocs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.Locator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Factory for producing writers for {@code EidTable}
 */
final class VniTableWriterFactory extends AbstractLispWriterFactoryBase implements WriterFactory {

    private final NamingContext bridgeDomainContext;

    private VniTableWriterFactory(final InstanceIdentifier<Lisp> lispInstanceIdentifier,
                                  final FutureJVppCore vppApi,
                                  final EidMappingContext localMappingContext,
                                  final EidMappingContext remoteMappingContext,
                                  final NamingContext bridgeDomainContext) {
        super(lispInstanceIdentifier, vppApi, localMappingContext, remoteMappingContext);
        this.bridgeDomainContext = checkNotNull(bridgeDomainContext, "Bridge domain context cannot be null");
    }

    public static VniTableWriterFactory newInstance(
            @Nonnull final InstanceIdentifier<Lisp> lispInstanceIdentifier,
            @Nonnull final FutureJVppCore vppApi,
            @Nonnull final EidMappingContext localMappingContext,
            @Nonnull final EidMappingContext remoteMappingContext,
            @Nonnull final NamingContext bridgeDomainContext) {
        return new VniTableWriterFactory(lispInstanceIdentifier, vppApi, localMappingContext, remoteMappingContext,
                bridgeDomainContext);
    }

    @Override
    public void init(final ModifiableWriterRegistryBuilder registry) {
        final InstanceIdentifier<VniTable> vniTableId =
                lispInstanceIdentifier.child(LispFeatureData.class).child(EidTable.class).child(VniTable.class);

        final InstanceIdentifier<VrfSubtable> vrfSubtableId = vniTableId.child(VrfSubtable.class);
        final InstanceIdentifier<BridgeDomainSubtable> bridgeDomainSubtableId =
                vniTableId.child(BridgeDomainSubtable.class);

        registry.add(new GenericListWriter<>(vniTableId, new VniTableCustomizer(vppApi)));

        registry.add(new GenericWriter<>(vrfSubtableId, new VrfSubtableCustomizer(vppApi)));
        registry.add(new GenericWriter<>(bridgeDomainSubtableId,
                new BridgeDomainSubtableCustomizer(vppApi, bridgeDomainContext)));

        //VniTable - > VrfSubtable -> LocalMappings - > LocalMapping
        final InstanceIdentifier<LocalMapping> localMappingSubtreeId = InstanceIdentifier.create(LocalMapping.class);
        registry.subtreeAdd(ImmutableSet.of(localMappingSubtreeId
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.Eid.class)),
                new GenericListWriter<>(
                        vrfSubtableId.child(LocalMappings.class).child(LocalMapping.class),
                        new LocalMappingCustomizer(vppApi, localMappingContext)));
        //VniTable - > BridgeDomainSubtable -> LocalMappings - > LocalMapping
        registry.subtreeAdd(ImmutableSet.of(localMappingSubtreeId
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.Eid.class)),
                new GenericListWriter<>(bridgeDomainSubtableId.child(LocalMappings.class)
                        .child(LocalMapping.class),
                        new LocalMappingCustomizer(vppApi, localMappingContext)));

        //VniTable - > VrfSubtable -> RemoteMappings - > RemoteMapping
        final InstanceIdentifier<RemoteMapping> remoteMappingSubtreeId = InstanceIdentifier.create(RemoteMapping.class);
        registry.subtreeAdd(ImmutableSet.of(remoteMappingSubtreeId
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.Eid.class),
                remoteMappingSubtreeId.child(Rlocs.class),
                remoteMappingSubtreeId.child(Rlocs.class).child(Locator.class),
                remoteMappingSubtreeId.child(MapReply.class)),
                new GenericListWriter<>(
                        vrfSubtableId.child(RemoteMappings.class).child(RemoteMapping.class),
                        new RemoteMappingCustomizer(vppApi, remoteMappingContext)));
        //VniTable - > BridgeDomainSubtable -> RemoteMappings - > RemoteMapping
        registry.subtreeAdd(ImmutableSet.of(remoteMappingSubtreeId
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.Eid.class),
                remoteMappingSubtreeId.child(Rlocs.class),
                remoteMappingSubtreeId.child(Rlocs.class).child(Locator.class),
                remoteMappingSubtreeId.child(MapReply.class)),
                new GenericListWriter<>(bridgeDomainSubtableId.child(RemoteMappings.class)
                        .child(RemoteMapping.class),
                        new RemoteMappingCustomizer(vppApi, remoteMappingContext)));

        //VniTable - > VrfSubtable -> RemoteMappings - > RemoteMapping - > Adjacencies - > Adjacency
        final InstanceIdentifier<Adjacency> adjacencySubtreeId = InstanceIdentifier.create(Adjacency.class);
        registry.subtreeAdd(ImmutableSet.of(adjacencySubtreeId
                        .child(LocalEid.class), adjacencySubtreeId.child(RemoteEid.class)),
                new GenericListWriter<>(
                        vrfSubtableId.child(RemoteMappings.class).child(RemoteMapping.class)
                                .child(Adjacencies.class).child(Adjacency.class),
                        new AdjacencyCustomizer(vppApi, localMappingContext, remoteMappingContext)));
        //VniTable - > BridgeDomainSubtable -> RemoteMappings - > RemoteMapping - > Adjacencies - > Adjacency
        registry.subtreeAdd(ImmutableSet.of(adjacencySubtreeId
                        .child(LocalEid.class), adjacencySubtreeId.child(RemoteEid.class)),
                new GenericListWriter<>(
                        bridgeDomainSubtableId.child(RemoteMappings.class)
                                .child(RemoteMapping.class)
                                .child(Adjacencies.class).child(Adjacency.class),
                        new AdjacencyCustomizer(vppApi, localMappingContext, remoteMappingContext)));
    }
}
