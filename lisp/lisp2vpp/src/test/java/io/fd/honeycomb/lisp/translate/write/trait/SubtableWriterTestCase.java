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
