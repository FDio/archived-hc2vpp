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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Preconditions;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.attributes.interconnection.BridgeBasedBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.BridgeDomainDetails;
import org.openvpp.jvpp.dto.BridgeDomainDetailsReplyDump;
import org.openvpp.jvpp.dto.BridgeDomainDump;
import org.openvpp.jvpp.dto.BridgeDomainSwIfDetails;
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading ietf-interfaces:interfaces-state/interface/iface_name/v3po:l2
 */
public class L2Customizer extends FutureJVppCustomizer
        implements ChildReaderCustomizer<L2, L2Builder> {

    private static final Logger LOG = LoggerFactory.getLogger(L2Customizer.class);
    private final NamingContext interfaceContext;
    private final NamingContext bridgeDomainContext;

    public L2Customizer(@Nonnull final FutureJVpp futureJvpp,
                        @Nonnull final NamingContext interfaceContext,
                        @Nonnull final NamingContext bridgeDomainContext) {
        super(futureJvpp);
        this.interfaceContext = Preconditions.checkNotNull(interfaceContext, "interfaceContext should not be null");
        this.bridgeDomainContext = Preconditions.checkNotNull(bridgeDomainContext, "bridgeDomainContext should not be null");
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final L2 readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setL2(readValue);
    }

    @Nonnull
    @Override
    public L2Builder getBuilder(@Nonnull final InstanceIdentifier<L2> id) {
        return new L2Builder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2Builder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        LOG.debug("Reading attributes for L2: {}", id);
        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final int ifaceId = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());

        final SwInterfaceDetails iface = InterfaceUtils.getVppInterfaceDetails(getFutureJVpp(), key,
                ifaceId, ctx.getModificationCache());
        LOG.debug("Interface details for interface: {}, details: {}", key.getName(), iface);

        final BridgeDomainDetailsReplyDump dumpReply = getDumpReply();
        final Optional<BridgeDomainSwIfDetails> bdForInterface = getBridgeDomainForInterface(ifaceId, dumpReply);
        if (bdForInterface.isPresent()) {
            final BridgeDomainSwIfDetails bdSwIfDetails = bdForInterface.get();
            final BridgeBasedBuilder bbBuilder = new BridgeBasedBuilder();
            bbBuilder.setBridgeDomain(bridgeDomainContext.getName(bdSwIfDetails.bdId, ctx.getMappingContext()));

            // Set BVI if the bridgeDomainDetails.bviSwIfIndex == current sw if index
            final Optional<BridgeDomainDetails> bridgeDomainForInterface =
                getBridgeDomainForInterface(ifaceId, dumpReply, bdForInterface.get().bdId);
            // Since we already found an interface assigned to a bridge domain, the details for BD must be present
            checkState(bridgeDomainForInterface.isPresent());
            if(bridgeDomainForInterface.get().bviSwIfIndex == ifaceId) {
                bbBuilder.setBridgedVirtualInterface(true);
            }

            if (bdSwIfDetails.shg != 0) {
                bbBuilder.setSplitHorizonGroup((short)bdSwIfDetails.shg);
            }
            builder.setInterconnection(bbBuilder.build());
        }

        // TODO is there a way to check if interconnection is XconnectBased?
    }

    private Optional<BridgeDomainSwIfDetails> getBridgeDomainForInterface(final int ifaceId,
                                                                          final BridgeDomainDetailsReplyDump reply) {
        if (null == reply || null == reply.bridgeDomainSwIfDetails || reply.bridgeDomainSwIfDetails.isEmpty()) {
            return Optional.empty();
        }
        // interface can be added to only one BD only
        return reply.bridgeDomainSwIfDetails.stream().filter(a -> a.swIfIndex == ifaceId).findFirst();
    }


    private Optional<BridgeDomainDetails> getBridgeDomainForInterface(final int ifaceId,
                                                                      final BridgeDomainDetailsReplyDump reply,
                                                                      int bdId) {
        return reply.bridgeDomainDetails.stream().filter(a -> a.bdId == bdId).findFirst();
    }

    private BridgeDomainDetailsReplyDump getDumpReply() {
        // We need to perform full bd dump, because there is no way
        // to ask VPP for BD details given interface id/name (TODO add it to vpp.api?)
        // TODO cache dump result
        final BridgeDomainDump request = new BridgeDomainDump();
        request.bdId = -1;

        final CompletableFuture<BridgeDomainDetailsReplyDump> bdCompletableFuture =
                getFutureJVpp().bridgeDomainSwIfDump(request).toCompletableFuture();
        return TranslateUtils.getReply(bdCompletableFuture);
    }
}
