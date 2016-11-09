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

package io.fd.honeycomb.lisp.context.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.util.RWUtils;
import java.util.stream.Collector;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.AdjacenciesIdentificationContexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.AdjacenciesIdentification;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.AdjacenciesIdentificationKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.mapping.EidIdentificatorPair;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.mapping.EidIdentificatorPairBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.MappingId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class AdjacenciesMappingContext {

    private static final Collector<Mapping, ?, Mapping> SINGLE_ITEM_COLLECTOR = RWUtils.singleItemCollector();

    private final KeyedInstanceIdentifier<AdjacenciesIdentification, AdjacenciesIdentificationKey>
            namingContextIid;

    /**
     * Create new naming context
     *
     * @param instanceName name of this context instance. Will be used as list item identifier within context data tree
     */
    public AdjacenciesMappingContext(@Nonnull final String instanceName) {
        namingContextIid = InstanceIdentifier.create(AdjacenciesIdentificationContexts.class).child(
                AdjacenciesIdentification.class, new AdjacenciesIdentificationKey(instanceName));
    }

    /**
     * Retrieve name for mapping stored provided mappingContext instance.
     *
     * @param localEidId     {@code MappingId} for local eid
     * @param remoteEidId    {@code MappingId} for remote eid
     * @param mappingContext mapping context providing context data for current transaction
     * @return name mapped to provided index
     */
    @Nonnull
    public synchronized String getAdjacencyId(
            @Nonnull final String localEidId,
            @Nonnull final String remoteEidId,
            @Nonnull final MappingContext mappingContext) {

        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));
        checkState(read.isPresent(), "No adjacencies mappings stored");

        return read.get().getMapping().stream()
                .filter(mapping -> isSame(pairForCombination(localEidId, remoteEidId), mapping))
                .collect(SINGLE_ITEM_COLLECTOR).getId();
    }

    private boolean isSame(final EidIdentificatorPair currentPair, final Mapping mapping) {
        // EidIdentificatorPair is container so it needs to be compared like this
        final EidIdentificatorPair mappingPair = mapping.getEidIdentificatorPair();
        return currentPair.getLocalEidId().equals(mappingPair.getLocalEidId())
                && currentPair.getRemoteEidId().equals(mappingPair.getRemoteEidId());
    }

    private EidIdentificatorPair pairForCombination(final @Nonnull String localEidId,
                                                    final @Nonnull String remoteEidId) {
        return new EidIdentificatorPairBuilder()
                .setLocalEidId(new MappingId(localEidId))
                .setRemoteEidId(new MappingId(remoteEidId))
                .build();
    }

    /**
     * Check whether mapping is present for index.
     *
     * @param localEidId     {@code MappingId} for local eid
     * @param remoteEidId    {@code MappingId} for remote eid
     * @param mappingContext mapping context providing context data for current transaction
     * @return true if present, false otherwise
     */
    public synchronized boolean containsId(
            @Nonnull final String localEidId,
            @Nonnull final String remoteEidId,
            @Nonnull final MappingContext mappingContext) {
        final Optional<Mappings> read = mappingContext.read(namingContextIid.child(Mappings.class));

        return read.isPresent() &&
                read.get().getMapping()
                        .stream()
                        .anyMatch(mapping -> isSame(pairForCombination(localEidId, remoteEidId), mapping));
    }

    /**
     * Add mapping to current context
     *
     * @param index          index of a mapped item
     * @param localEidId     {@code MappingId} for local eid
     * @param remoteEidId    {@code MappingId} for remote eid
     * @param mappingContext mapping context providing context data for current transaction
     */
    public synchronized void addEidPair(
            @Nonnull final String index,
            @Nonnull final String localEidId,
            @Nonnull final String remoteEidId,
            final MappingContext mappingContext) {

        final KeyedInstanceIdentifier<Mapping, MappingKey> mappingIid = getMappingIid(index);
        mappingContext.put(mappingIid, new MappingBuilder().setId(index).setEidIdentificatorPair(
                pairForCombination(localEidId, remoteEidId)).build());
    }

    private KeyedInstanceIdentifier<Mapping, MappingKey> getMappingIid(final String index) {
        return namingContextIid.child(Mappings.class).child(Mapping.class, new MappingKey(index));
    }


    /**
     * Remove mapping from current context
     *
     * @param index          identificator of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     */
    public synchronized void removeForIndex(@Nonnull final String index, final MappingContext mappingContext) {
        mappingContext.delete(getMappingIid(index));
    }

    /**
     * Returns index value associated with the given name.
     *
     * @param index          index whitch should value sits on
     * @param mappingContext mapping context providing context data for current transaction
     * @return integer index value matching supplied name
     * @throws IllegalArgumentException if name was not found
     */
    public synchronized EidIdentificatorPair getEidPair(@Nonnull final String index,
                                                        final MappingContext mappingContext) {
        final Optional<Mapping> read = mappingContext.read(getMappingIid(index));
        checkArgument(read.isPresent(), "No mapping stored for index: %s", index);
        return read.get().getEidIdentificatorPair();
    }

    /**
     * Check whether mapping is present for name.
     *
     * @param index          index of a mapped item
     * @param mappingContext mapping context providing context data for current transaction
     * @return true if present, false otherwise
     */
    public synchronized boolean containsEidPairForIndex(@Nonnull final String index,
                                                        @Nonnull final MappingContext mappingContext) {
        return mappingContext.read(getMappingIid(index)).isPresent();
    }
}
