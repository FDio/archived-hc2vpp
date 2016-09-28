package io.fd.honeycomb.lisp.translate.read.dump.executor;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.DumpExecutionFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpCallFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpTimeoutException;
import io.fd.honeycomb.vpp.test.read.JvppDumpExecutorTest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import io.fd.vpp.jvpp.core.dto.LispLocatorSetDetails;
import io.fd.vpp.jvpp.core.dto.LispLocatorSetDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.LispLocatorSetDump;


public class LocatorSetsDumpExecutorTest extends JvppDumpExecutorTest<LocatorSetsDumpExecutor> {

    public static final byte[] LOCATOR_SET_NAME_BYTES = "loc-set".getBytes(StandardCharsets.UTF_8);

    private LispLocatorSetDetailsReplyDump validDump;

    @Captor
    private ArgumentCaptor<LispLocatorSetDump> requestCaptor;

    @Before
    public void init() {
        validDump = new LispLocatorSetDetailsReplyDump();
        LispLocatorSetDetails detail = new LispLocatorSetDetails();
        detail.lsIndex = 2;
        detail.lsName = LOCATOR_SET_NAME_BYTES;
        detail.context = 4;

        validDump.lispLocatorSetDetails = ImmutableList.of(detail);
    }

    @Test
    public void testExecuteDumpTimeout() throws Exception {
        doThrowTimeoutExceptionWhen().lispLocatorSetDump(any());
        try {
            getExecutor().executeDump(EntityDumpExecutor.NO_PARAMS);
        } catch (Exception e) {
            assertTrue(e instanceof DumpTimeoutException);
            assertTrue(e.getCause() instanceof TimeoutException);
            return;
        }
        fail("Test should have thrown exception");
    }

    @Test(expected = DumpCallFailedException.class)
    public void testExecuteDumpHalted() throws DumpExecutionFailedException {
        doThrowFailExceptionWhen().lispLocatorSetDump(any());
        getExecutor().executeDump(EntityDumpExecutor.NO_PARAMS);
    }

    @Test
    public void testExecuteDump() throws DumpExecutionFailedException {
        doReturnResponseWhen(validDump).lispLocatorSetDump(any());

        final LispLocatorSetDetailsReplyDump replyDump = getExecutor().executeDump(EntityDumpExecutor.NO_PARAMS);
        verify(api, times(1)).lispLocatorSetDump(requestCaptor.capture());

        final LispLocatorSetDump request = requestCaptor.getValue();

        assertNotNull(request);
        assertEquals(1, request.filter);

        assertNotNull(replyDump);
        assertNotNull(replyDump.lispLocatorSetDetails);
        assertEquals(1, replyDump.lispLocatorSetDetails.size());
        final LispLocatorSetDetails detail = replyDump.lispLocatorSetDetails.get(0);

        assertNotNull(detail);
        assertEquals(4, detail.context);
        assertEquals(2, detail.lsIndex);
        assertEquals(LOCATOR_SET_NAME_BYTES, detail.lsName);
    }

    @Override
    protected LocatorSetsDumpExecutor initExecutor() {
        return new LocatorSetsDumpExecutor(api);
    }
}