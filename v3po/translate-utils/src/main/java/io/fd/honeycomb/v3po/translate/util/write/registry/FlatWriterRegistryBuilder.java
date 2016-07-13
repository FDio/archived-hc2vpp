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

package io.fd.honeycomb.v3po.translate.util.write.registry;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import io.fd.honeycomb.v3po.translate.util.AbstractSubtreeManagerRegistryBuilderBuilder;
import io.fd.honeycomb.v3po.translate.write.Writer;
import io.fd.honeycomb.v3po.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.honeycomb.v3po.translate.write.registry.WriterRegistry;
import io.fd.honeycomb.v3po.translate.write.registry.WriterRegistryBuilder;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder for {@link FlatWriterRegistry} allowing users to specify inter-writer relationships.
 */
@NotThreadSafe
public final class FlatWriterRegistryBuilder
        extends AbstractSubtreeManagerRegistryBuilderBuilder<Writer<? extends DataObject>, WriterRegistry>
        implements ModifiableWriterRegistryBuilder, WriterRegistryBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(FlatWriterRegistryBuilder.class);

    @Override
    protected Writer<? extends DataObject> getSubtreeHandler(final @Nonnull Set<InstanceIdentifier<?>> handledChildren,
                                                             final @Nonnull Writer<? extends DataObject> writer) {
        return SubtreeWriter.createForWriter(handledChildren, writer);
    }

    /**
     * Create FlatWriterRegistry with writers ordered according to submitted relationships.
     */
    @Override
    public WriterRegistry build() {
        final ImmutableMap<InstanceIdentifier<?>, Writer<?>> mappedWriters = getMappedHandlers();
        LOG.debug("Building writer registry with writers: {}",
                mappedWriters.keySet().stream()
                        .map(InstanceIdentifier::getTargetType)
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", ")));
        LOG.trace("Building writer registry with writers: {}", mappedWriters);
        return new FlatWriterRegistry(mappedWriters);
    }

    @VisibleForTesting
    @Override
    protected ImmutableMap<InstanceIdentifier<?>, Writer<? extends DataObject>> getMappedHandlers() {
        return super.getMappedHandlers();
    }
}
