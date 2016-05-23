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

package io.fd.honeycomb.v3po.translate.util;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.fd.honeycomb.v3po.translate.MappingContext;
import io.fd.honeycomb.v3po.translate.ModificationCache;
import io.fd.honeycomb.v3po.translate.read.ChildReader;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import java.io.Closeable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reader wrapper that periodically invokes a read to determine whether reads are still fully functional.
 * In case a specific error occurs, Keep-alive failure listener gets notified.
 */
public final class KeepaliveReaderWrapper<D extends DataObject> implements ChildReader<D>, Runnable, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(KeepaliveReaderWrapper.class);

    private static final NoopReadContext CTX = new NoopReadContext();

    private final ChildReader<D> delegate;
    private final Class<? extends Exception> exceptionType;
    private final KeepaliveFailureListener failureListener;
    private final ScheduledFuture<?> scheduledFuture;

    /**
     * Create new Keepalive wrapper
     *
     * @param delegate underlying reader performing actual reads
     * @param executor scheduled executor service to schedule keepalive calls
     * @param exception type of exception used to differentiate keepalive exception from other exceptions
     * @param delayInSeconds number of seconds to wait between keepalive calls
     * @param failureListener listener to be called whenever a keepalive failure is detected
     */
    public KeepaliveReaderWrapper(@Nonnull final ChildReader<D> delegate,
                                  @Nonnull final ScheduledExecutorService executor,
                                  @Nonnull final Class<? extends Exception> exception,
                                  @Nonnegative final int delayInSeconds,
                                  @Nonnull final KeepaliveFailureListener failureListener) {
        this.delegate = delegate;
        this.exceptionType = exception;
        this.failureListener = failureListener;
        Preconditions.checkArgument(delayInSeconds > 0, "Delay cannot be < 0");
        LOG.debug("Starting keep-alive execution on top of: {} with delay of: {} seconds", delegate, delayInSeconds);
        scheduledFuture = executor.scheduleWithFixedDelay(this, delayInSeconds, delayInSeconds, TimeUnit.SECONDS);
    }

    @Nonnull
    @Override
    public Optional<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                               @Nonnull final ReadContext ctx) throws ReadFailedException {
        return delegate.read(id, ctx);
    }

    @Override
    public void read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                     @Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final ReadContext ctx)
        throws ReadFailedException {
        delegate.read(id, parentBuilder, ctx);
    }

    @Nonnull
    @Override
    public InstanceIdentifier<D> getManagedDataObjectType() {
        return delegate.getManagedDataObjectType();
    }

    @Override
    public void run() {
        LOG.trace("Invoking keepalive");
        try {
            final Optional<? extends DataObject> read = read(delegate.getManagedDataObjectType(), CTX);
            LOG.debug("Keepalive executed successfully with data: {}", read);
        } catch (Exception e) {
            if(exceptionType.isAssignableFrom(e.getClass())) {
                LOG.warn("Keepalive failed. Notifying listener", e);
                failureListener.onKeepaliveFailure();
            }
            LOG.warn("Keepalive failed unexpectedly", e);
            throw new IllegalArgumentException("Unexpected failure during keep-alive execution", e);
        }
    }

    @Override
    public void close() {
        // Do not interrupt, it's not our executor
        scheduledFuture.cancel(false);
    }

    /**
     * Listener that gets called whenever keepalive fails as expected
     */
    public interface KeepaliveFailureListener {

        void onKeepaliveFailure();
    }

    private static final class NoopMappingContext implements MappingContext {
        @Override
        public <T extends DataObject> Optional<T> read(@Nonnull final InstanceIdentifier<T> currentId) {
            return Optional.absent();
        }

        @Override
        public void delete(final InstanceIdentifier<?> path) {}

        @Override
        public <T extends DataObject> void merge(final InstanceIdentifier<T> path, final T data) {}

        @Override
        public <T extends DataObject> void put(final InstanceIdentifier<T> path, final T data) {}

        @Override
        public void close() {}
    }

    private static class NoopReadContext implements ReadContext {

        private final ModificationCache modificationCache = new ModificationCache();

        @Nonnull
        @Override
        public ModificationCache getModificationCache() {
            return modificationCache;
        }

        @Nonnull
        @Override
        public MappingContext getMappingContext() {
            return new NoopMappingContext();
        }

        @Override
        public void close() {

        }
    }
}
