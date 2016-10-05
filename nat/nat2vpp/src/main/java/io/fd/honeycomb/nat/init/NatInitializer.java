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

package io.fd.honeycomb.nat.init;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.data.init.AbstractDataTreeConverter;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.NatConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.NatConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.NatState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.NatInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.MappingTableBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Initialize nat-config from nat-state.
 */
public final class NatInitializer extends AbstractDataTreeConverter<NatState, NatConfig> {

    @Inject
    public NatInitializer(@Named("honeycomb-initializer") @Nonnull final DataBroker bindingDataBroker) {
        super(bindingDataBroker, InstanceIdentifier.create(NatState.class), InstanceIdentifier.create(NatConfig.class));
    }

    @Override
    public NatConfig convert(final NatState operationalData) {
        return new NatConfigBuilder()
                .setNatInstances(new NatInstancesBuilder()
                        .setNatInstance(operationalData.getNatInstances().getNatInstance().stream()
                                .map(operNatInstance -> new NatInstanceBuilder()
                                        .setId(operNatInstance.getId())
                                        .setMappingTable(new MappingTableBuilder()
                                                .setMappingEntry(
                                                        operNatInstance.getMappingTable().getMappingEntry().stream()
                                                                .map(operEntry -> new MappingEntryBuilder(operEntry).build())
                                                                .collect(Collectors.toList()))
                                                .build())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .build();
        // TODO implement initialization for nat inbound/outbound NAT feature after VPP-459
    }
}
