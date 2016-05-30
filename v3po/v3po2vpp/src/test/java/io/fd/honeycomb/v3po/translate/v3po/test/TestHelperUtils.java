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

package io.fd.honeycomb.v3po.translate.v3po.test;

import org.openvpp.jvpp.VppCallbackException;
import org.openvpp.jvpp.dto.JVppReply;

import java.util.concurrent.CompletableFuture;

public class TestHelperUtils {
    /**
     * Static helper method for creation of Exception failure state in CompletableFuture object
     * @param retval result of the operation in exception
     * @return CompletableFuture with VppCallbackException as a cause
     */
    public static CompletableFuture<? extends JVppReply>  createFutureException(final int retval) {
        final CompletableFuture<? extends JVppReply> replyFuture = new CompletableFuture<>();
        replyFuture.completeExceptionally(new VppCallbackException("test-call", 1, retval));
        return replyFuture;
    }
}
