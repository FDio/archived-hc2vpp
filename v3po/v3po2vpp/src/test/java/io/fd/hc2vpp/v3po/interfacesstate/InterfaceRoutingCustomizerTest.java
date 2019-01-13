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

package io.fd.hc2vpp.v3po.interfacesstate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.SwInterfaceGetTableReply;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.interfaces.state._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.interfaces.state._interface.RoutingBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceRoutingCustomizerTest extends ReaderCustomizerTest<Routing, RoutingBuilder> {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final int IF_ID = 1;
    private static final Long IP4_VRF_ID = 1L;
    private static final Long IP6_VRF_ID = 2L;

    private NamingContext interfacesContext;

    public InterfaceRoutingCustomizerTest() {
        super(Routing.class, VppInterfaceStateAugmentationBuilder.class);
    }

    @Override
    public void setUp() {
        interfacesContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME, IF_ID, IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<Routing, RoutingBuilder> initCustomizer() {
        return new InterfaceRoutingCustomizer(api, interfacesContext);
    }

    private InstanceIdentifier<Routing> getRoutingId(final String name) {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(name))
             .augmentation(VppInterfaceStateAugmentation.class).child(Routing.class);
    }

    @Test
    public void testRead() throws Exception {
        final RoutingBuilder builder = mock(RoutingBuilder.class);
        when(api.swInterfaceGetTable(any())).thenReturn(future(tableReply(IP4_VRF_ID))).thenReturn(future(tableReply(IP6_VRF_ID)));
        getCustomizer().readCurrentAttributes(getRoutingId(IF_NAME), builder, ctx);

        verify(builder).setIpv4VrfId(new VniReference(IP4_VRF_ID));
        verify(builder).setIpv6VrfId(new VniReference(IP6_VRF_ID));
    }

    @Test
    public void testReadRoutingNotDefined() throws Exception {
        final RoutingBuilder builder = mock(RoutingBuilder.class);
        final Long vrfId = 0L;
        when(api.swInterfaceGetTable(any())).thenReturn(future(tableReply(vrfId)));
        getCustomizer().readCurrentAttributes(getRoutingId(IF_NAME), builder, ctx);
        verifyZeroInteractions(builder);
    }

    private SwInterfaceGetTableReply tableReply(final Long vrfId) {
        final SwInterfaceGetTableReply reply = new SwInterfaceGetTableReply();
        reply.vrfId = vrfId.intValue();
        return reply;
    }
}
