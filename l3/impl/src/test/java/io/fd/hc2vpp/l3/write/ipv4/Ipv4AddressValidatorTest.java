/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.l3.write.ipv4;

import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.Subnet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.Netmask;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.NetmaskBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad;

public class Ipv4AddressValidatorTest {

    private Ipv4AddressValidator validator;
    private static final String IFC_NAME = "tapTest";
    private static final String IP_ADDR = "192.168.2.1";

    @Mock
    private WriteContext writeContext;

    @Before
    public void setUp() {
        initMocks(this);
        NamingContext ifcContext = new NamingContext("testInterfaceContext", "testInterfaceContext");
        validator = new Ipv4AddressValidator(ifcContext);
    }

    @Test
    public void testWriteSuccessful()
            throws DataValidationFailedException.CreateValidationFailedException {
        PrefixLength length = new PrefixLengthBuilder().setPrefixLength(new Integer(24).shortValue()).build();
        validator.validateWrite(Ipv4AddressCustomizerTest.getAddressId(IFC_NAME), createAddress(length), writeContext);
    }

    @Test
    public void testDeleteSuccessful()
            throws DataValidationFailedException.DeleteValidationFailedException {
        PrefixLength length = new PrefixLengthBuilder().setPrefixLength(new Integer(24).shortValue()).build();
        validator.validateDelete(Ipv4AddressCustomizerTest.getAddressId(IFC_NAME), createAddress(length), writeContext);
    }

    @Test
    public void testWriteNetmaskSuccessful()
            throws DataValidationFailedException.CreateValidationFailedException {
        Netmask netmask = new NetmaskBuilder().setNetmask(new DottedQuad(IP_ADDR)).build();
        validator.validateWrite(Ipv4AddressCustomizerTest.getAddressId(IFC_NAME), createAddress(netmask), writeContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteNetmaskFailed()
            throws DataValidationFailedException.CreateValidationFailedException {
        validator.validateWrite(Ipv4AddressCustomizerTest.getAddressId(IFC_NAME), createAddress(null), writeContext);
    }

    private Address createAddress(Subnet subnet) {
        Ipv4AddressNoZone noZoneIp = new Ipv4AddressNoZone(new Ipv4Address(IP_ADDR));
        return new AddressBuilder().setIp(noZoneIp).setSubnet(subnet).build();
    }
}
