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

package io.fd.hc2vpp.lisp.gpe.translate.write;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeEntryIdentifier.fromEntry;
import static io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeLocatorPair.fromLocatorPair;
import static java.util.Objects.nonNull;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeEntryMappingContext;
import io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeLocatorPairMappingContext;
import io.fd.hc2vpp.lisp.gpe.translate.service.GpeStateCheckService;
import io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType;
import io.fd.hc2vpp.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.GpeAddDelFwdEntry;
import io.fd.vpp.jvpp.core.dto.GpeAddDelFwdEntryReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.types.GpeLocator;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocalEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocatorPairs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.locator.pair.LocatorPair;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GpeForwardEntryCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<GpeEntry, GpeEntryKey>, EidTranslator, JvppReplyConsumer {

    private final GpeStateCheckService gpeStateCheckService;
    private final GpeEntryMappingContext entryMappingCtx;
    private final GpeLocatorPairMappingContext locatorPairCtx;

    public GpeForwardEntryCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                     @Nonnull final GpeStateCheckService gpeStateCheckService,
                                     @Nonnull final GpeEntryMappingContext entryMappingCtx,
                                     @Nonnull final GpeLocatorPairMappingContext locatorPairCtx) {
        super(futureJVppCore);
        this.gpeStateCheckService = gpeStateCheckService;
        this.entryMappingCtx = entryMappingCtx;
        this.locatorPairCtx = locatorPairCtx;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<GpeEntry> id,
                                       @Nonnull final GpeEntry dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        gpeStateCheckService.checkGpeEnabledAfter(writeContext);
        getReplyForWrite(sendRequestAndMap(true, dataAfter, writeContext.getMappingContext()).toCompletableFuture(),
                id);
    }


    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<GpeEntry> id,
                                        @Nonnull final GpeEntry dataBefore,
                                        @Nonnull final GpeEntry dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        gpeStateCheckService.checkGpeEnabledAfter(writeContext);
        getReplyForDelete(sendRequestAndMap(false, dataBefore, writeContext.getMappingContext()).toCompletableFuture(),
                id);
        getReplyForUpdate(sendRequestAndMap(true, dataAfter, writeContext.getMappingContext()).toCompletableFuture(),
                id, dataBefore, dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<GpeEntry> id,
                                        @Nonnull final GpeEntry dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        gpeStateCheckService.checkGpeEnabledBefore(writeContext);
        getReplyForDelete(sendRequestAndMap(false, dataBefore, writeContext.getMappingContext()).toCompletableFuture(),
                id);
    }

    private CompletableFuture<GpeAddDelFwdEntryReply> sendRequestAndMap(final boolean add,
                                                                        final GpeEntry data,
                                                                        final MappingContext mappingContext) {
        final CompletableFuture<GpeAddDelFwdEntryReply> reply =
                getFutureJVpp().gpeAddDelFwdEntry(bindRequest(add, data)).toCompletableFuture();

        /*
         * sync to disallow synchronization issues
         */
        synchronized (entryMappingCtx) {
            synchronized (locatorPairCtx) {
                if (add) {
                    entryMappingCtx.addMapping(data.getId(), fromEntry(data), mappingContext);
                    Optional.ofNullable(data.getLocatorPairs()).orElse(Collections.emptyList()).forEach(
                            locatorPair -> locatorPairCtx
                                    .addMapping(data.getId(), locatorPair.getId(), fromLocatorPair(locatorPair),
                                            mappingContext));
                } else {
                    entryMappingCtx.removeMapping(data.getId(), mappingContext);
                    locatorPairCtx.removeMapping(data.getId(), mappingContext);
                }
            }
        }

        return reply;
    }

    private GpeAddDelFwdEntry bindRequest(final boolean add, @Nonnull final GpeEntry entry) {
        final GpeAddDelFwdEntry request = new GpeAddDelFwdEntry();
        request.isAdd = booleanToByte(add);
        request.vni = entry.getVni().byteValue();
        request.dpTable = entry.getDpTable().byteValue();

        final LocalEid localEid = Optional.ofNullable(entry.getLocalEid())
                .orElseThrow(() -> new IllegalArgumentException("Local eid cannot be null"));
        final RemoteEid remoteEid = Optional.ofNullable(entry.getRemoteEid())
                .orElseThrow(() -> new IllegalArgumentException("Remote eid cannot be null"));

        final EidType localEidType = getEidType(localEid);
        final EidType remoteEidType = getEidType(remoteEid);
        checkArgument(localEidType == remoteEidType, "Different eid type detected - Local[%s]/Remote[%s]",
                localEidType,
                remoteEidType);

        request.eidType = (byte) localEidType.getVppTypeBinding();
        request.lclEid = getEidAsByteArray(localEid);
        request.lclLen = getPrefixLength(localEid);

        request.rmtEid = getEidAsByteArray(remoteEid);
        request.rmtLen = getPrefixLength(remoteEid);

        if (nonNull(entry.getAction())) {
            request.action = (byte) entry.getAction().getIntValue();
        }

        if (nonNull(entry.getLocatorPairs())) {
            request.locs = toRequestLocators(entry.getLocatorPairs());
            request.locNum = request.locs.length;
        }

        return request;
    }


    // Locators vector must be ordered in way that local locators are first ,then remote.
    // Pair is translated to two locators, one(local) with local address and weight, second one(remote) with remote
    // address
    private GpeLocator[] toRequestLocators(final List<LocatorPairs> pairs) {
        return pairs.stream()
                .flatMap(locatorPairContainer -> {
                    final LocatorPair locatorPair =
                            checkNotNull(locatorPairContainer.getLocatorPair(), "Locator pair cannot be null");

                    final boolean isLocalIpv6 = isIpv6(locatorPair.getLocalLocator());
                    final boolean isRemoteIpv6 = isIpv6(locatorPair.getRemoteLocator());

                    checkArgument(isLocalIpv6 == isRemoteIpv6,
                            "Invalid combination for locator pair - Local[ipv6=%s]/Remote[ipv6=%s]", isLocalIpv6,
                            isRemoteIpv6);

                    GpeLocator localLocator = new GpeLocator();
                    localLocator.addr = ipAddressToArray(locatorPair.getLocalLocator());
                    localLocator.isIp4 = booleanToByte(!isLocalIpv6);
                    localLocator.weight = locatorPair.getWeight().byteValue();

                    GpeLocator remoteLocator = new GpeLocator();
                    remoteLocator.addr = ipAddressToArray(locatorPair.getRemoteLocator());
                    remoteLocator.isIp4 = booleanToByte(!isRemoteIpv6);

                    return Stream.of(localLocator, remoteLocator);
                })
                .sorted((first, second) -> {
                    if (first.weight == 0 && second.weight == 0) {
                        return 0;
                    } else if (first.weight == 0) {
                        return 1;
                    } else {
                        return -1;
                    }
                }).toArray(GpeLocator[]::new);
    }
}
