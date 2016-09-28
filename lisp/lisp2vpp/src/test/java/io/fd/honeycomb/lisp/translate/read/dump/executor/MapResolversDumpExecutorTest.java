package io.fd.honeycomb.lisp.translate.read.dump.executor;


import static org.junit.Assert.assertArrayEquals;
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
import org.openvpp.jvpp.core.dto.LispMapResolverDetails;
import org.openvpp.jvpp.core.dto.LispMapResolverDetailsReplyDump;


public class MapResolversDumpExecutorTest extends JvppDumpExecutorTest<MapResolversDumpExecutor> {

    private LispMapResolverDetailsReplyDump validDump;

    @Before
    public void init() {
        validDump = new LispMapResolverDetailsReplyDump();
        final LispMapResolverDetails details = new LispMapResolverDetails();
        details.isIpv6 = 0;
        details.ipAddress = new byte[]{-64, -88, 5, 4};
        details.context = 7;

        validDump.lispMapResolverDetails = ImmutableList.of(details);
    }

    @Test
    public void testExecuteDumpTimeout() throws Exception {
        doThrowTimeoutExceptionWhen().lispMapResolverDump(Mockito.any());
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
        doThrowFailExceptionWhen().lispMapResolverDump(Mockito.any());
        getExecutor().executeDump(EntityDumpExecutor.NO_PARAMS);
    }

    @Test
    public void testExecuteDump() throws DumpExecutionFailedException {
        doReturnResponseWhen(validDump).lispMapResolverDump(Mockito.any());
        final LispMapResolverDetailsReplyDump reply = getExecutor().executeDump(EntityDumpExecutor.NO_PARAMS);

        assertNotNull(reply);
        assertEquals(1, reply.lispMapResolverDetails.size());

        final LispMapResolverDetails detail = reply.lispMapResolverDetails.get(0);
        assertEquals(7, detail.context);
        assertEquals(0, detail.isIpv6);
        assertArrayEquals(new byte[]{-64, -88, 5, 4}, detail.ipAddress);
    }

    @Override
    protected MapResolversDumpExecutor initExecutor() {
        return new MapResolversDumpExecutor(api);
    }
}