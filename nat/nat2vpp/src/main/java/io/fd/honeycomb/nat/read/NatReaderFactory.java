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

package io.fd.honeycomb.nat.read;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.fd.honeycomb.nat.util.MappingEntryContext;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.snat.dto.SnatStaticMappingDetailsReplyDump;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.NatState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.NatStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.mapping.entry.ExternalSrcPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.mapping.entry.InternalSrcPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.NatInstances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.NatInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.MappingTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.MappingTableBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.mapping.table.MappingEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NatReaderFactory implements ReaderFactory {

    private static final InstanceIdentifier<NatState> NAT_OPER_ID = InstanceIdentifier.create(NatState.class);
    private static final InstanceIdentifier<NatInstances> NAT_INSTANCES_ID = NAT_OPER_ID.child(NatInstances.class);
    private static final InstanceIdentifier<NatInstance> NAT_INSTANCE_ID = NAT_INSTANCES_ID.child(NatInstance.class);
    private static final InstanceIdentifier<MappingTable> MAP_TABLE_ID = NAT_INSTANCE_ID.child(MappingTable.class);
    private static final InstanceIdentifier<MappingEntry> MAP_ENTRY_ID = MAP_TABLE_ID.child(MappingEntry.class);

    private final MappingEntryContext mappingEntryContext;
    private final DumpCacheManager<SnatStaticMappingDetailsReplyDump, Void> dumpCacheManager;

    @Inject
    public NatReaderFactory(final FutureJVppSnatFacade jvppSnat,
                            final MappingEntryContext mappingEntryContext) {
        this.mappingEntryContext = mappingEntryContext;
        this.dumpCacheManager = new DumpCacheManager.DumpCacheManagerBuilder<SnatStaticMappingDetailsReplyDump, Void>()
                .withExecutor(new MappingEntryCustomizer.MappingEntryDumpExecutor(jvppSnat))
                .build();
    }

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        registry.addStructuralReader(NAT_OPER_ID, NatStateBuilder.class);
        registry.addStructuralReader(NAT_INSTANCES_ID, NatInstancesBuilder.class);
        registry.add(new GenericListReader<>(NAT_INSTANCE_ID, new NatInstanceCustomizer(dumpCacheManager)));
        registry.addStructuralReader(MAP_TABLE_ID, MappingTableBuilder.class);
        registry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(MappingEntry.class).child(ExternalSrcPort.class),
                InstanceIdentifier.create(MappingEntry.class).child(InternalSrcPort.class)),
                new GenericListReader<>(MAP_ENTRY_ID,
                        new MappingEntryCustomizer(dumpCacheManager, mappingEntryContext)));

        // TODO VPP-453 Implement address range read

    }
}
