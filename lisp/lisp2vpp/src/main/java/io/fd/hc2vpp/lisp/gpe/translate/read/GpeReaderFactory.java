/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.lisp.gpe.translate.read;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.lisp.gpe.GpeModule;
import io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeLocatorPairMappingContext;
import io.fd.hc2vpp.lisp.gpe.translate.service.GpeStateCheckService;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.impl.read.GenericInitReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.GpeState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.GpeStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.GpeEntryTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.GpeEntryTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocalEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocatorPairs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.locator.pair.LocatorPair;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GpeReaderFactory implements ReaderFactory {

    private static final InstanceIdentifier<GpeState>
            GPE_STATE_ID = InstanceIdentifier.create(GpeState.class);
    private static final InstanceIdentifier<GpeFeatureData>
            GPE_FEATURE_ID = GPE_STATE_ID.child(GpeFeatureData.class);
    private static final InstanceIdentifier<GpeEntryTable>
            GPE_ENTRY_TABLE_ID = GPE_FEATURE_ID.child(GpeEntryTable.class);
    private static final InstanceIdentifier<GpeEntry>
            GPE_ENTRY_ID = GPE_ENTRY_TABLE_ID.child(GpeEntry.class);

    @Inject
    private FutureJVppCore api;

    @Inject
    private GpeStateCheckService gpeStateCheckService;

    @Inject
    @Named(GpeModule.GPE_ENTRY_MAPPING_CTX)
    private NamingContext gpeEntryMappingContext;

    @Inject
    @Named(GpeModule.GPE_TO_LOCATOR_PAIR_CTX)
    private GpeLocatorPairMappingContext gpeLocatorPairMappingContext;

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        registry.addStructuralReader(GPE_STATE_ID, GpeStateBuilder.class);
        registry.add(new GenericInitReader<>(GPE_FEATURE_ID, new GpeFeatureCustomizer(api)));
        registry.addStructuralReader(GPE_ENTRY_TABLE_ID, GpeEntryTableBuilder.class);

        final InstanceIdentifier<GpeEntry> entrySubtreeId = InstanceIdentifier.create(GpeEntry.class);
        registry.subtreeAdd(ImmutableSet.of(
                entrySubtreeId.child(LocalEid.class),
                entrySubtreeId.child(RemoteEid.class),
                entrySubtreeId.child(LocatorPairs.class),
                entrySubtreeId.child(LocatorPairs.class).child(LocatorPair.class)),
                new GenericInitListReader<>(GPE_ENTRY_ID,
                        new GpeForwardEntryCustomizer(api, gpeStateCheckService, gpeEntryMappingContext,
                                gpeLocatorPairMappingContext)));
    }
}
