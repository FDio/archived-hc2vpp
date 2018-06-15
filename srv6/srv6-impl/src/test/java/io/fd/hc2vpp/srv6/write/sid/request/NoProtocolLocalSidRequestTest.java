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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.fd.hc2vpp.srv6.Srv6IIds;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SrLocalsidAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;

public class NoProtocolLocalSidRequestTest extends LocalSidRequestTest {

    private static final int END_VALUE = 1;

    private static void assertRequestBody(final SrLocalsidAddDel jvppRequest) {
        assertNull(jvppRequest.nhAddr6);
        assertNull(jvppRequest.nhAddr4);
        assertEquals(0, jvppRequest.vlanIndex);
        assertEquals(0, jvppRequest.swIfIndex);
    }

    private static NoProtocolLocalSidRequest createValidRequest(final FutureJVppCore api) {
        final NoProtocolLocalSidRequest request = new NoProtocolLocalSidRequest(api);
        request.setLocalSidAddress(new Ipv6Address("A::0"));
        request.setFunction(END_VALUE);
        request.setInstallFibTable(0);
        return request;
    }

    @Test(expected = NullPointerException.class)
    public void testNoAddress() throws WriteFailedException {
        final NoProtocolLocalSidRequest request = new NoProtocolLocalSidRequest(api);
        request.write(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LS_SID);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoBehavior() throws WriteFailedException {
        final NoProtocolLocalSidRequest request = new NoProtocolLocalSidRequest(api);
        request.setLocalSidAddress(SID);
        request.write(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LS_SID);
    }

    @Test
    public void testWriteValid() throws WriteFailedException {
        createValidRequest(api).write(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LS_SID);
        verify(api, times(1)).srLocalsidAddDel(requestcaptor.capture());
        final SrLocalsidAddDel jvppRequest = requestcaptor.getValue();
        assertIsCreate(jvppRequest);
        assertRequestBody(jvppRequest);
    }

    @Test
    public void testDeleteValid() throws WriteFailedException {
        createValidRequest(api).delete(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LS_SID);
        verify(api, times(1)).srLocalsidAddDel(requestcaptor.capture());
        final SrLocalsidAddDel jvppRequest = requestcaptor.getValue();
        assertIsDelete(jvppRequest);
        assertBaseFields(jvppRequest, SID_BYTES, 0, END_VALUE, 1);
        assertRequestBody(jvppRequest);
    }
}
