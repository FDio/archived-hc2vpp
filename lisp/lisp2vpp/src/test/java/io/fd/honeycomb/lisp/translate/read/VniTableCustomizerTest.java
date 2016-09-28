package io.fd.honeycomb.lisp.translate.read;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.LispEidTableMapDetails;
import org.openvpp.jvpp.core.dto.LispEidTableMapDetailsReplyDump;


public class VniTableCustomizerTest extends ListReaderCustomizerTest<VniTable, VniTableKey, VniTableBuilder> {

    private InstanceIdentifier<VniTable> validId;

    public VniTableCustomizerTest() {
        super(VniTable.class, EidTableBuilder.class);
    }

    @Before
    public void init() {
        validId = InstanceIdentifier.create(EidTable.class).child(VniTable.class, new VniTableKey(2L));

        final LispEidTableMapDetailsReplyDump replyDump = new LispEidTableMapDetailsReplyDump();
        final LispEidTableMapDetails detail = new LispEidTableMapDetails();
        detail.dpTable = 3;
        detail.vni = 2;
        detail.context = 4;
        replyDump.lispEidTableMapDetails = ImmutableList.of(detail);

        when(api.lispEidTableMapDump(any())).thenReturn(future(replyDump));
    }

    @Test
    public void getAllIds() throws Exception {
        final List<VniTableKey> keys = getCustomizer().getAllIds(validId, ctx);

        assertEquals(1, keys.size());

        final VniTableKey key = keys.get(0);
        assertNotNull(key);
        //due to ambigous call (long,long) vs (Object,Object)
        assertEquals(2L, key.getVirtualNetworkIdentifier().longValue());

    }

    @Test
    public void readCurrentAttributes() throws Exception {
        VniTableBuilder builder = new VniTableBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);

        final VniTable table = builder.build();

        assertNotNull(table);
        assertEquals(3L, table.getTableId().longValue());
        assertEquals(2L, table.getVirtualNetworkIdentifier().longValue());
    }

    @Override
    protected ReaderCustomizer<VniTable, VniTableBuilder> initCustomizer() {
        return new VniTableCustomizer(api);
    }
}