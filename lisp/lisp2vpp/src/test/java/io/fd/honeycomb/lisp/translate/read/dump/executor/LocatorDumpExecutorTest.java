package io.fd.honeycomb.lisp.translate.read.dump.executor;


import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.LocatorDumpParams.LocatorDumpParamsBuilder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.LocatorDumpParams;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.DumpExecutionFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpCallFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpTimeoutException;
import io.fd.honeycomb.vpp.test.read.JvppDumpExecutorTest;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import io.fd.vpp.jvpp.core.dto.LispLocatorDetails;
import io.fd.vpp.jvpp.core.dto.LispLocatorDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.LispLocatorDump;


public class LocatorDumpExecutorTest extends JvppDumpExecutorTest<LocatorDumpExecutor> {

    @Captor
    private ArgumentCaptor<LispLocatorDump> requestCaptor;

    private LispLocatorDetailsReplyDump validDump;

    @Before
    public void init() {
        validDump = new LispLocatorDetailsReplyDump();
        LispLocatorDetails detail = new LispLocatorDetails();

        detail.swIfIndex = 1;
        detail.priority = 2;
        detail.local = 1;
        detail.weight = 3;
        detail.isIpv6 = 0;
        detail.context = 8;
        detail.ipAddress = new byte[]{-64, -88, 4, 2};

        validDump.lispLocatorDetails = ImmutableList.of(detail);
    }

    @Test
    public void testExecuteDumpTimeout() throws Exception {
        doThrowTimeoutExceptionWhen().lispLocatorDump(Mockito.any());
        try {
            getExecutor().executeDump(new LocatorDumpParamsBuilder().build());
        } catch (Exception e) {
            assertTrue(e instanceof DumpTimeoutException);
            assertTrue(e.getCause() instanceof TimeoutException);
            return;
        }
        fail("Test should have thrown exception");
    }

    @Test(expected = DumpCallFailedException.class)
    public void testExecuteDumpHalted() throws DumpExecutionFailedException {
        doThrowFailExceptionWhen().lispLocatorDump(Mockito.any());
        getExecutor().executeDump(new LocatorDumpParamsBuilder().build());
    }

    @Test
    public void testExecuteDump() throws DumpExecutionFailedException {
        doReturnResponseWhen(validDump).lispLocatorDump(Mockito.any());

        final LocatorDumpParams params = new LocatorDumpParamsBuilder().setLocatorSetIndex(5).build();

        final LispLocatorDetailsReplyDump reply = getExecutor().executeDump(params);
        verify(api, times(1)).lispLocatorDump(requestCaptor.capture());

        final LispLocatorDump request = requestCaptor.getValue();

        //check passed params
        assertNotNull(request);
        assertEquals(5, request.lsIndex);

        //check result
        assertNotNull(reply);
        assertEquals(1, reply.lispLocatorDetails.size());

        final LispLocatorDetails details = reply.lispLocatorDetails.get(0);
        assertEquals(1, details.swIfIndex);
        assertEquals(2, details.priority);
        assertEquals(1, details.local);
        assertEquals(3, details.weight);
        assertEquals(0, details.isIpv6);
        assertEquals(8, details.context);
        assertArrayEquals(new byte[]{-64, -88, 4, 2}, details.ipAddress);
    }

    @Override
    protected LocatorDumpExecutor initExecutor() {
        return new LocatorDumpExecutor(api);
    }
}