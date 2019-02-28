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

package io.fd.hc2vpp.nat.read;

import static io.fd.hc2vpp.nat.NatIds.ADDRESS_POOL_ID;
import static io.fd.hc2vpp.nat.NatIds.MAPPING_ENTRY_ID;
import static io.fd.hc2vpp.nat.NatIds.MAPPING_TABLE_ID;
import static io.fd.hc2vpp.nat.NatIds.NAT64_PREFIXES_ID;
import static io.fd.hc2vpp.nat.NatIds.NAT_ID;
import static io.fd.hc2vpp.nat.NatIds.NAT_INSTANCES_ID;
import static io.fd.hc2vpp.nat.NatIds.NAT_INSTANCE_ID;
import static io.fd.hc2vpp.nat.NatIds.POLICY_ID;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.fd.hc2vpp.nat.util.MappingEntryContext;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.jvpp.nat.dto.Nat44StaticMappingDetailsReplyDump;
import io.fd.jvpp.nat.dto.Nat64BibDetailsReplyDump;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.NatBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.mapping.entry.ExternalSrcPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.mapping.entry.InternalSrcPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.InstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.MappingTableBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.Nat64Prefixes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.nat64.prefixes.DestinationIpv4Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NatReaderFactory implements ReaderFactory {

    private final FutureJVppNatFacade jvppNat;
    private final MappingEntryContext mappingEntryContext;
    private final DumpCacheManager<Nat44StaticMappingDetailsReplyDump, Void> mapEntryNat44DumpMgr;
    private final DumpCacheManager<Nat64BibDetailsReplyDump, Void> mapEntryNat64DumpMgr;

    @Inject
    public NatReaderFactory(final FutureJVppNatFacade jvppNat,
                            final MappingEntryContext mappingEntryContext) {
        this.jvppNat = jvppNat;
        this.mappingEntryContext = mappingEntryContext;
        this.mapEntryNat44DumpMgr =
                new DumpCacheManager.DumpCacheManagerBuilder<Nat44StaticMappingDetailsReplyDump, Void>()
                        .withExecutor(new MappingEntryCustomizer.MappingEntryNat44DumpExecutor(jvppNat))
                        .acceptOnly(Nat44StaticMappingDetailsReplyDump.class)
                        .build();
        this.mapEntryNat64DumpMgr =
                new DumpCacheManager.DumpCacheManagerBuilder<Nat64BibDetailsReplyDump, Void>()
                        .withExecutor(new MappingEntryCustomizer.MappingEntryNat64DumpExecutor(jvppNat))
                        .acceptOnly(Nat64BibDetailsReplyDump.class)
                        .build();
    }

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        registry.addStructuralReader(NAT_ID, NatBuilder.class);
        registry.addStructuralReader(NAT_INSTANCES_ID, InstancesBuilder.class);
        registry.add(new GenericInitListReader<>(NAT_INSTANCE_ID,
                new NatInstanceCustomizer(mapEntryNat44DumpMgr, mapEntryNat64DumpMgr)));
        registry.addStructuralReader(MAPPING_TABLE_ID, MappingTableBuilder.class);
        registry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(MappingEntry.class).child(ExternalSrcPort.class),
                InstanceIdentifier.create(MappingEntry.class).child(InternalSrcPort.class)),
                new GenericInitListReader<>(MAPPING_ENTRY_ID,
                        new MappingEntryCustomizer(mapEntryNat44DumpMgr, mapEntryNat64DumpMgr, mappingEntryContext)));

        registry.add(new GenericInitListReader<>(POLICY_ID, new PolicyCustomizer()));
        registry.add(new GenericInitListReader<>(ADDRESS_POOL_ID, new ExternalIpPoolCustomizer(jvppNat)));

        // nat64-prefixes
        registry.subtreeAdd(
                Sets.newHashSet(InstanceIdentifier.create(Nat64Prefixes.class).child(DestinationIpv4Prefix.class)),
                new GenericListReader<>(NAT64_PREFIXES_ID, new Nat64PrefixesCustomizer(jvppNat)));
    }
}
