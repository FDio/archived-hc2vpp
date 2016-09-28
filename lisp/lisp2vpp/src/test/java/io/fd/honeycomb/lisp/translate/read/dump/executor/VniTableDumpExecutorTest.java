package io.fd.honeycomb.lisp.translate.read.dump.executor;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.DumpExecutionFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpCallFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpTimeoutException;
import io.fd.honeycomb.vpp.test.read.JvppDumpExecutorTest;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openvpp.jvpp.core.dto.LispEidTableMapDetails;
import org.openvpp.jvpp.core.dto.LispEidTableMapDetailsReplyDump;


public class VniTableDumpExecutorTest extends JvppDumpExecutorTest<VniTableDumpExecutor> {

    private LispEidTableMapDetailsReplyDump validDump;

    @Before
    public void init() {
        validDump = new LispEidTableMapDetailsReplyDump();
        LispEidTableMapDetails detail = new LispEidTableMapDetails();
        detail.dpTable = 1;
        detail.vni = 2;
        detail.context = 4;
        validDump.lispEidTableMapDetails = ImmutableList.of(detail);
    }

    @Test(expected = DumpCallFailedException.class)
    public void testExecuteDumpFail() throws DumpExecutionFailedException {
        doThrowFailExceptionWhen().lispEidTableMapDump(Mockito.any());
        getExecutor().executeDump(EntityDumpExecutor.NO_PARAMS);
    }


    @Test
    public void testExecuteDumpTimeout() throws Exception {
        doThrowTimeoutExceptionWhen().lispEidTableMapDump(Mockito.any());
        try {
            getExecutor().executeDump(EntityDumpExecutor.NO_PARAMS);
        } catch (Exception e) {
            assertTrue(e instanceof DumpTimeoutException);
            assertTrue(e.getCause() instanceof TimeoutException);
            return;
        }
        fail("Test should have thrown exception");
    }

    @Test
    public void testExecuteDump() throws DumpExecutionFailedException {

        doReturnResponseWhen(validDump).lispEidTableMapDump(Mockito.any());
        final LispEidTableMapDetailsReplyDump reply = getExecutor().executeDump(EntityDumpExecutor.NO_PARAMS);

        assertNotNull(reply);
        assertEquals(1, reply.lispEidTableMapDetails.size());
        final LispEidTableMapDetails detail = reply.lispEidTableMapDetails.get(0);

        assertEquals(4, detail.context);
        assertEquals(1, detail.dpTable);
        assertEquals(2, detail.vni);
    }

    @Override
    protected VniTableDumpExecutor initExecutor() {
        return new VniTableDumpExecutor(api);
    }
}