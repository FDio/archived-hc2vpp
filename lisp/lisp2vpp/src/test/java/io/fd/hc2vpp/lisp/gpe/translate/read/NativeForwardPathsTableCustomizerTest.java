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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.InitializingListReaderCustomizerTest;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.jvpp.core.dto.GpeNativeFwdRpathsGet;
import io.fd.jvpp.core.dto.Ip6FibDetails;
import io.fd.jvpp.core.dto.Ip6FibDetailsReplyDump;
import io.fd.jvpp.core.dto.IpFibDetails;
import io.fd.jvpp.core.dto.IpFibDetailsReplyDump;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.NativeForwardPathsTablesState;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.NativeForwardPathsTablesStateBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.state.NativeForwardPathsTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.state.NativeForwardPathsTableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.state.NativeForwardPathsTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NativeForwardPathsTableCustomizerTest extends
        InitializingListReaderCustomizerTest<NativeForwardPathsTable, NativeForwardPathsTableKey, NativeForwardPathsTableBuilder> {

    static final int TABLE_0_IDX = 1;
    static final int TABLE_1_IDX = 2;
    static final int TABLE_2_IDX = 3;

    InstanceIdentifier<NativeForwardPathsTable> validId;

    public NativeForwardPathsTableCustomizerTest() {
        super(NativeForwardPathsTable.class, NativeForwardPathsTablesStateBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        final GpeNativeFwdRpathsGet requestV4 = new GpeNativeFwdRpathsGet();
        requestV4.isIp4 = 0;
        final GpeNativeFwdRpathsGet requestV6 = new GpeNativeFwdRpathsGet();
        requestV6.isIp4 = 1;
        when(api.ipFibDump(any())).thenReturn(future(getReplyV4()));
        when(api.ip6FibDump(any())).thenReturn(future(getReplyV6()));
        validId = InstanceIdentifier.create(NativeForwardPathsTablesState.class)
                .child(NativeForwardPathsTable.class, new NativeForwardPathsTableKey((long) TABLE_0_IDX));
    }

    @Test
    public void testGetAll() throws ReadFailedException {
        final List<NativeForwardPathsTableKey> allIds = getCustomizer().getAllIds(validId, ctx);
        assertEquals(3, allIds.size());
        assertTrue(allIds.contains(new NativeForwardPathsTableKey((long) TABLE_0_IDX)));
        assertTrue(allIds.contains(new NativeForwardPathsTableKey((long) TABLE_1_IDX)));
        assertTrue(allIds.contains(new NativeForwardPathsTableKey((long) TABLE_2_IDX)));
    }

    @Test
    public void testReadCurrent() throws ReadFailedException {
        NativeForwardPathsTableBuilder builder = new NativeForwardPathsTableBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);
        final long lTableId = TABLE_0_IDX;
        assertEquals(lTableId, builder.getTableId().intValue());
        assertEquals(lTableId, builder.key().getTableId().intValue());
    }

    private IpFibDetailsReplyDump getReplyV4() {
        IpFibDetailsReplyDump reply = new IpFibDetailsReplyDump();
        IpFibDetails table0 = new IpFibDetails();
        table0.tableId = TABLE_0_IDX;
        IpFibDetails table2 = new IpFibDetails();
        table2.tableId = TABLE_2_IDX;
        reply.ipFibDetails = Arrays.asList(table0, table2);
        return reply;
    }

    private Ip6FibDetailsReplyDump getReplyV6() {
        Ip6FibDetailsReplyDump reply = new Ip6FibDetailsReplyDump();
        Ip6FibDetails table1 = new Ip6FibDetails();
        table1.tableId = TABLE_1_IDX;
        reply.ip6FibDetails = Arrays.asList(table1);
        return reply;
    }

    @Override
    protected ReaderCustomizer<NativeForwardPathsTable, NativeForwardPathsTableBuilder> initCustomizer() {
        return new NativeForwardPathsTableCustomizer(api);
    }
}
