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

package io.fd.hc2vpp.dhcp.write;

import static io.fd.hc2vpp.dhcp.write.DhcpRelayCustomizerTest.RELAYS_PATH;
import static io.fd.hc2vpp.dhcp.write.DhcpRelayCustomizerTest.getId;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.dhcp.helpers.SchemaContextTestHelper;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.dhcp.rev180629.dhcp.attributes.Relays;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.dhcp.rev180629.dhcp.attributes.relays.Relay;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.dhcp.rev180629.dhcp.attributes.relays.RelayBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv4;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZoneBuilder;

@RunWith(HoneycombTestRunner.class)
public class DhcpRelayValidatorTest implements SchemaContextTestHelper {

    @Mock
    private WriteContext writeContext;
    private DhcpRelayValidator validator;

    @Before
    public void setUp() {
        initMocks(this);
        validator = new DhcpRelayValidator();
    }

    @Test
    public void testWrite(@InjectTestData(resourcePath = "/relay/ipv4DhcpRelay.json", id = RELAYS_PATH) Relays relays)
            throws DataValidationFailedException.CreateValidationFailedException {
        final int rxVrfId = 0;
        validator.validateWrite(getId(rxVrfId, Ipv4.class), extractRelay(relays), writeContext);
    }

    @Test
    public void testUpdate(
            @InjectTestData(resourcePath = "/relay/ipv6DhcpRelayBefore.json", id = RELAYS_PATH) Relays relaysBefore,
            @InjectTestData(resourcePath = "/relay/ipv6DhcpRelayAfter.json", id = RELAYS_PATH) Relays relayAfter)
            throws DataValidationFailedException.UpdateValidationFailedException {
        final int rxVrfId = 1;
        validator.validateUpdate(getId(rxVrfId, Ipv6.class), extractRelay(relaysBefore), extractRelay(relayAfter),
                writeContext);
    }

    @Test
    public void testDelete(@InjectTestData(resourcePath = "/relay/ipv4DhcpRelay.json", id = RELAYS_PATH) Relays relays)
            throws DataValidationFailedException.DeleteValidationFailedException {
        final int rxVrfId = 0;
        validator.validateDelete(getId(rxVrfId, Ipv4.class), extractRelay(relays), writeContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMixedIpAddressFamilies(
            @InjectTestData(resourcePath = "/relay/ipv4DhcpRelay.json", id = RELAYS_PATH) Relays relays) {
        RelayBuilder builder = new RelayBuilder();
        builder.fieldsFrom(extractRelay(relays));
        builder.setGatewayAddress(IpAddressNoZoneBuilder.getDefaultInstance("2001::10"));
        validator.validateRelay(builder.build());
    }

    private Relay extractRelay(Relays relays) {
        return relays.getRelay().get(0);
    }
}
