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

package io.fd.hc2vpp.srv6.write.sid.request;

import static io.fd.vpp.jvpp.Assertions.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.fd.hc2vpp.srv6.Srv6IIds;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SrLocalsidAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Arrays;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;

public class XConnectLocalSidRequestTest extends LocalSidRequestTest {

    private static final IpAddress NEXT_HOP_ADDRESS = new IpAddress(new Ipv6Address("B::0"));
    private static final byte[] NEXT_HOP_ADDRESS_BYTES = new byte[]{0, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final int END_DX6_VALUE = 6;

    private static XConnectLocalSidRequest createValidRequest(final FutureJVppCore api) {
        final XConnectLocalSidRequest request = new XConnectLocalSidRequest(api);
        request.setLocalSidAddress(SID);
        request.setNextHopAddress(NEXT_HOP_ADDRESS);
        request.setOutgoingInterfaceIndex(7);
        request.setVlanIndex(4);
        request.setInstallFibTable(0);
        request.setFunction(END_DX6_VALUE);
        return request;
    }

    @Test(expected = NullPointerException.class)
    public void testNoAddress() throws WriteFailedException {
        final XConnectLocalSidRequest request = new XConnectLocalSidRequest(api);
        request.write(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LS_SID);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoBehavior() throws WriteFailedException {
        final XConnectLocalSidRequest request = new XConnectLocalSidRequest(api);
        request.setLocalSidAddress(SID);
        request.write(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LS_SID);
    }

    @Test
    public void testWriteValid() throws WriteFailedException {
        createValidRequest(api).write(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LS_SID);
        verify(api, times(1)).srLocalsidAddDel(requestcaptor.capture());

        final SrLocalsidAddDel jvppRequest = requestcaptor.getValue();

        assertIsCreate(jvppRequest);
        assertBaseFields(jvppRequest, SID_BYTES, 0, END_DX6_VALUE, 1);
        assertEquals(7, jvppRequest.swIfIndex);
        assertEquals(4, jvppRequest.vlanIndex);
        assertTrue(Arrays.equals(NEXT_HOP_ADDRESS_BYTES, jvppRequest.nhAddr6));
    }

    @Test
    public void testDeleteValid() throws WriteFailedException {
        createValidRequest(api).delete(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LS_SID);
        verify(api, times(1)).srLocalsidAddDel(requestcaptor.capture());

        final SrLocalsidAddDel jvppRequest = requestcaptor.getValue();

        assertIsDelete(jvppRequest);
        assertBaseFields(jvppRequest, SID_BYTES, 0, END_DX6_VALUE, 1);
        assertEquals(7, jvppRequest.swIfIndex);
        assertEquals(4, jvppRequest.vlanIndex);
        assertTrue(Arrays.equals(NEXT_HOP_ADDRESS_BYTES, jvppRequest.nhAddr6));
    }
}
