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
import com.google.common.primitives.Longs;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.v3po.interfacesstate.InterfaceDataTranslator;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.core.dto.L2FibTableDump;
import io.fd.vpp.jvpp.core.dto.L2FibTableEntry;
import io.fd.vpp.jvpp.core.dto.L2FibTableEntryReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.L2FibFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.L2FibForward;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.fib.attributes.L2FibTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.fib.attributes.L2FibTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.fib.attributes.l2.fib.table.L2FibEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.fib.attributes.l2.fib.table.L2FibEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.fib.attributes.l2.fib.table.L2FibEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class L2FibEntryCustomizer extends FutureJVppCustomizer
        implements InitializingListReaderCustomizer<L2FibEntry, L2FibEntryKey, L2FibEntryBuilder>, ByteDataTranslator,
        InterfaceDataTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(L2FibEntryCustomizer.class);

    private static final Collector<L2FibTableEntry, ?, L2FibTableEntry> SINGLE_ITEM_COLLECTOR =
            RWUtils.singleItemCollector();

    private final NamingContext bdContext;
    private final NamingContext interfaceContext;

    public L2FibEntryCustomizer(@Nonnull final FutureJVppCore futureJVppCore, @Nonnull final NamingContext bdContext,
                                @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
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
            // TODO HONEYCOMB-186 use cached l2FibTable
            final L2FibTableEntry entry = dumpL2Fibs(id, bdId).stream().filter(e -> key.getPhysAddress()
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
    private List<L2FibTableEntry> dumpL2Fibs(final InstanceIdentifier<L2FibEntry> id, final int bdId)
            throws ReadFailedException {
        final L2FibTableDump l2FibRequest = new L2FibTableDump();
        l2FibRequest.bdId = bdId;

        final CompletableFuture<L2FibTableEntryReplyDump> l2FibTableDumpCompletableFuture =
                getFutureJVpp().l2FibTableDump(l2FibRequest).toCompletableFuture();

        final L2FibTableEntryReplyDump dump = getReplyForRead(l2FibTableDumpCompletableFuture, id);

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
        return dumpL2Fibs(id, bdId).stream()
                .map(entry -> new L2FibEntryKey(
                        new PhysAddress(vppPhysAddrToYang(Longs.toByteArray(entry.mac), 2))))
                .collect(Collectors.toList());
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

    @Override
    public Initialized<L2FibEntry> init(
            @Nonnull final InstanceIdentifier<L2FibEntry> id,
            @Nonnull final L2FibEntry readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                // Convert operational object to config. VPP does not support setting BVI (see v3po.yang)
                new L2FibEntryBuilder(readValue).setBridgedVirtualInterface(null).build());
    }

    static InstanceIdentifier<L2FibEntry> getCfgId(
            final @Nonnull InstanceIdentifier<L2FibEntry> id) {
        return BridgeDomainCustomizer.getCfgId(RWUtils.cutId(id, BridgeDomain.class)).child(
                L2FibTable.class).child(L2FibEntry.class, new L2FibEntryKey(id.firstKeyOf(L2FibEntry.class)));
    }
}
