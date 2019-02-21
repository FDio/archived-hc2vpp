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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv4NeighbourValidatorTest {

    private Ipv4NeighbourValidator validator;
    private static final String IFACE_NAME = "parent";
    private static final String MAC_ADDR = "aa:bb:cc:ee:11:22";
    private static final String IP_ADDR = "192.168.2.1";

    private static final InstanceIdentifier<Neighbor> IID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
                    .augmentation(Interface1.class).child(Ipv4.class).child(Neighbor.class);

    @Mock
    private WriteContext writeContext;

    @Before
    public void setUp() {
        initMocks(this);
        NamingContext ifcContext = new NamingContext("testInterfaceContext", "testInterfaceContext");
        validator = new Ipv4NeighbourValidator(ifcContext);
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
        final Ipv4AddressNoZone noZoneIp = ipAddress.isEmpty()
                ? null
                : new Ipv4AddressNoZone(new Ipv4Address(ipAddress));
        final PhysAddress mac = macAddr.isEmpty()
                ? null
                : new PhysAddress(macAddr);
        return new NeighborBuilder().setIp(noZoneIp).setLinkLayerAddress(mac).build();
    }
}
