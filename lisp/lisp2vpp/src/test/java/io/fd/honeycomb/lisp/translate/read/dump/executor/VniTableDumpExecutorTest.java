package io.fd.honeycomb.lisp.translate.read.dump.executor;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.vpp.test.read.JvppDumpExecutorTest;
import io.fd.vpp.jvpp.VppCallbackException;
import io.fd.vpp.jvpp.core.dto.LispEidTableVniDetails;
import io.fd.vpp.jvpp.core.dto.LispEidTableVniDetailsReplyDump;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class VniTableDumpExecutorTest extends JvppDumpExecutorTest<VniTableDumpExecutor> {

    private LispEidTableVniDetailsReplyDump validDump;
    private InstanceIdentifier<VniTable> identifier;

    @Before
    public void init() {
        validDump = new LispEidTableVniDetailsReplyDump();
        identifier = InstanceIdentifier.create(VniTable.class);
        LispEidTableVniDetails detail = new LispEidTableVniDetails();
        detail.vni = 2;
        detail.context = 4;
        validDump.lispEidTableVniDetails = ImmutableList.of(detail);
    }

    @Test
    public void testExecuteDumpFail() throws Exception {
        doThrowFailExceptionWhen().lispEidTableVniDump(Mockito.any());
        try {
            getExecutor().executeDump(identifier, EntityDumpExecutor.NO_PARAMS);
        } catch (ReadFailedException e) {
            assertTrue(e.getCause() instanceof VppCallbackException);
            return;
        }

        fail("Test should have thrown ReadFailedException");
    }

    @Test
    public void testExecuteDumpTimeout() throws Exception {
        doThrowTimeoutExceptionWhen().lispEidTableVniDump(Mockito.any());
        try {
            getExecutor().executeDump(identifier, EntityDumpExecutor.NO_PARAMS);
        } catch (ReadFailedException e) {
            assertTrue(e.getCause() instanceof TimeoutException);
            return;
        }
        fail("Test should have thrown ReadFailedException");
    }

    @Test
    public void testExecuteDump() throws Exception {

        doReturnResponseWhen(validDump).lispEidTableVniDump(Mockito.any());
        final LispEidTableVniDetailsReplyDump reply =
                getExecutor().executeDump(identifier, EntityDumpExecutor.NO_PARAMS);

        assertNotNull(reply);
        assertEquals(1, reply.lispEidTableVniDetails.size());
        final LispEidTableVniDetails detail = reply.lispEidTableVniDetails.get(0);

        assertEquals(4, detail.context);
        assertEquals(2, detail.vni);
    }

    @Override
    protected VniTableDumpExecutor initExecutor() {
        return new VniTableDumpExecutor(api);
    }
}