package io.fd.honeycomb.lisp.translate.read.dump.executor;

import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.FilterType;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.MappingsDumpParamsBuilder;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.QuantityType;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.vpp.test.read.JvppDumpExecutorTest;
import io.fd.vpp.jvpp.VppCallbackException;
import io.fd.vpp.jvpp.core.dto.LispEidTableDetails;
import io.fd.vpp.jvpp.core.dto.LispEidTableDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.LispEidTableDump;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MappingsDumpExecutorTest extends JvppDumpExecutorTest<MappingsDumpExecutor> {

    private static final byte[] EID = {-64, -88, 2, 1};

    @Captor
    private ArgumentCaptor<LispEidTableDump> requestCaptor;

    private InstanceIdentifier identifier;

    private LispEidTableDetailsReplyDump validDump;
    private MappingsDumpParams emptyParams;
    private MappingsDumpParams validParams;

    @Before
    public void init() {
        identifier = InstanceIdentifier.create(LocalMapping.class);
        validDump = new LispEidTableDetailsReplyDump();

        LispEidTableDetails detail = new LispEidTableDetails();
        detail.action = 0;
        detail.authoritative = 1;
        detail.context = 4;
        detail.eid = new byte[]{-64, -88, 2, 1};
        detail.eidPrefixLen = 32;
        detail.isLocal = 1;
        detail.locatorSetIndex = 2;
        detail.ttl = 7;
        detail.vni = 2;

        validDump.lispEidTableDetails = ImmutableList.of(detail);

        emptyParams = MappingsDumpParamsBuilder.newInstance().build();
        validParams =
                MappingsDumpParamsBuilder.newInstance().setVni(2).setPrefixLength((byte) 32).setEidSet(QuantityType.ALL)
                        .setEid(EID)
                        .setEidType(EidType.IPV4).setFilter(FilterType.LOCAL).build();
    }

    @Test
    public void testExecuteDumpTimeout() throws Exception {
        doThrowTimeoutExceptionWhen().lispEidTableDump(any());
        try {
            getExecutor().executeDump(identifier, emptyParams);
        } catch (Exception e) {
            assertTrue(e instanceof ReadFailedException);
            assertTrue(e.getCause() instanceof TimeoutException);
            assertEquals(identifier, ((ReadFailedException) e).getFailedId());
            return;
        }
        fail("Test should have thrown exception");
    }

    @Test
    public void testExecuteDumpHalted() throws Exception {
        doThrowFailExceptionWhen().lispEidTableDump(any());
        try {
            getExecutor().executeDump(identifier, emptyParams);
        } catch (ReadFailedException e) {
            assertTrue(e.getCause() instanceof VppCallbackException);
            assertEquals(identifier, ((ReadFailedException) e).getFailedId());
            return;
        }
        fail("Test should have thrown ReadFailedException");
    }

    @Test
    public void testExecuteDump() throws ReadFailedException {
        doReturnResponseWhen(validDump).lispEidTableDump(any());
        final LispEidTableDetailsReplyDump reply = getExecutor().executeDump(identifier, validParams);
        verify(api, times(1)).lispEidTableDump(requestCaptor.capture());

        final LispEidTableDump request = requestCaptor.getValue();
        assertNotNull(request);
        assertEquals(2, request.vni);
        assertEquals(QuantityType.ALL.getValue(), request.eidSet);
        assertArrayEquals(EID, request.eid);
        assertEquals(EidType.IPV4.getValue(), request.eidType);
        assertEquals(FilterType.LOCAL.getValue(), request.filter);
        assertEquals(32, request.prefixLength);

        assertNotNull(reply);
        assertEquals(1, reply.lispEidTableDetails.size());

        final LispEidTableDetails detail = reply.lispEidTableDetails.get(0);

        assertNotNull(detail);
        assertEquals(0, detail.action);
        assertEquals(1, detail.authoritative);
        assertEquals(4, detail.context);
        assertArrayEquals(EID, detail.eid);
        assertEquals(32, detail.eidPrefixLen);
        assertEquals(1, detail.isLocal);
        assertEquals(2, detail.locatorSetIndex);
        assertEquals(7, detail.ttl);
        assertEquals(2, detail.vni);
    }

    @Override
    protected MappingsDumpExecutor initExecutor() {
        return new MappingsDumpExecutor(api);
    }
}