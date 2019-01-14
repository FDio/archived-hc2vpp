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

package io.fd.hc2vpp.v3po.interfacesstate;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.test.util.InterfaceDumpHelper;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.MacTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetails;
import io.fd.vpp.jvpp.core.dto.SwInterfaceTapV2Details;
import io.fd.vpp.jvpp.core.dto.SwInterfaceTapV2DetailsReplyDump;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.interfaces.state._interface.TapV2;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.interfaces.state._interface.TapV2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TapV2CustomizerTest extends ReaderCustomizerTest<TapV2, TapV2Builder> implements InterfaceDumpHelper,
        Ipv4Translator, Ipv6Translator, MacTranslator {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "tapV21";
    private static final String DEVICE_NAME = "testTapV2Device";
    private static final int IF_INDEX = 1;
    private static final String HOST_BRIDGE = "TestBridge";
    private static final String HOST_IPV4_PREFIX = "192.168.255.100";
    private static final byte HOST_IPV4_PREFIX_LEN = 24;
    private static final String HOST_IPV6_PREFIX = "a::100";
    private static final byte HOST_IPV6_PREFIX_LEN = -128;
    private static final int HOST_IPV6_PREFIX_LEN_EXP = 128;
    private static final int RX_TX_RING_SIZE = 512;
    private static final String HOST_MAC = "00:ee:ee:ee:ee:ee";
    private static final String HOST_NAMESPACE = "testHostNS";

    private static final InstanceIdentifier<TapV2> IID =
            InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
                    .augmentation(VppInterfaceStateAugmentation.class).child(TapV2.class);
    private NamingContext interfaceContext;

    @Mock
    private InterfaceCacheDumpManager dumpCacheManager;

    public TapV2CustomizerTest() {
        super(TapV2.class, VppInterfaceStateAugmentationBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_CTX_NAME);
        when(dumpCacheManager.getInterfaceDetail(IID, ctx, IF_NAME)).thenReturn(ifaceDetails());
    }

    private SwInterfaceDetails ifaceDetails() {
        final SwInterfaceDetails details = new SwInterfaceDetails();
        details.swIfIndex = IF_INDEX;
        details.interfaceName = IF_NAME.getBytes();
        details.tag = new byte[64];
        return details;
    }

    @Override
    protected ReaderCustomizer<TapV2, TapV2Builder> initCustomizer() {
        return new TapV2Customizer(api, interfaceContext, dumpCacheManager);
    }

    @Test
    public void testRead() throws ReadFailedException {
        final TapV2Builder builder = mock(TapV2Builder.class);
        when(api.swInterfaceTapV2Dump(any())).thenReturn(future(tapDump()));
        getCustomizer().readCurrentAttributes(IID, builder, ctx);
        verify(builder).setHostInterfaceName(IF_NAME);
        verify(builder).setDeviceName(DEVICE_NAME);
        verify(builder).setHostBridge(HOST_BRIDGE);
        verify(builder)
                .setHostIpv4Address(new Ipv4Prefix(String.format("%s/%d", HOST_IPV4_PREFIX, HOST_IPV4_PREFIX_LEN)));
        verify(builder)
                .setHostIpv6Address(new Ipv6Prefix(String.format("%s/%d", HOST_IPV6_PREFIX, HOST_IPV6_PREFIX_LEN_EXP)));
        verify(builder).setTxRingSize(RX_TX_RING_SIZE);
        verify(builder).setRxRingSize(RX_TX_RING_SIZE);
        verify(builder).setHostNamespace(HOST_NAMESPACE);
        verify(builder).setHostMac(new PhysAddress(HOST_MAC));
    }

    @Test(expected = ReadFailedException.class)
    public void testReadFailed() throws ReadFailedException {
        when(api.swInterfaceTapV2Dump(any())).thenReturn(failedFuture());
        getCustomizer().readCurrentAttributes(IID, mock(TapV2Builder.class), ctx);
    }

    private SwInterfaceTapV2DetailsReplyDump tapDump() {
        final SwInterfaceTapV2DetailsReplyDump reply = new SwInterfaceTapV2DetailsReplyDump();
        final SwInterfaceTapV2Details details = new SwInterfaceTapV2Details();
        details.devName = DEVICE_NAME.getBytes(UTF_8);
        details.swIfIndex = IF_INDEX;
        details.hostBridge = HOST_BRIDGE.getBytes(UTF_8);
        details.hostNamespace = HOST_NAMESPACE.getBytes(UTF_8);
        details.hostIfName = IF_NAME.getBytes(UTF_8);
        details.hostIp4PrefixLen = HOST_IPV4_PREFIX_LEN;
        details.hostIp4Addr = ipv4AddressNoZoneToArray(HOST_IPV4_PREFIX);
        details.hostIp6Addr = ipv6AddressNoZoneToArray(new Ipv6AddressNoZone(HOST_IPV6_PREFIX));
        details.hostIp6PrefixLen = HOST_IPV6_PREFIX_LEN;
        details.hostMacAddr = parseMac(HOST_MAC);
        details.txRingSz = details.rxRingSz = RX_TX_RING_SIZE;
        reply.swInterfaceTapV2Details.add(details);
        return reply;
    }
}
