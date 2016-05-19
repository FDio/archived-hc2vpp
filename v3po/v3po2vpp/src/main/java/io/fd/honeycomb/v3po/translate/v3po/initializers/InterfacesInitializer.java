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

package io.fd.honeycomb.v3po.translate.v3po.initializers;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.vpp.data.init.AbstractDataTreeConverter;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes ietf-interfaces config data based on operational state
 */
public class InterfacesInitializer extends AbstractDataTreeConverter<InterfacesState, Interfaces> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfacesInitializer.class);

    public InterfacesInitializer(@Nonnull final DataBroker bindingDataBroker) {
        super(bindingDataBroker, InstanceIdentifier.create(InterfacesState.class),
                InstanceIdentifier.create(Interfaces.class));
    }

    @Override
    protected Interfaces convert(final InterfacesState operationalData) {
        LOG.debug("InterfacesInitializer.convert()");
        InterfacesBuilder interfacesBuilder = new InterfacesBuilder();
        interfacesBuilder.setInterface(Lists.transform(operationalData.getInterface(), CONVERT_INTERFACE));
        return interfacesBuilder.build();
    }

    private static final Function<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface, Interface>
            CONVERT_INTERFACE =
            new Function<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface, Interface>() {
                @Nullable
                @Override
                public Interface apply(
                        @Nullable final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface input) {

                    InterfaceBuilder builder = new InterfaceBuilder();
                    builder.setKey(new InterfaceKey(input.getKey().getName()));
                    builder.setName(input.getName());
                    // builder.setDescription(); not present in interfaces-state
                    builder.setType(input.getType());
                    builder.setEnabled(AdminStatus.Up.equals(input.getAdminStatus()));
                    // builder.setLinkUpDownTrapEnable(); not present in interfaces-state
                    return builder.build();
                }
            };
}
