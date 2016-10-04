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

package io.fd.honeycomb.lisp.translate.read.factory;


import com.google.common.collect.ImmutableSet;
import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.translate.read.AdjacencyCustomizer;
import io.fd.honeycomb.lisp.translate.read.BridgeDomainSubtableCustomizer;
import io.fd.honeycomb.lisp.translate.read.LocalMappingCustomizer;
import io.fd.honeycomb.lisp.translate.read.RemoteMappingCustomizer;
import io.fd.honeycomb.lisp.translate.read.VniTableCustomizer;
import io.fd.honeycomb.lisp.translate.read.VrfSubtableCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.LispState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.adjacencies.grouping.Adjacencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.adjacencies.grouping.AdjacenciesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.adjacencies.grouping.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.adjacencies.grouping.adjacencies.adjacency.LocalEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.adjacencies.grouping.adjacencies.adjacency.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.LocalMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.RemoteMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.negative.mapping.MapReply;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.Rlocs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.Locator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Factory that produces {@code Reader} for {@code EidTable}<br> with all its inhired child readers
 */
final class EidTableReaderFactory extends AbstractLispReaderFactoryBase implements ReaderFactory {

    private EidTableReaderFactory(final InstanceIdentifier<LispState> lispStateId,
                                  final FutureJVppCore vppApi,
                                  final NamingContext interfaceContext,
                                  final NamingContext locatorSetContext,
                                  final NamingContext bridgeDomainContext,
                                  final EidMappingContext localMappingContext,
                                  final EidMappingContext remoteMappingContext) {
        super(lispStateId, vppApi, interfaceContext, locatorSetContext, bridgeDomainContext, localMappingContext,
                remoteMappingContext);
    }

    public static EidTableReaderFactory newInstance(@Nonnull final InstanceIdentifier<LispState> lispStateId,
                                                    @Nonnull final FutureJVppCore vppApi,
                                                    @Nonnull final NamingContext interfaceContext,
                                                    @Nonnull final NamingContext locatorSetContext,
                                                    @Nonnull final NamingContext bridgeDomainContext,
                                                    @Nonnull final EidMappingContext localMappingContext,
                                                    @Nonnull final EidMappingContext remoteMappingContext) {
        return new EidTableReaderFactory(lispStateId, vppApi, interfaceContext, locatorSetContext, bridgeDomainContext,
                localMappingContext,
                remoteMappingContext);
    }

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        final InstanceIdentifier<EidTable> eidTableInstanceIdentifier =
                lispStateId.child(LispFeatureData.class).child(EidTable.class);
        final InstanceIdentifier<VniTable> vniTableInstanceIdentifier =
                eidTableInstanceIdentifier.child(VniTable.class);
        final InstanceIdentifier<VrfSubtable> vrfSubtable = vniTableInstanceIdentifier.child(VrfSubtable.class);
        final InstanceIdentifier<BridgeDomainSubtable> bridgeDomainSubtable =
                vniTableInstanceIdentifier.child(BridgeDomainSubtable.class);

        final InstanceIdentifier<LocalMappings> vrfTableLocalMappingsInstanceIdentifier =
                vrfSubtable.child(LocalMappings.class);
        final InstanceIdentifier<RemoteMappings> vrfTableRemoteMappingsInstanceIdentifier =
                vrfSubtable.child(RemoteMappings.class);
        final InstanceIdentifier<Adjacencies> vrfTableAdjacenciesInstanceIdentifier =
                vrfSubtable.child(RemoteMappings.class).child(RemoteMapping.class).child(Adjacencies.class);

        final InstanceIdentifier<LocalMappings> bridgeDomainLocalMappingsInstanceIdentifier =
                bridgeDomainSubtable.child(LocalMappings.class);
        final InstanceIdentifier<RemoteMappings> bridgeDomainRemoteMappingsInstanceIdentifier =
                bridgeDomainSubtable.child(RemoteMappings.class);
        final InstanceIdentifier<Adjacencies> bridgeDomainAdjacenciesInstanceIdentifier =
                bridgeDomainSubtable.child(RemoteMappings.class).child(RemoteMapping.class).child(Adjacencies.class);

        //EidTable
        registry.addStructuralReader(eidTableInstanceIdentifier, EidTableBuilder.class);
        //EidTable -> VniTable
        registry.add(new GenericListReader<>(vniTableInstanceIdentifier, new VniTableCustomizer(vppApi)));

