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

package io.fd.hc2vpp.common.translate.util;

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.dto.JVppReply;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trait providing logic for consuming reply's to jvpp api calls
 */
public interface JvppReplyConsumer {

    JvppReplyConsumer INSTANCE = new JvppReplyConsumer() {
    };

    /**
     * Consumes reply for jvpp call representing any write operation
     * Should be used in case of calls where it's not clear which write crud operation respective
     * call represents, for ex. setRouting
     */
    default <R extends JVppReply<?>> R getReplyForWrite(@Nonnull Future<R> future,
                                                            @Nonnull final InstanceIdentifier<?> replyType)
            throws WriteFailedException {

        return getReplyForWrite(future, replyType, JvppReplyTimeoutHolder.getTimeout());
    }

    /**
     * Consumes reply for jvpp call representing any write operation
     * Should be used in case of calls where it's not clear which write crud operation respective
     * call represents, for ex. setRouting
     */
    default <R extends JVppReply<?>> R getReplyForWrite(@Nonnull Future<R> future,
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
    default <R extends JVppReply<?>> R getReplyForCreate(@Nonnull Future<R> future,
                                                             @Nonnull final InstanceIdentifier<?> replyType,
                                                             @Nonnull final DataObject data)
        throws WriteFailedException.CreateFailedException {
        return getReplyForCreate(future, replyType, data, JvppReplyTimeoutHolder.getTimeout());
    }

    /**
     * Consumes reply for jvpp call representing create operation
     */
    default <R extends JVppReply<?>> R getReplyForCreate(@Nonnull Future<R> future,
                                                             @Nonnull final InstanceIdentifier<?> replyType,
                                                             @Nonnull final DataObject data,
                                                             @Nonnegative final int timeoutInSeconds)
        throws WriteFailedException.CreateFailedException {
        try {
            return getReply(future, timeoutInSeconds);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(replyType, data, e);
        } catch (TimeoutException e) {
            throw new WriteFailedException.CreateFailedException(replyType, data,
                new WriteTimeoutException(replyType, e));
        }
    }

    /**
     * Consumes reply for jvpp call representing update operation
     */
    default <R extends JVppReply<?>> R getReplyForUpdate(@Nonnull Future<R> future,
                                                             @Nonnull final InstanceIdentifier<?> replyType,
                                                             @Nonnull final DataObject dataBefore,
                                                             @Nonnull final DataObject dataAfter)
        throws WriteFailedException.UpdateFailedException {
        return getReplyForUpdate(future, replyType, dataBefore, dataAfter, JvppReplyTimeoutHolder.getTimeout());
    }

    /**
     * Consumes reply for jvpp call representing update operation
     */
    default <R extends JVppReply<?>> R getReplyForUpdate(@Nonnull Future<R> future,
                                                             @Nonnull final InstanceIdentifier<?> replyType,
                                                             @Nonnull final DataObject dataBefore,
                                                             @Nonnull final DataObject dataAfter,
                                                             @Nonnegative final int timeoutInSeconds)
        throws WriteFailedException.UpdateFailedException {
        try {
            return getReply(future, timeoutInSeconds);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.UpdateFailedException(replyType, dataBefore, dataAfter, e);
        } catch (TimeoutException e) {
            throw new WriteFailedException.UpdateFailedException(replyType, dataBefore, dataAfter,
                new WriteTimeoutException(replyType, e));
        }
    }

    /**
     * Consumes reply for jvpp call representing delete operation
     */
    default <R extends JVppReply<?>> R getReplyForDelete(@Nonnull Future<R> future,
                                                             @Nonnull final InstanceIdentifier<?> replyType)
        throws WriteFailedException.DeleteFailedException {
        return getReplyForDelete(future, replyType, JvppReplyTimeoutHolder.getTimeout());
    }

    /**
     * Consumes reply for jvpp call representing delete operation
     */
    default <R extends JVppReply<?>> R getReplyForDelete(@Nonnull Future<R> future,
                                                             @Nonnull final InstanceIdentifier<?> replyType,
                                                             @Nonnegative final int timeoutInSeconds)
        throws WriteFailedException.DeleteFailedException {
        try {
            return getReply(future, timeoutInSeconds);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.DeleteFailedException(replyType, e);
        } catch (TimeoutException e) {
            throw new WriteFailedException.DeleteFailedException(replyType, new WriteTimeoutException(replyType, e));
        }
    }

    default <R extends JVppReply<?>> R getReplyForRead(@Nonnull Future<R> future,
                                                           @Nonnull final InstanceIdentifier<?> replyType)
            throws ReadFailedException {
        return getReplyForRead(future, replyType, JvppReplyTimeoutHolder.getTimeout());
    }

    default <R extends JVppReply<?>> R getReplyForRead(@Nonnull Future<R> future,
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

    default <R extends JVppReply<?>> R getReply(@Nonnull Future<R> future)
            throws TimeoutException, VppBaseCallException {
        return getReply(future, JvppReplyTimeoutHolder.getTimeout());
    }

    default <R extends JVppReply<?>> R getReply(@Nonnull Future<R> future,
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
            if (e.getCause() instanceof VppBaseCallException) {
                throw (VppBaseCallException) (e.getCause());
            }
            throw new IllegalStateException(e);
        }
    }

    /**
     * Wrapper for reply timeout
     */
    class JvppReplyTimeoutHolder {
        private static final Logger LOG = LoggerFactory.getLogger(JvppReplyTimeoutHolder.class);
        private static Optional<Integer> timeout = Optional.empty();

        private JvppReplyTimeoutHolder() {
            throw new UnsupportedOperationException("Utility class cannot be instantiated.");
        }

        public static void setupTimeout(@Nonnegative final int jvppTimeout) {
            if (timeout.isPresent()) {
                // do not fail on reconfigure, to not disturb restarts
                LOG.warn("JVpp timeout already configured");
                return;
            }
            timeout = Optional.of(jvppTimeout);
            LOG.info("Jvpp reply timeout configured to {} seconds", timeout.get());
        }

        public static int getTimeout() {
            return timeout.orElse(5);
        }
    }
}
