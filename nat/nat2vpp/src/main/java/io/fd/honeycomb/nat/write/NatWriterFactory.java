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

package io.fd.honeycomb.nat.write;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.fd.honeycomb.nat.util.MappingEntryContext;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.NatConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.mapping.entry.ExternalSrcPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.mapping.entry.InternalSrcPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.NatInstances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.MappingTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPool;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Nat Writers registration.
 */
public final class NatWriterFactory implements WriterFactory {

    private static final InstanceIdentifier<NatConfig> NAT_CFG_ID = InstanceIdentifier.create(NatConfig.class);
    private static final InstanceIdentifier<NatInstance> NAT_INSTANCE_ID =
            NAT_CFG_ID.child(NatInstances.class).child(NatInstance.class);
    private static final InstanceIdentifier<MappingEntry> MAP_ENTRY_ID =
            NAT_INSTANCE_ID.child(MappingTable.class).child(MappingEntry.class);

    private final FutureJVppSnatFacade jvppSnat;
    private final MappingEntryContext mappingEntryContext;

    @Inject
    public NatWriterFactory(final FutureJVppSnatFacade jvppSnat,
                            final MappingEntryContext mappingEntryContext) {
        this.jvppSnat = jvppSnat;
        this.mappingEntryContext = mappingEntryContext;
    }

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        // Nat-instance
        registry.add(new GenericListWriter<>(NAT_INSTANCE_ID, new NatInstaceCustomizer()));
        //  Mapping-entry
        registry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(MappingEntry.class).child(ExternalSrcPort.class),
                InstanceIdentifier.create(MappingEntry.class).child(InternalSrcPort.class)),
                new GenericListWriter<>(MAP_ENTRY_ID, new MappingEntryCustomizer(jvppSnat, mappingEntryContext)));

        // External address pool has to be executed before mapping entry. Because adding mapping entries requires to
        //  already have an IP range predefined ... in some cases
        registry.addBefore(new GenericListWriter<>(NAT_INSTANCE_ID.child(ExternalIpAddressPool.class),
                new ExternalIpPoolCustomizer(jvppSnat)),
                MAP_ENTRY_ID);
    }
}
