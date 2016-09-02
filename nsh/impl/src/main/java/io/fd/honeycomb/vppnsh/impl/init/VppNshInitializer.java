/*
 * Copyright (c) 2016 Intel and/or its affiliates.
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

package io.fd.honeycomb.vppnsh.impl.init;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.data.init.AbstractDataTreeConverter;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNsh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNshBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNshState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.NshEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.NshEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.NshMaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.NshMapsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.entries.NshEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.entries.NshEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMapBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an initializer for VppNsh plugin. Its main goal is to revers-engineer configuration data (config "true")
 * for Nsh model from operational data. In this case, we are trying to recreate Nsh container from NshsState
 * container. Thanks to symmetrical nature of the model, it's pretty straightforward.
 *
 * This is very useful when the lower layer already contains some data that should be revers-engineer to config data
 * in honeycomb in order to get HC and lower layer to sync... It makes life of upper layers much easier
 *
 * However it's not always possible to perform this task so the initializers are optional for plugins
 */
public class VppNshInitializer extends AbstractDataTreeConverter<VppNshState, VppNsh> {

    private static final Logger LOG = LoggerFactory.getLogger(VppNshInitializer.class);
    private static final InstanceIdentifier<VppNshState> OPER_ID = InstanceIdentifier.create(VppNshState.class);
    private static final InstanceIdentifier<VppNsh> CFG_ID = InstanceIdentifier.create(VppNsh.class);

    @Inject
    public VppNshInitializer(@Named("honeycomb-initializer") final DataBroker bindingDataBroker) {
        super(bindingDataBroker, OPER_ID, CFG_ID);
    }

    @Override
    protected VppNsh convert(final VppNshState operationalData) {
        // Just convert operational data into config data
        // The operational data are queried from lower layer using readerCustomizers from this plugin

        LOG.info("Initializing VppNsh config data from: {}", operationalData);

        VppNshBuilder vppNshBuilder = new VppNshBuilder();

        NshEntriesBuilder nshEntriesBuilder = new NshEntriesBuilder()
                .setNshEntry(operationalData.getNshEntries().getNshEntry().stream()
                        .map(oper -> new NshEntryBuilder(oper).setName(oper.getName()).build())
                        .collect(Collectors.toList()));
        vppNshBuilder.setNshEntries(nshEntriesBuilder.build());

        NshMapsBuilder nshMapsBuilder = new NshMapsBuilder()
                .setNshMap(operationalData.getNshMaps().getNshMap().stream()
                        .map(oper -> new NshMapBuilder(oper).setName(oper.getName()).build())
                        .collect(Collectors.toList()));
        vppNshBuilder.setNshMaps(nshMapsBuilder.build());

        return vppNshBuilder.build();
    }
}
