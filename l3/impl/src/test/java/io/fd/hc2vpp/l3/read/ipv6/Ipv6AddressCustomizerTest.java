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

import static org.junit.Assert.assertEquals;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.hc2vpp.l3.read.InterfaceChildNodeTest;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import java.util.Arrays;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv6.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv6.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv6.AddressKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv6AddressCustomizerTest extends ListReaderCustomizerTest<Address, AddressKey, AddressBuilder>
        implements InterfaceChildNodeTest {

    private InstanceIdentifier<Address> instanceIdentifier;

    public Ipv6AddressCustomizerTest() {
        super(Address.class, Ipv6Builder.class);
    }

    @Override
    protected void setUp() throws Exception {
        instanceIdentifier = InstanceIdentifier.create(InterfacesState.class)
                .child(Interface.class, new InterfaceKey(IFACE_2_NAME))
                .augmentation(Interface2.class)
                .child(Ipv6.class)
                .child(Address.class, new AddressKey(IPV6_TWO_ADDRESS_COMPRESSED));
        defineMapping(mappingContext, IFACE_2_NAME, IFACE_2_ID, INTERFACE_CONTEXT_NAME);
        mockAddressDump(api, dumpV6AddressesIfaceTwo(), v6Addresses());
    }

    @Override
    protected ReaderCustomizer<Address, AddressBuilder> initCustomizer() {
        return new Ipv6AddressCustomizer(api, INTERFACE_CONTEXT);
    }

    @Test
    public void testGetAll() throws ReadFailedException {
        verifyList(Arrays.asList(
                new AddressKey(IPV6_ONE_ADDRESS_COMPRESSED), new AddressKey(IPV6_TWO_ADDRESS_COMPRESSED)),
                getCustomizer().getAllIds(instanceIdentifier, ctx));
    }

    @Test
    public void testReadCurrent() throws ReadFailedException {
        AddressBuilder builder = new AddressBuilder();
        getCustomizer().readCurrentAttributes(instanceIdentifier, builder, ctx);

        assertEquals(IPV6_TWO_ADDRESS_COMPRESSED, builder.getIp());
        assertEquals(IPV6_TWO_PREFIX, builder.getPrefixLength().longValue());
    }
}