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

package io.fd.honeycomb.translate.v3po.interfacesstate.ip;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.CacheKeyFactory;
import io.fd.honeycomb.translate.util.read.cache.IdentifierCacheKeyFactory;
import io.fd.honeycomb.translate.vpp.util.Ipv4Translator;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import io.fd.vpp.jvpp.core.dto.IpAddressDetails;
import io.fd.vpp.jvpp.core.dto.IpAddressDetailsReplyDump;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces.state._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.AddressKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceIpv4AddressCustomizerTest extends ListReaderCustomizerTest<Address, AddressKey, AddressBuilder>
        implements Ipv4Translator {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final String SUB_IF_NAME = "local0.1";
    private static final String SUB_IF_2_NAME = "local0.2";
    private static final long SUB_IF_ID = 1;
    private static final int SUB_IF_INDEX = 11;
    private static final int SUB_IF_2_INDEX = 12;
    private static final InstanceIdentifier<Ipv4> IP4_IID =
            InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
                    .augmentation(SubinterfaceStateAugmentation.class)
                    .child(SubInterfaces.class).child(SubInterface.class, new SubInterfaceKey(SUB_IF_ID))
                    .child(Ipv4.class);
    private InstanceIdentifier<Address> ifaceOneAddressOneIdentifier;
    private InstanceIdentifier<Address> ifaceTwoAddressOneIdentifier;
    private CacheKeyFactory cacheKeyFactory;
    private static final Ipv4AddressNoZone IP1 = new Ipv4AddressNoZone("10.1.1.1");
    private static final Ipv4AddressNoZone IP2 = new Ipv4AddressNoZone("10.1.1.2");
    private static final short PREFIX_LENGTH = 16;

    private NamingContext interfaceContext;

    public SubInterfaceIpv4AddressCustomizerTest() {
        super(Address.class, Ipv4Builder.class);
    }

    @Override
    protected void setUp() throws Exception {
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);

        ifaceOneAddressOneIdentifier = InstanceIdentifier.create(InterfacesState.class)
                .child(Interface.class, new InterfaceKey(IF_NAME))
                .augmentation(SubinterfaceStateAugmentation.class)
                .child(SubInterfaces.class)
                .child(SubInterface.class, new SubInterfaceKey(1L))
                .child(Ipv4.class)
                .child(Address.class, new AddressKey(new Ipv4AddressNoZone("192.168.2.1")));
        ifaceTwoAddressOneIdentifier = InstanceIdentifier.create(InterfacesState.class)
                .child(Interface.class, new InterfaceKey(IF_NAME))
                .augmentation(SubinterfaceStateAugmentation.class)
                .child(SubInterfaces.class)
                .child(SubInterface.class, new SubInterfaceKey(2L))
                .child(Ipv4.class)
                .child(Address.class, new AddressKey(new Ipv4AddressNoZone("192.168.2.1")));

        // to simulate complex key
        cacheKeyFactory = new IdentifierCacheKeyFactory(ImmutableSet.of(SubInterface.class));

        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_CTX_NAME);
        defineMapping(mappingContext, SUB_IF_NAME, SUB_IF_INDEX, IFC_CTX_NAME);
        defineMapping(mappingContext, SUB_IF_2_NAME, SUB_IF_2_INDEX, IFC_CTX_NAME);
    }

    @Override
    protected ListReaderCustomizer<Address, AddressKey, AddressBuilder> initCustomizer() {
        return new SubInterfaceIpv4AddressCustomizer(api, interfaceContext);
    }

    private static InstanceIdentifier<Address> getId() {
        return IP4_IID.child(Address.class);
    }

    private static InstanceIdentifier<Address> getId(final Ipv4AddressNoZone ip) {
        return IP4_IID.child(Address.class, new AddressKey(ip));
    }

    @Test
    public void testRead() throws ReadFailedException {
        final AddressBuilder builder = mock(AddressBuilder.class);
        when(api.ipAddressDump(any())).thenReturn(future(dump()));
        getCustomizer().readCurrentAttributes(getId(IP2), builder, ctx);
        verify(builder).setIp(IP2);
        verify(builder).setSubnet(new PrefixLengthBuilder().setPrefixLength(PREFIX_LENGTH).build());
    }

    @Test(expected = ReadFailedException.class)
    public void testReadFailed() throws ReadFailedException {
        when(api.ipAddressDump(any())).thenReturn(failedFuture());
        getCustomizer().readCurrentAttributes(getId(IP1), mock(AddressBuilder.class), ctx);
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {
        when(api.ipAddressDump(any())).thenReturn(future(dump()));
        final List<AddressKey> allIds = getCustomizer().getAllIds(getId(), ctx);
        assertThat(allIds, hasSize(2));
        assertThat(allIds, containsInAnyOrder(new AddressKey(IP1), new AddressKey(IP2)));
    }

    @Test(expected = ReadFailedException.class)
    public void testGetAllIdsFailed() throws ReadFailedException {
        when(api.ipAddressDump(any())).thenReturn(failedFuture());
        getCustomizer().getAllIds(getId(), ctx);
    }

    @Test
    public void testCacheScope() {

    }

    @Test
    public void testCachingScopeSpecificRequest() throws ReadFailedException {
        fillCacheForTwoIfaces();
        final AddressBuilder ifaceOneAddressBuilder = new AddressBuilder();
        final AddressBuilder ifaceTwoAddressBuilder = new AddressBuilder();

        getCustomizer().readCurrentAttributes(ifaceOneAddressOneIdentifier, ifaceOneAddressBuilder, ctx);
        getCustomizer().readCurrentAttributes(ifaceTwoAddressOneIdentifier, ifaceTwoAddressBuilder, ctx);

        // addresses have caching scope of parent interface, so returned address should have respective prefix lengths
        assertEquals("192.168.2.1", ifaceOneAddressBuilder.getIp().getValue());
        assertTrue(ifaceOneAddressBuilder.getSubnet() instanceof PrefixLength);
        assertEquals(22, PrefixLength.class.cast(ifaceOneAddressBuilder.getSubnet()).getPrefixLength().intValue());

        assertEquals("192.168.2.1", ifaceTwoAddressBuilder.getIp().getValue());
        assertTrue(ifaceTwoAddressBuilder.getSubnet() instanceof PrefixLength);
        assertEquals(23, PrefixLength.class.cast(ifaceTwoAddressBuilder.getSubnet()).getPrefixLength().intValue());
    }

    @Test
    public void testCachingScopeGetAll() throws ReadFailedException {
        fillCacheForFirstIfaceSecondEmpty();

        final List<AddressKey> keysForIfaceOne = getCustomizer().getAllIds(ifaceOneAddressOneIdentifier, ctx);
        Assert.assertThat(keysForIfaceOne, hasSize(1));
        final AddressKey keyIfaceOne = keysForIfaceOne.get(0);
        assertEquals("192.168.2.1", keyIfaceOne.getIp().getValue());

        final List<AddressKey> keysForIfaceTwo = getCustomizer().getAllIds(ifaceTwoAddressOneIdentifier, ctx);
        Assert.assertThat(keysForIfaceTwo, is(empty()));
    }

    private void fillCacheForTwoIfaces() {
        IpAddressDetails detailIfaceOneAddressOne = new IpAddressDetails();
        IpAddressDetails detailIfaceTwoAddressOne = new IpAddressDetails();
        IpAddressDetailsReplyDump replyIfaceOne = new IpAddressDetailsReplyDump();
        IpAddressDetailsReplyDump replyIfaceTwo = new IpAddressDetailsReplyDump();

        replyIfaceOne.ipAddressDetails = Arrays.asList(detailIfaceOneAddressOne);
        replyIfaceTwo.ipAddressDetails = Arrays.asList(detailIfaceTwoAddressOne);

        detailIfaceOneAddressOne.ip = reverseBytes(
                ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"))));
        detailIfaceOneAddressOne.prefixLength = 22;

        detailIfaceTwoAddressOne.ip = reverseBytes(
                ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"))));
        detailIfaceTwoAddressOne.prefixLength = 23;

        cache.put(cacheKeyFactory.createKey(ifaceOneAddressOneIdentifier), replyIfaceOne);
        cache.put(cacheKeyFactory.createKey(ifaceTwoAddressOneIdentifier), replyIfaceTwo);
    }

    private void fillCacheForFirstIfaceSecondEmpty() {
        IpAddressDetails detailIfaceOneAddressOne = new IpAddressDetails();
        IpAddressDetailsReplyDump replyIfaceOne = new IpAddressDetailsReplyDump();
        replyIfaceOne.ipAddressDetails = Arrays.asList(detailIfaceOneAddressOne);

        detailIfaceOneAddressOne.ip = reverseBytes(
                ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"))));
        detailIfaceOneAddressOne.prefixLength = 22;

        cache.put(cacheKeyFactory.createKey(ifaceOneAddressOneIdentifier), replyIfaceOne);
        cache.put(cacheKeyFactory.createKey(ifaceTwoAddressOneIdentifier), new IpAddressDetailsReplyDump());
    }

    private IpAddressDetailsReplyDump dump() {
        final IpAddressDetailsReplyDump reply = new IpAddressDetailsReplyDump();

        final IpAddressDetails details1 = new IpAddressDetails();
        details1.ip = new byte[]{1, 1, 1, 10};
        details1.prefixLength = (byte) PREFIX_LENGTH;
        reply.ipAddressDetails.add(details1);

        final IpAddressDetails details2 = new IpAddressDetails();
        details2.ip = new byte[]{2, 1, 1, 10};
        details2.prefixLength = (byte) PREFIX_LENGTH;
        reply.ipAddressDetails.add(details2);

        return reply;
    }
}