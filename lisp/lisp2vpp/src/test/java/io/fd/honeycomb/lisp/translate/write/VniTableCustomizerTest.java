package io.fd.honeycomb.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.LispEidTableAddDelMap;
import io.fd.vpp.jvpp.core.dto.LispEidTableAddDelMapReply;


public class VniTableCustomizerTest extends WriterCustomizerTest {

    @Captor
    private ArgumentCaptor<LispEidTableAddDelMap> requestCaptor;

    private VniTableCustomizer customizer;
    private InstanceIdentifier<VniTable> emptyId;

    private VniTable emptyData;
    private VniTable validData;

    @Before
    public void init() {
        customizer = new VniTableCustomizer(api);

        emptyId = InstanceIdentifier.create(VniTable.class);

        emptyData = new VniTableBuilder().build();
        validData = new VniTableBuilder().setTableId(2L).setVirtualNetworkIdentifier(3L).build();

        when(api.lispEidTableAddDelMap(any())).thenReturn(future(new LispEidTableAddDelMapReply()));
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesEmptyData() throws Exception {
        customizer.writeCurrentAttributes(emptyId, emptyData, writeContext);
    }


    @Test
    public void testWriteCurrentAttributes() throws Exception {
        customizer.writeCurrentAttributes(emptyId, validData, writeContext);
        verify(api, times(1)).lispEidTableAddDelMap(requestCaptor.capture());
        verifyRequest(requestCaptor.getValue(), (byte) 1, 2L, 3L);
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws Exception {
        customizer.updateCurrentAttributes(emptyId, emptyData, emptyData, writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesEmptyData() throws Exception {
        customizer.deleteCurrentAttributes(emptyId, emptyData, writeContext);
    }

    @Test
    public void testDeleteCurrentAttributes() throws Exception {
        customizer.deleteCurrentAttributes(emptyId, validData, writeContext);
        verify(api, times(1)).lispEidTableAddDelMap(requestCaptor.capture());
        verifyRequest(requestCaptor.getValue(), (byte) 0, 2L, 3L);
    }

    private static void verifyRequest(final LispEidTableAddDelMap request, final byte isAdd,
                                      final long dpTable,
                                      final long vni) {
        assertNotNull(request);
        assertEquals(isAdd, request.isAdd);
        assertEquals(dpTable, request.dpTable);
        assertEquals(vni, request.vni);

    }

}