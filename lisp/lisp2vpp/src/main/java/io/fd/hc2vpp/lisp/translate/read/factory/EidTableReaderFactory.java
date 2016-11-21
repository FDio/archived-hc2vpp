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

package io.fd.hc2vpp.lisp.translate.read.factory;


import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.lisp.translate.AbstractLispInfraFactoryBase;
import io.fd.hc2vpp.lisp.translate.read.AdjacencyCustomizer;
import io.fd.hc2vpp.lisp.translate.read.BridgeDomainSubtableCustomizer;
import io.fd.hc2vpp.lisp.translate.read.LocalMappingCustomizer;
import io.fd.hc2vpp.lisp.translate.read.RemoteMappingCustomizer;
import io.fd.hc2vpp.lisp.translate.read.VniTableCustomizer;
import io.fd.hc2vpp.lisp.translate.read.VrfSubtableCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.Adjacencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.AdjacenciesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.adjacency.LocalEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.adjacency.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.LocalMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.RemoteMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.negative.mapping.MapReply;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.Rlocs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.Locator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.EidTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Factory that produces {@code Reader} for {@code EidTable}<br> with all its inhired child readers
 */
public final class EidTableReaderFactory extends AbstractLispInfraFactoryBase implements ReaderFactory {

    private static final InstanceIdentifier<EidTable> EID_TABLE_IID =
            LISP_OPERATIONAL_IDENTIFIER.child(LispFeatureData.class).child(EidTable.class);

    private static final InstanceIdentifier<VniTable> VNI_TABLE_IID = EID_TABLE_IID.child(VniTable.class);

    private static final InstanceIdentifier<VrfSubtable> VRF_SUBTABLE_IID = VNI_TABLE_IID.child(VrfSubtable.class);

    private static final InstanceIdentifier<BridgeDomainSubtable> BRIDGE_DOMAIN_SUBTABLE_IID =
            VNI_TABLE_IID.child(BridgeDomainSubtable.class);

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {

        //EidTable
        registry.addStructuralReader(EID_TABLE_IID, EidTableBuilder.class);
        //EidTable -> VniTable
        registry.add(new GenericListReader<>(VNI_TABLE_IID, new VniTableCustomizer(vppApi)));

        //EidTable -> VniTable -> VrfSubtable
        registry.add(new GenericReader<>(VRF_SUBTABLE_IID, new VrfSubtableCustomizer(vppApi)));

        //EidTable -> VniTable -> BridgeDomainSubtable
        registry.add(new GenericReader<>(BRIDGE_DOMAIN_SUBTABLE_IID,
                new BridgeDomainSubtableCustomizer(vppApi, bridgeDomainContext)));

        addLocalMappingSubtree(registry);
        addRemoteMappingSubtree(registry);
        addAdjacenciesSubtree(registry);
    }

    /**
     * EidTable -> VniTable -> VrfSubtable -> LocalMappings
     * EidTable -> VniTable -> BridgeDomainSubtable -> LocalMappings
     * EidTable -> VniTable -> VrfSubtable -> LocalMappings -> LocalMapping
     * EidTable -> VniTable -> BridgeDomainSubtable -> LocalMappings -> LocalMapping
     */
    private void addLocalMappingSubtree(final @Nonnull ModifiableReaderRegistryBuilder registry) {
        final InstanceIdentifier<LocalMappings> vrfTableLocalMappingsId = VRF_SUBTABLE_IID.child(LocalMappings.class);

        final InstanceIdentifier<LocalMappings> bridgeDomainLocalMappingsId =
                BRIDGE_DOMAIN_SUBTABLE_IID.child(LocalMappings.class);

        final InstanceIdentifier<LocalMapping> localMappingSubtreeId = InstanceIdentifier.create(LocalMapping.class);
        final ImmutableSet<InstanceIdentifier<?>> localMappingHandledChildren = ImmutableSet.of(localMappingSubtreeId
                .child(Eid.class));

        registry.addStructuralReader(vrfTableLocalMappingsId, LocalMappingsBuilder.class);
        registry.addStructuralReader(bridgeDomainLocalMappingsId, LocalMappingsBuilder.class);

        registry.subtreeAdd(localMappingHandledChildren,
                new GenericListReader<>(vrfTableLocalMappingsId.child(LocalMapping.class),
                        new LocalMappingCustomizer(vppApi, locatorSetContext, localMappingContext)));

        registry.subtreeAdd(localMappingHandledChildren,
                new GenericListReader<>(bridgeDomainLocalMappingsId.child(LocalMapping.class),
                        new LocalMappingCustomizer(vppApi, locatorSetContext, localMappingContext)));
    }

