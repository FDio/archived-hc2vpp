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
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.v3po.interfaces.ip.subnet.validation.SubnetValidationException;
import io.fd.honeycomb.translate.v3po.interfaces.ip.subnet.validation.SubnetValidator;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import org.openvpp.jvpp.core.dto.IpAddressDetailsReplyDump;
import org.openvpp.jvpp.core.dto.SwInterfaceAddDelAddress;
import org.openvpp.jvpp.core.dto.SwInterfaceAddDelAddressReply;

public class Ipv4AddressCustomizerTest extends WriterCustomizerTest {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IFACE_NAME = "eth0";
    private static final int IFACE_ID = 123;

    @Mock
    private SubnetValidator subnetValidator;

    private NamingContext interfaceContext;
    private Ipv4AddressCustomizer customizer;

    @Before
    public void setUp() throws Exception {
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);

        customizer = new Ipv4AddressCustomizer(api, interfaceContext, subnetValidator);

        doReturn(future(new IpAddressDetailsReplyDump())).when(api).ipAddressDump(any());
        when(writeContext.readAfter(Mockito.any()))
                .thenReturn(Optional.of(new Ipv4Builder().setAddress(Collections.emptyList()).build()));
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
        doReturn(future(new SwInterfaceAddDelAddressReply())).when(api).swInterfaceAddDelAddress(any(SwInterfaceAddDelAddress.class));
    }

    private void whenSwInterfaceAddDelAddressThenFailure() {
        doReturn(failedFuture()).when(api).swInterfaceAddDelAddress(any(SwInterfaceAddDelAddress.class));
    }

    @Test
    public void testAddPrefixLengthIpv4Address() throws Exception {
        doNothing().when(subnetValidator).checkNotAddingToSameSubnet(Mockito.anyList());

        final InstanceIdentifier<Address> id = getAddressId(IFACE_NAME);
        when(writeContext.readBefore(id)).thenReturn(Optional.absent());

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        PrefixLength length = new PrefixLengthBuilder().setPrefixLength(new Integer(24).shortValue()).build();
        Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(length).build();

        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        whenSwInterfaceAddDelAddressThenSuccess();

        customizer.writeCurrentAttributes(id, data, writeContext);

        verify(api).swInterfaceAddDelAddress(generateSwInterfaceAddDelAddressRequest(new byte[]{-64, -88, 2, 1},
                (byte) 1, (byte) 24));
    }

    @Test
    public void testAddPrefixLengthIpv4AddressFailed() throws Exception {
        final InstanceIdentifier<Address> id = getAddressId(IFACE_NAME);
        when(writeContext.readBefore(id)).thenReturn(Optional.absent());

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        PrefixLength length = new PrefixLengthBuilder().setPrefixLength(new Integer(24).shortValue()).build();
        Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(length).build();

        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        whenSwInterfaceAddDelAddressThenFailure();

        try {
            customizer.writeCurrentAttributes(id, data, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).swInterfaceAddDelAddress(
                    generateSwInterfaceAddDelAddressRequest(new byte[]{-64, -88, 2, 1},
                            (byte) 1, (byte) 24));
            return;
        }
        fail("WriteFailedException was expected");
    }

    @Test
    public void testAddPrefixLengthIpv4AddressConflicted() throws Exception {

        final InstanceIdentifier<Address> id = getAddressId(IFACE_NAME);
        when(writeContext.readBefore(id)).thenReturn(Optional.absent());

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        PrefixLength length = new PrefixLengthBuilder().setPrefixLength(new Integer(24).shortValue()).build();
        Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(length).build();
        final List<Address> addressList = Arrays.asList(data);

        //throws when validation invoked
        doThrow(SubnetValidationException.forConflictingData((short) 24, Arrays.asList(data))).when(subnetValidator)
                .checkNotAddingToSameSubnet(addressList);

        //fake data return from WriteContext
        doReturn(Optional.of(new Ipv4Builder().setAddress(addressList).build())).when(writeContext)
                .readAfter(argThat(matchInstanceIdentifier(Ipv4.class)));

        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);

        try {
            customizer.writeCurrentAttributes(id, data, writeContext);
        } catch (WriteFailedException e) {
            //verifies if cause of exception is correct type
            assertTrue(e.getCause() instanceof SubnetValidationException);

            //verify that validation call was invoked with data from writeContext
            verify(subnetValidator, times(1)).checkNotAddingToSameSubnet(addressList);
        }

    }

    private static ArgumentMatcher<InstanceIdentifier<?>> matchInstanceIdentifier(
            Class<?> desiredClass) {
        return new ArgumentMatcher<InstanceIdentifier<?>>() {
            @Override
            public boolean matches(final Object o) {
                return o instanceof InstanceIdentifier && ((InstanceIdentifier) o).getTargetType().equals(desiredClass);
            }
        };
    }

    @Test(expected =  WriteFailedException.UpdateFailedException.class)
    public void testUpdate() throws Exception {
        final Address data = mock(Address.class);
        customizer.updateCurrentAttributes(getAddressId(IFACE_NAME), data, data, writeContext);
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

        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        whenSwInterfaceAddDelAddressThenSuccess();

        customizer.deleteCurrentAttributes(id, data, writeContext);

        verify(api).swInterfaceAddDelAddress(generateSwInterfaceAddDelAddressRequest(new byte[]{-64, -88, 2, 1},
                (byte) 0, (byte) 24));
    }

    @Test
    public void testDeletePrefixLengthIpv4AddressFailed() throws Exception {
        final InstanceIdentifier<Address> id = getAddressId(IFACE_NAME);

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        PrefixLength length = new PrefixLengthBuilder().setPrefixLength(new Integer(24).shortValue()).build();
        Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(length).build();

        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        whenSwInterfaceAddDelAddressThenFailure();

        try {
            customizer.deleteCurrentAttributes(id, data, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).swInterfaceAddDelAddress(
                    generateSwInterfaceAddDelAddressRequest(new byte[]{-64, -88, 2, 1},
                            (byte) 0, (byte) 24));
            return;
        }
        fail("WriteFailedException was expec16ted");
    }

    @Test
    public void testNetmaskFailed() {
        final int expectedPrefixLength = 1;
        final String stringMask = "128.0.0.0";
        final InstanceIdentifier<Address> id = getAddressId(IFACE_NAME);
        when(writeContext.readBefore(id)).thenReturn(Optional.absent());

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        Netmask subnet = new NetmaskBuilder().setNetmask(new DottedQuad(stringMask)).build();
        Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(subnet).build();

        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        whenSwInterfaceAddDelAddressThenFailure();

        try {
            customizer.writeCurrentAttributes(id, data, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).swInterfaceAddDelAddress(generateSwInterfaceAddDelAddressRequest(new byte[]{-64, -88, 2, 1},
                (byte) 1, (byte) expectedPrefixLength));
            return;
        }
        fail("WriteFailedException was expec16ted");

    }

    private void testSingleNetmask(final int expectedPrefixLength, final String stringMask) throws Exception {
        final InstanceIdentifier<Address> id = getAddressId(IFACE_NAME);
        when(writeContext.readBefore(id)).thenReturn(Optional.absent());

        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
        Netmask subnet = new NetmaskBuilder().setNetmask(new DottedQuad(stringMask)).build();
        Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(subnet).build();

        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        whenSwInterfaceAddDelAddressThenSuccess();

        customizer.writeCurrentAttributes(id, data, writeContext);

        verify(api).swInterfaceAddDelAddress(generateSwInterfaceAddDelAddressRequest(new byte[]{-64, -88, 2, 1},
            (byte) 1, (byte) expectedPrefixLength));
    }

    private void testSingleIllegalNetmask(final String stringMask) throws Exception {
        try {
            final InstanceIdentifier<Address> id = getAddressId(IFACE_NAME);
            when(writeContext.readBefore(id)).thenReturn(Optional.absent());

            Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"));
            Netmask subnet = new NetmaskBuilder().setNetmask(new DottedQuad(stringMask)).build();
            Address data = new AddressBuilder().setIp(noZoneIp).setSubnet(subnet).build();

            defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
            whenSwInterfaceAddDelAddressThenSuccess();

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
