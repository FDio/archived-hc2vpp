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

package io.fd.hc2vpp.nat.write;

import static io.fd.hc2vpp.nat.NatIds.ADDRESS_POOL_ID;
import static io.fd.hc2vpp.nat.NatIds.MAPPING_ENTRY_ID;
import static io.fd.hc2vpp.nat.NatIds.NAT64_PREFIXES_ID;
import static io.fd.hc2vpp.nat.NatIds.NAT_INSTANCE_ID;
import static io.fd.hc2vpp.nat.NatIds.POLICY_ID;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.fd.hc2vpp.nat.util.MappingEntryContext;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nat.rev180510.ExternalIpAddressPoolAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.mapping.entry.ExternalSrcPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.mapping.entry.InternalSrcPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.Nat64Prefixes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.nat64.prefixes.DestinationIpv4Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Nat Writers registration.
 */
public final class NatWriterFactory implements WriterFactory {

    private final FutureJVppNatFacade jvppNat;
    private final MappingEntryContext mappingEntryContext;

    @Inject
    public NatWriterFactory(final FutureJVppNatFacade jvppNat,
                            final MappingEntryContext mappingEntryContext) {
        this.jvppNat = jvppNat;
        this.mappingEntryContext = mappingEntryContext;
    }

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        // +-- nat
        //    +-- instances/instance
        registry.add(new GenericListWriter<>(NAT_INSTANCE_ID, new NatInstaceCustomizer(), new NatInstanceValidator()));
        //       +-- mapping-table/mapping-entry
        registry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(MappingEntry.class).child(ExternalSrcPort.class),
            InstanceIdentifier.create(MappingEntry.class).child(InternalSrcPort.class)),
            new GenericListWriter<>(MAPPING_ENTRY_ID,
                new MappingEntryCustomizer(jvppNat, mappingEntryContext),
                new MappingEntryValidator()));

        //       +-- policy
        registry.add(new GenericListWriter<>(POLICY_ID, new PolicyCustomizer(), new PolicyValidator()));

        //          +-- external-ip-address-pool
        registry.subtreeAddBefore(
            // External address pool has to be executed before mapping entry. Because adding mapping entries
            // requires to already have an IP range predefined ... in some cases
            Sets.newHashSet(InstanceIdentifier.create(ExternalIpAddressPool.class)
                .augmentation(ExternalIpAddressPoolAugmentation.class)),
            new GenericListWriter<>(ADDRESS_POOL_ID,
                new ExternalIpPoolCustomizer(jvppNat),
                new ExternalIpPoolValidator()),
            MAPPING_ENTRY_ID);

        //          +-- nat64-prefixes
        registry.subtreeAdd(
            Sets.newHashSet(InstanceIdentifier.create(Nat64Prefixes.class).child(DestinationIpv4Prefix.class)),
            new GenericListWriter<>(NAT64_PREFIXES_ID,
                new Nat64PrefixesCustomizer(jvppNat),
                new Nat64PrefixesValidator()));
    }
}
