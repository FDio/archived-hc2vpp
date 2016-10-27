/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.honeycomb.lisp.translate.write.trait;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import io.fd.vpp.jvpp.core.dto.LispEidTableAddDelMap;
import io.fd.vpp.jvpp.core.dto.LispEidTableAddDelMapReply;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;

public class SubtableWriterTestCase extends WriterCustomizerTest implements SubtableWriter {
    @Captor
    protected ArgumentCaptor<LispEidTableAddDelMap> requestCaptor;


    protected void verifyAddDelEidTableAddDelMapInvokedCorrectly(final int addDel, final int vni, final int tableId,
                                                                 final int isL2) {
        verify(api, times(1)).lispEidTableAddDelMap(requestCaptor.capture());

        final LispEidTableAddDelMap request = requestCaptor.getValue();
        assertNotNull(request);
        assertEquals(addDel, request.isAdd);
        assertEquals(vni, request.vni);
        assertEquals(tableId, request.dpTable);
        assertEquals(isL2, request.isL2);
    }

    protected void whenAddDelEidTableAddDelMapSuccess() {
        when(api.lispEidTableAddDelMap(Mockito.any(LispEidTableAddDelMap.class)))
                .thenReturn(future(new LispEidTableAddDelMapReply()));
    }

    protected void whenAddDelEidTableAddDelMapFail() {
        when(api.lispEidTableAddDelMap(Mockito.any(LispEidTableAddDelMap.class)))
                .thenReturn(failedFuture());
    }
}
