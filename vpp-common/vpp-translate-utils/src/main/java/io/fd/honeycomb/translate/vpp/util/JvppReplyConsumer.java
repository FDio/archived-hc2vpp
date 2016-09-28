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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.dto.JVppReply;

/**
 * Trait providing logic for consuming reply's to jvpp api calls
 */
public interface JvppReplyConsumer {

    int DEFAULT_TIMEOUT_IN_SECONDS = 5;

    default <REP extends JVppReply<?>> REP getReplyForWrite(@Nonnull Future<REP> future,
                                                            @Nonnull final InstanceIdentifier<?> replyType)
            throws VppBaseCallException, WriteTimeoutException {
        return getReplyForWrite(future, replyType, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    default <REP extends JVppReply<?>> REP getReplyForWrite(@Nonnull Future<REP> future,
                                                            @Nonnull final InstanceIdentifier<?> replyType,
                                                            @Nonnegative final int timeoutInSeconds)
            throws VppBaseCallException, WriteTimeoutException {
        try {
            return getReply(future, timeoutInSeconds);
        } catch (TimeoutException e) {
            throw new WriteTimeoutException(replyType, e);
        }
    }

    default <REP extends JVppReply<?>> REP getReplyForRead(@Nonnull Future<REP> future,
                                                           @Nonnull final InstanceIdentifier<?> replyType)
            throws VppBaseCallException, ReadTimeoutException {
        return getReplyForRead(future, replyType, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    default <REP extends JVppReply<?>> REP getReplyForRead(@Nonnull Future<REP> future,
                                                           @Nonnull final InstanceIdentifier<?> replyType,
                                                           @Nonnegative final int timeoutInSeconds)
            throws VppBaseCallException, ReadTimeoutException {
        try {
            return getReply(future, timeoutInSeconds);
        } catch (TimeoutException e) {
            throw new ReadTimeoutException(replyType, e);
        }
    }

    default <REP extends JVppReply<?>> REP getReply(@Nonnull Future<REP> future)
            throws TimeoutException, VppBaseCallException {
        return getReply(future, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    default <REP extends JVppReply<?>> REP getReply(@Nonnull Future<REP> future,
                                                    @Nonnegative final int timeoutInSeconds)
            throws TimeoutException, VppBaseCallException {
        try {
            checkArgument(timeoutInSeconds > 0, "Timeout cannot be < 0");
            return future.get(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        } catch (ExecutionException e) {
            // Execution exception could generally contains any exception
            // when using exceptions instead of return codes just rethrow it for processing on corresponding place
            if (e instanceof ExecutionException && (e.getCause() instanceof VppBaseCallException)) {
                throw (VppBaseCallException) (e.getCause());
            }
            throw new IllegalStateException(e);
        }
    }
}
