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

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.dto.JVppReply;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Trait providing logic for consuming reply's to jvpp api calls
 */
public interface JvppReplyConsumer {

    int DEFAULT_TIMEOUT_IN_SECONDS = 5;

    /**
     * Consumes reply for jvpp call representing any write operation
     * Should be used in case of calls where it's not clear which write crud operation respective
     * call represents, for ex. setRouting
     */
    default <REP extends JVppReply<?>> REP getReplyForWrite(@Nonnull Future<REP> future,
                                                            @Nonnull final InstanceIdentifier<?> replyType)
            throws WriteFailedException {
        return getReplyForWrite(future, replyType, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    /**
     * Consumes reply for jvpp call representing any write operation
     * Should be used in case of calls where it's not clear which write crud operation respective
     * call represents, for ex. setRouting
     */
    default <REP extends JVppReply<?>> REP getReplyForWrite(@Nonnull Future<REP> future,
                                                            @Nonnull final InstanceIdentifier<?> replyType,
                                                            @Nonnegative final int timeoutInSeconds)
            throws WriteFailedException {
        try {
            return getReply(future, timeoutInSeconds);
        } catch (TimeoutException e) {
            throw new WriteTimeoutException(replyType, e);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException(replyType, e);
        }

    }

    /**
     * Consumes reply for jvpp call representing create operation
     */
    default <REP extends JVppReply<?>> REP getReplyForCreate(@Nonnull Future<REP> future,
                                                             @Nonnull final InstanceIdentifier<?> replyType,
                                                             @Nonnull final DataObject data)
            throws WriteFailedException {
        return getReplyForCreate(future, replyType, data, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    /**
     * Consumes reply for jvpp call representing create operation
     */
    default <REP extends JVppReply<?>> REP getReplyForCreate(@Nonnull Future<REP> future,
                                                             @Nonnull final InstanceIdentifier<?> replyType,
                                                             @Nonnull final DataObject data,
                                                             @Nonnegative final int timeoutInSeconds)
            throws WriteFailedException {
        try {
            return getReply(future, timeoutInSeconds);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(replyType, data, e);
        } catch (TimeoutException e) {
            throw new WriteTimeoutException(replyType, e);
        }
    }

    /**
     * Consumes reply for jvpp call representing update operation
     */
    default <REP extends JVppReply<?>> REP getReplyForUpdate(@Nonnull Future<REP> future,
                                                             @Nonnull final InstanceIdentifier<?> replyType,
                                                             @Nonnull final DataObject dataBefore,
                                                             @Nonnull final DataObject dataAfter)
            throws WriteFailedException {
        return getReplyForUpdate(future, replyType, dataBefore, dataAfter, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    /**
     * Consumes reply for jvpp call representing update operation
     */
    default <REP extends JVppReply<?>> REP getReplyForUpdate(@Nonnull Future<REP> future,
                                                             @Nonnull final InstanceIdentifier<?> replyType,
                                                             @Nonnull final DataObject dataBefore,
                                                             @Nonnull final DataObject dataAfter,
                                                             @Nonnegative final int timeoutInSeconds)
            throws WriteFailedException {
        try {
            return getReply(future, timeoutInSeconds);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.UpdateFailedException(replyType, dataBefore, dataAfter, e);
        } catch (TimeoutException e) {
            throw new WriteTimeoutException(replyType, e);
        }
    }

    /**
     * Consumes reply for jvpp call representing delete operation
     */
    default <REP extends JVppReply<?>> REP getReplyForDelete(@Nonnull Future<REP> future,
                                                             @Nonnull final InstanceIdentifier<?> replyType)
            throws WriteFailedException {
        return getReplyForDelete(future, replyType, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    /**
     * Consumes reply for jvpp call representing delete operation
     */
    default <REP extends JVppReply<?>> REP getReplyForDelete(@Nonnull Future<REP> future,
                                                             @Nonnull final InstanceIdentifier<?> replyType,
                                                             @Nonnegative final int timeoutInSeconds)
            throws WriteFailedException {
        try {
            return getReply(future, timeoutInSeconds);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.DeleteFailedException(replyType, e);
        } catch (TimeoutException e) {
            throw new WriteTimeoutException(replyType, e);
        }
    }

    default <REP extends JVppReply<?>> REP getReplyForRead(@Nonnull Future<REP> future,
                                                           @Nonnull final InstanceIdentifier<?> replyType)
            throws ReadFailedException {
        return getReplyForRead(future, replyType, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    default <REP extends JVppReply<?>> REP getReplyForRead(@Nonnull Future<REP> future,
                                                           @Nonnull final InstanceIdentifier<?> replyType,
                                                           @Nonnegative final int timeoutInSeconds)
            throws ReadFailedException {
        try {
            return getReply(future, timeoutInSeconds);
        } catch (TimeoutException e) {
            throw new ReadTimeoutException(replyType, e);
        } catch (VppBaseCallException e) {
            throw new ReadFailedException(replyType, e);
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
