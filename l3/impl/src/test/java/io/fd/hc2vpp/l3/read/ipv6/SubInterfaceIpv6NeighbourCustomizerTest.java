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
import io.fd.hc2vpp.l3.read.ipv6.subinterface.SubInterfaceIpv6NeighbourCustomizer;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import java.util.Arrays;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.ip6.attributes.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.ip6.attributes.Ipv6Builder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.ip6.attributes.ipv6.Neighbor;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.ip6.attributes.ipv6.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.ip6.attributes.ipv6.NeighborKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceIpv6NeighbourCustomizerTest extends ListReaderCustomizerTest<Neighbor, NeighborKey, NeighborBuilder>
        implements InterfaceChildNodeTest {

    private InstanceIdentifier<Neighbor> instanceIdentifier;

    public SubInterfaceIpv6NeighbourCustomizerTest() {
        super(Neighbor.class, Ipv6Builder.class);
    }

    @Override
    protected void setUp() throws Exception {
        instanceIdentifier = InstanceIdentifier.create(Interfaces.class)
                .child(Interface.class, new InterfaceKey(IFACE_2_NAME))
                .augmentation(SubinterfaceAugmentation.class)
                .child(SubInterfaces.class)
                .child(SubInterface.class, new SubInterfaceKey((long) SUB_IFACE_2_ID))
                .child(Ipv6.class)
                .child(Neighbor.class, new NeighborKey(IPV6_TWO_ADDRESS_COMPRESSED));
        defineMapping(mappingContext, IFACE_2_NAME, IFACE_2_ID, INTERFACE_CONTEXT_NAME);
        defineMapping(mappingContext, SUB_IFACE_2_NAME, SUB_IFACE_2_ID, INTERFACE_CONTEXT_NAME);
        mockNeighborDump(api, dumpV6NeighborsSubIfaceTwo(), v6Neighbors());
    }

    @Test
    public void testGetAll() throws ReadFailedException {
        verifyList(Arrays.asList(
                new NeighborKey(IPV6_ONE_ADDRESS_COMPRESSED),
                new NeighborKey(IPV6_TWO_ADDRESS_COMPRESSED)),
                getCustomizer().getAllIds(instanceIdentifier, ctx));
    }

    @Test
    public void readCurrent() throws ReadFailedException {
        NeighborBuilder builder = new NeighborBuilder();
        getCustomizer().readCurrentAttributes(instanceIdentifier, builder, ctx);

        assertEquals(IPV6_TWO_ADDRESS_COMPRESSED, builder.getIp());
        assertEquals(MAC_FOUR_ADDRESS, builder.getLinkLayerAddress());
    }

    @Override
    protected ReaderCustomizer<Neighbor, NeighborBuilder> initCustomizer() {
        return new SubInterfaceIpv6NeighbourCustomizer(api, INTERFACE_CONTEXT);
    }
}
