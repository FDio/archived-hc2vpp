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

import static io.fd.honeycomb.v3po.translate.v3po.interfacesstate.InterfaceUtils.vppPhysAddrToYang;
import static io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils.byteToBoolean;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.L2FibFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.L2FibForward;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.L2FibTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.L2FibTableDump;
import org.openvpp.jvpp.dto.L2FibTableEntry;
import org.openvpp.jvpp.dto.L2FibTableEntryReplyDump;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class L2FibEntryCustomizer extends FutureJVppCustomizer
    implements ListReaderCustomizer<L2FibEntry, L2FibEntryKey, L2FibEntryBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(L2FibEntryCustomizer.class);

    private static final Collector<L2FibTableEntry, ?, L2FibTableEntry> SINGLE_ITEM_COLLECTOR =
        RWUtils.singleItemCollector();

    private final NamingContext bdContext;
    private final NamingContext interfaceContext;

    public L2FibEntryCustomizer(@Nonnull final FutureJVpp futureJVpp, @Nonnull final NamingContext bdContext,
                                @Nonnull final NamingContext interfaceContext) {
        super(futureJVpp);
        this.bdContext = Preconditions.checkNotNull(bdContext, "bdContext should not be null");
        this.interfaceContext = Preconditions.checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<L2FibEntry> id,
                                      @Nonnull final L2FibEntryBuilder builder, @Nonnull final ReadContext ctx)
        throws ReadFailedException {

        final L2FibEntryKey key = id.firstKeyOf(id.getTargetType());
        final BridgeDomainKey bridgeDomainKey = id.firstKeyOf(BridgeDomain.class);
        final int bdId = bdContext.getIndex(bridgeDomainKey.getName(), ctx.getMappingContext());
        LOG.debug("Reading L2 FIB entry: key={}. bridgeDomainKey={}, bdId={}", key, bridgeDomainKey, bdId);

        try {
            // TODO use cached l2FibTable
            final L2FibTableEntry entry = dumpL2Fibs(bdId).stream().filter(e -> key.getPhysAddress()
                .equals(new PhysAddress(vppPhysAddrToYang(Longs.toByteArray(e.mac), 2))))
                .collect(SINGLE_ITEM_COLLECTOR);

            builder.setAction(byteToBoolean(entry.filterMac)
                ? L2FibFilter.class
                : L2FibForward.class);
            builder.setBridgedVirtualInterface(byteToBoolean(entry.bviMac));

            if (entry.swIfIndex != -1) {
                builder.setOutgoingInterface(interfaceContext.getName(entry.swIfIndex, ctx.getMappingContext()));
            }
            builder.setStaticConfig(byteToBoolean(entry.staticMac));
            builder.setPhysAddress(key.getPhysAddress());
            builder.setKey(key);
        } catch (Exception e) {
            throw new ReadFailedException(id, e);
        }
    }

    @Nonnull
    private List<L2FibTableEntry> dumpL2Fibs(final int bdId) throws VppBaseCallException {
        final L2FibTableDump l2FibRequest = new L2FibTableDump();
        l2FibRequest.bdId = bdId;

        final CompletableFuture<L2FibTableEntryReplyDump> l2FibTableDumpCompletableFuture =
            getFutureJVpp().l2FibTableDump(l2FibRequest).toCompletableFuture();

        final L2FibTableEntryReplyDump dump = TranslateUtils.getReply(l2FibTableDumpCompletableFuture);

        if (null == dump || null == dump.l2FibTableEntry) {
            return Collections.emptyList();
        } else {
            return dump.l2FibTableEntry;
        }
    }

    @Nonnull
    @Override
    public List<L2FibEntryKey> getAllIds(@Nonnull final InstanceIdentifier<L2FibEntry> id,
                                         @Nonnull final ReadContext ctx) throws ReadFailedException {
        final BridgeDomainKey bridgeDomainKey = id.firstKeyOf(BridgeDomain.class);
        final int bdId = bdContext.getIndex(bridgeDomainKey.getName(), ctx.getMappingContext());

        LOG.debug("Reading L2 FIB for bridge domain {} (bdId={})", bridgeDomainKey, bdId);
        try {
            return dumpL2Fibs(bdId).stream()
                .map(entry -> new L2FibEntryKey(new PhysAddress(vppPhysAddrToYang(Longs.toByteArray(entry.mac), 2)))
                ).collect(Collectors.toList());
        } catch (VppBaseCallException e) {
            throw new ReadFailedException(id, e);
        }
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<L2FibEntry> readData) {
        ((L2FibTableBuilder) builder).setL2FibEntry(readData);
    }

    @Nonnull
    @Override
    public L2FibEntryBuilder getBuilder(@Nonnull final InstanceIdentifier<L2FibEntry> id) {
        return new L2FibEntryBuilder();
    }
}
