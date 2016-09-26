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

package io.fd.honeycomb.translate.v3po.interfaces;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.RoutingBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.SwInterfaceSetTable;
import org.openvpp.jvpp.core.dto.SwInterfaceSetTableReply;

public class RoutingCustomizerTest extends WriterCustomizerTest {
    private static final String IFACE_CTX_NAME = "interface-ctx";
    private static final String IF_NAME = "eth1";
    private static final int IF_INDEX = 1;
    private static final InstanceIdentifier<Routing> IID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME))
            .augmentation(VppInterfaceAugmentation.class).child(Routing.class);

    private RoutingCustomizer customizer;

    @Override
    protected void setUp() throws Exception {
        customizer = new RoutingCustomizer(api, new NamingContext("ifacePrefix", IFACE_CTX_NAME));
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFACE_CTX_NAME);
    }

    @Test
    public void testWrite() throws WriteFailedException {
        final int vrfId = 123;
        when(api.swInterfaceSetTable(any())).thenReturn(future(new SwInterfaceSetTableReply()));
        customizer.writeCurrentAttributes(IID, routing(vrfId), writeContext);
        verify(api).swInterfaceSetTable(expectedRequest(vrfId));
    }

    @Test(expected = WriteFailedException.CreateFailedException.class)
    public void testWriteFailed() throws WriteFailedException {
        when(api.swInterfaceSetTable(any())).thenReturn(failedFuture());
        customizer.writeCurrentAttributes(IID, routing(213), writeContext);
    }

    @Test
    public void testUpdate() throws WriteFailedException {
        when(api.swInterfaceSetTable(any())).thenReturn(future(new SwInterfaceSetTableReply()));
        customizer.updateCurrentAttributes(IID, routing(123L), null, writeContext);
        verifyZeroInteractions(api);
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdateFailed() throws WriteFailedException {
        when(api.swInterfaceSetTable(any())).thenReturn(failedFuture());
        customizer.updateCurrentAttributes(IID, routing(123L), routing(321L), writeContext);
    }

    @Test
    public void testDelete() throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, routing(123), writeContext);
        verifyZeroInteractions(api);
    }

    private Routing routing(final long vrfId) {
        return new RoutingBuilder().setVrfId(vrfId).build();
    }

    private SwInterfaceSetTable expectedRequest(final int vrfId) {
        final SwInterfaceSetTable request = new SwInterfaceSetTable();
        request.isIpv6 = 0;
        request.swIfIndex = IF_INDEX;
        request.vrfId = vrfId;
        return request;
    }
}