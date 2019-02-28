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

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.InitializingListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.jvpp.core.dto.Ip6FibDetails;
import io.fd.jvpp.core.dto.Ip6FibDetailsReplyDump;
import io.fd.jvpp.core.dto.IpFibDetails;
import io.fd.jvpp.core.dto.IpFibDetailsReplyDump;
import io.fd.jvpp.core.types.FibPath;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv4;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.FibTablesBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FibTableCustomizerTest extends InitializingListReaderCustomizerTest<Table, TableKey, TableBuilder>
    implements AddressTranslator {

    private static final String IPV4_VRF_1 = "IPV4_VRF_1";
    private static final IpAddress NEXT_HOP_1 = new IpAddress(new Ipv6Address("a::1"));
    private static final IpAddress NEXT_HOP_2 = new IpAddress(new Ipv4Address("10.0.0.254"));
    private static final InstanceIdentifier<Table> TABLE_V4_ID =
            FibManagementIIds.FM_FIB_TABLES.child(Table.class, new TableKey(Ipv4.class, new VniReference(1L)));
    private static final InstanceIdentifier<Table> TABLE_V6_ID =
            FibManagementIIds.FM_FIB_TABLES.child(Table.class, new TableKey(Ipv6.class, new VniReference(1L)));
    private static final IpAddress IP_ADDR_1 = new IpAddress(new Ipv6Address("a::"));
    private static final IpAddress IP_ADDR_2 = new IpAddress(new Ipv4Address("10.0.0.1"));
    private DumpCacheManager<Ip6FibDetailsReplyDump, Void> manager_v6;
    private DumpCacheManager<IpFibDetailsReplyDump, Void> manager_v4;

    @Mock
    private EntityDumpExecutor<Ip6FibDetailsReplyDump, Void> executor_v6;

    @Mock
    private EntityDumpExecutor<IpFibDetailsReplyDump, Void> executor_v4;

    @Mock
    private ModificationCache cache;

    public FibTableCustomizerTest() {
        super(Table.class, FibTablesBuilder.class);
    }

    @Override
    public void setUp() throws ReadFailedException {
        manager_v6 = new DumpCacheManager.DumpCacheManagerBuilder<Ip6FibDetailsReplyDump, Void>()
                .withExecutor(executor_v6)
                .acceptOnly(Ip6FibDetailsReplyDump.class)
                .build();
        manager_v4 = new DumpCacheManager.DumpCacheManagerBuilder<IpFibDetailsReplyDump, Void>()
                .withExecutor(executor_v4)
                .acceptOnly(IpFibDetailsReplyDump.class)
                .build();

        when(executor_v6.executeDump(any(), any())).thenReturn(replyDumpV6());
        when(executor_v4.executeDump(any(), any())).thenReturn(replyDumpV4());
        when(ctx.getModificationCache()).thenReturn(cache);
    }

    private Ip6FibDetailsReplyDump replyDumpV6() {
        Ip6FibDetailsReplyDump replyDump = new Ip6FibDetailsReplyDump();

        //simple
        Ip6FibDetails ip6FibDetails = new Ip6FibDetails();
        ip6FibDetails.tableId = 1;
        ip6FibDetails.address = ipAddressToArray(IP_ADDR_1);
        ip6FibDetails.addressLength = 22;
        ip6FibDetails.path = new FibPath[]{};

        FibPath path = new FibPath();
        path.weight = 3;
        path.nextHop = ipAddressToArray(NEXT_HOP_1);
        path.afi = 0;
        path.swIfIndex = 1;
        ip6FibDetails.path = new FibPath[]{path};

        replyDump.ip6FibDetails = Collections.singletonList(ip6FibDetails);
        return replyDump;
    }

    private IpFibDetailsReplyDump replyDumpV4() {
        IpFibDetailsReplyDump replyDump = new IpFibDetailsReplyDump();

        //simple
        IpFibDetails detail = new IpFibDetails();
        detail.tableId = 1;
        detail.address = ipAddressToArray(IP_ADDR_2);
        detail.addressLength = 24;
        detail.tableName = IPV4_VRF_1.getBytes();
        detail.path = new FibPath[]{};

        FibPath path = new FibPath();
        path.weight = 3;
        path.nextHop = ipAddressToArray(NEXT_HOP_2);
        path.afi = 0;
        path.swIfIndex = 1;
        detail.path = new FibPath[]{path};

        replyDump.ipFibDetails = Collections.singletonList(detail);
        return replyDump;
    }

    @Test
    public void getAllIds() throws Exception {
        final List<TableKey> keys = getCustomizer().getAllIds(TABLE_V6_ID, ctx);

        assertThat(keys, hasSize(2));
        assertThat(keys, hasItems(new TableKey(Ipv6.class, new VniReference(1L)),
                new TableKey(Ipv4.class, new VniReference(1L))));
    }

    @Test
    public void readCurrentAttributesSimpleHop() throws Exception {
        TableBuilder builder = new TableBuilder();
        getCustomizer().readCurrentAttributes(TABLE_V6_ID, builder, ctx);

        Assert.assertEquals(Ipv6.class, builder.getAddressFamily());
        Assert.assertEquals(1L, builder.getTableId().getValue().longValue());
        Assert.assertNull(builder.getName());

        builder = new TableBuilder();
        getCustomizer().readCurrentAttributes(TABLE_V4_ID, builder, ctx);

        Assert.assertEquals(Ipv4.class, builder.getAddressFamily());
        Assert.assertEquals(1L, builder.getTableId().getValue().longValue());
        Assert.assertEquals(IPV4_VRF_1, builder.getName());
    }

    @Test
    public void testInit() {
        final Table data = new TableBuilder().build();
        invokeInitTest(TABLE_V4_ID, data, TABLE_V4_ID, data);
    }

    @Override
    protected ReaderCustomizer<Table, TableBuilder> initCustomizer() {
        return new FibTableCustomizer(manager_v4, manager_v6);
    }
}
