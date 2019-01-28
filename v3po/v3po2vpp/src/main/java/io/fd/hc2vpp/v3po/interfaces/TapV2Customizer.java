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

package io.fd.hc2vpp.v3po.interfaces;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.fd.hc2vpp.common.translate.util.AbstractInterfaceTypeCustomizer;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.MacTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.TapCreateV2;
import io.fd.vpp.jvpp.core.dto.TapCreateV2Reply;
import io.fd.vpp.jvpp.core.dto.TapDeleteV2;
import io.fd.vpp.jvpp.core.dto.TapDeleteV2Reply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.interfaces._interface.TapV2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapV2Customizer extends AbstractInterfaceTypeCustomizer<TapV2>
        implements MacTranslator, Ipv4Translator, Ipv6Translator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TapV2Customizer.class);
    private final NamingContext interfaceContext;

    public TapV2Customizer(final FutureJVppCore vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    @Override
    protected Class<? extends InterfaceType> getExpectedInterfaceType() {
        return org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.TapV2.class;
    }

    @Override
    protected final void writeInterface(@Nonnull final InstanceIdentifier<TapV2> id, @Nonnull final TapV2 dataAfter,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();
        createTapV2(id, ifcName, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<TapV2> id, @Nonnull final TapV2 dataBefore,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();

        final int index;
        try {
            index = interfaceContext.getIndex(ifcName, writeContext.getMappingContext());
        } catch (IllegalArgumentException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }

        deleteTapV2(id, ifcName, index, dataBefore, writeContext);
    }

    private void createTapV2(final InstanceIdentifier<TapV2> id, final String swIfName, final TapV2 tapv2,
                             final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Setting TapV2 interface: {}. TapV2: {}", swIfName, tapv2);
        final CompletionStage<TapCreateV2Reply> tapV2CreateFuture = getFutureJVpp()
                .tapCreateV2(getTapV2CreateRequest(tapv2));
        final TapCreateV2Reply reply = getReplyForCreate(tapV2CreateFuture.toCompletableFuture(), id, tapv2);
        LOG.debug("TapV2 set successfully for: {}, TapV2: {}", swIfName, tapv2);
        // Add new interface to our interface context
        interfaceContext.addName(reply.swIfIndex, swIfName, writeContext.getMappingContext());
    }

    private void deleteTapV2(final InstanceIdentifier<TapV2> id, final String swIfName, final int index,
                             final TapV2 dataBefore, final WriteContext writeContext)
            throws WriteFailedException {
        LOG.debug("Deleting TapV2 interface: {}. TapV2: {}", swIfName, dataBefore);
        final CompletionStage<TapDeleteV2Reply> vxlanAddDelTunnelReplyCompletionStage =
                getFutureJVpp().tapDeleteV2(getTapV2DeleteRequest(index));
        getReplyForDelete(vxlanAddDelTunnelReplyCompletionStage.toCompletableFuture(), id);
        LOG.debug("TapV2 deleted successfully for: {}, TapV2: {}", swIfName, dataBefore);
        // Remove deleted interface from interface context
        interfaceContext.removeName(swIfName, writeContext.getMappingContext());
    }

    private TapCreateV2 getTapV2CreateRequest(final TapV2 tapv2) {
        final TapCreateV2 tapConnect = new TapCreateV2();
        final PhysAddress mac = tapv2.getMac();
        if (mac == null) {
            tapConnect.useRandomMac = 1;
            tapConnect.macAddress = new byte[6];
        } else {
            tapConnect.useRandomMac = 0;
            tapConnect.macAddress = parseMac(mac.getValue());
        }

        final Integer rxRingSz = tapv2.getRxRingSize();
        if (rxRingSz != null) {
            tapConnect.rxRingSz = rxRingSz.shortValue();
        }

        final Integer txRingSz = tapv2.getTxRingSize();
        if (txRingSz != null) {
            tapConnect.txRingSz = txRingSz.shortValue();
        }

        final String tag = tapv2.getTag();
        if (tag != null) {
            tapConnect.tag = tag.getBytes(StandardCharsets.US_ASCII);
        }

        setHostProperties(tapConnect, tapv2);
        return tapConnect;
    }

    private TapDeleteV2 getTapV2DeleteRequest(final int swIndex) {
        final TapDeleteV2 tapDeleteV2 = new TapDeleteV2();
        tapDeleteV2.swIfIndex = swIndex;
        return tapDeleteV2;
    }

    private void setHostProperties(TapCreateV2 tapConnect, TapV2 tapv2) {

        final PhysAddress hostMacAddress = tapv2.getHostMac();
        if (hostMacAddress != null) {
            tapConnect.hostMacAddr = parseMac(hostMacAddress.getValue());
            tapConnect.hostMacAddrSet = 1;
        } else {
            tapConnect.hostMacAddr = new byte[6];
            tapConnect.hostMacAddrSet = 0;
        }

        final String hostIfName = tapv2.getHostInterfaceName();
        if (hostIfName != null) {
            tapConnect.hostIfName = hostIfName.getBytes(UTF_8);
            tapConnect.hostIfNameSet = 1;
        } else {
            tapConnect.hostIfNameSet = 0;
            tapConnect.hostIfName = new byte[64];
        }

        final String hostBridge = tapv2.getHostBridge();
        if (hostBridge != null) {
            tapConnect.hostBridgeSet = 1;
            tapConnect.hostBridge = hostBridge.getBytes(UTF_8);
        } else {
            tapConnect.hostBridgeSet = 0;
            tapConnect.hostBridge = new byte[64];
        }

        final String hostNamespace = tapv2.getHostNamespace();
        if (hostNamespace != null) {
            tapConnect.hostNamespaceSet = 1;
            tapConnect.hostNamespace = hostNamespace.getBytes(UTF_8);
        } else {
            tapConnect.hostNamespaceSet = 0;
            tapConnect.hostNamespace = new byte[64];
        }

        final Ipv4Prefix hostIpv4address = tapv2.getHostIpv4Address();
        if (hostIpv4address != null) {
            tapConnect.hostIp4Addr = ipv4AddressPrefixToArray(hostIpv4address);
            tapConnect.hostIp4AddrSet = 1;
            tapConnect.hostIp4PrefixLen = extractPrefix(hostIpv4address);
        } else {
            tapConnect.hostIp4Addr = new byte[4];
            tapConnect.hostIp4AddrSet = 0;
            tapConnect.hostIp4PrefixLen = 0;
        }

        final Ipv4Address hostIpv4GW = tapv2.getHostIpv4Gateway();
        if (hostIpv4GW != null) {
            tapConnect.hostIp4Gw = ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(hostIpv4GW));
            tapConnect.hostIp4GwSet = 1;
        } else {
            tapConnect.hostIp4Gw = new byte[4];
            tapConnect.hostIp4GwSet = 0;
        }

        final Ipv6Prefix hostIpv6address = tapv2.getHostIpv6Address();
        if (hostIpv6address != null) {
            tapConnect.hostIp6Addr = ipv6AddressPrefixToArray(hostIpv6address);
            tapConnect.hostIp6AddrSet = 1;
            tapConnect.hostIp6PrefixLen = extractPrefix(hostIpv6address);
        } else {
            tapConnect.hostIp6Addr = new byte[16];
            tapConnect.hostIp6AddrSet = 0;
            tapConnect.hostIp6PrefixLen = 0;
        }

        final Ipv6Address hostIpv6GW = tapv2.getHostIpv6Gateway();
        if (hostIpv6GW != null) {
            tapConnect.hostIp6Gw =  ipv6AddressNoZoneToArray(hostIpv6GW);
            tapConnect.hostIp6GwSet = 1;
        } else {
            tapConnect.hostIp6Gw = new byte[16];
            tapConnect.hostIp6GwSet = 0;
        }
    }
}
