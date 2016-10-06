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
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.vpp.test.read.JvppDumpExecutorTest;
import io.fd.vpp.jvpp.VppCallbackException;
import io.fd.vpp.jvpp.core.dto.LispLocatorDetails;
import io.fd.vpp.jvpp.core.dto.LispLocatorDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.LispLocatorDump;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class LocatorDumpExecutorTest extends JvppDumpExecutorTest<LocatorDumpExecutor> {

    @Captor
    private ArgumentCaptor<LispLocatorDump> requestCaptor;

    private InstanceIdentifier identifier;
    private LispLocatorDetailsReplyDump validDump;

    @Before
    public void init() {
        identifier = InstanceIdentifier.create(LocatorSet.class);
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
            getExecutor().executeDump(identifier, new LocatorDumpParamsBuilder().build());
        } catch (Exception e) {
            assertTrue(e instanceof ReadFailedException);
            assertTrue(e.getCause() instanceof TimeoutException);
            assertEquals(identifier, ((ReadFailedException) e).getFailedId());
            return;
        }
        fail("Test should have thrown exception");
    }

    @Test
    public void testExecuteDumpHalted() throws ReadFailedException {
        doThrowFailExceptionWhen().lispLocatorDump(Mockito.any());
        try {
            getExecutor().executeDump(identifier, new LocatorDumpParamsBuilder().build());
        } catch (ReadFailedException e) {
            assertTrue(e.getCause() instanceof VppCallbackException);
            assertEquals(identifier, ((ReadFailedException) e).getFailedId());
            return;
        }
        fail("Test should have thrown ReadFailedException");
    }

    @Test
    public void testExecuteDump() throws ReadFailedException {
        doReturnResponseWhen(validDump).lispLocatorDump(Mockito.any());

        final LocatorDumpParams params = new LocatorDumpParamsBuilder().setLocatorSetIndex(5).build();

        final LispLocatorDetailsReplyDump reply = getExecutor().executeDump(identifier, params);
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