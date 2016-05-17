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

package io.fd.honeycomb.v3po.translate.v3po.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.MappingContext;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContextKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility adapter on top of {@link MappingContext}
 */
public final class NamingContext implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NamingContext.class);

    private final String artificialNamePrefix;
    private KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContext, NamingContextKey>
        namingContextIid;

    /**
     * Collector expecting only a single resulting item from a stream
     */
    private static final Collector<Mapping, ?, Mapping> SINGLE_ITEM_COLLECTOR = Collectors.collectingAndThen(
        Collectors.toList(),
        list -> {
            if (list.size() != 1) {
                throw new IllegalStateException("Unexpected size of list: " + list + ". Single item expected");
            }
            return list.get(0);
        });

    public NamingContext(final String artificialNamePrefix, final String instanceName) {
        this.artificialNamePrefix = artificialNamePrefix;
        namingContextIid = InstanceIdentifier.create(Contexts.class).child(
            org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContext.class,
            new NamingContextKey(instanceName));
    }

    @Nonnull
    public synchronized String getName(final int index, final MappingContext mappingContext) {
        if (!containsName(index, mappingContext)) {
            final String artificialName = getArtificialName(index);
            LOG.info("Assigning artificial name: {} for index: {}", artificialName, index);
            addName(index, artificialName, mappingContext);
        }

        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));
        checkState(read.isPresent(), "Mapping for index: %s is not present. But should be", index);

        return read.get().getMapping().stream()
            .filter(mapping -> mapping.getIndex().equals(index))
            .collect(SINGLE_ITEM_COLLECTOR).getName();
    }

    public synchronized boolean containsName(final int index, final MappingContext mappingContext) {
        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));
        return read.isPresent()
            ? read.get().getMapping().stream().anyMatch(mapping -> mapping.getIndex().equals(index))
            : false;
    }

    public synchronized void addName(final int index, final String name, final MappingContext mappingContext) {
        final KeyedInstanceIdentifier<Mapping, MappingKey> mappingIid = getMappingIid(name);
        mappingContext.put(mappingIid, new MappingBuilder().setIndex(index).setName(name).build());
    }

    private KeyedInstanceIdentifier<Mapping, MappingKey> getMappingIid(final String name) {
        return namingContextIid.child(Mappings.class).child(Mapping.class, new MappingKey(name));
    }

    public synchronized void removeName(final String name, final MappingContext mappingContext) {
        mappingContext.delete(getMappingIid(name));
    }

    /**
     * Returns index value associated with the given name.
     *
     * @param name the name whose associated index value is to be returned
     * @return integer index value matching supplied name
     * @throws IllegalArgumentException if name was not found
     */
    public synchronized int getIndex(final String name, final MappingContext mappingContext) {
        final Optional<Mapping> read = mappingContext.read(getMappingIid(name));
        checkArgument(read.isPresent(), "No mapping stored for name: %s", name);
        return read.get().getIndex();

    }

    public synchronized boolean containsIndex(final String name, final MappingContext mappingContext) {
        return mappingContext.read(getMappingIid(name)).isPresent();
    }

    private String getArtificialName(final int index) {
        return artificialNamePrefix + index;
    }

    @Override
    public void close() throws Exception {
        /// Not removing the mapping from backing storage
    }
}
