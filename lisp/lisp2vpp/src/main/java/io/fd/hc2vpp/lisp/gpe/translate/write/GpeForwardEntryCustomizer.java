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
import static java.util.Objects.nonNull;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.GpeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocalEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.locator.pairs.grouping.LocatorPair;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GpeForwardEntryCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<GpeEntry, GpeEntryKey>, EidTranslator, JvppReplyConsumer {

    private final GpeStateCheckService gpeStateCheckService;
    private final NamingContext entryMappingCtx;

    public GpeForwardEntryCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                     @Nonnull final GpeStateCheckService gpeStateCheckService,
                                     @Nonnull final NamingContext entryMappingCtx) {
        super(futureJVppCore);
        this.gpeStateCheckService = gpeStateCheckService;
        this.entryMappingCtx = entryMappingCtx;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<GpeEntry> id,
                                       @Nonnull final GpeEntry dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        gpeStateCheckService.checkGpeEnabledAfter(writeContext);
        final GpeAddDelFwdEntryReply replyForWrite =
                getReplyForWrite(sendRequestAndMap(true, dataAfter).toCompletableFuture(), id);
        addDelMapping(true, dataAfter, replyForWrite, writeContext.getMappingContext());
    }


    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<GpeEntry> id,
                                        @Nonnull final GpeEntry dataBefore,
                                        @Nonnull final GpeEntry dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        gpeStateCheckService.checkGpeEnabledAfter(writeContext);
        final GpeAddDelFwdEntryReply replyForDelete = getReplyForDelete(
                sendRequestAndMap(false, dataBefore).toCompletableFuture(), id);
        addDelMapping(false, dataBefore, replyForDelete, writeContext.getMappingContext());

        final GpeAddDelFwdEntryReply replyForUpdate = getReplyForUpdate(
                sendRequestAndMap(true, dataAfter).toCompletableFuture(), id, dataBefore, dataAfter);
        addDelMapping(true, dataAfter, replyForUpdate, writeContext.getMappingContext());
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<GpeEntry> id,
                                        @Nonnull final GpeEntry dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        gpeStateCheckService.checkGpeEnabledBefore(writeContext);
        final GpeAddDelFwdEntryReply replyForDelete =
                getReplyForDelete(sendRequestAndMap(false, dataBefore).toCompletableFuture(), id);
        addDelMapping(false, dataBefore, replyForDelete, writeContext.getMappingContext());
    }

    private CompletableFuture<GpeAddDelFwdEntryReply> sendRequestAndMap(final boolean add,
                                                                        final GpeEntry data) {
        return getFutureJVpp().gpeAddDelFwdEntry(bindRequest(add, data)).toCompletableFuture();
    }

    private void addDelMapping(final boolean add,
                            final GpeEntry data,
                            final GpeAddDelFwdEntryReply reply,
                            final MappingContext mappingContext){
         /*
         * sync to prevent synchronization issues
         */
        synchronized (entryMappingCtx) {
            if (add) {
                entryMappingCtx.addName(reply.fwdEntryIndex, data.getId(), mappingContext);
            } else {
                entryMappingCtx.removeName(data.getId(), mappingContext);
            }
        }
    }

    private GpeAddDelFwdEntry bindRequest(final boolean add, @Nonnull final GpeEntry entry) {
        final GpeAddDelFwdEntry request = new GpeAddDelFwdEntry();
        request.isAdd = booleanToByte(add);
        request.vni = entry.getVni().byteValue();
        request.dpTable = entry.getDpTable().byteValue();

        final RemoteEid remoteEid = Optional.ofNullable(entry.getRemoteEid())
                .orElseThrow(() -> new IllegalArgumentException("Remote eid cannot be null"));
        final EidType remoteEidType = getEidType(remoteEid);

        // for gpe entries, local eid does not have to be specified
        final LocalEid localEid = entry.getLocalEid();
        if (localEid != null) {
            final EidType localEidType = getEidType(localEid);
            checkArgument(localEidType == remoteEidType, "Different eid type detected - Local[%s]/Remote[%s]",
                    localEidType,
                    remoteEidType);
            request.lclEid = getEidAsByteArray(localEid);
            request.lclLen = getPrefixLength(localEid);
        }

        request.eidType = (byte) remoteEidType.getVppTypeBinding();
        request.rmtEid = getEidAsByteArray(remoteEid);
        request.rmtLen = getPrefixLength(remoteEid);

        if (nonNull(entry.getAction())) {
            request.action = (byte) entry.getAction().getIntValue();
        }

        if (nonNull(entry.getLocatorPair())) {
            request.locs = toRequestLocators(entry.getLocatorPair());
            request.locNum = request.locs.length;
        }

        return request;
    }


    // Locators vector must be ordered in way that local locators are first ,then remote.
    // Pair is translated to two locators, one(local) with local address and weight, second one(remote) with remote
    // address
    private GpeLocator[] toRequestLocators(final List<LocatorPair> pairs) {
        final List<GpeLocator> localLocators = pairs.stream()
                .map(locatorPair -> {
                    final boolean isLocalIpv6 = isIpv6(locatorPair.getLocalLocator());
                    final boolean isRemoteIpv6 = isIpv6(locatorPair.getRemoteLocator());

                    checkArgument(isLocalIpv6 == isRemoteIpv6,
                            "Invalid combination for locator pair - Local[ipv6=%s]/Remote[ipv6=%s]", isLocalIpv6,
                            isRemoteIpv6);

                    GpeLocator localLocator = new GpeLocator();
                    localLocator.addr = ipAddressToArray(locatorPair.getLocalLocator());
                    localLocator.isIp4 = booleanToByte(!isLocalIpv6);
                    localLocator.weight = locatorPair.getWeight().byteValue();
                    return localLocator;
                }).collect(Collectors.toList());

        final List<GpeLocator> remoteLocators = pairs.stream()
                .map(locatorPair -> {
                    final boolean isRemoteIpv6 = isIpv6(locatorPair.getRemoteLocator());

                    GpeLocator remoteLocator = new GpeLocator();
                    remoteLocator.addr = ipAddressToArray(locatorPair.getRemoteLocator());
                    remoteLocator.isIp4 = booleanToByte(!isRemoteIpv6);
                    return remoteLocator;
                }).collect(Collectors.toList());

        return Stream.of(localLocators, remoteLocators).flatMap(Collection::stream).toArray(GpeLocator[]::new);
    }
}
