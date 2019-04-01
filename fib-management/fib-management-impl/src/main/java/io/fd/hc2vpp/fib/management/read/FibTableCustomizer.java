/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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


package io.fd.hc2vpp.fib.management.read;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.jvpp.core.dto.Ip6FibDetailsReplyDump;
import io.fd.jvpp.core.dto.IpFibDetailsReplyDump;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.AddressFamilyIdentity;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv4;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.FibTablesBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class FibTableCustomizer implements InitializingListReaderCustomizer<Table, TableKey, TableBuilder>, ByteDataTranslator {
    private final DumpCacheManager<IpFibDetailsReplyDump, Void> ipv4DumpManager;
    private final DumpCacheManager<Ip6FibDetailsReplyDump, Void> ipv6DumpManager;

    FibTableCustomizer(final DumpCacheManager<IpFibDetailsReplyDump, Void> ipv4DumpManager,
                       final DumpCacheManager<Ip6FibDetailsReplyDump, Void> ipv6DumpManager) {

        this.ipv4DumpManager = ipv4DumpManager;
        this.ipv6DumpManager = ipv6DumpManager;
    }

    @Nonnull
    @Override
    public List<TableKey> getAllIds(@Nonnull final InstanceIdentifier<Table> instanceIdentifier,
                                    @Nonnull final ReadContext readContext) throws ReadFailedException {
        return Stream.concat(ipv4DumpManager.getDump(instanceIdentifier, readContext.getModificationCache())
                        .orElse(new IpFibDetailsReplyDump())
                        .ipFibDetails.stream()
                        .filter(ipFibDetails -> ipFibDetails.tableId >= 0)
                        .map(ipFibDetails -> new TableKey(Ipv4.class, new VniReference((long) ipFibDetails.tableId)))
                        .distinct(),
                ipv6DumpManager.getDump(instanceIdentifier, readContext.getModificationCache())
                        .orElse(new Ip6FibDetailsReplyDump())
                        .ip6FibDetails.stream()
                        .filter(ip6FibDetails -> ip6FibDetails.tableId >= 0)
                        .map(ipFibDetails -> new TableKey(Ipv6.class, new VniReference((long) ipFibDetails.tableId))))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<Table> list) {
        ((FibTablesBuilder) builder).setTable(list);
    }

    @Nonnull
    @Override
    public TableBuilder getBuilder(@Nonnull final InstanceIdentifier<Table> instanceIdentifier) {
        return new TableBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Table> instanceIdentifier,
                                      @Nonnull final TableBuilder builder, @Nonnull final ReadContext readContext)
            throws ReadFailedException {
        TableKey tableKey = instanceIdentifier.firstKeyOf(Table.class);

        if (tableKey.getAddressFamily().equals(Ipv4.class)) {
            ipv4DumpManager.getDump(instanceIdentifier, readContext.getModificationCache())
                    .orElse(new IpFibDetailsReplyDump())
                    .ipFibDetails.stream()
                    .filter(ipFibDetails -> ipFibDetails.tableId == tableKey.getTableId().getValue().intValue())
                    .findFirst().ifPresent(
                    ipFibDetails -> parseFibDetails(ipFibDetails.tableId, ipFibDetails.tableName, Ipv4.class, builder));

        } else {
            ipv6DumpManager.getDump(instanceIdentifier, readContext.getModificationCache())
                    .orElse(new Ip6FibDetailsReplyDump())
                    .ip6FibDetails.stream()
                    .filter(ipFibDetails -> ipFibDetails.tableId == tableKey.getTableId().getValue().intValue())
                    .findFirst().ifPresent(
                    ipFibDetails -> parseFibDetails(ipFibDetails.tableId, ipFibDetails.tableName, Ipv6.class, builder));
        }
    }

    private void parseFibDetails(final Integer tableId, final byte[] tableName,
                                 final Class<? extends AddressFamilyIdentity> addressFamily,
                                 final TableBuilder builder) {
        builder.setAddressFamily(addressFamily)
                .setTableId(new VniReference(Integer.toUnsignedLong(tableId)));

        if (tableName != null) {
            // table name is optional
            builder.setName(toString(tableName));
        }
    }

    @Nonnull
    @Override
    public Initialized<Table> init(@Nonnull final InstanceIdentifier<Table> id,
                                   @Nonnull final Table readValue,
                                   @Nonnull final ReadContext ctx) {
        return Initialized.create(id, readValue);
    }
}
