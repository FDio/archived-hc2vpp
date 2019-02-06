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

package io.fd.hc2vpp.l3.read.ipv6.nd;

import static io.fd.hc2vpp.l3.read.ipv6.nd.NdProxyCustomizer.getCfgId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.InitializingListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.Ip6NdProxyDetails;
import io.fd.vpp.jvpp.core.dto.Ip6NdProxyDetailsReplyDump;
import io.fd.vpp.jvpp.core.types.Ip6Address;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.NdProxyIp6StateAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces.state._interface.ipv6.NdProxies;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces.state._interface.ipv6.NdProxiesBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces.state._interface.ipv6.nd.proxies.NdProxy;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces.state._interface.ipv6.nd.proxies.NdProxyBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces.state._interface.ipv6.nd.proxies.NdProxyKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv6;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NdProxyCustomizerTest extends InitializingListReaderCustomizerTest<NdProxy, NdProxyKey, NdProxyBuilder> {
    private static final String IF1_NAME = "eth1";
    private static final int IF1_INDEX = 1;
    private static final String IF2_NAME = "eth2";
    private static final int IF2_INDEX = 2;
    private static final String IFACE_CTX_NAME = "ifc-test-instance";

    public NdProxyCustomizerTest() {
        super(NdProxy.class, NdProxiesBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        defineMapping(mappingContext, IF1_NAME, IF1_INDEX, IFACE_CTX_NAME);
        defineMapping(mappingContext, IF2_NAME, IF2_INDEX, IFACE_CTX_NAME);
        final Ip6NdProxyDetailsReplyDump reply = new Ip6NdProxyDetailsReplyDump();
        final Ip6NdProxyDetails proxy1 = new Ip6NdProxyDetails();
        proxy1.swIfIndex = IF1_INDEX;
        proxy1.ip = new Ip6Address();
        proxy1.ip.ip6Address = new byte[] {0x20, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x01};
        final Ip6NdProxyDetails proxy2 = new Ip6NdProxyDetails();
        proxy2.swIfIndex = IF1_INDEX;
        proxy2.ip = new Ip6Address();
        proxy2.ip.ip6Address = new byte[] {0x20, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x02};
        final Ip6NdProxyDetails proxy3 = new Ip6NdProxyDetails();
        proxy3.swIfIndex = IF2_INDEX;
        proxy3.ip = new Ip6Address();
        proxy3.ip.ip6Address = new byte[] {0x20, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x03};
        reply.ip6NdProxyDetails.add(proxy1);
        reply.ip6NdProxyDetails.add(proxy2);
        reply.ip6NdProxyDetails.add(proxy3);
        when(api.ip6NdProxyDump(any())).thenReturn(future(reply));
    }

    @Override
    protected ReaderCustomizer<NdProxy, NdProxyBuilder> initCustomizer() {
        return new NdProxyCustomizer(api, new NamingContext("ifacePrefix", IFACE_CTX_NAME));
    }

    @Test
    public void readCurrent() throws ReadFailedException {
        final NdProxyBuilder builder = new NdProxyBuilder();
        final Ipv6AddressNoZone address = new Ipv6AddressNoZone("2001::1");
        getCustomizer().readCurrentAttributes(getId(IF1_NAME, address), builder, ctx);
        assertEquals(builder.getAddress(), address);
    }

    @Test
    public void testGetAllInterface1() throws ReadFailedException {
        final List<NdProxyKey> allIds = getCustomizer().getAllIds(getProxiesId(IF1_NAME).child(NdProxy.class), ctx);
        assertThat(allIds, containsInAnyOrder(
            new NdProxyKey(new Ipv6AddressNoZone("2001::1")),
            new NdProxyKey(new Ipv6AddressNoZone("2001::2"))
        ));
    }

    @Test
    public void testGetAllInterface2() throws ReadFailedException {
        final List<NdProxyKey> allIds = getCustomizer().getAllIds(getProxiesId(IF2_NAME).child(NdProxy.class), ctx);
        assertThat(allIds, containsInAnyOrder(new NdProxyKey(new Ipv6AddressNoZone("2001::3"))));
    }

    @Test
    public void testInit() {
        final Ipv6AddressNoZone address = new Ipv6AddressNoZone("2001::1");
        final InstanceIdentifier<NdProxy> id = getId(IF1_NAME, address);
        invokeInitTest(
            id,
            new NdProxyBuilder().setAddress(address).build(),
            getCfgId(id),
            new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces._interface.ipv6.nd.proxies.NdProxyBuilder()
                .setAddress(address).build()
        );
    }

    private InstanceIdentifier<NdProxy> getId(final String ifName, final Ipv6AddressNoZone address) {
        return getProxiesId(ifName).child(NdProxy.class, new NdProxyKey(address));
    }

    private InstanceIdentifier<NdProxies> getProxiesId(final String ifName) {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(ifName))
            .augmentation(Interface2.class).child(Ipv6.class).augmentation(NdProxyIp6StateAugmentation.class)
            .child(NdProxies.class);
    }
}
