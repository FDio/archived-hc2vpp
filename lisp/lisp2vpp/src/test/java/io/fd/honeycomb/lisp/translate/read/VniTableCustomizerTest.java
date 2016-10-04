package io.fd.honeycomb.lisp.translate.read;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpCallFailedException;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import io.fd.vpp.jvpp.VppCallbackException;
import io.fd.vpp.jvpp.core.dto.LispEidTableVniDetails;
import io.fd.vpp.jvpp.core.dto.LispEidTableVniDetailsReplyDump;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VniTableCustomizerTest extends ListReaderCustomizerTest<VniTable, VniTableKey, VniTableBuilder> {

    private InstanceIdentifier<VniTable> validId;

    public VniTableCustomizerTest() {
        super(VniTable.class, EidTableBuilder.class);
    }

    @Before
    public void init() {
        validId = InstanceIdentifier.create(EidTable.class).child(VniTable.class, new VniTableKey(12L));
    }

    @Test
    public void testReadAllSuccessfull() throws ReadFailedException {
        whenLispEidTableVniDumpReturnValid();
        final List<VniTableKey> keys = getCustomizer().getAllIds(validId, ctx);

        assertNotNull(keys);
        assertEquals(3, keys.size());
        assertTrue(keys.contains(new VniTableKey(12L)));
        assertTrue(keys.contains(new VniTableKey(14L)));
        assertTrue(keys.contains(new VniTableKey(16L)));
    }

    @Test
    public void testReadAllFailed() {
        whenLispEidTableVniDumpThrowException();
        try {
            getCustomizer().getAllIds(validId, ctx);
        } catch (ReadFailedException e) {
            assertTrue(e instanceof ReadFailedException);
            assertTrue(e.getCause() instanceof DumpCallFailedException);
            assertTrue(e.getCause().getCause() instanceof VppCallbackException);
        }

    }

    @Test
    public void testReadAttributes() throws ReadFailedException {
        whenLispEidTableVniDumpReturnValid();
        VniTableBuilder builder = new VniTableBuilder();

        customizer.readCurrentAttributes(validId, builder, ctx);

        final VniTable table = builder.build();
        assertNotNull(table);
        assertEquals(12L, table.getVirtualNetworkIdentifier().longValue());
    }

    private void whenLispEidTableVniDumpReturnValid() {

        LispEidTableVniDetailsReplyDump dump = new LispEidTableVniDetailsReplyDump();
        LispEidTableVniDetails details1 = new LispEidTableVniDetails();
        details1.vni = 14;

        LispEidTableVniDetails details2 = new LispEidTableVniDetails();
        details2.vni = 12;

        LispEidTableVniDetails details3 = new LispEidTableVniDetails();
        details3.vni = 16;

        dump.lispEidTableVniDetails = ImmutableList.of(details1, details2, details3);

        when(api.lispEidTableVniDump(Mockito.any())).thenReturn(CompletableFuture.completedFuture(dump));
    }

    private void whenLispEidTableVniDumpThrowException() {
        when(api.lispEidTableVniDump(Mockito.any()))
                .thenReturn(new CompletableFuture<LispEidTableVniDetailsReplyDump>() {
                    @Override
                    public LispEidTableVniDetailsReplyDump get(final long l, final TimeUnit timeUnit)
                            throws InterruptedException, ExecutionException, TimeoutException {
                        throw new ExecutionException(new VppCallbackException("lispEidTableVniDump", 1, -2));
                    }
                });
    }

    @Override
    protected ReaderCustomizer<VniTable, VniTableBuilder> initCustomizer() {
        return new VniTableCustomizer(api);
    }
}