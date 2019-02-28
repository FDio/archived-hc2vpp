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

package io.fd.hc2vpp.routing.write;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.helpers.SchemaContextTestHelper;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.SwInterfaceIp6NdRaPrefix;
import io.fd.jvpp.core.dto.SwInterfaceIp6NdRaPrefixReply;
import io.fd.jvpp.core.types.Address;
import io.fd.jvpp.core.types.AddressFamily;
import io.fd.jvpp.core.types.AddressUnion;
import io.fd.jvpp.core.types.Ip6Address;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.Ipv61;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.interfaces._interface.ipv6.Ipv6RouterAdvertisements;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.interfaces._interface.ipv6.ipv6.router.advertisements.PrefixList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.interfaces._interface.ipv6.ipv6.router.advertisements.prefix.list.Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class PrefixCustomizerTest extends WriterCustomizerTest implements SchemaContextTestHelper {

    private static final String CTX_NAME = "interface-context";
    private static final String IFC_NAME = "eth0";
    private static final int IFC_INDEX = 1;
    private static final InstanceIdentifier<Prefix> IID = InstanceIdentifier
            .create(Interfaces.class)
            .child(Interface.class, new InterfaceKey(IFC_NAME))
            .augmentation(Interface1.class)
            .child(Ipv6.class)
            .augmentation(Ipv61.class)
            .child(Ipv6RouterAdvertisements.class)
            .child(PrefixList.class)
            .child(Prefix.class);

    private static final String RA_PATH = "/ietf-interfaces:interfaces" +
        "/ietf-interfaces:interface[ietf-interfaces:name='" + IFC_NAME + "']" +
        "/ietf-ip:ipv6" +
        "/hc2vpp-ietf-ipv6-unicast-routing:ipv6-router-advertisements/hc2vpp-ietf-ipv6-unicast-routing:prefix-list";

    private PrefixCustomizer customizer;
    private NamingContext interfaceContext = new NamingContext("ifaces", CTX_NAME);

    @Override
    protected void setUpTest() {
        customizer = new PrefixCustomizer(api, interfaceContext);
        defineMapping(mappingContext, IFC_NAME, IFC_INDEX, CTX_NAME);
        when(api.swInterfaceIp6NdRaPrefix(any())).thenReturn(future(new SwInterfaceIp6NdRaPrefixReply()));
    }

    @Test
    public void testWriteSimple(@InjectTestData(resourcePath = "/ra/simplePrefix.json", id = RA_PATH) PrefixList prefixList)
        throws WriteFailedException {
        final Prefix data = getPrefix(prefixList);
        customizer.writeCurrentAttributes(IID, data, writeContext);
        final SwInterfaceIp6NdRaPrefix request = new SwInterfaceIp6NdRaPrefix();
        request.swIfIndex = IFC_INDEX;

        // 2001:0db8:0a0b:12f0:0000:0000:0000:0002/64
        request.prefix = getPrefix(
                new byte[]{0x20, 0x01, 0x0d, (byte) 0xb8, 0x0a, 0x0b, 0x12, (byte) 0xf0, 0, 0, 0, 0, 0, 0, 0, 0x02},
                (byte) 64);
        request.valLifetime = 2592000; // default value
        request.prefLifetime = 604800; // default value
        verify(api).swInterfaceIp6NdRaPrefix(request);
    }

    public io.fd.jvpp.core.types.Prefix getPrefix(byte[] ip6address, byte length) {
        io.fd.jvpp.core.types.Prefix prefix = new io.fd.jvpp.core.types.Prefix();
        Address address = new Address();
        address.af = AddressFamily.ADDRESS_IP6;
        Ip6Address ip6Address = new Ip6Address();
        ip6Address.ip6Address = ip6address;
        address.un = new AddressUnion(ip6Address);
        prefix.addressLength = length;
        prefix.address = address;
        return prefix;
    }

    @Test
    public void testUpdate(@InjectTestData(resourcePath = "/ra/complexPrefix.json", id = RA_PATH) PrefixList prefixList)
        throws WriteFailedException {
        final Prefix data = getPrefix(prefixList);
        customizer.updateCurrentAttributes(IID, mock(Prefix.class), data, writeContext);
        final SwInterfaceIp6NdRaPrefix request = new SwInterfaceIp6NdRaPrefix();
        request.swIfIndex = IFC_INDEX;

        // 2001:0db8:0a0b:12f0:0000:0000:0000:0002/64
        request.prefix = getPrefix(
                new byte[]{0x20, 0x01, 0x0d, (byte) 0xb8, 0x0a, 0x0b, 0x12, (byte) 0xf0, 0, 0, 0, 0, 0, 0, 0, 0x02},
                (byte) 64);
        request.noAdvertise = 1;
        request.noAutoconfig = 1;
        request.valLifetime = -1;
        request.prefLifetime = 604800; // default value
        verify(api).swInterfaceIp6NdRaPrefix(request);
    }

    @Test
    public void testDelete(@InjectTestData(resourcePath = "/ra/simplePrefix.json", id = RA_PATH) PrefixList prefixList)
        throws WriteFailedException {
        final Prefix data = getPrefix(prefixList);
        customizer.deleteCurrentAttributes(IID, data, writeContext);
        final SwInterfaceIp6NdRaPrefix request = new SwInterfaceIp6NdRaPrefix();
        request.swIfIndex = IFC_INDEX;
        // 2001:0db8:0a0b:12f0:0000:0000:0000:0002/64
        request.prefix = getPrefix(
                new byte[]{0x20, 0x01, 0x0d, (byte) 0xb8, 0x0a, 0x0b, 0x12, (byte) 0xf0, 0, 0, 0, 0, 0, 0, 0, 0x02},
                (byte) 64);
        request.isNo = 1;
        verify(api).swInterfaceIp6NdRaPrefix(request);
    }

    private static Prefix getPrefix(final PrefixList prefixList) {
        return prefixList.getPrefix().get(0);
    }
}
