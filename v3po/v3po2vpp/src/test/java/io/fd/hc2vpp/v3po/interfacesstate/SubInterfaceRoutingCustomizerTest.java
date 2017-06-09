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

package io.fd.hc2vpp.v3po.interfacesstate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.SwInterfaceGetTable;
import io.fd.vpp.jvpp.core.dto.SwInterfaceGetTableReply;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.interfaces.state._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.interfaces.state._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.sub._interface.routing.attributes.Routing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.sub._interface.routing.attributes.RoutingBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceRoutingCustomizerTest extends ReaderCustomizerTest<Routing, RoutingBuilder> implements
        ByteDataTranslator {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final int IF_ID = 1;
    private static final String SUBIF_NAME = "local0.4";
    private static final int SUBIF_ID = 4;
    private static final InstanceIdentifier<Routing> VALID_ID =
            InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
                    .augmentation(SubinterfaceStateAugmentation.class)
                    .child(SubInterfaces.class)
                    .child(SubInterface.class, new SubInterfaceKey((long) SUBIF_ID))
                    .child(Routing.class);
    private static final int IPV4_VRF = 4;
    private static final int IPV6_VRF = 6;
    private static final int NO_VRF = 0;

    private NamingContext interfacesContext;

    public SubInterfaceRoutingCustomizerTest() {
        super(Routing.class, SubInterfaceBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        interfacesContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME, IF_ID, IFC_CTX_NAME);
        defineMapping(mappingContext, SUBIF_NAME, SUBIF_ID, IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<Routing, RoutingBuilder> initCustomizer() {
        return new SubInterfaceRoutingCustomizer(api, interfacesContext);
    }

    @Test
    public void testReadAttributesAllDefined() throws ReadFailedException {
        when(api.swInterfaceGetTable(request(false, SUBIF_ID))).thenReturn(future(reply(IPV4_VRF)));
        when(api.swInterfaceGetTable(request(true, SUBIF_ID))).thenReturn(future(reply(IPV6_VRF)));
        final RoutingBuilder routingBuilder = new RoutingBuilder();
        getCustomizer().readCurrentAttributes(VALID_ID, routingBuilder, ctx);
        assertEquals(IPV4_VRF, routingBuilder.getIpv4VrfId().intValue());
        assertEquals(IPV6_VRF, routingBuilder.getIpv6VrfId().intValue());
    }

    @Test
    public void testReadAttributesOneDefined() throws ReadFailedException {
        when(api.swInterfaceGetTable(request(false, SUBIF_ID))).thenReturn(future(reply(IPV4_VRF)));
        when(api.swInterfaceGetTable(request(true, SUBIF_ID))).thenReturn(future(reply(NO_VRF)));
        final RoutingBuilder routingBuilder = new RoutingBuilder();
        getCustomizer().readCurrentAttributes(VALID_ID, routingBuilder, ctx);
        assertEquals(IPV4_VRF, routingBuilder.getIpv4VrfId().intValue());
        assertNull(routingBuilder.getIpv6VrfId());
    }

    @Test
    public void testReadAttributesNoDefined() throws ReadFailedException {
        when(api.swInterfaceGetTable(any())).thenReturn(future(reply(NO_VRF)));
        final RoutingBuilder routingBuilder = new RoutingBuilder();
        getCustomizer().readCurrentAttributes(VALID_ID, routingBuilder, ctx);
        assertNull(routingBuilder.getIpv4VrfId());
        assertNull(routingBuilder.getIpv6VrfId());
    }

    private SwInterfaceGetTable request(final boolean ipv6, final int index) {
        SwInterfaceGetTable request = new SwInterfaceGetTable();
        request.isIpv6 = booleanToByte(ipv6);
        request.swIfIndex = index;
        return request;
    }

    private SwInterfaceGetTableReply reply(final int vrf) {
        SwInterfaceGetTableReply reply = new SwInterfaceGetTableReply();
        reply.vrfId = vrf;
        return reply;
    }
}