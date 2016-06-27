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

package io.fd.honeycomb.v3po.translate.v3po.interfaces.ip;

import static io.fd.honeycomb.v3po.translate.v3po.test.ContextTestUtils.mockMapping;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.MappingContext;
import io.fd.honeycomb.v3po.translate.v3po.test.TestHelperUtils;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.Netmask;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.NetmaskBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.VppInvocationException;
import org.openvpp.jvpp.dto.SwInterfaceAddDelAddress;
import org.openvpp.jvpp.dto.SwInterfaceAddDelAddressReply;
import org.openvpp.jvpp.future.FutureJVpp;

public class Ipv4AddressCustomizerTest {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IFACE_NAME = "eth0";
    private static final int IFACE_ID = 123;

    @Mock
    private WriteContext writeContext;
    @Mock
    private MappingContext mappingContext;
    @Mock
    private FutureJVpp api;

    private NamingContext interfaceContext;
    private Ipv4AddressCustomizer customizer;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        doReturn(mappingContext).when(writeContext).getMappingContext();
        interfaceContext = new NamingContext("generatedlIfaceName", IFC_CTX_NAME);

        customizer = new Ipv4AddressCustomizer(api, interfaceContext);
    }

    private static InstanceIdentifier<Address> getAddressId(final String ifaceName) {
        return InstanceIdentifier.builder(Interfaces.class)
            .child(Interface.class, new InterfaceKey(ifaceName))
            .augmentation(Interface1.class)
            .child(Ipv4.class)
            .child(Address.class)
            .build();
    }

    private void whenSwInterfaceAddDelAddressThenSuccess() {
        final CompletableFuture<SwInterfaceAddDelAddressReply> replyFuture = new CompletableFuture<>();
        final SwInterfaceAddDelAddressReply reply = new SwInterfaceAddDelAddressReply();
        replyFuture.complete(reply);
        doReturn(replyFuture).when(api).swInterfaceAddDelAddress(any(SwInterfaceAddDelAddress.class));
    }

    private void whenSwInterfaceAddDelAddressThenFailure() {
        doReturn(TestHelperUtils.createFutureException()).when(api)
            .swInterfaceAddDelAddress(any(SwInterfaceAddDelAddress.class));
    }

    private void verifySwInterfaceAddDelAddressWasInvoked(final SwInterfaceAddDelAddress expected) throws
        VppInvocationException {
        ArgumentCaptor<SwInterfaceAddDelAddress> argumentCaptor =
            ArgumentCaptor.forClass(SwInterfaceAddDelAddress.class);
        verify(api).swInterfaceAddDelAddress(argumentCaptor.capture());
        verifySwInterfaceAddDelAddressWasInvoked(expected, argumentCaptor.getValue());
    }

    private void verifySwInterfaceAddDelAddressWasInvoked(final SwInterfaceAddDelAddress expected,
                                                          final SwInterfaceAddDelAddress actual) throws
        VppInvocationException {
        assertArrayEquals(expected.address, actual.address);
        assertEquals(expected.addressLength, actual.addressLength);
        assertEquals(expected.delAll, actual.delAll);
        assertEquals(expected.isAdd, actual.isAdd);
        assertEquals(expected.isIpv6, actual.isIpv6);
        assertEquals(expected.swIfIndex, actual.swIfIndex);
    }

    @Test
    public void testAddPrefixLengthIpv4Address() throws Exception {
        final InstanceIdentifier<Address> id = getAddressId(IFACE_NAME);

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        PrefixLength length = new PrefixLengthBuilder().setPrefixLength(new Integer(24).shortValue()).build();
        Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(length).build();

        mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        whenSwInterfaceAddDelAddressThenSuccess();

        customizer.writeCurrentAttributes(id, data, writeContext);

        verifySwInterfaceAddDelAddressWasInvoked(generateSwInterfaceAddDelAddressRequest(new byte[] {-64, -88, 2, 1},
            (byte) 1, (byte) 24));
    }

    @Test
    public void testAddPrefixLengthIpv4AddressFailed() throws Exception {
        final InstanceIdentifier<Address> id = getAddressId(IFACE_NAME);

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        PrefixLength length = new PrefixLengthBuilder().setPrefixLength(new Integer(24).shortValue()).build();
        Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(length).build();

        mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        whenSwInterfaceAddDelAddressThenFailure();

        try {
            customizer.writeCurrentAttributes(id, data, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifySwInterfaceAddDelAddressWasInvoked(
                generateSwInterfaceAddDelAddressRequest(new byte[] {-64, -88, 2, 1},
                    (byte) 1, (byte) 24));
            return;
        }
        fail("WriteFailedException was expected");
    }

    private SwInterfaceAddDelAddress generateSwInterfaceAddDelAddressRequest(final byte[] address, final byte isAdd,
                                                                             final byte prefixLength) {
        final SwInterfaceAddDelAddress request = new SwInterfaceAddDelAddress();
        request.swIfIndex = IFACE_ID;
        request.isAdd = isAdd;
        request.isIpv6 = 0;
        request.delAll = 0;
        request.addressLength = prefixLength;
        request.address = address;
        return request;
    }

    @Test
    public void testDeletePrefixLengthIpv4Address() throws Exception {
        final InstanceIdentifier<Address> id = getAddressId(IFACE_NAME);

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        PrefixLength length = new PrefixLengthBuilder().setPrefixLength(new Integer(24).shortValue()).build();
        Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(length).build();

        mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        whenSwInterfaceAddDelAddressThenSuccess();

        customizer.deleteCurrentAttributes(id, data, writeContext);

        verifySwInterfaceAddDelAddressWasInvoked(generateSwInterfaceAddDelAddressRequest(new byte[] {-64, -88, 2, 1},
            (byte) 0, (byte) 24));
    }

    @Test
    public void testDeletePrefixLengthIpv4AddressFailed() throws Exception {
        final InstanceIdentifier<Address> id = getAddressId(IFACE_NAME);

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        PrefixLength length = new PrefixLengthBuilder().setPrefixLength(new Integer(24).shortValue()).build();
        Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(length).build();

        mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        whenSwInterfaceAddDelAddressThenFailure();

        try {
            customizer.deleteCurrentAttributes(id, data, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifySwInterfaceAddDelAddressWasInvoked(
                generateSwInterfaceAddDelAddressRequest(new byte[] {-64, -88, 2, 1},
                    (byte) 0, (byte) 24));
            return;
        }
        fail("WriteFailedException was expec16ted");
    }

    @Test
    public void testExtract() {
        final InstanceIdentifier<Address> id = getAddressId(IFACE_NAME);

        Address address = new AddressBuilder().build();
        Ipv4 parentData = new Ipv4Builder().setAddress(Arrays.asList(address)).build();

        Optional<List<Address>> addressesOptional = customizer.extract(id, parentData);

        assertEquals(true, addressesOptional.isPresent());
        assertEquals(1, addressesOptional.get().size());
        assertEquals(true, addressesOptional.get().contains(address));
    }

    private void testSingleNetmask(final int expectedPrefixLength, final String stringMask) throws Exception {
        final InstanceIdentifier<Address> id = getAddressId(IFACE_NAME);

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        Netmask subnet = new NetmaskBuilder().setNetmask(new DottedQuad(stringMask)).build();
        Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(subnet).build();

        mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);

        final CompletableFuture<SwInterfaceAddDelAddressReply> replyFuture = new CompletableFuture<>();
        replyFuture.complete(new SwInterfaceAddDelAddressReply());
        ArgumentCaptor<SwInterfaceAddDelAddress> argumentCaptor =
            ArgumentCaptor.forClass(SwInterfaceAddDelAddress.class);
        doReturn(replyFuture).when(api).swInterfaceAddDelAddress(argumentCaptor.capture());

        customizer.writeCurrentAttributes(id, data, writeContext);

        verifySwInterfaceAddDelAddressWasInvoked(generateSwInterfaceAddDelAddressRequest(new byte[] {-64, -88, 2, 1},
            (byte) 1, (byte) expectedPrefixLength), argumentCaptor.getValue());
    }

    private void testSingleIllegalNetmask(final String stringMask) throws Exception {
        try {
            final InstanceIdentifier<Address> id = getAddressId(IFACE_NAME);

            Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
            Netmask subnet = new NetmaskBuilder().setNetmask(new DottedQuad(stringMask)).build();
            Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(subnet).build();

            mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);

            final CompletableFuture<SwInterfaceAddDelAddressReply> replyFuture = new CompletableFuture<>();
            replyFuture.complete(new SwInterfaceAddDelAddressReply());
            ArgumentCaptor<SwInterfaceAddDelAddress> argumentCaptor =
                ArgumentCaptor.forClass(SwInterfaceAddDelAddress.class);
            doReturn(replyFuture).when(api).swInterfaceAddDelAddress(argumentCaptor.capture());

            customizer.writeCurrentAttributes(id, data, writeContext);
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("IllegalArgumentException expected");

    }

    /**
     * Test contiguous netmask length from QuadDotted notation
     */
    @Test
    public void testNetmaskLength() throws Exception {
        testSingleNetmask(1, "128.0.0.0");
        testSingleNetmask(2, "192.0.0.0");
        testSingleNetmask(8, "255.0.0.0");
        testSingleNetmask(9, "255.128.0.0");
        testSingleNetmask(16, "255.255.0.0");
        testSingleNetmask(24, "255.255.255.0");
    }

    @Test
    public void testNetmaskIllegal() throws Exception {
        testSingleIllegalNetmask("");
        testSingleIllegalNetmask(".");
        testSingleIllegalNetmask(".255");
        testSingleIllegalNetmask("255");
        testSingleIllegalNetmask("255.");
        testSingleIllegalNetmask("255.255");
        testSingleIllegalNetmask("255.255.0");
        testSingleIllegalNetmask("255.255.255.");
        testSingleIllegalNetmask("255.255.255.256");
        testSingleIllegalNetmask("0.0.0.0");
        testSingleIllegalNetmask("10.10.10.10");
        testSingleIllegalNetmask("255.1.255.0");
        testSingleIllegalNetmask("255.255.255.255");
    }

}
