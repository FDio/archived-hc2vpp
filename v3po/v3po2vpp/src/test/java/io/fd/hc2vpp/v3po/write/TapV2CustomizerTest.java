/*
 * Copyright (c) 2018 Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.v3po.write;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.MacTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.jvpp.core.dto.TapCreateV2;
import io.fd.jvpp.core.dto.TapCreateV2Reply;
import io.fd.jvpp.core.dto.TapDeleteV2;
import io.fd.jvpp.core.dto.TapDeleteV2Reply;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.TapV2;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.TapV2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TapV2CustomizerTest extends WriterCustomizerTest
        implements Ipv4Translator, Ipv6Translator, MacTranslator {

    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private TapV2Customizer tapCustomizer;
    private static final String HOST_IF_NAME = "tapV21";
    private static final String HOST_BRIDGE = "TestBridge";
    private static final String HOST_IPV4_PREFIX = "192.168.255.100";
    private static final byte HOST_IPV4_PREFIX_LEN = 24;
    private static final String HOST_IPV4_GW = "192.168.255.1";
    private static final String HOST_IPV6_PREFIX = "a::100";
    private static final String HOST_IPV6_GW = "a::1";
    private static final byte HOST_IPV6_PREFIX_LEN = -128;
    private static final int HOST_IPV6_PREFIX_LEN_EXP = 128;
    private static final int RX_TX_RING_SIZE = 512;
    private static final String HOST_MAC = "00:ee:ee:ee:ee:ee";
    private static final String HOST_NAMESPACE = "testHostNS";

    @Override
    public void setUpTest() throws Exception {
        InterfaceTypeTestUtils.setupWriteContext(writeContext,
                org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.TapV2.class);
        tapCustomizer = new TapV2Customizer(api, new NamingContext("ifcintest", IFC_TEST_INSTANCE));
    }

    @Test
    public void testCreate() throws Exception {
        final AtomicInteger idx = new AtomicInteger(0);
        doAnswer((i) -> {
            final TapCreateV2 tapData = i.getArgument(0);
            if (tapData == null) {
                return failedFuture();
            }

            Assert.assertArrayEquals(tapData.hostBridge, HOST_BRIDGE.getBytes());
            Assert.assertEquals(tapData.hostBridgeSet, 1);
            Assert.assertArrayEquals(tapData.hostIfName, HOST_IF_NAME.getBytes());
            Assert.assertEquals(tapData.hostIfNameSet, 1);
            Assert.assertArrayEquals(tapData.hostNamespace, HOST_NAMESPACE.getBytes());
            Assert.assertEquals(tapData.hostNamespaceSet, 1);
            Assert.assertArrayEquals(tapData.hostIp4Addr, ipv4AddressPrefixToArray(
                    new Ipv4Prefix(String.format("%s/%d", HOST_IPV4_PREFIX, HOST_IPV4_PREFIX_LEN))));
            Assert.assertEquals(tapData.hostIp4AddrSet, 1);
            Assert.assertEquals(tapData.hostIp4PrefixLen, HOST_IPV4_PREFIX_LEN);
            Assert.assertArrayEquals(tapData.hostIp6Addr, ipv6AddressPrefixToArray(
                    new Ipv6Prefix(String.format("%s/%d", HOST_IPV6_PREFIX, HOST_IPV6_PREFIX_LEN_EXP))));
            Assert.assertEquals(tapData.hostIp6AddrSet, 1);
            Assert.assertEquals(tapData.hostIp6PrefixLen, HOST_IPV6_PREFIX_LEN);
            Assert.assertArrayEquals(tapData.hostIp4Gw, ipv4AddressPrefixToArray(
                    new Ipv4Prefix(String.format("%s/%d", HOST_IPV4_GW, 24))));
            Assert.assertEquals(tapData.hostIp4GwSet, 1);
            Assert.assertArrayEquals(tapData.hostIp6Gw, ipv6AddressPrefixToArray(
                    new Ipv6Prefix(String.format("%s/%d", HOST_IPV6_GW, 96))));
            Assert.assertEquals(tapData.hostIp6GwSet, 1);
            Assert.assertArrayEquals(tapData.hostMacAddr, parseMac(HOST_MAC));
            Assert.assertEquals(tapData.hostMacAddrSet, 1);
            Assert.assertEquals(tapData.rxRingSz, RX_TX_RING_SIZE);
            Assert.assertEquals(tapData.txRingSz, RX_TX_RING_SIZE);
            final TapCreateV2Reply t = new TapCreateV2Reply();
            t.swIfIndex = idx.getAndIncrement();
            return future(t);

        }).when(api).tapCreateV2(any(TapCreateV2.class));

        tapCustomizer.writeCurrentAttributes(getTapId("tap"), getTapData("tap"), writeContext);
        tapCustomizer.writeCurrentAttributes(getTapId("tap2"), getTapData("tap2"), writeContext);

        verify(api, times(2)).tapCreateV2(any(TapCreateV2.class));
        verify(mappingContext).put(eq(mappingIid("tap", IFC_TEST_INSTANCE)), eq(
                mapping("tap", 0).get()));
        verify(mappingContext).put(eq(mappingIid("tap2", IFC_TEST_INSTANCE)), eq(
                mapping("tap2", 1).get()));
    }

    @Test
    public void testDelete() throws Exception {
        final TapCreateV2Reply t = new TapCreateV2Reply();
        t.swIfIndex = 0;
        doReturn(future(t)).when(api).tapCreateV2(any(TapCreateV2.class));

        doReturn(future(new TapDeleteV2Reply())).when(api).tapDeleteV2(any(TapDeleteV2.class));
        tapCustomizer.writeCurrentAttributes(getTapId("tap-v2"), getTapData("tap-v2"), writeContext);
        defineMapping(mappingContext, "tap-v2", 1, IFC_TEST_INSTANCE);
        tapCustomizer.deleteCurrentAttributes(getTapId("tap-v2"), getTapData("tap-v2"), writeContext);

        verify(api).tapCreateV2(any(TapCreateV2.class));
        verify(api).tapDeleteV2(any(TapDeleteV2.class));
        verify(mappingContext).delete(eq(mappingIid("tap-v2", IFC_TEST_INSTANCE)));
    }

    private InstanceIdentifier<TapV2> getTapId(final String tap) {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(tap)).augmentation(
                VppInterfaceAugmentation.class).child(TapV2.class);
    }

    private TapV2 getTapData(final String tap) {
        return new TapV2Builder()
                .setHostInterfaceName(HOST_IF_NAME)
                .setMac(new PhysAddress(HOST_MAC))
                .setTag(tap + "_tag")
                .setHostBridge(HOST_BRIDGE)
                .setHostIpv4Address(new Ipv4Prefix(String.format("%s/%s", HOST_IPV4_PREFIX, HOST_IPV4_PREFIX_LEN)))
                .setHostIpv4Gateway(new Ipv4Address(HOST_IPV4_GW))
                .setHostIpv6Address(new Ipv6Prefix(String.format("%s/%s", HOST_IPV6_PREFIX, HOST_IPV6_PREFIX_LEN_EXP)))
                .setHostIpv6Gateway(new Ipv6Address(HOST_IPV6_GW))
                .setRxRingSize(RX_TX_RING_SIZE)
                .setTxRingSize(RX_TX_RING_SIZE)
                .setHostMac(new PhysAddress(HOST_MAC))
                .setHostNamespace(HOST_NAMESPACE)
                .build();
    }
}
