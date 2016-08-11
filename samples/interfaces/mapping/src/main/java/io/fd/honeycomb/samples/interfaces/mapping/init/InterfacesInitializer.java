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

package io.fd.honeycomb.samples.interfaces.mapping.init;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.data.init.AbstractDataTreeConverter;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.Interfaces;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.InterfacesState;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.interfaces.InterfaceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an initializer for interfaces plugin. Its main goal is to revers-engineer configuration data (config "true")
 * for interfaces model from operational data. In this case, we are trying to recreate Interfaces container from InterfacesState
 * container. Thanks to symmetrical nature of the model, it's pretty straightforward.
 *
 * This is very useful when the lower layer already contains some data that should be revers-engineer to config data
 * in honeycomb in order to get HC and lower layer to sync... It makes life of upper layers much easier
 *
 * However it's not always possible to perform this task so the initializers are optional for plugins
 */
public class InterfacesInitializer extends AbstractDataTreeConverter<InterfacesState, Interfaces> {

    private static final Logger LOG = LoggerFactory.getLogger(InterfacesInitializer.class);

    @Inject
    public InterfacesInitializer(@Named("honeycomb-initializer") final DataBroker bindingDataBroker) {
        super(bindingDataBroker,
                InstanceIdentifier.create(InterfacesState.class), InstanceIdentifier.create(Interfaces.class));
    }

    @Override
    protected Interfaces convert(final InterfacesState operationalData) {
        // Just convert operational data into config data
        // The operational data are queried from lower layer using readerCustomizers from this plugin

        LOG.info("Initializing interfaces config data from: {}", operationalData);

        return new InterfacesBuilder()
                .setInterface(operationalData.getInterface().stream()
                        .map(oper -> new InterfaceBuilder()
                                .setMtu(oper.getMtu())
                                .setInterfaceId(oper.getInterfaceId())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
