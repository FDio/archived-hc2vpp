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

import static io.fd.honeycomb.v3po.translate.v3po.test.ContextTestUtils.getMapping;
import static io.fd.honeycomb.v3po.translate.v3po.test.ContextTestUtils.getMappingIid;
import static io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils.reverseBytes;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.ModificationCache;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.RootReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.test.ListReaderCustomizerTest;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.AddressKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.openvpp.jvpp.dto.IpAddressDetails;
import org.openvpp.jvpp.dto.IpAddressDetailsReplyDump;
import org.openvpp.jvpp.dto.IpAddressDump;

public class Ipv4AddressCustomizerTest extends ListReaderCustomizerTest<Address, AddressKey, AddressBuilder> {

    private static final String IFACE_NAME = "eth0";
    private static final int IFACE_ID = 1;

    private NamingContext interfacesContext;

    public Ipv4AddressCustomizerTest() {
        super(Address.class);
    }

    @Override
    public void setUpBefore() {
        interfacesContext = new NamingContext("generatedIfaceName", "test-instance");
    }

    @Override
    protected RootReaderCustomizer<Address, AddressBuilder> initCustomizer() {
        final KeyedInstanceIdentifier<Mapping, MappingKey> eth0Id = getMappingIid(IFACE_NAME, "test-instance");
        final Optional<Mapping> eth0 = getMapping(IFACE_NAME, IFACE_ID);

        final List<Mapping> allMappings = Lists.newArrayList(getMapping(IFACE_NAME, IFACE_ID).get());
        final Mappings allMappingsBaObject = new MappingsBuilder().setMapping(allMappings).build();
        doReturn(Optional.of(allMappingsBaObject)).when(mappingContext).read(eth0Id.firstIdentifierOf(Mappings.class));

        doReturn(eth0).when(mappingContext).read(eth0Id);

        return new Ipv4AddressCustomizer(api, interfacesContext);
    }

    private static InstanceIdentifier<Address> getId(final String address) {
        return InstanceIdentifier.builder(InterfacesState.class)
            .child(Interface.class, new InterfaceKey(IFACE_NAME))
            .augmentation(Interface2.class)
            .child(Ipv4.class)
            .child(Address.class, new AddressKey(new Ipv4AddressNoZone(new Ipv4Address(address))))
            .build();
    }

