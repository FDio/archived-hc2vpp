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

package io.fd.honeycomb.translate.v3po.interfaces.ip;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.io.BaseEncoding;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.v3po.test.ContextTestUtils;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.IpNeighborAddDel;
import org.openvpp.jvpp.core.dto.IpNeighborAddDelReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;

public class Ipv4NeighbourCustomizerTest {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IFACE_NAME = "parent";
    private static final int IFACE_ID = 5;

    @Mock
    private FutureJVppCore jvpp;

    @Mock
    private WriteContext context;

    @Mock
    private MappingContext mappingContext;

    @Mock
    private Mapping mapping;

    private ArgumentCaptor<IpNeighborAddDel> requestCaptor;
    private Ipv4NeighbourCustomizer customizer;
    private NamingContext namingContext;


    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(context.getMappingContext()).thenReturn(mappingContext);

        namingContext = new NamingContext("prefix", IFC_CTX_NAME);
        ContextTestUtils.mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);

        customizer = new Ipv4NeighbourCustomizer(jvpp,namingContext);

        requestCaptor = ArgumentCaptor.forClass(IpNeighborAddDel.class);
        CompletableFuture<IpNeighborAddDelReply> future = new CompletableFuture<>();
        future.complete(new IpNeighborAddDelReply());
        when(jvpp.ipNeighborAddDel(Mockito.any(IpNeighborAddDel.class))).thenReturn(future);
    }

    @Test
    public void testWriteCurrentAttributes() throws WriteFailedException {

        InterfaceKey intfKey = new InterfaceKey(IFACE_NAME);

        InstanceIdentifier<Neighbor> id = InstanceIdentifier.builder(Interfaces.class).child(Interface.class, intfKey)
                .augmentation(Interface1.class).child(Ipv4.class).child(Neighbor.class).build();

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        PhysAddress mac = new PhysAddress("aa:bb:cc:ee:11:22");

        Neighbor data = new NeighborBuilder().setIp(noZoneIp).setLinkLayerAddress(mac).build();

        customizer.writeCurrentAttributes(id, data, context);

        verify(jvpp, times(1)).ipNeighborAddDel(requestCaptor.capture());

        IpNeighborAddDel request = requestCaptor.getValue();

        assertEquals(0, request.isIpv6);
        assertEquals(1, request.isAdd);
        assertEquals(1, request.isStatic);
        assertEquals("1.2.168.192", TranslateUtils.arrayToIpv4AddressNoZone(request.dstAddress).getValue());
        assertEquals("aabbccee1122", BaseEncoding.base16().lowerCase().encode(request.macAddress));
        assertEquals(5, request.swIfIndex);
    }

    @Test
    public void testDeleteCurrentAttributes() throws WriteFailedException {
        InterfaceKey intfKey = new InterfaceKey(IFACE_NAME);

        InstanceIdentifier<Neighbor> id = InstanceIdentifier.builder(Interfaces.class).child(Interface.class, intfKey)
                .augmentation(Interface1.class).child(Ipv4.class).child(Neighbor.class).build();

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        PhysAddress mac = new PhysAddress("aa:bb:cc:ee:11:22");

        Neighbor data = new NeighborBuilder().setIp(noZoneIp).setLinkLayerAddress(mac).build();

        customizer.deleteCurrentAttributes(id, data, context);

        verify(jvpp, times(1)).ipNeighborAddDel(requestCaptor.capture());

        IpNeighborAddDel request = requestCaptor.getValue();

        assertEquals(0, request.isIpv6);
        assertEquals(0, request.isAdd);
        assertEquals(1, request.isStatic);
        assertEquals("1.2.168.192", TranslateUtils.arrayToIpv4AddressNoZone(request.dstAddress).getValue());
        assertEquals("aabbccee1122", BaseEncoding.base16().lowerCase().encode(request.macAddress));
        assertEquals(5, request.swIfIndex);
    }

}