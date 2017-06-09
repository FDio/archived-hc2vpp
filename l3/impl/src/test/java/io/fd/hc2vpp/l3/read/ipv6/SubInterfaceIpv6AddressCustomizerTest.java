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

package io.fd.hc2vpp.l3.read.ipv6;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.hc2vpp.l3.read.InterfaceChildNodeTest;
import io.fd.hc2vpp.l3.read.ipv6.subinterface.SubInterfaceIpv6AddressCustomizer;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import java.util.Arrays;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.interfaces.state._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.sub._interface.ip6.attributes.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.sub._interface.ip6.attributes.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.sub._interface.ip6.attributes.ipv6.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.sub._interface.ip6.attributes.ipv6.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.sub._interface.ip6.attributes.ipv6.AddressKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceIpv6AddressCustomizerTest extends ListReaderCustomizerTest<Address, AddressKey, AddressBuilder>
        implements InterfaceChildNodeTest {

    private InstanceIdentifier<Address> instanceIdentifier;

    public SubInterfaceIpv6AddressCustomizerTest() {
        super(Address.class, Ipv6Builder.class);
    }

    @Override
    protected void setUp() throws Exception {
        instanceIdentifier = InstanceIdentifier.create(InterfacesState.class)
                .child(Interface.class, new InterfaceKey(IFACE_NAME))
                .augmentation(SubinterfaceStateAugmentation.class)
                .child(SubInterfaces.class)
                .child(SubInterface.class, new SubInterfaceKey((long) SUB_IFACE_ID))
                .child(Ipv6.class)
                .child(Address.class, new AddressKey(IPV6_TWO_ADDRESS_COMPRESSED));

        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, INTERFACE_CONTEXT_NAME);
        defineMapping(mappingContext, SUB_IFACE_NAME, SUB_IFACE_ID, INTERFACE_CONTEXT_NAME);
        mockAddressDump(api, dumpV6AddressesSubIfaceOne(), v6Addresses());
    }

    @Test
    public void testGetAll() throws ReadFailedException {
        verifyList(Arrays.asList(
                new AddressKey(IPV6_ONE_ADDRESS_COMPRESSED), new AddressKey(IPV6_TWO_ADDRESS_COMPRESSED)),
                getCustomizer().getAllIds(instanceIdentifier, ctx));
    }

    @Override
    protected ReaderCustomizer<Address, AddressBuilder> initCustomizer() {
        return new SubInterfaceIpv6AddressCustomizer(api, INTERFACE_CONTEXT);
    }
}