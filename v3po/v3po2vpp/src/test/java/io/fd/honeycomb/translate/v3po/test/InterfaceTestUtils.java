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

package io.fd.honeycomb.translate.v3po.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.SwInterfaceDetails;
import org.openvpp.jvpp.core.dto.SwInterfaceDetailsReplyDump;
import org.openvpp.jvpp.core.dto.SwInterfaceDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;

public final class InterfaceTestUtils {
    private InterfaceTestUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void whenSwInterfaceDumpThenReturn(final FutureJVppCore api, final List<SwInterfaceDetails> interfaceList)
        throws ExecutionException, InterruptedException, VppBaseCallException, TimeoutException {
        final CompletionStage<SwInterfaceDetailsReplyDump> replyCS = mock(CompletionStage.class);
        final CompletableFuture<SwInterfaceDetailsReplyDump> replyFuture = mock(CompletableFuture.class);
        when(replyCS.toCompletableFuture()).thenReturn(replyFuture);
        final SwInterfaceDetailsReplyDump reply = new SwInterfaceDetailsReplyDump();
        reply.swInterfaceDetails = interfaceList;
        when(replyFuture.get(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(reply);
        when(api.swInterfaceDump(any(SwInterfaceDump.class))).thenReturn(replyCS);
    }
}
