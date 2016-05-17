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

package io.fd.honeycomb.v3po.translate.v3po.vppstate;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.L2Fib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.L2FibBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.L2FibKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.BridgeDomainDetails;
import org.openvpp.jvpp.dto.BridgeDomainDetailsReplyDump;
import org.openvpp.jvpp.dto.BridgeDomainDump;
import org.openvpp.jvpp.dto.BridgeDomainSwIfDetails;
import org.openvpp.jvpp.dto.L2FibTableDump;
import org.openvpp.jvpp.dto.L2FibTableEntry;
import org.openvpp.jvpp.dto.L2FibTableEntryReplyDump;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BridgeDomainCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomainCustomizer.class);
    private final NamingContext bdContext;
    private final NamingContext interfaceContext;

    public BridgeDomainCustomizer(@Nonnull final FutureJVpp futureJVpp, @Nonnull final NamingContext bdContext,
                                  @Nonnull final NamingContext interfaceContext) {
        super(futureJVpp);
        this.bdContext = Preconditions.checkNotNull(bdContext, "bdContext should not be null");
        this.interfaceContext = Preconditions.checkNotNull(interfaceContext, "interfaceContext should not be null");;
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

        builder.setInterface(getIfcs(bridgeDomainDetails, reply.bridgeDomainSwIfDetails, context));

        final L2FibTableDump l2FibRequest = new L2FibTableDump();
        l2FibRequest.bdId = bdId;
        try {
            final L2FibTableEntryReplyDump dump =
                    getFutureJVpp().l2FibTableDump(l2FibRequest).toCompletableFuture().get();
            final List<L2Fib> l2Fibs;

            if(null == dump || null == dump.l2FibTableEntry) {
                l2Fibs = Collections.emptyList();
            } else {
                l2Fibs = Lists.newArrayListWithCapacity(dump.l2FibTableEntry.size());
                for (L2FibTableEntry entry : dump.l2FibTableEntry) {
                    // entry.mac is a long value in the format 66:55:44:33:22:11:XX:XX
                    // where mac address is 11:22:33:44:55:66
                    final PhysAddress address = new PhysAddress(getMacAddress(Longs.toByteArray(entry.mac)));
                    l2Fibs.add(new L2FibBuilder()
                        .setAction((byteToBoolean(entry.filterMac)
                            ? L2Fib.Action.Filter
                            : L2Fib.Action.Forward))
                        .setBridgedVirtualInterface(byteToBoolean(entry.bviMac))
                        .setOutgoingInterface(interfaceContext.getName(entry.swIfIndex, context.getMappingContext()))
                        .setStaticConfig(byteToBoolean(entry.staticMac))
                        .setPhysAddress(address)
                        .setKey(new L2FibKey(address))
                        .build());
                }
            }
            builder.setL2Fib(l2Fibs);

        } catch (Exception e) {
            LOG.warn("Failed to acquire l2FibTableDump for domain id={}", bdId, e);
        }
    }

    // TODO move to utils
    private static Boolean byteToBoolean(final byte aByte) {
        if (aByte == 0) {
            return Boolean.FALSE;
        } else if (aByte == 1) {
            return Boolean.TRUE;
        }
        throw new IllegalArgumentException(String.format("0 or 1 was expected but was %d", aByte));
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

    // TODO move to some utility class
    private static String getMacAddress(byte[] mac) {
        StringBuilder sb = new StringBuilder(18);
        for (int i=5; i>=0; --i) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(String.format("%02x", mac[i]));
        }
        return sb.toString();
    }

    private List<Interface> getIfcs(final BridgeDomainDetails bridgeDomainDetails,
                                    final List<BridgeDomainSwIfDetails> bridgeDomainSwIfDetails,
                                    final ReadContext context) {
        final List<Interface> ifcs = new ArrayList<>(bridgeDomainSwIfDetails.size());
        for (BridgeDomainSwIfDetails anInterface : bridgeDomainSwIfDetails) {
            final String interfaceName = interfaceContext.getName(anInterface.swIfIndex, context.getMappingContext());
            if (anInterface.bdId == bridgeDomainDetails.bdId) {
                ifcs.add(new InterfaceBuilder()
                        .setBridgedVirtualInterface(bridgeDomainDetails.bviSwIfIndex == anInterface.swIfIndex)
                        .setSplitHorizonGroup((short) anInterface.shg)
                        .setName(interfaceName)
                        .setKey(new InterfaceKey(interfaceName))
                        .build());
            }


        }
        return ifcs;
    }

    @Nonnull
    @Override
    public BridgeDomainBuilder getBuilder(@Nonnull final InstanceIdentifier<BridgeDomain> id) {
        return new BridgeDomainBuilder();
    }

    @Nonnull
    @Override
    public List<BridgeDomainKey> getAllIds(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                           @Nonnull final ReadContext context) {
        final BridgeDomainDump request = new BridgeDomainDump();
        request.bdId = -1; // dump call

        BridgeDomainDetailsReplyDump reply;
        try {
            reply = getFutureJVpp().bridgeDomainDump(request).toCompletableFuture().get();
        } catch (Exception e) {
            throw new IllegalStateException("Bridge domain dump failed", e); // TODO ReadFailedException?
        }

        if(reply == null || reply.bridgeDomainDetails == null) {
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
}
