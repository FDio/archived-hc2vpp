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
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.ipv4.AddressKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.ip4.attributes.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.IpAddressDetails;
import org.openvpp.jvpp.core.dto.IpAddressDetailsReplyDump;

public class SubInterfaceIpv4AddressCustomizerTest extends ListReaderCustomizerTest<Address, AddressKey, AddressBuilder> {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final String SUB_IF_NAME = "local0.1";
    private static final long SUB_IF_ID = 1;
    private static final int SUB_IF_INDEX = 11;
    private static final InstanceIdentifier<Ipv4> IP4_IID =
        InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
            .augmentation(SubinterfaceStateAugmentation.class)
            .child(SubInterfaces.class).child(SubInterface.class, new SubInterfaceKey(SUB_IF_ID))
            .child(Ipv4.class);
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
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_CTX_NAME);
        defineMapping(mappingContext, SUB_IF_NAME, SUB_IF_INDEX, IFC_CTX_NAME);
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

    private IpAddressDetailsReplyDump dump() {
        final IpAddressDetailsReplyDump reply = new IpAddressDetailsReplyDump();

        final IpAddressDetails details1 = new IpAddressDetails();
        details1.ip = new byte[] {1, 1, 1, 10};
        details1.prefixLength = (byte) PREFIX_LENGTH;
        reply.ipAddressDetails.add(details1);

        final IpAddressDetails details2 = new IpAddressDetails();
        details2.ip = new byte[] {2, 1, 1, 10};
        details2.prefixLength = (byte) PREFIX_LENGTH;
        reply.ipAddressDetails.add(details2);

        return reply;
    }
}