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

package io.fd.hc2vpp.l3.write.ipv6.nd;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.Ip6NdProxyAddDel;
import io.fd.jvpp.core.dto.Ip6NdProxyAddDelReply;
import io.fd.jvpp.core.types.Ip6Address;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.NdProxyIp6Augmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces._interface.ipv6.NdProxies;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces._interface.ipv6.nd.proxies.NdProxy;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces._interface.ipv6.nd.proxies.NdProxyBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces._interface.ipv6.nd.proxies.NdProxyKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv6;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NdProxyCustomizerTest extends WriterCustomizerTest {
    private static final String IF_NAME = "eth1";
    private static final InstanceIdentifier<NdProxies> ND_PROXIES_IID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME))
            .augmentation(Interface1.class).child(Ipv6.class).augmentation(NdProxyIp6Augmentation.class)
            .child(NdProxies.class);
    private static final int IF_INDEX = 1;

    private static final String IFACE_CTX_NAME = "ifc-test-instance";

    private NdProxyCustomizer customizer;

    @Override
    protected void setUpTest() {
        customizer = new NdProxyCustomizer(api, new NamingContext("ifacePrefix", IFACE_CTX_NAME));
        when(api.ip6NdProxyAddDel(any())).thenReturn(future(new Ip6NdProxyAddDelReply()));
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFACE_CTX_NAME);
    }

    @Test
    public void testWrite() throws WriteFailedException {
        final Ipv6AddressNoZone address = new Ipv6AddressNoZone("2001::1");
        final NdProxy data = new NdProxyBuilder().setAddress(address).build();
        customizer.writeCurrentAttributes(getId(address), data, writeContext);
        final Ip6NdProxyAddDel request = new Ip6NdProxyAddDel();
        request.swIfIndex = IF_INDEX;
        request.ip = new Ip6Address();
        request.ip.ip6Address = new byte[] {0x20, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x01};
        verify(api).ip6NdProxyAddDel(request);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws WriteFailedException {
        final Ipv6AddressNoZone address = new Ipv6AddressNoZone("2001::2");
        final NdProxy data = new NdProxyBuilder().setAddress(address).build();
        customizer.updateCurrentAttributes(getId(address), mock(NdProxy.class), data, writeContext);
    }

    @Test
    public void testDelete() throws WriteFailedException {
        final Ipv6AddressNoZone address = new Ipv6AddressNoZone("2001::3");
        final NdProxy data = new NdProxyBuilder().setAddress(address).build();
        customizer.deleteCurrentAttributes(getId(address), data, writeContext);
        final Ip6NdProxyAddDel request = new Ip6NdProxyAddDel();
        request.isDel = 1;
        request.swIfIndex = IF_INDEX;
        request.ip = new Ip6Address();
        request.ip.ip6Address = new byte[] {0x20, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x03};
        verify(api).ip6NdProxyAddDel(request);
    }

    private InstanceIdentifier<NdProxy> getId(final Ipv6AddressNoZone address) {
        return ND_PROXIES_IID.child(NdProxy.class, new NdProxyKey(address));
    }
}
