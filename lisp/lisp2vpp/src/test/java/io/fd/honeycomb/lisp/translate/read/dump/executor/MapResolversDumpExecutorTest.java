package io.fd.honeycomb.lisp.translate.read.dump.executor;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.vpp.test.read.JvppDumpExecutorTest;
import io.fd.vpp.jvpp.VppCallbackException;
import io.fd.vpp.jvpp.core.dto.LispMapResolverDetails;
import io.fd.vpp.jvpp.core.dto.LispMapResolverDetailsReplyDump;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class MapResolversDumpExecutorTest extends JvppDumpExecutorTest<MapResolversDumpExecutor> {

    private LispMapResolverDetailsReplyDump validDump;

    private InstanceIdentifier identifier;

    @Before
    public void init() {
        identifier = InstanceIdentifier.create(MapResolver.class);
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
            getExecutor().executeDump(identifier, EntityDumpExecutor.NO_PARAMS);
        } catch (ReadFailedException e) {
            assertTrue(e.getCause() instanceof TimeoutException);
            assertEquals(identifier, ((ReadFailedException) e).getFailedId());
            return;
        }
        fail("Test should have thrown ReadFailedException");
    }

    @Test
    public void testExecuteDumpHalted() throws ReadFailedException {
        doThrowFailExceptionWhen().lispMapResolverDump(Mockito.any());
        try {
            getExecutor().executeDump(identifier, EntityDumpExecutor.NO_PARAMS);
        } catch (ReadFailedException e) {
            assertTrue(e.getCause() instanceof VppCallbackException);
            assertEquals(identifier, ((ReadFailedException) e).getFailedId());
            return;
        }
        fail("Test should have thrown ReadFailedException");
    }

    @Test
    public void testExecuteDump() throws ReadFailedException {
        doReturnResponseWhen(validDump).lispMapResolverDump(Mockito.any());
        final LispMapResolverDetailsReplyDump reply =
                getExecutor().executeDump(identifier, EntityDumpExecutor.NO_PARAMS);

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