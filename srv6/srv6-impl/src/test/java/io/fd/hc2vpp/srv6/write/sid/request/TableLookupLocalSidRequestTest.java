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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.fd.hc2vpp.srv6.Srv6IIds;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SrLocalsidAddDel;
import org.junit.Test;

public class TableLookupLocalSidRequestTest extends LocalSidRequestTest {

    private static final int END_T_VALUE = 3;

    @Test(expected = NullPointerException.class)
    public void testNoAddress() throws WriteFailedException {
        final TableLookupLocalSidRequest request = new TableLookupLocalSidRequest(api);
        request.write(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LS_SID);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoBehavior() throws WriteFailedException {
        final TableLookupLocalSidRequest request = new TableLookupLocalSidRequest(api);
        request.setLocalSidAddress(SID);
        request.write(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LS_SID);
    }

    @Test
    public void testWriteValid() throws WriteFailedException {
        createValidRequest().write(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LS_SID);
        verify(api, times(1)).srLocalsidAddDel(requestcaptor.capture());
        final SrLocalsidAddDel jvppRequest = requestcaptor.getValue();

        assertIsCreate(jvppRequest);
        assertBaseFields(jvppRequest, SID_BYTES, 0, END_T_VALUE, 1);
        assertEquals(7, jvppRequest.swIfIndex);
    }

    @Test
    public void testDeleteValid() throws WriteFailedException {
        createValidRequest().delete(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LS_SID);
        verify(api, times(1)).srLocalsidAddDel(requestcaptor.capture());
        final SrLocalsidAddDel jvppRequest = requestcaptor.getValue();

        assertIsDelete(jvppRequest);
        assertBaseFields(jvppRequest, SID_BYTES, 0, END_T_VALUE, 1);
        assertEquals(7, jvppRequest.swIfIndex);
    }

    private TableLookupLocalSidRequest createValidRequest() {
        final TableLookupLocalSidRequest request = new TableLookupLocalSidRequest(api);
        request.setLookupFibTable(7);
        request.setInstallFibTable(0);
        request.setLocalSidAddress(SID);
        request.setFunction(END_T_VALUE);
        return request;
    }

}
