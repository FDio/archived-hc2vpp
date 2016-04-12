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

package io.fd.honeycomb.v3po.impl.trans.r.util;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import io.fd.honeycomb.v3po.impl.trans.r.ListVppReader;
import io.fd.honeycomb.v3po.impl.trans.r.ReaderRegistry;
import io.fd.honeycomb.v3po.impl.trans.r.VppReader;
import io.fd.honeycomb.v3po.impl.trans.util.VppRWUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple reader registry able to perform and aggregated read (ROOT read) on top of all
 * provided readers. Also able to delegate a specific read to one of the delegate readers.
 *
 * This could serve as a utility to hold & hide all available readers in upper layers.
 */
public final class DelegatingReaderRegistry implements ReaderRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(DelegatingReaderRegistry.class);

    private final Map<Class<? extends DataObject>, VppReader<? extends DataObject>> rootReaders;

    /**
     * Create new {@link DelegatingReaderRegistry}
     *
     * @param rootReaders List of delegate readers
     */
    public DelegatingReaderRegistry(@Nonnull final List<VppReader<? extends DataObject>> rootReaders) {
        this.rootReaders = VppRWUtils.uniqueLinkedIndex(checkNotNull(rootReaders), VppRWUtils.MANAGER_CLASS_FUNCTION);
    }

    @Override
    @Nonnull
    public Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> readAll() {
        LOG.debug("Reading from all delegates: {}", this);
        LOG.trace("Reading from all delegates: {}", rootReaders.values());

        final Multimap<InstanceIdentifier<? extends DataObject>, DataObject> objects = LinkedListMultimap.create();
        for (VppReader<? extends DataObject> rootReader : rootReaders.values()) {
            LOG.debug("Reading from delegate: {}", rootReader);

            if(rootReader instanceof ListVppReader) {
                final List<? extends DataObject> listEntries =
                    ((ListVppReader) rootReader).readList(rootReader.getManagedDataObjectType());
                if(!listEntries.isEmpty()) {
                    objects.putAll(rootReader.getManagedDataObjectType(), listEntries);
                }
            } else {
                final Optional<? extends DataObject> read = rootReader.read(rootReader.getManagedDataObjectType());
                if(read.isPresent()) {
                    objects.putAll(rootReader.getManagedDataObjectType(), Collections.singletonList(read.get()));
                }
            }
        }

        return objects;
    }

    @Nonnull
    @Override
    public Optional<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id) {
        final InstanceIdentifier.PathArgument first = checkNotNull(
            Iterables.getFirst(id.getPathArguments(), null), "Empty id");
        final VppReader<? extends DataObject> vppReader = rootReaders.get(first.getType());
        checkNotNull(vppReader,
            "Unable to read %s. Missing reader. Current readers for: %s", id, rootReaders.keySet());
        LOG.debug("Reading from delegate: {}", vppReader);
        return vppReader.read(id);
    }

    /**
     * @throws UnsupportedOperationException This getter is not supported for reader registry since it does not manage a
     *                                       specific node type
     */
    @Nonnull
    @Override
    public InstanceIdentifier<DataObject> getManagedDataObjectType() {
        throw new UnsupportedOperationException("Root registry has no type");
    }
}
