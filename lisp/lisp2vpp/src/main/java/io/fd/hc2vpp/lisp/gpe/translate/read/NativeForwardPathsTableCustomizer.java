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

package io.fd.hc2vpp.lisp.gpe.translate.read;

import com.google.common.primitives.UnsignedInts;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.vpp.jvpp.core.dto.Ip6FibDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.Ip6FibDump;
import io.fd.vpp.jvpp.core.dto.IpFibDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.IpFibDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.NativeForwardPathsTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.NativeForwardPathsTablesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables.state.NativeForwardPathsTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables.state.NativeForwardPathsTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables.state.NativeForwardPathsTableKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class NativeForwardPathsTableCustomizer extends FutureJVppCustomizer implements
        InitializingListReaderCustomizer<NativeForwardPathsTable, NativeForwardPathsTableKey, NativeForwardPathsTableBuilder>,
        JvppReplyConsumer {

    // no need to recreate, has no params
    private static final IpFibDump REQUEST_V4 = new IpFibDump();
    private static final Ip6FibDump REQUEST_V6 = new Ip6FibDump();
    private static final IpFibDetailsReplyDump DEFAULT_REPLY_V4 = new IpFibDetailsReplyDump();
    private static final Ip6FibDetailsReplyDump DEFAULT_REPLY_V6 = new Ip6FibDetailsReplyDump();
    private DumpCacheManager<IpFibDetailsReplyDump, Void> dumpCacheManagerV4;
    private DumpCacheManager<Ip6FibDetailsReplyDump, Void> dumpCacheManagerV6;

    public NativeForwardPathsTableCustomizer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);

        // there's no lisp specific dump for tables created by gpe_add_del_iface,
        // so have to use standard fib dump
        dumpCacheManagerV4 = new DumpCacheManagerBuilder<IpFibDetailsReplyDump, Void>()
                .acceptOnly(IpFibDetailsReplyDump.class)
                .withExecutor((identifier, params) -> getReplyForRead(
                        getFutureJVpp().ipFibDump(REQUEST_V4).toCompletableFuture(),
                        identifier)).build();

        dumpCacheManagerV6 = new DumpCacheManagerBuilder<Ip6FibDetailsReplyDump, Void>()
                .acceptOnly(Ip6FibDetailsReplyDump.class)
                .withExecutor((identifier, params) -> getReplyForRead(
                        getFutureJVpp().ip6FibDump(REQUEST_V6).toCompletableFuture(),
                        identifier)).build();
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<NativeForwardPathsTable> id,
                                                  @Nonnull final NativeForwardPathsTable readValue,
                                                  @Nonnull final ReadContext ctx) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTable
                cfgValue =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTableBuilder()
                        .setTableId(readValue.getTableId())
                        .build();

        final KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTable, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTableKey>
                cfgKey = InstanceIdentifier.create(NativeForwardPathsTables.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTable.class,
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTableKey(
                                cfgValue.key()));

        return Initialized.create(cfgKey, cfgValue);
    }

    @Nonnull
    @Override
    public List<NativeForwardPathsTableKey> getAllIds(@Nonnull final InstanceIdentifier<NativeForwardPathsTable> id,
                                                      @Nonnull final ReadContext context) throws ReadFailedException {

        return Stream.concat(v4FibsStream(id, context), v6FibsStream(id, context))
                .mapToLong(UnsignedInts::toLong)
                .distinct()
                .mapToObj(NativeForwardPathsTableKey::new)
                .collect(Collectors.toList());
    }


    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder,
                      @Nonnull final List<NativeForwardPathsTable> readData) {
        ((NativeForwardPathsTablesStateBuilder) builder).setNativeForwardPathsTable(readData);
    }

    @Nonnull
    @Override
    public NativeForwardPathsTableBuilder getBuilder(@Nonnull final InstanceIdentifier<NativeForwardPathsTable> id) {
        return new NativeForwardPathsTableBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<NativeForwardPathsTable> id,
                                      @Nonnull final NativeForwardPathsTableBuilder builder,
                                      @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        final Long tableId = id.firstKeyOf(NativeForwardPathsTable.class).getTableId();

        final OptionalLong optionalTable = Stream.concat(v4FibsStream(id, ctx), v6FibsStream(id, ctx))
                .mapToLong(UnsignedInts::toLong)
                .distinct()
                .filter(tblId -> tblId == tableId)
                .findAny();

        if (optionalTable.isPresent()) {
            final long existingTableId = optionalTable.getAsLong();
            builder.setTableId(existingTableId);
            builder.withKey(new NativeForwardPathsTableKey(existingTableId));
        }
    }

    private Stream<Integer> v4FibsStream(final InstanceIdentifier<NativeForwardPathsTable> id,
                                         final ReadContext ctx) throws ReadFailedException {

        return dumpCacheManagerV4.getDump(id, ctx.getModificationCache()).or(DEFAULT_REPLY_V4)
                .ipFibDetails.stream()
                .map(ipFibDetails -> ipFibDetails.tableId);
    }

    private Stream<Integer> v6FibsStream(final InstanceIdentifier<NativeForwardPathsTable> id,
                                         final ReadContext ctx) throws ReadFailedException {

        return dumpCacheManagerV6.getDump(id, ctx.getModificationCache()).or(DEFAULT_REPLY_V6)
                .ip6FibDetails.stream()
                .map(ip6FibDetails -> ip6FibDetails.tableId);
    }
}
