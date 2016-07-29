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

package io.fd.honeycomb.translate.v3po.initializers;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;

public class InterfacesInitializerTest {

    @Mock
    private DataBroker bindingDataBroker;

    private InterfacesInitializer interfacesInitializer;

    @Before
    public void setUp() {
        initMocks(this);
        interfacesInitializer = new InterfacesInitializer(bindingDataBroker);
    }

    @Test
    public void testConvert() throws Exception {
        final InterfacesState operationalData = operationalData();
        final Interfaces expectedConfigData = expectedConfigData();

        final Interfaces configData = interfacesInitializer.convert(operationalData);
        assertEquals(expectedConfigData, configData);
    }

    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface operInterface(
            String name, Class<? extends InterfaceType> inerfaceType, Interface.AdminStatus adminStatus) {
        final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder
                iface =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder();
        iface.setKey(
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey(
                        name));
        iface.setName(name);
        iface.setType(inerfaceType);
        iface.setAdminStatus(adminStatus);
        return iface.build();
    }

    private InterfacesState operationalData() {
        final InterfacesStateBuilder builder = new InterfacesStateBuilder();
        builder.setInterface(
                Arrays.asList(
                        operInterface("eth1", EthernetCsmacd.class, Interface.AdminStatus.Up),
                        operInterface("eth2", EthernetCsmacd.class, Interface.AdminStatus.Down)
                ));
        return builder.build();
    }

    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface configInterface(
            String name, Class<? extends InterfaceType> inerfaceType, boolean isEnabled) {
        final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder
                iface =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder();
        iface.setKey(
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey(
                        name));
        iface.setName(name);
        iface.setType(inerfaceType);
        iface.setEnabled(isEnabled);
        return iface.build();
    }

    private Interfaces expectedConfigData() {
        final InterfacesBuilder builder = new InterfacesBuilder();

        builder.setInterface(
                Arrays.asList(
                        configInterface("eth1", EthernetCsmacd.class, true),
                        configInterface("eth2", EthernetCsmacd.class, false)
                ));
        return builder.build();
    }

}