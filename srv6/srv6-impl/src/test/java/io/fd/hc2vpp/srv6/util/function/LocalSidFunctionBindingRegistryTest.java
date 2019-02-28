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

package io.fd.hc2vpp.srv6.util.function;

import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.hc2vpp.srv6.util.JvppRequestTest;
import io.fd.hc2vpp.srv6.write.sid.request.LocalSidFunctionRequest;
import io.fd.hc2vpp.srv6.write.sid.request.NoProtocolLocalSidRequest;
import io.fd.hc2vpp.srv6.write.sid.request.TableLookupLocalSidRequest;
import io.fd.hc2vpp.srv6.write.sid.request.XConnectLocalSidRequest;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.jvpp.core.dto.SrLocalsidsDetails;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv4;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.multi.paths.v6.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.multi.paths.v6.paths.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.SidBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.End;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDt4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDt4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDt6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDt6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDx2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDx2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDx4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDx4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDx6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDx6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndT;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndTBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndX;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndXBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.PathAttrsCmn;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndDT4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndDT6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndDX2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndDX4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndDX6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.TableId;

public class LocalSidFunctionBindingRegistryTest extends JvppRequestTest {

    private static final Ipv6Address A = new Ipv6Address("A::101");
    private static final Ipv6AddressNoZone A_NO_ZONE = new Ipv6AddressNoZone("a::101");
    private static final Ipv4Address V4HOP = new Ipv4Address("0.0.1.0");
    private static final Ipv4AddressNoZone A_V4 = new Ipv4AddressNoZone("10.0.0.1");
    private static final String LOCAL_0 = "local0";
    private static final TableId TABLE_ID_4 = new TableId(4L);
    private static final TableKey TABLE_4_IPV6_KEY = new TableKey(Ipv6.class, new VniReference(TABLE_ID_4.getValue()));
    private static final TableKey TABLE_4_IPV4_KEY = new TableKey(Ipv4.class, new VniReference(TABLE_ID_4.getValue()));

    @Mock
    private ReadContext readCtx;

    @Override
    protected void init() {
        MockitoAnnotations.initMocks(this);
        defineMapping(mappingContext, "local0", 1, "interface-context");
        defineMapping(mappingContext, "vlan0", 2, "interface-context");
        when(ctx.getMappingContext()).thenReturn(mappingContext);
        when(readCtx.getMappingContext()).thenReturn(mappingContext);
        when(ctx.readAfter(FibManagementIIds.FM_FIB_TABLES.child(Table.class, TABLE_4_IPV6_KEY)))
                .thenReturn(Optional.of(
                        new TableBuilder().setTableId(TABLE_4_IPV6_KEY.getTableId()).withKey(TABLE_4_IPV6_KEY)
                                .setAddressFamily(TABLE_4_IPV6_KEY.getAddressFamily()).build()));
        when(ctx.readAfter(FibManagementIIds.FM_FIB_TABLES.child(Table.class, TABLE_4_IPV4_KEY)))
                .thenReturn(Optional.of(
                        new TableBuilder().setTableId(TABLE_4_IPV4_KEY.getTableId()).withKey(TABLE_4_IPV4_KEY)
                                .setAddressFamily(TABLE_4_IPV4_KEY.getAddressFamily()).build()));
    }

    @Test
    public void testEnd() {
        End end = new EndBuilder().build();
        Sid localSid = new SidBuilder()
                .setEnd(end)
                .setEndBehaviorType(
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.End.class)
                .build();
        LocalSidFunctionRequest request = WRITE_REGISTRY.bind(localSid, ctx);
        Assert.assertTrue(request instanceof NoProtocolLocalSidRequest);
        Assert.assertEquals(1, request.getFunction());
    }

    @Test
    public void testEndVpp() {
        SrLocalsidsDetails details = new SrLocalsidsDetails();
        details.behavior = 1;
        details.endPsp = 0;
        SidBuilder builder = new SidBuilder();
        READ_REGISTRY.bind(details, readCtx, builder);
        Assert.assertNotNull(builder.getEnd());
    }

    @Test
    public void testEndX() {
        EndX endX = new EndXBuilder()
                .setPaths(new PathsBuilder().setPath(Collections.singletonList(
                        new PathBuilder().setRole(PathAttrsCmn.Role.PRIMARY)
                                .setWeight(1L)
                                .setPathIndex((short) 1)
                                .setInterface(LOCAL_0)
                                .setNextHop(A)
                                .build())).build())
                .build();
        Sid localSid = new SidBuilder()
                .setEndX(endX)
                .setEndBehaviorType(
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndX.class)
                .build();
        LocalSidFunctionRequest request = WRITE_REGISTRY.bind(localSid, ctx);
        Assert.assertTrue(request instanceof XConnectLocalSidRequest);
        Assert.assertEquals(2, request.getFunction());
        XConnectLocalSidRequest xConnectRequest = XConnectLocalSidRequest.class.cast(request);
        Assert.assertEquals(A, xConnectRequest.getNextHopAddress().getIpv6Address());
        Assert.assertEquals(1, xConnectRequest.getOutgoingInterfaceIndex());
    }