    @Test
    public void testReadCurrentAttributesFromCache() throws ReadFailedException {
        ModificationCache cache = new ModificationCache();

        IpAddressDetails detail1 = new IpAddressDetails();
        IpAddressDetails detail2 = new IpAddressDetails();
        IpAddressDetails detail3 = new IpAddressDetails();

        detail1.ip = reverseBytes(
            TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"))));
        detail2.ip = reverseBytes(
            TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.2"))));
        detail3.ip = reverseBytes(
            TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.3"))));

        IpAddressDetailsReplyDump reply = new IpAddressDetailsReplyDump();
        reply.ipAddressDetails = ImmutableList.of(detail1, detail2, detail3);

        cache.put(Ipv4AddressCustomizer.class.getName(), reply);
        when(ctx.getModificationCache()).thenReturn(cache);

        final AddressBuilder builder = new AddressBuilder();
        final InstanceIdentifier<Address> id = getId("192.168.2.1");

        getCustomizer().readCurrentAttributes(id, builder, ctx);

        assertEquals("192.168.2.1", builder.getIp().getValue());
    }

    @Test
    public void testReadCurrentAttributesFromOperationalData() throws ReadFailedException {
        ModificationCache cache = new ModificationCache();

        IpAddressDetails detail1 = new IpAddressDetails();
        IpAddressDetails detail2 = new IpAddressDetails();
        IpAddressDetails detail3 = new IpAddressDetails();

        detail1.ip = reverseBytes(
            TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"))));
        detail2.ip = reverseBytes(
            TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.2"))));
        detail3.ip = reverseBytes(
            TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.3"))));

        IpAddressDetailsReplyDump reply = new IpAddressDetailsReplyDump();
        reply.ipAddressDetails = ImmutableList.of(detail1, detail2, detail3);

        CompletableFuture<IpAddressDetailsReplyDump> future = new CompletableFuture<>();
        future.complete(reply);

        when(api.ipAddressDump(Mockito.any(IpAddressDump.class))).thenReturn(future);
        when(ctx.getModificationCache()).thenReturn(cache);


        final AddressBuilder builder = new AddressBuilder();
        final InstanceIdentifier<Address> id = getId("192.168.2.1");

        getCustomizer().readCurrentAttributes(id, builder, ctx);

        assertEquals("192.168.2.1", builder.getIp().getValue());
    }

    @Test
    public void testGetAllIdsFromCache() throws ReadFailedException {
        ModificationCache cache = new ModificationCache();

        IpAddressDetails detail1 = new IpAddressDetails();
        IpAddressDetails detail2 = new IpAddressDetails();
        IpAddressDetails detail3 = new IpAddressDetails();

        detail1.ip = reverseBytes(
            TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"))));
        detail2.ip = reverseBytes(
            TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.2"))));
        detail3.ip = reverseBytes(
            TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.3"))));

        IpAddressDetailsReplyDump reply = new IpAddressDetailsReplyDump();
        reply.ipAddressDetails = ImmutableList.of(detail1, detail2, detail3);

        cache.put(Ipv4AddressCustomizer.class.getName(), reply);
        when(ctx.getModificationCache()).thenReturn(cache);

        final InstanceIdentifier<Address> id = getId("192.168.2.1");

        List<Ipv4AddressNoZone> ids = getCustomizer().getAllIds(id, ctx).stream()
            .map(key -> key.getIp())
            .collect(Collectors.toList());

        verify(api, times(0)).ipAddressDump(Mockito.any(IpAddressDump.class));
        assertEquals(3, ids.size());
        assertEquals(true, "192.168.2.1".equals(ids.get(0).getValue()));
        assertEquals(true, "192.168.2.2".equals(ids.get(1).getValue()));
        assertEquals(true, "192.168.2.3".equals(ids.get(2).getValue()));
    }

    @Test
    public void testGetAllIdsFromOperationalData() throws ReadFailedException {
        ModificationCache cache = new ModificationCache();

        IpAddressDetails detail1 = new IpAddressDetails();
        IpAddressDetails detail2 = new IpAddressDetails();
        IpAddressDetails detail3 = new IpAddressDetails();

        detail1.ip = reverseBytes(
            TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"))));
        detail2.ip = reverseBytes(
            TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.2"))));
        detail3.ip = reverseBytes(
            TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.3"))));

        IpAddressDetailsReplyDump reply = new IpAddressDetailsReplyDump();
        reply.ipAddressDetails = ImmutableList.of(detail1, detail2, detail3);

        CompletableFuture<IpAddressDetailsReplyDump> future = new CompletableFuture<>();
        future.complete(reply);

        when(api.ipAddressDump(Mockito.any(IpAddressDump.class))).thenReturn(future);
        when(ctx.getModificationCache()).thenReturn(cache);

        final InstanceIdentifier<Address> id = getId("192.168.2.1");

        List<Ipv4AddressNoZone> ids = getCustomizer().getAllIds(id, ctx).stream()
            .map(key -> key.getIp())
            .collect(Collectors.toList());

        assertEquals(3, ids.size());
        assertEquals(true, "192.168.2.1".equals(ids.get(0).getValue()));
        assertEquals(true, "192.168.2.2".equals(ids.get(1).getValue()));
        assertEquals(true, "192.168.2.3".equals(ids.get(2).getValue()));
    }

    @Test
    public void testMerge() {

        Address address = new AddressBuilder().build();
        Ipv4Builder ipv4Builder = new Ipv4Builder();
        getCustomizer().merge(ipv4Builder, Arrays.asList(address));

        assertEquals(1, ipv4Builder.getAddress().size());
        assertEquals(true, ipv4Builder.getAddress().contains(address));
    }

}
