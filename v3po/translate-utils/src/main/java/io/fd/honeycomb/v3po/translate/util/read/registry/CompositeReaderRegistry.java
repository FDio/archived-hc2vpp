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

package io.fd.honeycomb.v3po.translate.util.read.registry;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import io.fd.honeycomb.v3po.translate.read.ListReader;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.read.Reader;
import io.fd.honeycomb.v3po.translate.read.registry.ReaderRegistry;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple reader registry able to perform and aggregated read (ROOT read) on top of all provided readers. Also able to
 * delegate a specific read to one of the delegate readers.
 * <p/>
 * This could serve as a utility to hold & hide all available readers in upper layers.
 */
public final class CompositeReaderRegistry implements ReaderRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeReaderRegistry.class);

    private final Map<Class<? extends DataObject>, Reader<? extends DataObject, ? extends Builder<?>>> rootReaders;

    /**
     * Create new {@link CompositeReaderRegistry}.
     *
     * @param rootReaders List of delegate readers
     */
    public CompositeReaderRegistry(@Nonnull final List<Reader<? extends DataObject, ? extends Builder<?>>> rootReaders) {
        this.rootReaders = RWUtils.uniqueLinkedIndex(checkNotNull(rootReaders), RWUtils.MANAGER_CLASS_FUNCTION);
    }

    @VisibleForTesting
    Map<Class<? extends DataObject>, Reader<? extends DataObject, ? extends Builder<?>>> getRootReaders() {
        return rootReaders;
    }

    @Override
    @Nonnull
    public Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> readAll(
        @Nonnull final ReadContext ctx) throws ReadFailedException {

        LOG.debug("Reading from all delegates: {}", this);
        LOG.trace("Reading from all delegates: {}", rootReaders.values());

        final Multimap<InstanceIdentifier<? extends DataObject>, DataObject> objects = LinkedListMultimap.create();
        for (Reader<? extends DataObject, ? extends Builder<?>> rootReader : rootReaders.values()) {
            LOG.debug("Reading from delegate: {}", rootReader);

            if (rootReader instanceof ListReader) {
                final List<? extends DataObject> listEntries =
                        ((ListReader) rootReader).readList(rootReader.getManagedDataObjectType(), ctx);
                if (!listEntries.isEmpty()) {
                    objects.putAll(rootReader.getManagedDataObjectType(), listEntries);
                }
            } else {
                final Optional<? extends DataObject> read = rootReader.read(rootReader.getManagedDataObjectType(), ctx);
                if (read.isPresent()) {
                    objects.putAll(rootReader.getManagedDataObjectType(), Collections.singletonList(read.get()));
                }
            }
        }

        return objects;
    }

    @Nonnull
    @Override
    public Optional<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                               @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        final InstanceIdentifier.PathArgument first = checkNotNull(
                Iterables.getFirst(id.getPathArguments(), null), "Empty id");
        final Reader<? extends DataObject, ? extends Builder<?>> reader = rootReaders.get(first.getType());
        checkNotNull(reader,
                "Unable to read %s. Missing reader. Current readers for: %s", id, rootReaders.keySet());
        LOG.debug("Reading from delegate: {}", reader);
        return reader.read(id, ctx);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + rootReaders.keySet().stream().map(Class::getSimpleName).collect(Collectors.toList());
    }
}