    @Test
    public void testEndXVpp() {
        SrLocalsidsDetails details = new SrLocalsidsDetails();
        details.behavior = 2;
        details.endPsp = 0;
        details.xconnectNhAddr6 = AddressTranslator.INSTANCE.ipv6AddressNoZoneToArray(A);
        details.xconnectIfaceOrVrfTable = 1;
        SidBuilder builder = new SidBuilder();
        READ_REGISTRY.bind(details, readCtx, builder);
        Assert.assertNotNull(builder.getEndX());
        Assert.assertEquals(LOCAL_0, builder.getEndX().getPaths().getPath().get(0).getInterface());
        Assert.assertEquals(A_NO_ZONE, builder.getEndX().getPaths().getPath().get(0).getNextHop());
    }

    @Test
    public void testEndDX2() {
        EndDx2 endDx2 = new EndDx2Builder().setPaths(
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.end.dx2.PathsBuilder()
                        .setInterface(LOCAL_0)
                        .build()).build();
        Sid localSid = new SidBuilder()
                .setEndDx2(endDx2)
                .setEndBehaviorType(EndDX2.class)
                .build();
        LocalSidFunctionRequest request = WRITE_REGISTRY.bind(localSid, ctx);
        Assert.assertTrue(request instanceof XConnectLocalSidRequest);
        Assert.assertEquals(5, request.getFunction());
        XConnectLocalSidRequest xConnectRequest = XConnectLocalSidRequest.class.cast(request);
        Assert.assertNull(xConnectRequest.getNextHopAddress());
        Assert.assertEquals(1, xConnectRequest.getOutgoingInterfaceIndex());
    }

    @Test
    public void testEndDX2Vpp() {
        SrLocalsidsDetails details = new SrLocalsidsDetails();
        details.behavior = 5;
        details.xconnectIfaceOrVrfTable = 1;
        SidBuilder builder = new SidBuilder();
        READ_REGISTRY.bind(details, readCtx, builder);
        Assert.assertNotNull(builder.getEndDx2());
        Assert.assertEquals(LOCAL_0, builder.getEndDx2().getPaths().getInterface());
    }

    @Test
    public void testEndDX6() {
        EndDx6 endDx6 = new EndDx6Builder()
                .setPaths(new PathsBuilder()
                        .setPath(Collections.singletonList(new PathBuilder()
                                .setNextHop(A)
                                .setInterface(LOCAL_0)
                                .build()))
                        .build())
                .build();
        Sid localSid = new SidBuilder()
                .setEndDx6(endDx6)
                .setEndBehaviorType(EndDX6.class)
                .build();
        LocalSidFunctionRequest request = WRITE_REGISTRY.bind(localSid, ctx);
        Assert.assertTrue(request instanceof XConnectLocalSidRequest);
        Assert.assertEquals(6, request.getFunction());
        XConnectLocalSidRequest xConnectRequest = XConnectLocalSidRequest.class.cast(request);
        Assert.assertEquals(A, xConnectRequest.getNextHopAddress().getIpv6Address());
        Assert.assertEquals(1, xConnectRequest.getOutgoingInterfaceIndex());
    }

    @Test
    public void testEndDX6Vpp() {
        SrLocalsidsDetails details = new SrLocalsidsDetails();
        details.behavior = 6;
        details.xconnectIfaceOrVrfTable = 1;
        details.xconnectNhAddr6 = AddressTranslator.INSTANCE.ipv6AddressNoZoneToArray(A);
        SidBuilder builder = new SidBuilder();
        READ_REGISTRY.bind(details, readCtx, builder);
        Assert.assertNotNull(builder.getEndDx6());
        Assert.assertEquals(LOCAL_0, builder.getEndDx6().getPaths().getPath().get(0).getInterface());
        Assert.assertEquals(A_NO_ZONE, builder.getEndDx6().getPaths().getPath().get(0).getNextHop());
    }

    @Test
    public void testEndDX4() {
        EndDx4 endDx4 = new EndDx4Builder()
                .setPaths(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.multi.paths.v4.PathsBuilder()
                                .setPath(Collections.singletonList(
                                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.multi.paths.v4.paths.PathBuilder()
                                                .setNextHop(V4HOP)
                                                .setInterface(LOCAL_0)
                                                .build()))
                                .build())
                .build();
        Sid localSid = new SidBuilder()
                .setEndDx4(endDx4)
                .setEndBehaviorType(EndDX4.class)
                .build();
        LocalSidFunctionRequest request = WRITE_REGISTRY.bind(localSid, ctx);
        Assert.assertTrue(request instanceof XConnectLocalSidRequest);
        Assert.assertEquals(7, request.getFunction());
        XConnectLocalSidRequest xConnectRequest = XConnectLocalSidRequest.class.cast(request);
        Assert.assertEquals(V4HOP, xConnectRequest.getNextHopAddress().getIpv4Address());
        Assert.assertEquals(1, xConnectRequest.getOutgoingInterfaceIndex());
    }

