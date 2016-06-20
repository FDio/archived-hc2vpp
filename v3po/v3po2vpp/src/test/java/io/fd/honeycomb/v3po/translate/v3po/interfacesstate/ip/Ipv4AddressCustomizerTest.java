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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate.ip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.v3po.translate.ModificationCache;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.AddressKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.IpAddressDetails;
import org.openvpp.jvpp.dto.IpAddressDetailsReplyDump;
import org.openvpp.jvpp.dto.IpAddressDump;
import org.openvpp.jvpp.future.FutureJVpp;

public class Ipv4AddressCustomizerTest {

    @Test
    public void testGetBuilder() {
        assertNotNull(new Ipv4AddressCustomizer(mock(FutureJVpp.class)).getBuilder(null));
    }

    @Test
    public void testReadCurrentAttributesFromCache() throws ReadFailedException {
        ReadContext context = mock(ReadContext.class);
        ModificationCache cache = new ModificationCache();
        FutureJVpp jvpp = mock(FutureJVpp.class);

        IpAddressDetails detail1 = new IpAddressDetails();
        IpAddressDetails detail2 = new IpAddressDetails();
        IpAddressDetails detail3 = new IpAddressDetails();

        detail1.ip = TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1")));
        detail2.ip = TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.2")));
        detail3.ip = TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.3")));

        IpAddressDetailsReplyDump reply = new IpAddressDetailsReplyDump();
        reply.ipAddressDetails = ImmutableList.of(detail1, detail2, detail3);

        cache.put(Ipv4AddressCustomizer.class.getName(), reply);
        when(context.getModificationCache()).thenReturn(cache);

        AddressBuilder builder = new AddressBuilder();
        InstanceIdentifier<Address> id = InstanceIdentifier.builder(InterfacesState.class)
                .child(Interface.class)
                .augmentation(Interface2.class)
                .child(Ipv4.class)
                .child(Address.class, new AddressKey(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"))))
                .build();

        new Ipv4AddressCustomizer(jvpp).readCurrentAttributes(id, builder, context);

        assertEquals("1.2.168.192", builder.getIp().getValue());
    }

    @Test
    public void testReadCurrentAttributesFromOperationalData() throws ReadFailedException {
        ReadContext context = mock(ReadContext.class);
        ModificationCache cache = new ModificationCache();
        FutureJVpp jvpp = mock(FutureJVpp.class);

        IpAddressDetails detail1 = new IpAddressDetails();
        IpAddressDetails detail2 = new IpAddressDetails();
        IpAddressDetails detail3 = new IpAddressDetails();

        detail1.ip = TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1")));
        detail2.ip = TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.2")));
        detail3.ip = TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.3")));

        IpAddressDetailsReplyDump reply = new IpAddressDetailsReplyDump();
        reply.ipAddressDetails = ImmutableList.of(detail1, detail2, detail3);

        CompletableFuture<IpAddressDetailsReplyDump> future = new CompletableFuture<>();
        future.complete(reply);

        when(jvpp.ipAddressDump(Mockito.any(IpAddressDump.class))).thenReturn(future);
        when(context.getModificationCache()).thenReturn(cache);

        AddressBuilder builder = new AddressBuilder();
        InstanceIdentifier<Address> id = InstanceIdentifier.builder(InterfacesState.class)
                .child(Interface.class)
                .augmentation(Interface2.class)
                .child(Ipv4.class)
                .child(Address.class, new AddressKey(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"))))
                .build();

        new Ipv4AddressCustomizer(jvpp).readCurrentAttributes(id, builder, context);

        assertEquals("1.2.168.192", builder.getIp().getValue());
    }

    @Test
    public void testGetAllIdsFromCache() throws ReadFailedException {
        ReadContext context = mock(ReadContext.class);
        ModificationCache cache = new ModificationCache();
        FutureJVpp jvpp = mock(FutureJVpp.class);

        IpAddressDetails detail1 = new IpAddressDetails();
        IpAddressDetails detail2 = new IpAddressDetails();
        IpAddressDetails detail3 = new IpAddressDetails();

        detail1.ip = TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1")));
        detail2.ip = TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.2")));
        detail3.ip = TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.3")));

        IpAddressDetailsReplyDump reply = new IpAddressDetailsReplyDump();
        reply.ipAddressDetails = ImmutableList.of(detail1, detail2, detail3);

        cache.put(Ipv4AddressCustomizer.class.getName(), reply);
        when(context.getModificationCache()).thenReturn(cache);
        List<Ipv4AddressNoZone> ids = new Ipv4AddressCustomizer(jvpp).getAllIds(null, context).stream()
                .map(id -> id.getIp())
                .collect(Collectors.toList());

        verify(jvpp, times(0)).ipAddressDump(Mockito.any(IpAddressDump.class));
        assertEquals(3, ids.size());
        assertEquals(true, "1.2.168.192".equals(ids.get(0).getValue()));
        assertEquals(true, "2.2.168.192".equals(ids.get(1).getValue()));
        assertEquals(true, "3.2.168.192".equals(ids.get(2).getValue()));
    }

    @Test
    public void testGetAllIdsFromOperationalData() throws ReadFailedException {
        ReadContext context = mock(ReadContext.class);
        ModificationCache cache = new ModificationCache();
        FutureJVpp jvpp = mock(FutureJVpp.class);

        IpAddressDetails detail1 = new IpAddressDetails();
        IpAddressDetails detail2 = new IpAddressDetails();
        IpAddressDetails detail3 = new IpAddressDetails();

        detail1.ip = TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1")));
        detail2.ip = TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.2")));
        detail3.ip = TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.3")));

        IpAddressDetailsReplyDump reply = new IpAddressDetailsReplyDump();
        reply.ipAddressDetails = ImmutableList.of(detail1, detail2, detail3);

        CompletableFuture<IpAddressDetailsReplyDump> future = new CompletableFuture<>();
        future.complete(reply);

        when(jvpp.ipAddressDump(Mockito.any(IpAddressDump.class))).thenReturn(future);
        when(context.getModificationCache()).thenReturn(cache);
        List<Ipv4AddressNoZone> ids = new Ipv4AddressCustomizer(jvpp).getAllIds(null, context).stream()
                .map(id -> id.getIp())
                .collect(Collectors.toList());

        assertEquals(3, ids.size());
        assertEquals(true, "1.2.168.192".equals(ids.get(0).getValue()));
        assertEquals(true, "2.2.168.192".equals(ids.get(1).getValue()));
        assertEquals(true, "3.2.168.192".equals(ids.get(2).getValue()));
    }

    @Test
    public void testMerge() {

        Address address = new AddressBuilder().build();
        Ipv4Builder ipv4Builder = new Ipv4Builder();
        new Ipv4AddressCustomizer(mock(FutureJVpp.class)).merge(ipv4Builder, Arrays.asList(address));

        assertEquals(1, ipv4Builder.getAddress().size());
        assertEquals(true, ipv4Builder.getAddress().contains(address));
    }

}