        //EidTable -> VniTable -> VrfSubtable
        registry.add(new GenericReader<>(vrfSubtable, new VrfSubtableCustomizer(vppApi)));

        //EidTable -> VniTable -> BridgeDomainSubtable
        registry.add(new GenericReader<>(bridgeDomainSubtable,
                new BridgeDomainSubtableCustomizer(vppApi, bridgeDomainContext)));

        //EidTable -> VniTable -> VrfSubtable -> LocalMappings
        registry.addStructuralReader(vrfTableLocalMappingsInstanceIdentifier, LocalMappingsBuilder.class);
        //EidTable -> VniTable -> BridgeDomainSubtable -> LocalMappings
        registry.addStructuralReader(bridgeDomainLocalMappingsInstanceIdentifier, LocalMappingsBuilder.class);

        final InstanceIdentifier<LocalMapping> localMappingSubtreeId = InstanceIdentifier.create(LocalMapping.class);
        //EidTable -> VniTable -> VrfSubtable -> LocalMappings -> LocalMapping
        registry.subtreeAdd(ImmutableSet.of(localMappingSubtreeId
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.local.mappings.local.mapping.Eid.class)),
                new GenericListReader<>(vrfTableLocalMappingsInstanceIdentifier.child(LocalMapping.class),
                        new LocalMappingCustomizer(vppApi, locatorSetContext, localMappingContext)));

        //EidTable -> VniTable -> BridgeDomainSubtable -> LocalMappings -> LocalMapping
        registry.subtreeAdd(ImmutableSet.of(localMappingSubtreeId
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.local.mappings.local.mapping.Eid.class)),
                new GenericListReader<>(bridgeDomainLocalMappingsInstanceIdentifier.child(LocalMapping.class),
                        new LocalMappingCustomizer(vppApi, locatorSetContext, localMappingContext)));

        //EidTable -> VniTable -> VrfSubtable -> RemoteMappings
        registry.addStructuralReader(vrfTableRemoteMappingsInstanceIdentifier, RemoteMappingsBuilder.class);
        registry.addStructuralReader(bridgeDomainRemoteMappingsInstanceIdentifier, RemoteMappingsBuilder.class);

        final InstanceIdentifier<RemoteMapping> remoteMappingSubtreeId = InstanceIdentifier.create(RemoteMapping.class);
        registry.subtreeAdd(ImmutableSet.of(remoteMappingSubtreeId
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.remote.mappings.remote.mapping.Eid.class),
                remoteMappingSubtreeId.child(Rlocs.class),
                remoteMappingSubtreeId.child(Rlocs.class).child(Locator.class),
                remoteMappingSubtreeId.child(MapReply.class)),
                new GenericListReader<>(vrfTableRemoteMappingsInstanceIdentifier.child(RemoteMapping.class),
                        new RemoteMappingCustomizer(vppApi, remoteMappingContext)));
        registry.subtreeAdd(ImmutableSet.of(remoteMappingSubtreeId
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.remote.mappings.remote.mapping.Eid.class),
                remoteMappingSubtreeId.child(Rlocs.class),
                remoteMappingSubtreeId.child(Rlocs.class).child(Locator.class),
                remoteMappingSubtreeId.child(MapReply.class)),
                new GenericListReader<>(bridgeDomainRemoteMappingsInstanceIdentifier.child(RemoteMapping.class),
                        new RemoteMappingCustomizer(vppApi, remoteMappingContext)));

        registry.addStructuralReader(vrfTableAdjacenciesInstanceIdentifier, AdjacenciesBuilder.class);
        registry.addStructuralReader(bridgeDomainAdjacenciesInstanceIdentifier, AdjacenciesBuilder.class);

        final InstanceIdentifier<Adjacency> adjacencySubtreeId = InstanceIdentifier.create(Adjacency.class);
        registry.subtreeAdd(
                ImmutableSet.of(adjacencySubtreeId.child(LocalEid.class), adjacencySubtreeId.child(RemoteEid.class)),
                new GenericListReader<>(vrfTableAdjacenciesInstanceIdentifier.child(Adjacency.class),
                        new AdjacencyCustomizer(vppApi)));
        registry.subtreeAdd(
                ImmutableSet.of(adjacencySubtreeId.child(LocalEid.class), adjacencySubtreeId.child(RemoteEid.class)),
                new GenericListReader<>(bridgeDomainAdjacenciesInstanceIdentifier.child(Adjacency.class),
                        new AdjacencyCustomizer(vppApi)));
    }
}
