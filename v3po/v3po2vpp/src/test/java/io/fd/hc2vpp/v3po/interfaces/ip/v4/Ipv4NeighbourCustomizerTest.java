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

package io.fd.hc2vpp.v3po.interfaces.ip.v4;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfaces.ip.v4.Ipv4NeighbourCustomizer;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.IpNeighborAddDel;
import io.fd.vpp.jvpp.core.dto.IpNeighborAddDelReply;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.RoutingBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv4NeighbourCustomizerTest extends WriterCustomizerTest implements Ipv4Translator {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IFACE_NAME = "parent";
    private static final int IFACE_ID = 5;
    private static final InstanceIdentifier<Neighbor> IID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
            .augmentation(Interface1.class).child(Ipv4.class).child(Neighbor.class);

    private Ipv4NeighbourCustomizer customizer;

    @Override
    public void setUpTest() {
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        customizer = new Ipv4NeighbourCustomizer(api, new NamingContext("prefix", IFC_CTX_NAME));
    }

    @Test
    public void testWriteCurrentAttributes() throws WriteFailedException {
        when(writeContext.readBefore(IID.firstIdentifierOf(Interface.class))).thenReturn(Optional.absent());
        when(api.ipNeighborAddDel(any())).thenReturn(future(new IpNeighborAddDelReply()));
        customizer.writeCurrentAttributes(IID, getData(), writeContext);
        verify(api).ipNeighborAddDel(getExpectedRequest(true));
    }

    @Test
    public void testWriteCurrentAttributesFailed() {
        when(writeContext.readBefore(IID.firstIdentifierOf(Interface.class))).thenReturn(Optional.absent());
        when(api.ipNeighborAddDel(any())).thenReturn(failedFuture());
        try {
            customizer.writeCurrentAttributes(IID, getData(), writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).ipNeighborAddDel(getExpectedRequest(true));
            return;
        }
        fail("WriteFailedException expected");
    }
    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        customizer.updateCurrentAttributes(IID, getData(), getData(), writeContext);
    }

    @Test
    public void testDeleteCurrentAttributes() throws WriteFailedException {
        when(writeContext.readBefore(IID.firstIdentifierOf(Interface.class))).thenReturn(Optional.absent());
        when(api.ipNeighborAddDel(any())).thenReturn(future(new IpNeighborAddDelReply()));
        customizer.deleteCurrentAttributes(IID, getData(), writeContext);
        verify(api).ipNeighborAddDel(getExpectedRequest(false));
    }

    @Test
    public void testDeleteCurrentAttributesFailed() {
        when(writeContext.readBefore(IID.firstIdentifierOf(Interface.class))).thenReturn(Optional.absent());
        when(api.ipNeighborAddDel(any())).thenReturn(failedFuture());
        try {
            customizer.deleteCurrentAttributes(IID, getData(), writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).ipNeighborAddDel(getExpectedRequest(false));
            return;
        }
        fail("WriteFailedException expected");
    }

    @Test
    public void testVrfExtractionCornerCases() throws WriteFailedException {
        when(api.ipNeighborAddDel(any())).thenReturn(future(new IpNeighborAddDelReply()));

        when(writeContext.readBefore(IID.firstIdentifierOf(Interface.class)))
                // no augment
                .thenReturn(Optional.of(new InterfaceBuilder().build()))
                // empty augment
                .thenReturn(Optional.of(new InterfaceBuilder()
                        .addAugmentation(VppInterfaceAugmentation.class, new VppInterfaceAugmentationBuilder().build()).build()))
                //empty routing
                .thenReturn(Optional.of(new InterfaceBuilder()
                        .addAugmentation(VppInterfaceAugmentation.class, new VppInterfaceAugmentationBuilder()
                                .setRouting(new RoutingBuilder().build())
                                .build()).build()));


        customizer.writeCurrentAttributes(IID, getData(), writeContext);
        customizer.writeCurrentAttributes(IID, getData(), writeContext);
        customizer.writeCurrentAttributes(IID, getData(), writeContext);
        verify(api, times(3)).ipNeighborAddDel(getExpectedRequest(true));
    }

    private Neighbor getData() {
        final Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        final PhysAddress mac = new PhysAddress("aa:bb:cc:ee:11:22");
        return new NeighborBuilder().setIp(noZoneIp).setLinkLayerAddress(mac).build();
    }
    private IpNeighborAddDel getExpectedRequest(final boolean isAdd) {
        final IpNeighborAddDel request = new IpNeighborAddDel();
        request.isIpv6 = 0;
        request.isAdd = booleanToByte(isAdd);
        request.isStatic = 1;
        request.dstAddress = new byte[] {(byte) 192, (byte) 168, 2, 1};
        request.macAddress = new byte[] {(byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xee, 0x11, 0x22};
        request.swIfIndex = IFACE_ID;
        return request;
    }
}