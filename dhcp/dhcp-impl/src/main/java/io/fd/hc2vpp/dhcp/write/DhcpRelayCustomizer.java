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

package io.fd.hc2vpp.dhcp.write;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.DhcpProxyConfig;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.Relay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.RelayKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DhcpRelayCustomizer extends FutureJVppCustomizer implements ListWriterCustomizer<Relay, RelayKey>,
    JvppReplyConsumer, ByteDataTranslator, Ipv6Translator, Ipv4Translator {
    private static final Logger LOG = LoggerFactory.getLogger(DhcpRelayCustomizer.class);

    DhcpRelayCustomizer(final FutureJVppCore vppApi) {
        super(vppApi);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Relay> id, @Nonnull final Relay dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Writing Relay {} dataAfter={}", id, dataAfter);
        setRelay(id, dataAfter, writeContext, true);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Relay> id, @Nonnull final Relay dataBefore,
                                        @Nonnull final Relay dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Updating Relay {} before={} after={}", id, dataBefore, dataAfter);
        setRelay(id, dataAfter, writeContext, true);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Relay> id, @Nonnull final Relay dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Removing Relay {} dataBefore={}", id, dataBefore);
        setRelay(id, dataBefore, writeContext, false);
    }

    private void setRelay(final InstanceIdentifier<Relay> id, final Relay relay, final WriteContext writeContext,
                          final boolean isAdd) throws WriteFailedException {
        final DhcpProxyConfig request = new DhcpProxyConfig();
        request.rxVrfId = relay.getRxVrfId().byteValue();
        final boolean isIpv6 = Ipv6.class == relay.getAddressType();
        request.isIpv6 = booleanToByte(isIpv6);
        request.serverVrfId = relay.getServerVrfId().intValue();
        request.isAdd = booleanToByte(isAdd);
        request.dhcpServer = parseAddress(relay.getServerAddress(), isIpv6);
        request.dhcpSrcAddress = parseAddress(relay.getGatewayAddress(), isIpv6);
        getReplyForWrite(getFutureJVpp().dhcpProxyConfig(request).toCompletableFuture(), id);
    }

    private byte[] parseAddress(@Nonnull final IpAddress address, final boolean isIpv6) {
        if (isIpv6) {
            return ipv6AddressNoZoneToArray(address.getIpv6Address());
        } else {
            return ipv4AddressNoZoneToArray(address.getIpv4Address().getValue());
        }
    }
}
