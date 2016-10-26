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

package io.fd.honeycomb.translate.v3po.vppstate;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.core.dto.BridgeDomainDetails;
import io.fd.vpp.jvpp.core.dto.BridgeDomainDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.BridgeDomainDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BridgeDomainCustomizer extends FutureJVppCustomizer
        implements InitializingListReaderCustomizer<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder>, ByteDataTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomainCustomizer.class);
    private final NamingContext bdContext;

    public BridgeDomainCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                  @Nonnull final NamingContext bdContext) {
        super(futureJVppCore);
        this.bdContext = Preconditions.checkNotNull(bdContext, "bdContext should not be null");
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                      @Nonnull final BridgeDomainBuilder builder, @Nonnull final ReadContext context)
            throws ReadFailedException {
        LOG.debug("vppstate.BridgeDomainCustomizer.readCurrentAttributes: id={}, builderbuilder={}, context={}",
                id, builder, context);

        final BridgeDomainKey key = id.firstKeyOf(id.getTargetType());
        LOG.debug("vppstate.BridgeDomainCustomizer.readCurrentAttributes: key={}", key);

        final int bdId = bdContext.getIndex(key.getName(), context.getMappingContext());
        LOG.debug("vppstate.BridgeDomainCustomizer.readCurrentAttributes: bdId={}", bdId);

        BridgeDomainDetailsReplyDump reply;
        BridgeDomainDetails bridgeDomainDetails;
        final BridgeDomainDump request = new BridgeDomainDump();
        request.bdId = bdContext.getIndex(key.getName(), context.getMappingContext());
        try {
            reply = getFutureJVpp().bridgeDomainDump(request).toCompletableFuture().get();
            bridgeDomainDetails = Iterables.getOnlyElement(reply.bridgeDomainDetails);
        } catch (Exception e) {
            LOG.debug("Unable to read bridge domain: {}", key.getName(), e);
            return;
        }

        logBridgeDomainDetails(bridgeDomainDetails);

        builder.setName(key.getName());
        builder.setArpTermination(byteToBoolean(bridgeDomainDetails.arpTerm));
        builder.setFlood(byteToBoolean(bridgeDomainDetails.flood));
        builder.setForward(byteToBoolean(bridgeDomainDetails.forward));
        builder.setLearn(byteToBoolean(bridgeDomainDetails.learn));
        builder.setUnknownUnicastFlood(byteToBoolean(bridgeDomainDetails.uuFlood));
    }

    private void logBridgeDomainDetails(final BridgeDomainDetails bridgeDomainDetails) {
        LOG.debug("bridgeDomainDetails={}", bridgeDomainDetails);
        if (bridgeDomainDetails != null) {
            LOG.debug("bridgeDomainDetails.arpTerm={}", bridgeDomainDetails.arpTerm);
            LOG.debug("bridgeDomainDetails.bdId={}", bridgeDomainDetails.bdId);
            LOG.debug("bridgeDomainDetails.bviSwIfIndex={}", bridgeDomainDetails.bviSwIfIndex);
            LOG.debug("bridgeDomainDetails.flood={}", bridgeDomainDetails.flood);
            LOG.debug("bridgeDomainDetails.forward={}", bridgeDomainDetails.forward);
            LOG.debug("bridgeDomainDetails.learn={}", bridgeDomainDetails.learn);
            LOG.debug("bridgeDomainDetails.nSwIfs={}", bridgeDomainDetails.nSwIfs);
            LOG.debug("bridgeDomainDetails.uuFlood={}", bridgeDomainDetails.uuFlood);
        }
    }

    @Nonnull
    @Override
    public BridgeDomainBuilder getBuilder(@Nonnull final InstanceIdentifier<BridgeDomain> id) {
        return new BridgeDomainBuilder();
    }

    @Nonnull
    @Override
    public List<BridgeDomainKey> getAllIds(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                           @Nonnull final ReadContext context) throws ReadFailedException {
        final BridgeDomainDump request = new BridgeDomainDump();
        request.bdId = -1; // dump call

        BridgeDomainDetailsReplyDump reply;
        try {
            reply = getFutureJVpp().bridgeDomainDump(request).toCompletableFuture().get();
        } catch (Exception e) {
            throw new ReadFailedException(id, e);
        }

        if (reply == null || reply.bridgeDomainDetails == null) {
            return Collections.emptyList();
        }

        final int bIdsLength = reply.bridgeDomainDetails.size();
        LOG.debug("vppstate.BridgeDomainCustomizer.getAllIds: bIds.length={}", bIdsLength);
        if (bIdsLength == 0) {
            // No bridge domains
            return Collections.emptyList();
        }

        final List<BridgeDomainKey> allIds = new ArrayList<>(bIdsLength);
        for (BridgeDomainDetails detail : reply.bridgeDomainDetails) {
            logBridgeDomainDetails(detail);

            final String bName = bdContext.getName(detail.bdId, context.getMappingContext());
            LOG.debug("vppstate.BridgeDomainCustomizer.getAllIds: bName={}", bName);
            allIds.add(new BridgeDomainKey(bName));
        }

        return allIds;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder,
                      @Nonnull final List<BridgeDomain> readData) {
        ((BridgeDomainsBuilder) builder).setBridgeDomain(readData);
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.bridge.domains.BridgeDomain> init(
            @Nonnull final InstanceIdentifier<BridgeDomain> id,
            @Nonnull final BridgeDomain readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.bridge.domains.BridgeDomainBuilder()
                        .setName(readValue.getName())
                        .setLearn(readValue.isLearn())
                        .setUnknownUnicastFlood(readValue.isUnknownUnicastFlood())
                        .setArpTermination(readValue.isArpTermination())
                        .setFlood(readValue.isFlood())
                        .setForward(readValue.isForward())
                        .build());
    }

    static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.bridge.domains.BridgeDomain> getCfgId(
            final InstanceIdentifier<BridgeDomain> id) {
        return InstanceIdentifier.create(Vpp.class).child(BridgeDomains.class).child(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.bridge.domains.BridgeDomain.class,
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.bridge.domains.BridgeDomainKey(
                        id.firstKeyOf(BridgeDomain.class).getName()));
    }
}
