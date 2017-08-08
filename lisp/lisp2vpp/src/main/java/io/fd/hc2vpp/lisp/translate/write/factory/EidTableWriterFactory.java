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

package io.fd.hc2vpp.lisp.translate.write.factory;

import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.lisp.translate.AbstractLispInfraFactoryBase;
import io.fd.hc2vpp.lisp.translate.write.AdjacencyCustomizer;
import io.fd.hc2vpp.lisp.translate.write.BridgeDomainSubtableCustomizer;
import io.fd.hc2vpp.lisp.translate.write.LocalMappingCustomizer;
import io.fd.hc2vpp.lisp.translate.write.RemoteMappingCustomizer;
import io.fd.hc2vpp.lisp.translate.write.VniTableCustomizer;
import io.fd.hc2vpp.lisp.translate.write.VrfSubtableCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.adjacencies.grouping.Adjacencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.adjacencies.grouping.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.adjacencies.grouping.adjacencies.adjacency.LocalEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.adjacencies.grouping.adjacencies.adjacency.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.negative.mapping.MapReply;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.Rlocs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.Locator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.hmac.key.grouping.HmacKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Factory for producing writers for {@code EidTable}
 */
public final class EidTableWriterFactory extends AbstractLispInfraFactoryBase implements WriterFactory {

    static final InstanceIdentifier<VniTable> VNI_TABLE_ID =
            LISP_CONFIG_IDENTIFIER.child(LispFeatureData.class).child(EidTable.class).child(VniTable.class);

    static final InstanceIdentifier<VrfSubtable> VRF_SUBTABLE_ID = VNI_TABLE_ID.child(VrfSubtable.class);

    static final InstanceIdentifier<BridgeDomainSubtable> BRIDGE_DOMAIN_SUBTABLE_ID =
            VNI_TABLE_ID.child(BridgeDomainSubtable.class);


    @Override
    public void init(final ModifiableWriterRegistryBuilder registry) {

        registry.add(new GenericListWriter<>(VNI_TABLE_ID, new VniTableCustomizer(vppApi, lispStateCheckService)));

        registry.add(new GenericWriter<>(VRF_SUBTABLE_ID, new VrfSubtableCustomizer(vppApi)));
        registry.add(new GenericWriter<>(BRIDGE_DOMAIN_SUBTABLE_ID,
                new BridgeDomainSubtableCustomizer(vppApi, bridgeDomainContext)));

        addLocalMappingSubtree(registry);
        addRemoteMappingSubtree(registry);
        addAdjacenciesSubtree(registry);
    }

    /**
     * VniTable - > VrfSubtable -> LocalMappings - > LocalMapping
     * VniTable - > BridgeDomainSubtable -> LocalMappings - > LocalMapping
     */
    private void addLocalMappingSubtree(final ModifiableWriterRegistryBuilder registry) {
        final InstanceIdentifier<LocalMapping> localMappingSubtreeId = InstanceIdentifier.create(LocalMapping.class);

        final ImmutableSet<InstanceIdentifier<?>> localMappingHandledChildren =
                ImmutableSet.of(localMappingSubtreeId.child(Eid.class), localMappingSubtreeId.child(HmacKey.class));
        registry.subtreeAdd(localMappingHandledChildren,
                new GenericListWriter<>(VRF_SUBTABLE_ID.child(LocalMappings.class).child(LocalMapping.class),
                        new LocalMappingCustomizer(vppApi, localMappingContext)));

        registry.subtreeAdd(localMappingHandledChildren,
                new GenericListWriter<>(BRIDGE_DOMAIN_SUBTABLE_ID.child(LocalMappings.class).child(LocalMapping.class),
                        new LocalMappingCustomizer(vppApi, localMappingContext)));
    }

    /**
     * VniTable - > VrfSubtable -> RemoteMappings - > RemoteMapping
     * VniTable - > BridgeDomainSubtable -> RemoteMappings - > RemoteMapping
     */
    private void addRemoteMappingSubtree(final ModifiableWriterRegistryBuilder registry) {
        final InstanceIdentifier<RemoteMapping> remoteMappingSubtreeId =
                InstanceIdentifier.create(RemoteMapping.class);
        final ImmutableSet<InstanceIdentifier<?>> remoteMappingHandledChildren =
                ImmutableSet.of(remoteMappingSubtreeId
                                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.remote.mappings.remote.mapping.Eid.class),
                        remoteMappingSubtreeId.child(Rlocs.class),
                        remoteMappingSubtreeId.child(Rlocs.class).child(Locator.class),
                        remoteMappingSubtreeId.child(MapReply.class));

        registry.subtreeAdd(remoteMappingHandledChildren, new GenericListWriter<>(
                VRF_SUBTABLE_ID.child(RemoteMappings.class).child(RemoteMapping.class),
                new RemoteMappingCustomizer(vppApi, remoteMappingContext)));

        registry.subtreeAdd(remoteMappingHandledChildren, new GenericListWriter<>(
                BRIDGE_DOMAIN_SUBTABLE_ID.child(RemoteMappings.class).child(RemoteMapping.class),
                new RemoteMappingCustomizer(vppApi, remoteMappingContext)));
    }

    /**
     * VniTable - > VrfSubtable -> RemoteMappings - > RemoteMapping - > Adjacencies - > Adjacency
     * VniTable - > BridgeDomainSubtable -> RemoteMappings - > RemoteMapping - > Adjacencies - > Adjacency
     */
    private void addAdjacenciesSubtree(final ModifiableWriterRegistryBuilder registry) {
        final InstanceIdentifier<Adjacency> adjacencySubtreeId = InstanceIdentifier.create(Adjacency.class);

        final ImmutableSet<InstanceIdentifier<?>> adjacencyHandledChildren = ImmutableSet.of(adjacencySubtreeId
                .child(LocalEid.class), adjacencySubtreeId.child(RemoteEid.class));

        registry.subtreeAdd(adjacencyHandledChildren, new GenericListWriter<>(
                VRF_SUBTABLE_ID.child(RemoteMappings.class).child(RemoteMapping.class)
                        .child(Adjacencies.class).child(Adjacency.class),
                new AdjacencyCustomizer(vppApi, localMappingContext, remoteMappingContext,
                        adjacenciesMappingContext)));

        registry.subtreeAdd(adjacencyHandledChildren, new GenericListWriter<>(
                BRIDGE_DOMAIN_SUBTABLE_ID.child(RemoteMappings.class)
                        .child(RemoteMapping.class)
                        .child(Adjacencies.class).child(Adjacency.class),
                new AdjacencyCustomizer(vppApi, localMappingContext, remoteMappingContext,
                        adjacenciesMappingContext)));
    }
}
