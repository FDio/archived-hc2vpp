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

import com.google.common.collect.ImmutableSet;
import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.translate.write.AdjacencyCustomizer;
import io.fd.honeycomb.lisp.translate.write.LocalMappingCustomizer;
import io.fd.honeycomb.lisp.translate.write.RemoteMappingCustomizer;
import io.fd.honeycomb.lisp.translate.write.VniTableCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.Adjacencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.adjacency.LocalEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.adjacency.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.locator.list.positive.mapping.Rlocs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.Locator;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;


/**
 * Factory for producing writers for {@code EidTable}
 */
final class VniTableWriterFactory extends AbstractLispWriterFactoryBase implements WriterFactory {

    private VniTableWriterFactory(final InstanceIdentifier<Lisp> lispInstanceIdentifier,
                                  final FutureJVppCore vppApi,
                                  final EidMappingContext localMappingContext,
                                  final EidMappingContext remoteMappingContext) {
        super(lispInstanceIdentifier, vppApi, localMappingContext, remoteMappingContext);
    }

    public static VniTableWriterFactory newInstance(
            @Nonnull final InstanceIdentifier<Lisp> lispInstanceIdentifier,
            @Nonnull final FutureJVppCore vppApi,
            @Nonnull final EidMappingContext localMappingContext,
            @Nonnull final EidMappingContext remoteMappingContext) {
        return new VniTableWriterFactory(lispInstanceIdentifier, vppApi, localMappingContext, remoteMappingContext);
    }

    @Override
    public void init(final ModifiableWriterRegistryBuilder registry) {
        final InstanceIdentifier<VniTable> vniTableId =
                lispInstanceIdentifier.child(EidTable.class).child(VniTable.class);

        registry.add(new GenericListWriter<>(vniTableId, new VniTableCustomizer(vppApi)));


        final InstanceIdentifier<LocalMapping> localMappingSubtreeId = InstanceIdentifier.create(LocalMapping.class);
        registry.subtreeAdd(ImmutableSet.of(localMappingSubtreeId
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.local.mapping.Eid.class)),
                new GenericListWriter<>(vniTableId.child(LocalMappings.class).child(LocalMapping.class),
                        new LocalMappingCustomizer(vppApi, localMappingContext)));

        final InstanceIdentifier<RemoteMapping> remoteMappingSubtreeId = InstanceIdentifier.create(RemoteMapping.class);
        registry.subtreeAdd(ImmutableSet.of(remoteMappingSubtreeId
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.Eid.class),
                remoteMappingSubtreeId.child(Rlocs.class),
                remoteMappingSubtreeId.child(Rlocs.class).child(Locator.class)),
                new GenericListWriter<>(vniTableId.child(RemoteMappings.class).child(RemoteMapping.class),
                        new RemoteMappingCustomizer(vppApi, remoteMappingContext)));

        final InstanceIdentifier<Adjacency> adjacencySubtreeId = InstanceIdentifier.create(Adjacency.class);
        registry.subtreeAdd(ImmutableSet.of(adjacencySubtreeId
                        .child(LocalEid.class), adjacencySubtreeId.child(RemoteEid.class)),
                new GenericListWriter<>(vniTableId.child(Adjacencies.class).child(Adjacency.class),
                        new AdjacencyCustomizer(vppApi)));
    }
}
