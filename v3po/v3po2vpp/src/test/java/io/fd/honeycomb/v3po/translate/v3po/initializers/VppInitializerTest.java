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

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppStateBuilder;

public class VppInitializerTest {

    @Mock
    private DataBroker bindingDataBroker;

    private VppInitializer vppInitializer;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        vppInitializer = new VppInitializer(bindingDataBroker);
    }

    @Test
    public void testConvert() throws Exception {
        final VppState operationalData = operationalData();
        final Vpp expectedConfigData = expectedConfigData();
        final Vpp configData = vppInitializer.convert(operationalData);
        assertEquals(expectedConfigData, configData);
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain
    operBd(String name, boolean learn, boolean unknownUnicastFlood, boolean arpTermination, boolean flood,
           boolean forward) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainBuilder
                bd =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainBuilder();
        bd.setName(name);
        bd.setKey(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainKey(
                        name));
        bd.setLearn(learn);
        bd.setUnknownUnicastFlood(unknownUnicastFlood);
        bd.setArpTermination(arpTermination);
        bd.setFlood(flood);
        bd.setForward(forward);
        return bd.build();
    }

    private VppState operationalData() {
        final VppStateBuilder builder = new VppStateBuilder();

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomainsBuilder
                bdBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomainsBuilder();
        bdBuilder.setBridgeDomain(Arrays.asList(
                operBd("b1", true, true, true, true, true),
                operBd("b2", false, false, false, false, false)
        ));
        builder.setBridgeDomains(bdBuilder.build());
        return builder.build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain
    configBd(String name, boolean learn, boolean unknownUnicastFlood, boolean arpTermination, boolean flood,
           boolean forward) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder
                bd =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder();
        bd.setName(name);
        bd.setKey(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey(
                        name));
        bd.setLearn(learn);
        bd.setUnknownUnicastFlood(unknownUnicastFlood);
        bd.setArpTermination(arpTermination);
        bd.setFlood(flood);
        bd.setForward(forward);
        return bd.build();
    }

    private Vpp expectedConfigData() {
        final VppBuilder builder = new VppBuilder();

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomainsBuilder
                bdBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomainsBuilder();
        bdBuilder.setBridgeDomain(Arrays.asList(
                configBd("b1", true, true, true, true, true),
                configBd("b2", false, false, false, false, false)
        ));
        builder.setBridgeDomains(bdBuilder.build());
        return builder.build();
    }
}