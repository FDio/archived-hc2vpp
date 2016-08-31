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
import io.fd.honeycomb.lisp.translate.read.LocalMappingCustomizer;
import io.fd.honeycomb.lisp.translate.read.RemoteMappingCustomizer;
import io.fd.honeycomb.lisp.translate.read.VniTableCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.LispState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.Adjacencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.AdjacenciesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.LocalMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.RemoteMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.adjacency.LocalEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.adjacency.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.RemoteMapping;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.future.FutureJVppCore;


/**
 * Factory that produces {@code Reader} for {@code EidTable}<br> with all its inhired child readers
 */
final class EidTableReaderFactory extends AbstractLispReaderFactoryBase implements ReaderFactory {

    private EidTableReaderFactory(final InstanceIdentifier<LispState> lispStateId,
                                  final FutureJVppCore vppApi,
                                  final NamingContext interfaceContext,
                                  final NamingContext locatorSetContext,
                                  final EidMappingContext localMappingContext,
                                  final EidMappingContext remoteMappingContext) {
        super(lispStateId, vppApi,interfaceContext, locatorSetContext, localMappingContext, remoteMappingContext);
    }

    public static EidTableReaderFactory newInstance(@Nonnull final InstanceIdentifier<LispState> lispStateId,
                                                    @Nonnull final FutureJVppCore vppApi,
                                                    @Nonnull final NamingContext interfaceContext,
                                                    @Nonnull final NamingContext locatorSetContext,
                                                    @Nonnull final EidMappingContext localMappingContext,
                                                    @Nonnull final EidMappingContext remoteMappingContext) {
        return new EidTableReaderFactory(lispStateId, vppApi, interfaceContext, locatorSetContext, localMappingContext,
                remoteMappingContext);
    }

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        InstanceIdentifier<EidTable> eidTableInstanceIdentifier = lispStateId.child(EidTable.class);
        InstanceIdentifier<VniTable> vniTableInstanceIdentifier = eidTableInstanceIdentifier.child(VniTable.class);
        InstanceIdentifier<LocalMappings> localMappingsInstanceIdentifier =
                vniTableInstanceIdentifier.child(LocalMappings.class);
        InstanceIdentifier<RemoteMappings> remoteMappingsInstanceIdentifier =
                vniTableInstanceIdentifier.child(RemoteMappings.class);
        InstanceIdentifier<Adjacencies> adjacenciesInstanceIdentifier =
                vniTableInstanceIdentifier.child(Adjacencies.class);

        registry.addStructuralReader(eidTableInstanceIdentifier, EidTableBuilder.class);
        registry.add(new GenericListReader<>(vniTableInstanceIdentifier, new VniTableCustomizer(vppApi)));

        registry.addStructuralReader(localMappingsInstanceIdentifier, LocalMappingsBuilder.class);

        final InstanceIdentifier<LocalMapping> localMappingSubtreeId = InstanceIdentifier.create(LocalMapping.class);
        registry.subtreeAdd(ImmutableSet.of(localMappingSubtreeId
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.local.mapping.Eid.class)),
                new GenericListReader<>(localMappingsInstanceIdentifier.child(LocalMapping.class),
                        new LocalMappingCustomizer(vppApi, locatorSetContext, localMappingContext)));

        registry.addStructuralReader(remoteMappingsInstanceIdentifier, RemoteMappingsBuilder.class);

        final InstanceIdentifier<RemoteMapping> remoteMappingSubtreeId = InstanceIdentifier.create(RemoteMapping.class);
        registry.subtreeAdd(ImmutableSet.of(remoteMappingSubtreeId
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.Eid.class)),
                new GenericListReader<>(remoteMappingsInstanceIdentifier.child(RemoteMapping.class),
                        new RemoteMappingCustomizer(vppApi, remoteMappingContext)));

        registry.addStructuralReader(adjacenciesInstanceIdentifier, AdjacenciesBuilder.class);

        final InstanceIdentifier<Adjacency> adjacencySubtreeId = InstanceIdentifier.create(Adjacency.class);
        registry.subtreeAdd(
                ImmutableSet.of(adjacencySubtreeId.child(LocalEid.class), adjacencySubtreeId.child(RemoteEid.class)),
                new GenericListReader<>(adjacenciesInstanceIdentifier.child(Adjacency.class),
                        new AdjacencyCustomizer(vppApi)));
    }
}
