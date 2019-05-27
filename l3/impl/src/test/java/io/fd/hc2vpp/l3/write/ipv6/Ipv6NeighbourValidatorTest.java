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

package io.fd.hc2vpp.l3.write.ipv6;

import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev180222.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev180222.interfaces._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev180222.interfaces._interface.ipv6.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev180222.interfaces._interface.ipv6.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv6NeighbourValidatorTest {

    private Ipv6NeighbourValidator validator;
    private static final String IFACE_NAME = "parent";
    private static final String MAC_ADDR = "aa:bb:cc:ee:11:22";
    private static final String IP_ADDR = "2001:0db8:0a0b:12f0:0000:0000:0000:0001";

    private static final InstanceIdentifier<Neighbor> IID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
                    .augmentation(Interface1.class).child(Ipv6.class).child(Neighbor.class);

    @Mock
    private WriteContext writeContext;

    @Before
    public void setUp() {
        initMocks(this);
        NamingContext ifcContext = new NamingContext("testInterfaceContext", "testInterfaceContext");
        validator = new Ipv6NeighbourValidator(ifcContext);
    }

    @Test
    public void testWriteSuccessful()
            throws DataValidationFailedException.CreateValidationFailedException {
        validator.validateWrite(IID, getNeighbor(IP_ADDR, MAC_ADDR), writeContext);
    }

    @Test
    public void testDeleteSuccessful()
            throws DataValidationFailedException.DeleteValidationFailedException {
        validator.validateDelete(IID, getNeighbor(IP_ADDR, MAC_ADDR), writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteFailedMissingIP()
            throws DataValidationFailedException.CreateValidationFailedException {
        validator.validateWrite(IID, getNeighbor("", MAC_ADDR), writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteFailedMissingMAC()
            throws DataValidationFailedException.CreateValidationFailedException {
        validator.validateWrite(IID, getNeighbor(IP_ADDR, ""), writeContext);
    }

    private Neighbor getNeighbor(String ipAddress, String macAddr) {
        final Ipv6AddressNoZone noZoneIp = ipAddress.isEmpty()
                ? null
                : new Ipv6AddressNoZone(new Ipv6Address(ipAddress));
        final PhysAddress mac = macAddr.isEmpty()
                ? null
                : new PhysAddress(macAddr);
        return new NeighborBuilder().setIp(noZoneIp).setLinkLayerAddress(mac).build();
    }
}
