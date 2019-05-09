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

package io.fd.hc2vpp.l3.read.ipv4;

import static org.junit.Assert.assertEquals;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.hc2vpp.l3.read.InterfaceChildNodeTest;
import io.fd.hc2vpp.l3.read.ipv4.subinterface.SubInterfaceIpv4NeighbourCustomizer;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import java.util.Arrays;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.Ipv4;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.ipv4.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.ipv4.NeighborKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceIpv4NeighbourCustomizerTest extends ListReaderCustomizerTest<Neighbor, NeighborKey, NeighborBuilder>
        implements InterfaceChildNodeTest {

    private InstanceIdentifier<Neighbor> instanceIdentifier;

    public SubInterfaceIpv4NeighbourCustomizerTest() {
        super(Neighbor.class, Ipv4Builder.class);
    }

    @Override
    protected void setUp() throws Exception {
        instanceIdentifier = InstanceIdentifier.create(InterfacesState.class)
                .child(Interface.class, new InterfaceKey(IFACE_NAME))
                .augmentation(SubinterfaceStateAugmentation.class)
                .child(SubInterfaces.class)
                .child(SubInterface.class, new SubInterfaceKey((long) SUB_IFACE_ID))
                .child(Ipv4.class)
                .child(Neighbor.class, new NeighborKey(IPV4_TWO_ADDRESS));

        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, INTERFACE_CONTEXT_NAME);
        defineMapping(mappingContext, SUB_IFACE_NAME, SUB_IFACE_ID, INTERFACE_CONTEXT_NAME);
        mockNeighborDump(api, dumpV4NeighborsSubIfaceOne(), v4Neighbors());
    }

    @Test
    public void testGetAll() throws ReadFailedException {
        verifyList(Arrays.asList(new NeighborKey(IPV4_ONE_ADDRESS), new NeighborKey(IPV4_TWO_ADDRESS)),
                getCustomizer().getAllIds(instanceIdentifier, ctx));
    }

    @Test
    public void testReadCurrent() throws ReadFailedException {
        final NeighborBuilder neighborBuilder = new NeighborBuilder();
        getCustomizer().readCurrentAttributes(instanceIdentifier, neighborBuilder, ctx);

        assertEquals(MAC_TWO_ADDRESS, neighborBuilder.getLinkLayerAddress());
        assertEquals(IPV4_TWO_ADDRESS, neighborBuilder.getIp());
    }

    @Override
    protected ReaderCustomizer<Neighbor, NeighborBuilder> initCustomizer() {
        return new SubInterfaceIpv4NeighbourCustomizer(api, INTERFACE_CONTEXT);
    }
}
