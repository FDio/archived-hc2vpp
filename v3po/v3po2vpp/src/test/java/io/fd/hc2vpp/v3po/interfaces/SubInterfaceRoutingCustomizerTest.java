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

package io.fd.hc2vpp.v3po.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetTable;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetTableReply;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.routing.attributes.Routing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.routing.attributes.RoutingBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceRoutingCustomizerTest extends WriterCustomizerTest implements ByteDataTranslator {

    private static final String IFACE_CTX_NAME = "interface-ctx";
    private static final String IF_NAME = "eth1";
    private static final String SUBIF_NAME = "eth1.0";
    private static final int IF_INDEX = 1;
    private static final int SUBIF_INDEX = 0;
    private static final InstanceIdentifier<Routing> VALID_ID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME))
                    .augmentation(SubinterfaceAugmentation.class)
                    .child(SubInterfaces.class)
                    .child(SubInterface.class, new SubInterfaceKey((long) SUBIF_INDEX))
                    .child(Routing.class);
    private static final int DISABLE_VRF = 0;

    @Captor
    private ArgumentCaptor<SwInterfaceSetTable> requestCaptor;


    private SubInterfaceRoutingCustomizer customizer;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new SubInterfaceRoutingCustomizer(api, new NamingContext("ifacePrefix", IFACE_CTX_NAME));
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFACE_CTX_NAME);
        defineMapping(mappingContext, SUBIF_NAME, SUBIF_INDEX, IFACE_CTX_NAME);
        when(api.swInterfaceSetTable(any())).thenReturn(future(new SwInterfaceSetTableReply()));
        when(api.swInterfaceSetTable(any())).thenReturn(future(new SwInterfaceSetTableReply()));
    }

    @Test(expected = IllegalStateException.class)
    public void testWriteFailedV4AddressPresent() throws WriteFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.of(v4AddressPresent()));
        final Routing v4Routing = new RoutingBuilder().setIpv4VrfId(new VniReference(4L)).build();
        customizer.writeCurrentAttributes(VALID_ID, v4Routing, writeContext);
    }

    @Test(expected = IllegalStateException.class)
    public void testWriteFailedV6AddressPresent() throws WriteFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.of(v6AddressPresent()));
        final Routing v4Routing = new RoutingBuilder().setIpv4VrfId(new VniReference(4L)).build();
        customizer.writeCurrentAttributes(VALID_ID, v4Routing, writeContext);
    }

    @Test
    public void testWriteIpv4Vrf() throws WriteFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.absent());
        final Routing v4Routing = new RoutingBuilder().setIpv4VrfId(new VniReference(4L)).build();
        customizer.writeCurrentAttributes(VALID_ID, v4Routing, writeContext);
        verifySetTableRequest(1, Collections.singleton(request(false, SUBIF_INDEX, 4)));
    }


    @Test
    public void testWriteIpv6Vrf() throws WriteFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.absent());
        final Routing v6Routing = new RoutingBuilder().setIpv6VrfId(new VniReference(3L)).build();
        customizer.writeCurrentAttributes(VALID_ID, v6Routing, writeContext);
        verifySetTableRequest(1, Collections.singleton(request(true, SUBIF_INDEX, 3)));
    }

    @Test
    public void testUpdateIpv4Vrf() throws WriteFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.absent());
        final Routing routingBefore = new RoutingBuilder().setIpv6VrfId(new VniReference(3L))
            .setIpv4VrfId(new VniReference(4L)).build();
        final Routing routingAfter = new RoutingBuilder().setIpv6VrfId(new VniReference(3L))
            .setIpv4VrfId(new VniReference(5L)).build();
        customizer.updateCurrentAttributes(VALID_ID, routingBefore, routingAfter, writeContext);
        verifySetTableRequest(2, ImmutableSet.of(request(false, SUBIF_INDEX, 5),
                request(true, SUBIF_INDEX, 3)));
    }

    @Test
    public void testUpdateIpv6Vrf() throws WriteFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.absent());
        final Routing routingBefore = new RoutingBuilder().setIpv6VrfId(new VniReference(3L))
            .setIpv4VrfId(new VniReference(4L)).build();
        final Routing routingAfter = new RoutingBuilder().setIpv6VrfId(new VniReference(8L))
            .setIpv4VrfId(new VniReference(4L)).build();
        customizer.updateCurrentAttributes(VALID_ID, routingBefore, routingAfter, writeContext);
        verifySetTableRequest(2, ImmutableSet.of(request(false, SUBIF_INDEX, 4),
                request(true, SUBIF_INDEX, 8)));
    }

    @Test
    public void testDeleteIpv4Vrf() throws WriteFailedException {
        when(writeContext.readAfter(any(InstanceIdentifier.class))).thenReturn(Optional.absent());
        final Routing v4Routing = new RoutingBuilder().setIpv4VrfId(new VniReference(4L)).build();
        customizer.deleteCurrentAttributes(VALID_ID, v4Routing, writeContext);
        verifySetTableRequest(1, Collections.singleton(request(false, SUBIF_INDEX, DISABLE_VRF)));
    }


    @Test
    public void testDeleteIpv6Vrf() throws WriteFailedException {
        when(writeContext.readAfter(any(InstanceIdentifier.class))).thenReturn(Optional.absent());
        final Routing v6Routing = new RoutingBuilder().setIpv6VrfId(new VniReference(3L)).build();
        customizer.deleteCurrentAttributes(VALID_ID, v6Routing, writeContext);
        verifySetTableRequest(1, Collections.singleton(request(true, SUBIF_INDEX, DISABLE_VRF)));
    }

    private SwInterfaceSetTable request(final boolean ipv6, final int index, final int vrf) {
        final SwInterfaceSetTable request = new SwInterfaceSetTable();
        request.vrfId = vrf;
        request.swIfIndex = index;
        request.isIpv6 = booleanToByte(ipv6);
        return request;
    }

    private void verifySetTableRequest(final int times, final Set<SwInterfaceSetTable> requests) {
        verify(api, times(times)).swInterfaceSetTable(requestCaptor.capture());
        requestCaptor.getAllValues().containsAll(requests);
    }

    private static SubInterface v4AddressPresent() {
        return new SubInterfaceBuilder()
                .setIpv4(new Ipv4Builder()
                        .setAddress(Collections.singletonList(new AddressBuilder().build()))
                        .build())
                .build();
    }

    private static SubInterface v6AddressPresent(){
        return new SubInterfaceBuilder()
                .setIpv6(new Ipv6Builder()
                        .setAddress(Collections.singletonList(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.ipv6.AddressBuilder().build()))
                        .build())
                .build();
    }
}
