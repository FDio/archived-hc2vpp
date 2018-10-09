/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.core.dto.BridgeDomainDetails;
import io.fd.vpp.jvpp.core.dto.BridgeDomainDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.BridgeDomainDump;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetails;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.types.BridgeDomainSwIf;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.l2.state.attributes.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.l2.state.attributes.interconnection.BridgeBasedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing Interconnection read support.
 */
final class InterconnectionReadUtils implements InterfaceDataTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(InterconnectionReadUtils.class);

    private final NamingContext interfaceContext;
    private final NamingContext bridgeDomainContext;
    private final InterfaceCacheDumpManager dumpManager;
    private final DumpCacheManager<BridgeDomainDetailsReplyDump,Void> bdDumpManager;

    InterconnectionReadUtils(@Nonnull final FutureJVppCore futureJVppCore,
                             @Nonnull final NamingContext interfaceContext,
                             @Nonnull final NamingContext bridgeDomainContext,
                             @Nonnull final InterfaceCacheDumpManager dumpManager) {
        requireNonNull(futureJVppCore, "futureJVppCore should not be null");
        this.interfaceContext = requireNonNull(interfaceContext, "interfaceContext should not be null");
        this.bridgeDomainContext = requireNonNull(bridgeDomainContext, "bridgeDomainContext should not be null");
        this.dumpManager = requireNonNull(dumpManager, "dumpManager should not be null");
        this.bdDumpManager = new DumpCacheManager.DumpCacheManagerBuilder<BridgeDomainDetailsReplyDump, Void>()
                .acceptOnly(BridgeDomainDetailsReplyDump.class)
                .withExecutor((id, params) -> {
                    final BridgeDomainDump request = new BridgeDomainDump();
                    request.bdId = -1;

                    final CompletableFuture<BridgeDomainDetailsReplyDump> bdCompletableFuture =
                            futureJVppCore.bridgeDomainDump(request).toCompletableFuture();
                    return getReplyForRead(bdCompletableFuture, id);
                })
                .build();
    }

    @Nullable
    Interconnection readInterconnection(@Nonnull final InstanceIdentifier<?> id, @Nonnull final String ifaceName,
                                        @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        final int ifaceId = interfaceContext.getIndex(ifaceName, ctx.getMappingContext());

        final SwInterfaceDetails iface = dumpManager.getInterfaceDetail(id, ctx, ifaceName);
        LOG.debug("Interface details for interface: {}, details: {}", ifaceName, iface);

        final BridgeDomainDetailsReplyDump dumpReply = bdDumpManager.getDump(id, ctx.getModificationCache())
                .or(new BridgeDomainDetailsReplyDump());
        for (final BridgeDomainDetails bd : dumpReply.bridgeDomainDetails) {
            final Optional<BridgeDomainSwIf> bdIfAssignment = getBridgeDomainSwIf(ifaceId, bd);
            if (bdIfAssignment.isPresent()) {
                final BridgeDomainSwIf bridgeDomainSwIf = bdIfAssignment.get();
                final BridgeBasedBuilder bbBuilder = new BridgeBasedBuilder();
                bbBuilder.setBridgeDomain(bridgeDomainContext.getName(bd.bdId, ctx.getMappingContext()));

                // Set BVI if the bridgeDomainDetails.bviSwIfIndex == current sw if index
                final Optional<BridgeDomainDetails> bridgeDomainForInterface =
                        getBridgeDomainForInterface(dumpReply, bd.bdId);
                // Since we already found an interface assigned to a bridge domain, the details for BD must be present
                checkState(bridgeDomainForInterface.isPresent());
                if (bridgeDomainForInterface.get().bviSwIfIndex == ifaceId) {
                    bbBuilder.setBridgedVirtualInterface(true);
                } else {
                    bbBuilder.setBridgedVirtualInterface(false);
                }

                if (bridgeDomainSwIf.shg != 0) {
                    bbBuilder.setSplitHorizonGroup((short) bridgeDomainSwIf.shg);
                }
                return bbBuilder.build();
            }
        }
        // TODO HONEYCOMB-190 is there a way to check if interconnection is XconnectBased?

        return null;
    }

    private Optional<BridgeDomainSwIf> getBridgeDomainSwIf(final int ifaceId, @Nonnull final BridgeDomainDetails bd) {
        if (null == bd.swIfDetails) {
            return Optional.empty();
        }
        // interface can be added to only one BD only
        return Arrays.stream(bd.swIfDetails).filter(el -> el.swIfIndex == ifaceId).findFirst();
    }

    private Optional<BridgeDomainDetails> getBridgeDomainForInterface(final BridgeDomainDetailsReplyDump reply,
                                                                      int bdId) {
        return reply.bridgeDomainDetails.stream().filter(a -> a.bdId == bdId).findFirst();
    }
}
