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
import io.fd.vpp.jvpp.core.dto.LispEidTableVniDetails;
import io.fd.vpp.jvpp.core.dto.LispEidTableVniDetailsReplyDump;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import io.fd.vpp.jvpp.core.dto.LispEidTableMapDetails;
import io.fd.vpp.jvpp.core.dto.LispEidTableMapDetailsReplyDump;


public class VniTableDumpExecutorTest extends JvppDumpExecutorTest<VniTableDumpExecutor> {

    private LispEidTableVniDetailsReplyDump validDump;

    @Before
    public void init() {
        validDump = new LispEidTableVniDetailsReplyDump();
        LispEidTableVniDetails detail = new LispEidTableVniDetails();
        detail.vni = 2;
        detail.context = 4;
        validDump.lispEidTableVniDetails = ImmutableList.of(detail);
    }

    @Test(expected = DumpCallFailedException.class)
    public void testExecuteDumpFail() throws DumpExecutionFailedException {
        doThrowFailExceptionWhen().lispEidTableVniDump(Mockito.any());
        getExecutor().executeDump(EntityDumpExecutor.NO_PARAMS);
    }


    @Test
    public void testExecuteDumpTimeout() throws Exception {
        doThrowTimeoutExceptionWhen().lispEidTableVniDump(Mockito.any());
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

        doReturnResponseWhen(validDump).lispEidTableVniDump(Mockito.any());
        final LispEidTableVniDetailsReplyDump reply = getExecutor().executeDump(EntityDumpExecutor.NO_PARAMS);

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