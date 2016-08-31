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

package io.fd.honeycomb.translate.v3po.util.cache;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.v3po.util.cache.exceptions.check.DumpCheckFailedException;
import io.fd.honeycomb.translate.v3po.util.cache.exceptions.execution.DumpExecutionFailedException;
import io.fd.honeycomb.translate.v3po.util.cache.noop.NoopDumpPostProcessingFunction;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager responsible for returning Data object dumps<br> either from cache or by invoking specified {@link
 * EntityDumpExecutor}
 */
public final class DumpCacheManager<T, U> {

    private static final Logger LOG = LoggerFactory.getLogger(DumpCacheManager.class);

    private final EntityDumpExecutor<T, U> dumpExecutor;
    private final EntityDumpNonEmptyCheck<T> dumpNonEmptyCheck;
    private final EntityDumpPostProcessingFunction<T> postProcessor;

    private DumpCacheManager(DumpCacheManagerBuilder<T, U> builder) {
        this.dumpExecutor = builder.dumpExecutor;
        this.dumpNonEmptyCheck = builder.dumpNonEmptyCheck;
        this.postProcessor = builder.postProcessingFunction;
    }

    /**
     * Returns {@link Optional<T>} of dump
     */
    public Optional<T> getDump(@Nonnull String entityKey, @Nonnull ModificationCache cache, final U dumpParams)
            throws DumpExecutionFailedException {

        // this key binding to every log has its logic ,because every customizer have its own cache manager and if
        // there is need for debugging/fixing some complex call with a lot of data,you can get lost in those logs
        LOG.debug("Loading dump for KEY[{}]", entityKey);

        T dump = (T) cache.get(entityKey);

        if (dump == null) {
            LOG.debug("Dump for KEY[{}] not present in cache,invoking dump executor", entityKey);
            // binds and execute dump to be thread-save
            dump = dumpExecutor.executeDump(dumpParams);

            // this is not a critical exception, so its only logged here
            try {
                dumpNonEmptyCheck.assertNotEmpty(dump);
            } catch (DumpCheckFailedException e) {
                LOG.warn("Dump for KEY[{}] has been resolved as empty", entityKey, e);
                return Optional.absent();
            }

            // no need to check if post processor active,if wasn't set,default no-op will be used
            LOG.debug("Post-processing dump for KEY[{}]", entityKey);
            dump = postProcessor.apply(dump);

            LOG.debug("Caching dump for KEY[{}]", entityKey);
            cache.put(entityKey, dump);
            return Optional.of(dump);
        } else {
            return Optional.of(dump);
        }
    }

    public static final class DumpCacheManagerBuilder<T, U> {

        private EntityDumpExecutor<T, U> dumpExecutor;
        private EntityDumpNonEmptyCheck<T> dumpNonEmptyCheck;
        private EntityDumpPostProcessingFunction<T> postProcessingFunction;

        public DumpCacheManagerBuilder() {
            // for cases when user does not set specific post-processor
            postProcessingFunction = new NoopDumpPostProcessingFunction<T>();
        }

        public DumpCacheManagerBuilder<T, U> withExecutor(@Nonnull EntityDumpExecutor<T, U> executor) {
            this.dumpExecutor = executor;
            return this;
        }

        public DumpCacheManagerBuilder<T, U> withNonEmptyPredicate(@Nonnull EntityDumpNonEmptyCheck<T> check) {
            this.dumpNonEmptyCheck = check;
            return this;
        }

        public DumpCacheManagerBuilder<T, U> withPostProcessingFunction(
                EntityDumpPostProcessingFunction<T> postProcessingFunction) {
            this.postProcessingFunction = postProcessingFunction;
            return this;
        }

        public DumpCacheManager<T, U> build() {
            checkNotNull(dumpExecutor, "Dump executor cannot be null");
            checkNotNull(dumpNonEmptyCheck, "Dump verifier cannot be null");
            checkNotNull(postProcessingFunction,
                    "Dump post-processor cannot be null cannot be null, default implementation is used if its not set");

            return new DumpCacheManager<>(this);
        }
    }
}


