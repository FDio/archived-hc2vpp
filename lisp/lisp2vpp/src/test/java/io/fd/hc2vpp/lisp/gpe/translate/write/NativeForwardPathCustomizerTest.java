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

package io.fd.hc2vpp.lisp.gpe.translate.write;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.GpeAddDelNativeFwdRpath;
import io.fd.jvpp.core.dto.GpeAddDelNativeFwdRpathReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.NativeForwardPathsTables;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTableKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPath;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPathBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPathKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NativeForwardPathCustomizerTest extends WriterCustomizerTest {

    private static final long TABLE_ID = 1L;
    private static final String IFC_CTX = "ifc-ctx";
    private static final String ETH_0 = "eth-0";
    private static final String ETH_1 = "eth-1";
    private static final int ETH_0_IDX = 2;
    private static final int ETH_1_IDX = 7;
    private static final byte[] V4_WITH_IF_ADDR = {-64, -88, 2, 1};
    private static final byte[] V4_WITHOUT_IF_ADDR = {-64, -88, 2, 3};
    private static final byte[] V6_WITH_IF_ADDR = {32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 0, 1};

    private NamingContext ifcCtx;
    private NativeForwardPathCustomizer customizer;
    private InstanceIdentifier<NativeForwardPath> validId;

    @Captor
    private ArgumentCaptor<GpeAddDelNativeFwdRpath> requestCaptor;

    @Override
    protected void setUpTest() throws Exception {
        ifcCtx = new NamingContext("iface", IFC_CTX);
        defineMapping(mappingContext, ETH_0, ETH_0_IDX, IFC_CTX);
        defineMapping(mappingContext, ETH_1, ETH_1_IDX, IFC_CTX);
        customizer = new NativeForwardPathCustomizer(api, ifcCtx);
        validId = InstanceIdentifier.create(NativeForwardPathsTables.class)
                .child(NativeForwardPathsTable.class, new NativeForwardPathsTableKey(TABLE_ID))
                .child(NativeForwardPath.class,
                        new NativeForwardPathKey(new IpAddress(new Ipv4Address("192.168.2.1"))));
        when(api.gpeAddDelNativeFwdRpath(any())).thenReturn(future(new GpeAddDelNativeFwdRpathReply()));
    }

    @Test
    public void testWriteV4WithIfc() throws WriteFailedException {
        customizer.writeCurrentAttributes(validId, v4WithIfc(), writeContext);
        verify(api, times(1)).gpeAddDelNativeFwdRpath(requestCaptor.capture());
        final GpeAddDelNativeFwdRpath request = requestCaptor.getValue();
        assertEquals(desiredRequest(1, 1, V4_WITH_IF_ADDR, ETH_0_IDX, (int) TABLE_ID), request);
    }

    @Test
    public void testWriteV4WithoutIfc() throws WriteFailedException {
        customizer.writeCurrentAttributes(validId, v4WithoutIfc(), writeContext);
        verify(api, times(1)).gpeAddDelNativeFwdRpath(requestCaptor.capture());
        final GpeAddDelNativeFwdRpath request = requestCaptor.getValue();
        assertEquals(desiredRequest(1, 1, V4_WITHOUT_IF_ADDR, ~0, (int) TABLE_ID), request);
    }

    @Test
    public void testWriteV6() throws WriteFailedException {
        customizer.writeCurrentAttributes(validId, v6WithIfc(), writeContext);
        verify(api, times(1)).gpeAddDelNativeFwdRpath(requestCaptor.capture());
        final GpeAddDelNativeFwdRpath request = requestCaptor.getValue();
        assertEquals(desiredRequest(1, 0, V6_WITH_IF_ADDR, ETH_1_IDX, (int) TABLE_ID), request);
    }

    private static GpeAddDelNativeFwdRpath desiredRequest(final int add, final int isV4,
                                                          final byte[] addr, final int swIfIndex,
                                                          final int tableId) {
        GpeAddDelNativeFwdRpath request = new GpeAddDelNativeFwdRpath();
        request.isAdd = (byte) add;
        request.isIp4 = (byte) isV4;
        request.nhAddr = addr;
        request.nhSwIfIndex = swIfIndex;
        request.tableId = tableId;

        return request;
    }

    private static NativeForwardPath v6WithIfc() {
        return new NativeForwardPathBuilder()
                .setNextHopAddress(new IpAddress(new Ipv6Address("2001:0db8:0a0b:12f0:0000:0000:0000:0001")))
                .setNextHopInterface(ETH_1)
                .build();
    }

    private static NativeForwardPath v4WithoutIfc() {
        return new NativeForwardPathBuilder()
                .setNextHopAddress(new IpAddress(new Ipv4Address("192.168.2.3")))
                .build();
    }

    private static NativeForwardPath v4WithIfc() {
        return new NativeForwardPathBuilder()
                .setNextHopAddress(new IpAddress(new Ipv4Address("192.168.2.1")))
                .setNextHopInterface(ETH_0)
                .build();
    }
}