    @Test
    public void testEndDX4Vpp() {
        SrLocalsidsDetails details = new SrLocalsidsDetails();
        details.behavior = 7;
        details.xconnectIfaceOrVrfTable = 1;
        details.xconnectNhAddr4 = AddressTranslator.INSTANCE.ipv4AddressNoZoneToArray(A_V4);
        SidBuilder builder = new SidBuilder();
        READ_REGISTRY.bind(details, readCtx, builder);
        Assert.assertNotNull(builder.getEndDx4());
        Assert.assertEquals(LOCAL_0, builder.getEndDx4().getPaths().getPath().get(0).getInterface());
        Assert.assertEquals(A_V4, builder.getEndDx4().getPaths().getPath().get(0).getNextHop());
    }

    @Test
    public void testEndT() {
        EndT endT = new EndTBuilder().setLookupTableIpv6(TABLE_ID_4).build();
        Sid localSid = new SidBuilder()
                .setEndT(endT)
                .setEndBehaviorType(
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndT.class)
                .build();
        LocalSidFunctionRequest request = WRITE_REGISTRY.bind(localSid, ctx);
        Assert.assertTrue(request instanceof TableLookupLocalSidRequest);
        Assert.assertEquals(3, request.getFunction());
        TableLookupLocalSidRequest tableLookupRequest = TableLookupLocalSidRequest.class.cast(request);
        Assert.assertEquals(TABLE_ID_4.getValue().intValue(), tableLookupRequest.getLookupFibTable());
    }

    @Test
    public void testEndDTVpp() {
        SrLocalsidsDetails details = new SrLocalsidsDetails();
        details.behavior = 3;
        details.xconnectIfaceOrVrfTable = TABLE_ID_4.getValue().intValue();
        SidBuilder builder = new SidBuilder();
        READ_REGISTRY.bind(details, readCtx, builder);
        Assert.assertNotNull(builder.getEndT());
        Assert.assertEquals(TABLE_ID_4.getValue().intValue(),
                builder.getEndT().getLookupTableIpv6().getValue().intValue());
    }

    @Test
    public void testEndDT6() {
        EndDt6 endDt6 = new EndDt6Builder().setLookupTableIpv6(TABLE_ID_4).build();
        Sid localSid = new SidBuilder()
                .setEndDt6(endDt6)
                .setEndBehaviorType(EndDT6.class)
                .build();
        LocalSidFunctionRequest request = WRITE_REGISTRY.bind(localSid, ctx);
        Assert.assertTrue(request instanceof TableLookupLocalSidRequest);
        Assert.assertEquals(8, request.getFunction());
        TableLookupLocalSidRequest tableLookupRequest = TableLookupLocalSidRequest.class.cast(request);
        Assert.assertEquals(TABLE_ID_4.getValue().intValue(), tableLookupRequest.getLookupFibTable());
    }

    @Test
    public void testEndDT6Vpp() {
        SrLocalsidsDetails details = new SrLocalsidsDetails();
        details.behavior = 8;
        details.xconnectIfaceOrVrfTable = TABLE_ID_4.getValue().intValue();
        SidBuilder builder = new SidBuilder();
        READ_REGISTRY.bind(details, readCtx, builder);
        Assert.assertNotNull(builder.getEndDt6());
        Assert.assertEquals(TABLE_ID_4.getValue().intValue(),
                builder.getEndDt6().getLookupTableIpv6().getValue().intValue());
    }

    @Test
    public void testEndDT4() {
        EndDt4 endDt4 = new EndDt4Builder().setLookupTableIpv4(TABLE_ID_4).build();
        Sid localSid = new SidBuilder()
                .setEndDt4(endDt4)
                .setEndBehaviorType(EndDT4.class)
                .build();
        LocalSidFunctionRequest request = WRITE_REGISTRY.bind(localSid, ctx);
        Assert.assertTrue(request instanceof TableLookupLocalSidRequest);
        Assert.assertEquals(9, request.getFunction());
        TableLookupLocalSidRequest tableLookupRequest = TableLookupLocalSidRequest.class.cast(request);
        Assert.assertEquals(TABLE_ID_4.getValue().intValue(), tableLookupRequest.getLookupFibTable());
    }

    @Test
    public void testEndDT4Vpp() {
        SrLocalsidsDetails details = new SrLocalsidsDetails();
        details.behavior = 9;
        details.xconnectIfaceOrVrfTable = TABLE_ID_4.getValue().intValue();
        SidBuilder builder = new SidBuilder();
        READ_REGISTRY.bind(details, readCtx, builder);
        Assert.assertNotNull(builder.getEndDt4());
        Assert.assertEquals(TABLE_ID_4.getValue().intValue(),
                builder.getEndDt4().getLookupTableIpv4().getValue().intValue());
    }
}