    /**
     * EidTable -> VniTable -> VrfSubtable -> RemoteMappings
     * EidTable -> VniTable -> BridgeDomainSubtable -> RemoteMappings
     */
    private void addRemoteMappingSubtree(final @Nonnull ModifiableReaderRegistryBuilder registry) {
        final InstanceIdentifier<RemoteMappings> vrfTableRemoteMappingsId =
                VRF_SUBTABLE_IID.child(RemoteMappings.class);
        final InstanceIdentifier<RemoteMappings> bridgeDomainRemoteMappingsId =
                BRIDGE_DOMAIN_SUBTABLE_IID.child(RemoteMappings.class);

        final InstanceIdentifier<RemoteMapping> remoteMappingSubtreeId = InstanceIdentifier.create(RemoteMapping.class);

        registry.addStructuralReader(vrfTableRemoteMappingsId, RemoteMappingsBuilder.class);
        registry.addStructuralReader(bridgeDomainRemoteMappingsId, RemoteMappingsBuilder.class);

        final ImmutableSet<InstanceIdentifier<?>> remoteMappingHandledChildren =
                ImmutableSet.of(remoteMappingSubtreeId
                                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.Eid.class),
                        remoteMappingSubtreeId.child(Rlocs.class),
                        remoteMappingSubtreeId.child(Rlocs.class).child(Locator.class),
                        remoteMappingSubtreeId.child(MapReply.class));

        registry.subtreeAdd(remoteMappingHandledChildren,
                new GenericListReader<>(vrfTableRemoteMappingsId.child(RemoteMapping.class),
                        new RemoteMappingCustomizer(vppApi, locatorSetContext, remoteMappingContext)));
        registry.subtreeAdd(remoteMappingHandledChildren,
                new GenericListReader<>(bridgeDomainRemoteMappingsId.child(RemoteMapping.class),
                        new RemoteMappingCustomizer(vppApi, locatorSetContext, remoteMappingContext)));
    }

    /**
     * EidTable -> VniTable -> VrfSubtable -> RemoteMappings -> Adjacencies
     */
    private void addAdjacenciesSubtree(final @Nonnull ModifiableReaderRegistryBuilder registry) {
        final InstanceIdentifier<Adjacencies> vrfTableAdjacenciesId =
                VRF_SUBTABLE_IID.child(RemoteMappings.class).child(RemoteMapping.class).child(Adjacencies.class);
        final InstanceIdentifier<Adjacencies> bridgeDomainAdjacenciesId =
                BRIDGE_DOMAIN_SUBTABLE_IID.child(RemoteMappings.class).child(RemoteMapping.class)
                        .child(Adjacencies.class);
        final InstanceIdentifier<Adjacency> adjacencySubtreeId = InstanceIdentifier.create(Adjacency.class);
        final ImmutableSet<InstanceIdentifier<?>> adjacencyHandledChildren = ImmutableSet
                .of(adjacencySubtreeId.child(LocalEid.class), adjacencySubtreeId.child(RemoteEid.class));

        registry.addStructuralReader(vrfTableAdjacenciesId, AdjacenciesBuilder.class);
        registry.addStructuralReader(bridgeDomainAdjacenciesId, AdjacenciesBuilder.class);

        registry.subtreeAdd(adjacencyHandledChildren,
                new GenericListReader<>(vrfTableAdjacenciesId.child(Adjacency.class),
                        new AdjacencyCustomizer(vppApi, localMappingContext, remoteMappingContext,
                                adjacenciesMappingContext)));
        registry.subtreeAdd(adjacencyHandledChildren,
                new GenericListReader<>(bridgeDomainAdjacenciesId.child(Adjacency.class),
                        new AdjacencyCustomizer(vppApi, localMappingContext, remoteMappingContext,
                                adjacenciesMappingContext)));
    }
}
