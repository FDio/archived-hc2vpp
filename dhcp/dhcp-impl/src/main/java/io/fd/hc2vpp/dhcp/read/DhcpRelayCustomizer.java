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

package io.fd.hc2vpp.dhcp.read;

import com.google.common.base.Optional;
import com.google.common.primitives.UnsignedInts;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.vpp.jvpp.core.dto.DhcpProxyDetails;
import io.fd.vpp.jvpp.core.dto.DhcpProxyDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.DhcpProxyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv4;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev180629.dhcp.attributes.RelaysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev180629.dhcp.attributes.relays.Relay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev180629.dhcp.attributes.relays.RelayBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev180629.dhcp.attributes.relays.RelayKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev180629.relay.attributes.ServerBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class DhcpRelayCustomizer extends FutureJVppCustomizer
    implements InitializingListReaderCustomizer<Relay, RelayKey, RelayBuilder>,
    JvppReplyConsumer, ByteDataTranslator, Ipv6Translator, Ipv4Translator {

    private final DumpCacheManager<DhcpProxyDetailsReplyDump, Void> dumpManager;

    DhcpRelayCustomizer(final FutureJVppCore vppApi) {
        super(vppApi);
        dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<DhcpProxyDetailsReplyDump, Void>()
            .withExecutor(executor())
            .acceptOnly(DhcpProxyDetailsReplyDump.class)
            .build();
    }

    private EntityDumpExecutor<DhcpProxyDetailsReplyDump, Void> executor() {
        return (id, param) -> {
            DhcpProxyDump request = new DhcpProxyDump();
            request.isIp6 = 1;

            final CompletionStage<DhcpProxyDetailsReplyDump> result = getFutureJVpp().dhcpProxyDump(new DhcpProxyDump())
                .thenCombine(getFutureJVpp().dhcpProxyDump(request),
                    (ip4, ip6) -> {
                        ip4.dhcpProxyDetails.addAll(ip6.dhcpProxyDetails);
                        return ip4;
                    });
            return getReplyForRead(result.toCompletableFuture(), id);
        };
    }

    @Nonnull
    @Override
    public List<RelayKey> getAllIds(@Nonnull final InstanceIdentifier<Relay> id, @Nonnull final ReadContext context)
        throws ReadFailedException {
        Collections.emptyList();

        final Optional<DhcpProxyDetailsReplyDump> dump =
            dumpManager.getDump(id, context.getModificationCache());

        if (!dump.isPresent() || dump.get().dhcpProxyDetails.isEmpty()) {
            return Collections.emptyList();
        }

        return dump.get().dhcpProxyDetails.stream().map(detail -> new RelayKey(detail.isIpv6 == 1
            ? Ipv6.class
            : Ipv4.class,
            new VniReference(UnsignedInts.toLong(detail.rxVrfId)))).collect(Collectors.toList());
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<Relay> readData) {
        ((RelaysBuilder) builder).setRelay(readData);
    }

    @Nonnull
    @Override
    public RelayBuilder getBuilder(@Nonnull final InstanceIdentifier<Relay> id) {
        return new RelayBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Relay> id, @Nonnull final RelayBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        final Optional<DhcpProxyDetailsReplyDump> dump = dumpManager.getDump(id, ctx.getModificationCache());

        if (!dump.isPresent() || dump.get().dhcpProxyDetails.isEmpty()) {
            return;
        }

        final RelayKey key = id.firstKeyOf(Relay.class);

        final byte isIpv6 = (byte) (Ipv6.class == key.getAddressFamily()
            ? 1
            : 0);
        final int rxVrfId = key.getRxVrfId().getValue().intValue();

        final java.util.Optional<DhcpProxyDetails> result =
            dump.get().dhcpProxyDetails.stream().filter(d -> d.isIpv6 == isIpv6 && d.rxVrfId == rxVrfId).findFirst();

        if (result.isPresent()) {
            final DhcpProxyDetails detail = result.get();
            builder.setAddressFamily(key.getAddressFamily());
            builder.setRxVrfId(key.getRxVrfId());
            final boolean isIp6 = byteToBoolean(detail.isIpv6);
            builder.setGatewayAddress(readAddress(detail.dhcpSrcAddress, isIp6));
            if (detail.servers != null) {
                builder.setServer(Arrays.stream(detail.servers).map(
                    server -> new ServerBuilder()
                        .setAddress(readAddress(server.dhcpServer, isIp6))
                        .setVrfId(UnsignedInts.toLong(server.serverVrfId))
                        .build()
                ).collect(Collectors.toList()));
            }
        }
    }

    private IpAddressNoZone readAddress(final byte[] ip, final boolean isIp6) {
        if (isIp6) {
            return new IpAddressNoZone(arrayToIpv6AddressNoZone(ip));
        } else {
            return new IpAddressNoZone(arrayToIpv4AddressNoZone(ip));
        }
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<Relay> id,
                                                  @Nonnull final Relay readValue,
                                                  @Nonnull final ReadContext ctx) {
        return Initialized.create(id, readValue);
    }
}
