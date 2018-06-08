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

package io.fd.hc2vpp.fib.management.services;

import static io.fd.vpp.jvpp.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;
import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.hc2vpp.fib.management.helpers.SchemaContextTestHelper;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.Ip6FibDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.IpFibDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.IpTableAddDel;
import io.fd.vpp.jvpp.core.dto.IpTableAddDelReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FibTableServiceImplTest implements SchemaContextTestHelper, ByteDataTranslator, FutureProducer {

    private static final int FIB_TABLE_ID = 123456;
    private static final String FIB_TABLE_NAME = "VRF123456";

    @Inject
    @Mock
    private static FutureJVppCore api;

    @Mock
    private ModificationCache modificationCache;

    @Captor
    private ArgumentCaptor<IpTableAddDel> argumentCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(api.ipTableAddDel(any())).thenReturn(future(new IpTableAddDelReply()));
        when(api.ipFibDump(any())).thenReturn(future(new IpFibDetailsReplyDump()));
        when(api.ip6FibDump(any())).thenReturn(future(new Ip6FibDetailsReplyDump()));
        when(modificationCache.get(any())).thenReturn(null);

    }

    @Test(expected = FibTableService.FibTableDoesNotExistException.class)
    public void checkTableExistTest() throws ReadFailedException, FibTableService.FibTableDoesNotExistException {
        FibTableServiceImpl fibService = new FibTableServiceImpl(api);
        fibService.checkTableExist(FIB_TABLE_ID, modificationCache);
    }

    @Test
    public void writeIpv4Test() throws WriteFailedException {
        FibTableServiceImpl fibTableService = new FibTableServiceImpl(api);
        fibTableService.write(FibManagementIIds.FIB_MNGMNT, FIB_TABLE_ID, FIB_TABLE_NAME, false);

        verify(api, times(1)).ipTableAddDel(argumentCaptor.capture());

        assertTableAddDelRequest(argumentCaptor.getValue(), false);
    }

    @Test
    public void writeIpv6Test() throws WriteFailedException {
        FibTableServiceImpl fibTableService = new FibTableServiceImpl(api);
        fibTableService.write(FibManagementIIds.FIB_MNGMNT, FIB_TABLE_ID, FIB_TABLE_NAME, true);

        verify(api, times(1)).ipTableAddDel(argumentCaptor.capture());

        assertTableAddDelRequest(argumentCaptor.getValue(), true);
    }

    private void assertTableAddDelRequest(IpTableAddDel jvppRequest, boolean isIpv6) {
        assertEquals(ByteDataTranslator.BYTE_TRUE, jvppRequest.isAdd);
        assertEquals(ByteDataTranslator.INSTANCE.booleanToByte(isIpv6), jvppRequest.isIpv6);
        assertEquals(FIB_TABLE_ID, jvppRequest.tableId);
        Assert.assertArrayEquals(FIB_TABLE_NAME.getBytes(), jvppRequest.name);
    }
}
