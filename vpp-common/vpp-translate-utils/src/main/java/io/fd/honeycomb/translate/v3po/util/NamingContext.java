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

package io.fd.honeycomb.translate.v3po.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.MappingContext;
import java.util.stream.Collector;
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
 * Utility adapter on top of {@link MappingContext} storing integer to string mappings according to naming-context yang
 * model
 */
public final class NamingContext implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NamingContext.class);

    private final String artificialNamePrefix;
    private final KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContext, NamingContextKey>
        namingContextIid;

    private static final Collector<Mapping, ?, Mapping> SINGLE_ITEM_COLLECTOR = RWUtils.singleItemCollector();

    /**
     * Create new naming context
     *
     * @param artificialNamePrefix artificial name to be used for items without a name in VPP (or not provided)
     * @param instanceName name of this context instance. Will be used as list item identifier within context data tree
     */
    public NamingContext(@Nonnull final String artificialNamePrefix, @Nonnull final String instanceName) {
        this.artificialNamePrefix = artificialNamePrefix;
        namingContextIid = InstanceIdentifier.create(Contexts.class).child(
            org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContext.class,
            new NamingContextKey(instanceName));
    }

    /**
     * Retrieve name for mapping stored provided mappingContext instance. If not present, artificial name will be
     * generated.
     *
     * @param index index of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     *
     * @return name mapped to provided index
     */
    @Nonnull
    public synchronized String getName(final int index, @Nonnull final MappingContext mappingContext) {
        if (!containsName(index, mappingContext)) {
            final String artificialName = getArtificialName(index);
            addName(index, artificialName, mappingContext);
        }

        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));
        checkState(read.isPresent(), "Mapping for index: %s is not present. But should be", index);

        return read.get().getMapping().stream()
            .filter(mapping -> mapping.getIndex().equals(index))
            .collect(SINGLE_ITEM_COLLECTOR).getName();
    }

    /**
     * Retrieve name for mapping stored provided mappingContext instance. if present
     *
     * @param index index of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     *
     * @return name mapped to provided index
     */
    @Nonnull
    public synchronized Optional<String> getNameIfPresent(final int index,
                                                          @Nonnull final MappingContext mappingContext) {
        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));

        return read.isPresent()
                ? Optional.of(read.get().getMapping().stream()
                    .filter(mapping -> mapping.getIndex().equals(index))
                    .collect(SINGLE_ITEM_COLLECTOR)
                    .getName())
                : Optional.absent();
    }

    /**
     * Check whether mapping is present for index.
     *
     * @param index index of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     *
     * @return true if present, false otherwise
     */
    public synchronized boolean containsName(final int index, @Nonnull final MappingContext mappingContext) {
        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));
        return read.isPresent()
            ? read.get().getMapping().stream().anyMatch(mapping -> mapping.getIndex().equals(index))
            : false;
    }


    /**
     * Add mapping to current context
     *
     * @param index index of a mapped item
     * @param name name of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     */
    public synchronized void addName(final int index, final String name, final MappingContext mappingContext) {
        final KeyedInstanceIdentifier<Mapping, MappingKey> mappingIid = getMappingIid(name);
        mappingContext.put(mappingIid, new MappingBuilder().setIndex(index).setName(name).build());
    }

    private KeyedInstanceIdentifier<Mapping, MappingKey> getMappingIid(final String name) {
        return namingContextIid.child(Mappings.class).child(Mapping.class, new MappingKey(name));
    }

    /**
     * Remove mapping from current context
     *
     * @param name name of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     */
    public synchronized void removeName(final String name, final MappingContext mappingContext) {
        mappingContext.delete(getMappingIid(name));
    }

    /**
     * Returns index value associated with the given name.
     *
     * @param name the name whose associated index value is to be returned
     * @param mappingContext mapping context providing context data for current transaction
     *
     * @return integer index value matching supplied name
     * @throws IllegalArgumentException if name was not found
     */
    public synchronized int getIndex(final String name, final MappingContext mappingContext) {
        final Optional<Mapping> read = mappingContext.read(getMappingIid(name));
        checkArgument(read.isPresent(), "No mapping stored for name: %s", name);
        return read.get().getIndex();

    }

    /**
     * Check whether mapping is present for name.
     *
     * @param name name of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     *
     * @return true if present, false otherwise
     */
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
