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

package io.fd.honeycomb.vpp.test.util;

import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import io.fd.vpp.jvpp.VppCallbackException;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;

/**
 * <p>VPP translation test helper, that produces instances of {@link CompletableFuture} with desired results.</p>
 * <p>Useful when stubbing {@link FutureJVppCore} methods: <br>{@code when(api.showVersion(any())).thenReturn(future(new
 * ShowVersionReply()));}</p>
 */
public interface FutureProducer {

    /**
     * Returns {@link CompletableFuture} with desired result.
     *
     * @param result returned when {@link CompletableFuture#get()} is invoked
     * @param <T>    the result type of returned future
     */
    default <T> CompletableFuture<T> future(@Nonnull final T result) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        future.complete(result);
        return future;
    }

    /**
     * Returns {@link CompletableFuture} with provided {@link Exception} as a result.
     *
     * @param exception to be thrown when {@link CompletableFuture#get()} is invoked
     * @param <T>       the result type of returned future
     */
    default <T> CompletableFuture<T> failedFuture(@Nonnull final Exception exception) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(exception);
        return future;
    }

    /**
     * Returns {@link CompletableFuture} with VppCallbackException(retval = -1) as a cause.
     *
     * @param <T> the result type of returned future
     */
    default <T> CompletableFuture<T> failedFuture() {
        return failedFuture(new VppCallbackException("test-call", 1 /* ctxId */, -1 /* retval */));
    }
}
