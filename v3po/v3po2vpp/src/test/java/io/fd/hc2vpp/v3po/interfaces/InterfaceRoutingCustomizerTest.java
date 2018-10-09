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

package io.fd.hc2vpp.v3po.interfaces;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetTable;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetTableReply;
import java.util.Collections;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.interfaces._interface.RoutingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceRoutingCustomizerTest extends WriterCustomizerTest {
    private static final String IFACE_CTX_NAME = "interface-ctx";
    private static final String IF_NAME = "eth1";
    private static final int IF_INDEX = 1;
    private static final InstanceIdentifier<Routing> IID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME))
                    .augmentation(VppInterfaceAugmentation.class).child(Routing.class);

    private InterfaceRoutingCustomizer customizer;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new InterfaceRoutingCustomizer(api, new NamingContext("ifacePrefix", IFACE_CTX_NAME));
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFACE_CTX_NAME);
    }

    @Test
    public void testWrite() throws WriteFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.absent());
        final int vrfId = 123;
        when(api.swInterfaceSetTable(any())).thenReturn(future(new SwInterfaceSetTableReply()));
        customizer.writeCurrentAttributes(IID, routing(vrfId), writeContext);
        verify(api).swInterfaceSetTable(expectedRequest(vrfId));
    }

    @Test(expected = WriteFailedException.class)
    public void testWriteFailed() throws WriteFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.absent());
        when(api.swInterfaceSetTable(any())).thenReturn(failedFuture());
        customizer.writeCurrentAttributes(IID, routing(213), writeContext);
    }

    @Test(expected = IllegalStateException.class)
    public void testWriteFailedIpv4Present() throws WriteFailedException {
        when(writeContext.readBefore(RWUtils.cutId(IID, Interface.class)))
                .thenReturn(Optional.of(ifaceWithV4Address()));
        customizer.writeCurrentAttributes(IID, routing(213), writeContext);
    }

    @Test(expected = IllegalStateException.class)
    public void testWriteFailedIpv6Present() throws WriteFailedException {
        when(writeContext.readBefore(RWUtils.cutId(IID, Interface.class)))
                .thenReturn(Optional.of(ifaceWithV6Address()));
        customizer.writeCurrentAttributes(IID, routing(213), writeContext);
    }

    @Test
    public void testWriteEmptyIfaceData() throws WriteFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.of(new InterfaceBuilder().build()));
        final int vrfId = 123;
        when(api.swInterfaceSetTable(any())).thenReturn(future(new SwInterfaceSetTableReply()));
        customizer.writeCurrentAttributes(IID, routing(vrfId), writeContext);
        verify(api).swInterfaceSetTable(expectedRequest(vrfId));
    }

    private static Interface ifaceWithV4Address() {
        return new InterfaceBuilder()
                .addAugmentation(Interface1.class, new Interface1Builder()
                        .setIpv4(new Ipv4Builder()
                                .setAddress(Collections.singletonList(new AddressBuilder().build()))
                                .build())
                        .build())
                .build();
    }


    private static Interface ifaceWithV6Address() {
        return new InterfaceBuilder()
                .addAugmentation(Interface1.class, new Interface1Builder()
                        .setIpv6(new Ipv6Builder()
                                .setAddress(Collections.singletonList(
                                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv6.AddressBuilder()
                                                .build()))
                                .build())
                        .build())
                .build();
    }

    @Test(expected = WriteFailedException.class)
    public void testUpdateFailed() throws WriteFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.absent());
        when(api.swInterfaceSetTable(any())).thenReturn(failedFuture());
        customizer.updateCurrentAttributes(IID, routing(123L), routing(321L), writeContext);
    }

    @Test
    public void testDelete() throws WriteFailedException {
        when(writeContext.readAfter(any(InstanceIdentifier.class))).thenReturn(Optional.absent());
        when(api.swInterfaceSetTable(any())).thenReturn(future(new SwInterfaceSetTableReply()));
        customizer.deleteCurrentAttributes(IID, routing(123), writeContext);
        verify(api).swInterfaceSetTable(expectedRequest(0));
    }

    @Test(expected = WriteFailedException.DeleteFailedException.class)
    public void testDeleteFailed() throws WriteFailedException {
        when(writeContext.readAfter(any(InstanceIdentifier.class))).thenReturn(Optional.absent());
        when(api.swInterfaceSetTable(any())).thenReturn(failedFuture());
        customizer.deleteCurrentAttributes(IID, routing(123), writeContext);
    }

    private Routing routing(final long vrfId) {
        return new RoutingBuilder().setIpv4VrfId(new VniReference(vrfId)).build();
    }

    private SwInterfaceSetTable expectedRequest(final int vrfId) {
        final SwInterfaceSetTable request = new SwInterfaceSetTable();
        request.isIpv6 = 0;
        request.swIfIndex = IF_INDEX;
        request.vrfId = vrfId;
        return request;
    }
}