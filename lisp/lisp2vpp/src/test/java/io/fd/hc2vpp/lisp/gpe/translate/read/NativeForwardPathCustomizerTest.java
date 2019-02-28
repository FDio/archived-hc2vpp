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

import static io.fd.hc2vpp.lisp.gpe.translate.read.NativeForwardPathsTableCustomizerTest.TABLE_0_IDX;
import static io.fd.hc2vpp.lisp.gpe.translate.read.NativeForwardPathsTableCustomizerTest.TABLE_1_IDX;
import static io.fd.hc2vpp.lisp.gpe.translate.read.NativeForwardPathsTableCustomizerTest.TABLE_2_IDX;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.InitializingListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.jvpp.core.dto.GpeNativeFwdRpathsGet;
import io.fd.jvpp.core.dto.GpeNativeFwdRpathsGetReply;
import io.fd.jvpp.core.types.GpeNativeFwdRpath;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.NativeForwardPathsTablesState;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.state.NativeForwardPathsTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.state.NativeForwardPathsTableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.state.NativeForwardPathsTableKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.state._native.forward.paths.table.NativeForwardPath;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.state._native.forward.paths.table.NativeForwardPathBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.state._native.forward.paths.table.NativeForwardPathKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class NativeForwardPathCustomizerTest
        extends
        InitializingListReaderCustomizerTest<NativeForwardPath, NativeForwardPathKey, NativeForwardPathBuilder> {

    private static final String IFC_CTX = "ifc-ctx";
    private static final String ETH_0 = "eth-0";
    private static final String ETH_1 = "eth-1";
    private static final int ETH_0_IDX = 2;
    private static final int ETH_1_IDX = 7;

    private NamingContext interfaceContext;
    private KeyedInstanceIdentifier<NativeForwardPath, NativeForwardPathKey> validId;
    private KeyedInstanceIdentifier<NativeForwardPath, NativeForwardPathKey> defaultTableId;

    public NativeForwardPathCustomizerTest() {
        super(NativeForwardPath.class, NativeForwardPathsTableBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        interfaceContext = new NamingContext("iface", IFC_CTX);
        final GpeNativeFwdRpathsGet requestV4 = new GpeNativeFwdRpathsGet();
        requestV4.isIp4 = 1;
        final GpeNativeFwdRpathsGet requestV6 = new GpeNativeFwdRpathsGet();
        requestV6.isIp4 = 0;
        when(api.gpeNativeFwdRpathsGet(requestV4)).thenReturn(future(getReplyV4()));
        when(api.gpeNativeFwdRpathsGet(requestV6)).thenReturn(future(getReplyV6()));
        validId = InstanceIdentifier.create(NativeForwardPathsTablesState.class)
                .child(NativeForwardPathsTable.class, new NativeForwardPathsTableKey((long) TABLE_0_IDX))
                .child(NativeForwardPath.class,
                        new NativeForwardPathKey(new IpAddress(new Ipv4Address("192.168.2.1"))));
        defaultTableId = InstanceIdentifier.create(NativeForwardPathsTablesState.class)
                .child(NativeForwardPathsTable.class, new NativeForwardPathsTableKey((long) 0))
                .child(NativeForwardPath.class,
                        new NativeForwardPathKey(new IpAddress(new Ipv4Address("192.168.2.7"))));
        defineMapping(mappingContext, ETH_0, ETH_0_IDX, IFC_CTX);
        defineMapping(mappingContext, ETH_1, ETH_1_IDX, IFC_CTX);
    }

    @Test
    public void testGetAll() throws ReadFailedException {
        final List<NativeForwardPathKey> allIds = getCustomizer().getAllIds(validId, ctx);
        assertEquals(2, allIds.size());
        final String firstAddress = allIds.get(0).getNextHopAddress().getIpv6Address().getValue();
        final String secondAddress = allIds.get(1).getNextHopAddress().getIpv4Address().getValue();
        assertEquals("2001:db8:a0b:12f0::1", firstAddress);
        assertEquals("192.168.2.1", secondAddress);
    }

    @Test
    public void testGetAllDefaultTable() throws ReadFailedException {
        final List<NativeForwardPathKey> allIds = getCustomizer().getAllIds(defaultTableId, ctx);
        assertEquals(1, allIds.size());
        final String firstAddress = allIds.get(0).getNextHopAddress().getIpv4Address().getValue();
        assertEquals("192.168.3.7", firstAddress);
    }

    @Test
    public void testReadCurrent() throws ReadFailedException {
        final NativeForwardPathBuilder builder = new NativeForwardPathBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);

        assertEquals("192.168.2.1", builder.getNextHopAddress().getIpv4Address().getValue());
        assertEquals(ETH_1, builder.getNextHopInterface());
    }

    private GpeNativeFwdRpathsGetReply getReplyV4() {
        GpeNativeFwdRpathsGetReply reply = new GpeNativeFwdRpathsGetReply();
        GpeNativeFwdRpath table0Path1 = new GpeNativeFwdRpath();
        table0Path1.fibIndex = TABLE_0_IDX;
        table0Path1.nhAddr = new byte[]{-64, -88, 2, 1};
        table0Path1.isIp4 = 1;
        table0Path1.nhSwIfIndex = ETH_1_IDX;
        GpeNativeFwdRpath table2Path1 = new GpeNativeFwdRpath();
        table2Path1.fibIndex = TABLE_2_IDX;
        table2Path1.nhAddr = new byte[]{-64, -88, 3, 2};
        table2Path1.isIp4 = 1;
        GpeNativeFwdRpath tableDefaultPath1 = new GpeNativeFwdRpath();
        tableDefaultPath1.fibIndex = ~0;
        tableDefaultPath1.nhAddr = new byte[]{-64, -88, 3, 7};
        tableDefaultPath1.isIp4 = 1;

        reply.entries = new GpeNativeFwdRpath[]{table0Path1, table2Path1, tableDefaultPath1};
        return reply;
    }

    private GpeNativeFwdRpathsGetReply getReplyV6() {
        GpeNativeFwdRpathsGetReply reply = new GpeNativeFwdRpathsGetReply();
        GpeNativeFwdRpath table0Path2 = new GpeNativeFwdRpath();
        table0Path2.fibIndex = TABLE_0_IDX;
        table0Path2.nhAddr = new byte[]{32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 0, 1};
        table0Path2.isIp4 = 0;
        GpeNativeFwdRpath table1Path1 = new GpeNativeFwdRpath();
        table1Path1.fibIndex = TABLE_1_IDX;
        table1Path1.nhAddr = new byte[]{32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 3, 2};
        table1Path1.isIp4 = 0;
        table1Path1.nhSwIfIndex = ETH_0_IDX;
        reply.entries = new GpeNativeFwdRpath[]{table0Path2, table1Path1};
        return reply;
    }

    @Override
    protected ReaderCustomizer<NativeForwardPath, NativeForwardPathBuilder> initCustomizer() {
        return new NativeForwardPathCustomizer(api, interfaceContext);
    }
}
