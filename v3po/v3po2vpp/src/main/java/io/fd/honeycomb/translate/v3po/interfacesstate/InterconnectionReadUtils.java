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

package io.fd.honeycomb.translate.v3po.interfacesstate;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.BridgeBasedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.BridgeDomainDetails;
import org.openvpp.jvpp.dto.BridgeDomainDetailsReplyDump;
import org.openvpp.jvpp.dto.BridgeDomainDump;
import org.openvpp.jvpp.dto.BridgeDomainSwIfDetails;
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing Interconnection read support.
 */
// FIXME this should be customizer, but it is not possible because Interconnection is not a DataObject
final class InterconnectionReadUtils {

    private static final Logger LOG = LoggerFactory.getLogger(InterconnectionReadUtils.class);

    private final FutureJVpp futureJvpp;
    private final NamingContext interfaceContext;
    private final NamingContext bridgeDomainContext;

    InterconnectionReadUtils(@Nonnull final FutureJVpp futureJvpp,
                             @Nonnull final NamingContext interfaceContext,
                             @Nonnull final NamingContext bridgeDomainContext) {
        this.futureJvpp = requireNonNull(futureJvpp, "futureJvpp should not be null");
        this.interfaceContext = requireNonNull(interfaceContext, "interfaceContext should not be null");
        this.bridgeDomainContext = requireNonNull(bridgeDomainContext, "bridgeDomainContext should not be null");
    }

    @Nullable
    Interconnection readInterconnection(@Nonnull final InstanceIdentifier<?> id, @Nonnull final String ifaceName,
                                        @Nonnull final ReadContext ctx)
        throws ReadFailedException {
        final int ifaceId = interfaceContext.getIndex(ifaceName, ctx.getMappingContext());

        final SwInterfaceDetails iface = InterfaceUtils.getVppInterfaceDetails(futureJvpp, id, ifaceName,
            ifaceId, ctx.getModificationCache());
        LOG.debug("Interface details for interface: {}, details: {}", ifaceName, iface);

        final BridgeDomainDetailsReplyDump dumpReply = getDumpReply(id);
        final Optional<BridgeDomainSwIfDetails> bdForInterface = getBridgeDomainForInterface(ifaceId, dumpReply);
        if (bdForInterface.isPresent()) {
            final BridgeDomainSwIfDetails bdSwIfDetails = bdForInterface.get();
            final BridgeBasedBuilder bbBuilder = new BridgeBasedBuilder();
            bbBuilder.setBridgeDomain(bridgeDomainContext.getName(bdSwIfDetails.bdId, ctx.getMappingContext()));

            // Set BVI if the bridgeDomainDetails.bviSwIfIndex == current sw if index
            final Optional<BridgeDomainDetails> bridgeDomainForInterface =
                getBridgeDomainForInterface(dumpReply, bdForInterface.get().bdId);
            // Since we already found an interface assigned to a bridge domain, the details for BD must be present
            checkState(bridgeDomainForInterface.isPresent());
            if (bridgeDomainForInterface.get().bviSwIfIndex == ifaceId) {
                bbBuilder.setBridgedVirtualInterface(true);
            } else {
                bbBuilder.setBridgedVirtualInterface(false);
            }

            if (bdSwIfDetails.shg != 0) {
                bbBuilder.setSplitHorizonGroup((short) bdSwIfDetails.shg);
            }
            return bbBuilder.build();
        }
        // TODO is there a way to check if interconnection is XconnectBased?

        return null;
    }

    private Optional<BridgeDomainSwIfDetails> getBridgeDomainForInterface(final int ifaceId,
                                                                          final BridgeDomainDetailsReplyDump reply) {
        if (null == reply || null == reply.bridgeDomainSwIfDetails || reply.bridgeDomainSwIfDetails.isEmpty()) {
            return Optional.empty();
        }
        // interface can be added to only one BD only
        return reply.bridgeDomainSwIfDetails.stream().filter(a -> a.swIfIndex == ifaceId).findFirst();
    }

    private Optional<BridgeDomainDetails> getBridgeDomainForInterface(final BridgeDomainDetailsReplyDump reply,
                                                                      int bdId) {
        return reply.bridgeDomainDetails.stream().filter(a -> a.bdId == bdId).findFirst();
    }

    private BridgeDomainDetailsReplyDump getDumpReply(@Nonnull final InstanceIdentifier<?> id)
        throws ReadFailedException {
        try {
            // We need to perform full bd dump, because there is no way
            // to ask VPP for BD details given interface id/name (TODO add it to vpp.api?)
            // TODO cache dump result
            final BridgeDomainDump request = new BridgeDomainDump();
            request.bdId = -1;

            final CompletableFuture<BridgeDomainDetailsReplyDump> bdCompletableFuture =
                futureJvpp.bridgeDomainSwIfDump(request).toCompletableFuture();
            return TranslateUtils.getReplyForRead(bdCompletableFuture, id);
        } catch (VppBaseCallException e) {
            throw new ReadFailedException(id, e);
        }
    }
}
