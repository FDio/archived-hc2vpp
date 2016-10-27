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

package io.fd.honeycomb.translate.v3po.interfaces.ip;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.address.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.address.subnet.Netmask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.address.subnet.NetmaskBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.SwInterfaceAddDelAddress;
import io.fd.vpp.jvpp.core.dto.SwInterfaceAddDelAddressReply;

public class SubInterfaceIpv4AddressCustomizerTest extends WriterCustomizerTest implements ByteDataTranslator {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IFACE_NAME = "eth0";
    private static final int IFACE_INDEX = 0;
    private static final String SUBIF_NAME = "eth0.1";
    private static final long SUBIF_ID = 1;
    private static final int SUBIF_INDEX = 123;
    private static final InstanceIdentifier<Address> IID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
            .augmentation(SubinterfaceAugmentation.class).child(SubInterfaces.class)
            .child(SubInterface.class, new SubInterfaceKey(SUBIF_ID)).child(Ipv4.class).child(Address.class);

    private SubInterfaceIpv4AddressCustomizer customizer;

    @Override
    protected void setUp() {
        customizer = new SubInterfaceIpv4AddressCustomizer(api, new NamingContext("prefix", IFC_CTX_NAME));
        defineMapping(mappingContext, IFACE_NAME, IFACE_INDEX, IFC_CTX_NAME);
        defineMapping(mappingContext, SUBIF_NAME, SUBIF_INDEX, IFC_CTX_NAME);
    }

    @Test
    public void testWrite() throws WriteFailedException {
        when(api.swInterfaceAddDelAddress(any())).thenReturn(future(new SwInterfaceAddDelAddressReply()));
        customizer.writeCurrentAttributes(IID, address(prefixLength()), writeContext);
        verify(api).swInterfaceAddDelAddress(expectedRequest(true));
    }

    @Test
    public void testWriteFailed() {
        when(api.swInterfaceAddDelAddress(any())).thenReturn(failedFuture());
        try {
            customizer.writeCurrentAttributes(IID, address(prefixLength()), writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).swInterfaceAddDelAddress(expectedRequest(true));
            return;
        }
        fail("WriteFailedException expected");
    }

    @Test
    public void testDelete() throws WriteFailedException {
        when(api.swInterfaceAddDelAddress(any())).thenReturn(future(new SwInterfaceAddDelAddressReply()));
        customizer.deleteCurrentAttributes(IID, address(netmask()), writeContext);
        verify(api).swInterfaceAddDelAddress(expectedRequest(false));
    }

    @Test
    public void testDeleteFailed() {
        when(api.swInterfaceAddDelAddress(any())).thenReturn(failedFuture());
        try {
            customizer.deleteCurrentAttributes(IID, address(netmask()), writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).swInterfaceAddDelAddress(expectedRequest(false));
            return;
        }
        fail("WriteFailedException expected");
    }

    private SwInterfaceAddDelAddress expectedRequest(boolean isAdd) {
        final SwInterfaceAddDelAddress request = new SwInterfaceAddDelAddress();
        request.isAdd = booleanToByte(isAdd);
        request.swIfIndex = SUBIF_INDEX;
        request.isIpv6 = 0;
        request.delAll = 0;
        request.addressLength = 24;
        request.address = new byte[] {(byte) 192, (byte) 168, 2, 1};
        return request;
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdate() throws Exception {
        final Address address = address(prefixLength());
        customizer.updateCurrentAttributes(IID, address, address, writeContext);
    }

    private Address address(final Subnet subnet) {
        final Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        return new AddressBuilder().setIp(noZoneIp).setSubnet(subnet).build();
    }

    private PrefixLength prefixLength() {
        return new PrefixLengthBuilder().setPrefixLength(new Integer(24).shortValue()).build();
    }

    private Netmask netmask() {
        return new NetmaskBuilder().setNetmask(new DottedQuad("255.255.255.0")).build();
    }
}