/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import io.fd.vpp.jvpp.dto.JVppReply;

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