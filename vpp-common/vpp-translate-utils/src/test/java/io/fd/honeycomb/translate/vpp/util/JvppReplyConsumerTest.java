package io.fd.honeycomb.translate.vpp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.JVppReply;

public class JvppReplyConsumerTest implements JvppReplyConsumer {

    private static class AnDataObject implements DataObject {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return null;
        }
    }

    @Test
    public void testGetReplyForWriteTimeout() throws Exception {
        final Future<JVppReply<?>> future = mock(Future.class);
        when(future.get(anyLong(), eq(TimeUnit.SECONDS))).thenThrow(TimeoutException.class);
        final InstanceIdentifier<AnDataObject>
                replyType = InstanceIdentifier.create(AnDataObject.class);
        try {
            getReplyForWrite(future, replyType);
        } catch (WriteTimeoutException e) {
            assertTrue(e.getCause() instanceof TimeoutException);
            assertEquals(replyType, e.getFailedId());
            return;
        }
        fail("WriteTimeoutException was expected");
    }

    @Test
    public void testGetReplyForReadTimeout() throws Exception {
        final Future<JVppReply<?>> future = mock(Future.class);
        final InstanceIdentifier<AnDataObject> replyType =
                InstanceIdentifier.create(AnDataObject.class);
        when(future.get(anyLong(), eq(TimeUnit.SECONDS))).thenThrow(TimeoutException.class);
        try {
            getReplyForRead(future, replyType);
        } catch (ReadTimeoutException e) {
            assertTrue(e.getCause() instanceof TimeoutException);
            assertEquals(replyType, e.getFailedId());
            return;
        }
        fail("ReadTimeoutException was expected");
    }
}