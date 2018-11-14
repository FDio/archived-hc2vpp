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

package io.fd.hc2vpp.ipsec.read;

import com.google.common.base.Optional;
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
import io.fd.vpp.jvpp.core.dto.IpsecSpdDetails;
import io.fd.vpp.jvpp.core.dto.IpsecSpdDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.IpsecSpdDump;
import io.fd.vpp.jvpp.core.dto.IpsecSpdsDetails;
import io.fd.vpp.jvpp.core.dto.IpsecSpdsDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.IpsecSpdsDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecStateSpdAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.ipsec.state.Spd;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.ipsec.state.SpdBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.ipsec.state.SpdKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.ipsec.state.spd.SpdEntries;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.ipsec.state.spd.SpdEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IpsecSpdOperation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IpsecTrafficDirection;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpsecStateSpdCustomizer extends FutureJVppCustomizer
        implements JvppReplyConsumer, InitializingListReaderCustomizer<Spd, SpdKey, SpdBuilder>, Ipv4Translator,
        Ipv6Translator {

    private static final Logger LOG = LoggerFactory.getLogger(IpsecStateSpdCustomizer.class);
    private final DumpCacheManager<IpsecSpdDetailsReplyDump, Void> ipsecSpdDetailsReplyDumpManager;
    private final DumpCacheManager<IpsecSpdsDetailsReplyDump, Void> ipsecSpdsReplyDumpManager;

    public IpsecStateSpdCustomizer(final FutureJVppCore vppApi) {
        super(vppApi);
        IpsecStateSpdsReplyDumpExecutor spdsExecutor =
                new IpsecStateSpdCustomizer.IpsecStateSpdsReplyDumpExecutor(vppApi);
        this.ipsecSpdsReplyDumpManager = new DumpCacheManager.DumpCacheManagerBuilder<IpsecSpdsDetailsReplyDump, Void>()
                .withExecutor(spdsExecutor)
                .acceptOnly(IpsecSpdsDetailsReplyDump.class)
                .build();

        this.ipsecSpdDetailsReplyDumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<IpsecSpdDetailsReplyDump, Void>()
                        .withExecutor(
                                new IpsecStateSpdCustomizer.IpsecStateSpdDetailsDumpExecutor(vppApi, spdsExecutor))
                        .acceptOnly(IpsecSpdDetailsReplyDump.class)
                        .build();
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<Spd> id,
                                                  @Nonnull final Spd readValue,
                                                  @Nonnull final ReadContext ctx) {
        return Initialized.create(id, readValue);
    }

    @Nonnull
    @Override
    public SpdBuilder getBuilder(@Nonnull final InstanceIdentifier<Spd> id) {
        return new SpdBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Spd> id, @Nonnull final SpdBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        SpdKey key = id.firstKeyOf(Spd.class);
        builder.withKey(key);
        builder.setSpdId(key.getSpdId());
        Optional<IpsecSpdDetailsReplyDump> spdDump =
                ipsecSpdDetailsReplyDumpManager.getDump(id, ctx.getModificationCache());
        if (spdDump.isPresent()) {
            List<SpdEntries> spdEntries =
                    spdDump.get().ipsecSpdDetails.stream().map(details -> translateDetailToEntry(details))
                            .collect(Collectors.toList());
            builder.setSpdEntries(spdEntries);
        }
    }

    @Nonnull
    @Override
    public List<SpdKey> getAllIds(@Nonnull final InstanceIdentifier<Spd> id, @Nonnull final ReadContext context)
            throws ReadFailedException {
        List<SpdKey> spdKeys = new LinkedList<>();
        Optional<IpsecSpdsDetailsReplyDump> spdsDump =
                ipsecSpdsReplyDumpManager.getDump(id, context.getModificationCache());
        if (spdsDump.isPresent()) {
            spdKeys = spdsDump.get().ipsecSpdsDetails.stream().map(details -> new SpdKey(details.spdId))
                    .collect(Collectors.toList());
        }

        LOG.debug("SPDs found in VPP: {}", spdKeys);
        return spdKeys;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<Spd> readData) {
        ((IpsecStateSpdAugmentationBuilder) builder).setSpd(readData);
    }

    private SpdEntries translateDetailToEntry(final IpsecSpdDetails details) {

        SpdEntriesBuilder builder = new SpdEntriesBuilder();
        builder.setDirection(IpsecTrafficDirection.forValue(details.isOutbound))
                .setIsIpv6(ByteDataTranslator.INSTANCE.byteToBoolean(details.isIpv6))
                .setPriority(details.priority);
        switch (details.policy) {
            case 0:
                builder.setOperation(IpsecSpdOperation.Bypass);
                break;
            case 1:
                builder.setOperation(IpsecSpdOperation.Discard);
                break;
            case 3:
                builder.setOperation(IpsecSpdOperation.Protect);
                builder.setProtectSaId(details.saId);
                break;
        }

        if (builder.isIsIpv6()) {
            processIpv6AddressRanges(builder, details);
        } else {
            processIpv4AddressRanges(builder, details);
        }

        return builder.build();
    }

    private void processIpv4AddressRanges(final SpdEntriesBuilder builder, final IpsecSpdDetails details) {
        if (details.localStartAddr != null && details.localStartAddr.length > 0) {
            builder.setLaddrStart(IpAddressBuilder.getDefaultInstance(
                    new IpAddressNoZone(arrayToIpv4AddressNoZone(details.localStartAddr)).stringValue()));
        }
        if (details.localStopAddr != null && details.localStopAddr.length > 0) {
            builder.setLaddrStop(IpAddressBuilder.getDefaultInstance(
                    new IpAddressNoZone(arrayToIpv4AddressNoZone(details.localStopAddr)).stringValue()));
        }
        if (details.remoteStartAddr != null && details.remoteStartAddr.length > 0) {
            builder.setRaddrStart(IpAddressBuilder.getDefaultInstance(
                    new IpAddressNoZone(arrayToIpv4AddressNoZone(details.remoteStartAddr)).stringValue()));
        }
        if (details.remoteStopAddr != null && details.remoteStopAddr.length > 0) {
            builder.setRaddrStop(IpAddressBuilder.getDefaultInstance(
                    new IpAddressNoZone(arrayToIpv4AddressNoZone(details.remoteStopAddr)).stringValue()));
        }
    }

    private void processIpv6AddressRanges(final SpdEntriesBuilder builder, final IpsecSpdDetails details) {
        if (details.localStartAddr != null && details.localStartAddr.length > 0) {
            builder.setLaddrStart(IpAddressBuilder.getDefaultInstance(
                    new IpAddressNoZone(arrayToIpv6AddressNoZone(details.localStartAddr)).stringValue()));
        }
        if (details.localStopAddr != null && details.localStopAddr.length > 0) {
            builder.setLaddrStop(IpAddressBuilder.getDefaultInstance(
                    new IpAddressNoZone(arrayToIpv6AddressNoZone(details.localStopAddr)).stringValue()));
        }
        if (details.remoteStartAddr != null && details.remoteStartAddr.length > 0) {
            builder.setRaddrStart(IpAddressBuilder.getDefaultInstance(
                    new IpAddressNoZone(arrayToIpv6AddressNoZone(details.remoteStartAddr)).stringValue()));
        }
        if (details.remoteStopAddr != null && details.remoteStopAddr.length > 0) {
            builder.setRaddrStop(IpAddressBuilder.getDefaultInstance(
                    new IpAddressNoZone(arrayToIpv6AddressNoZone(details.remoteStopAddr)).stringValue()));
        }
    }

    public static class IpsecStateSpdDetailsDumpExecutor
            implements EntityDumpExecutor<IpsecSpdDetailsReplyDump, Void>, JvppReplyConsumer {
        private FutureJVppCore jvpp;
        private IpsecStateSpdsReplyDumpExecutor spdsDumpExecutor;

        public IpsecStateSpdDetailsDumpExecutor(
                final FutureJVppCore vppApi, final IpsecStateSpdsReplyDumpExecutor spdsDumpExecutor) {
            this.jvpp = vppApi;
            this.spdsDumpExecutor = spdsDumpExecutor;
        }

        @Nonnull
        @Override
        public IpsecSpdDetailsReplyDump executeDump(final InstanceIdentifier<?> identifier, final Void params)
                throws ReadFailedException {
            IpsecSpdDetailsReplyDump fullReplyDump = new IpsecSpdDetailsReplyDump();
            fullReplyDump.ipsecSpdDetails = new LinkedList<>();

            Optional<IpsecSpdsDetailsReplyDump> spdsReply =
                    Optional.of(spdsDumpExecutor.executeDump(identifier, params));
            IpsecSpdsDetailsReplyDump spdDump = spdsReply.get();
            for (IpsecSpdsDetails spdsDetail : spdDump.ipsecSpdsDetails) {
                IpsecSpdDump dump = new IpsecSpdDump();
                dump.spdId = spdsDetail.spdId;
                dump.saId = -1;
                IpsecSpdDetailsReplyDump reply =
                        getReplyForRead(jvpp.ipsecSpdDump(dump).toCompletableFuture(), identifier);
                fullReplyDump.ipsecSpdDetails.addAll(reply.ipsecSpdDetails);
            }

            return fullReplyDump;
        }
    }

    private class IpsecStateSpdsReplyDumpExecutor implements EntityDumpExecutor<IpsecSpdsDetailsReplyDump, Void> {
        private final FutureJVppCore jvpp;

        public IpsecStateSpdsReplyDumpExecutor(
                final FutureJVppCore vppApi) {
            this.jvpp = vppApi;
        }

        @Nonnull
        @Override
        public IpsecSpdsDetailsReplyDump executeDump(final InstanceIdentifier<?> identifier, final Void params)
                throws ReadFailedException {
            return getReplyForRead(jvpp.ipsecSpdsDump(new IpsecSpdsDump()).toCompletableFuture(), identifier);
        }
    }
}
