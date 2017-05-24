/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.lisp.gpe.translate.ctx;

import static java.lang.String.format;

import io.fd.honeycomb.translate.MappingContext;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.GpeEntryIdentificationCtxAugmentation;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.GpeEntryIdentificationContexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.GpeEntryIdentification;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.GpeEntryIdentificationKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.mapping.GpeEntryIdentificator;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.mapping.GpeEntryIdentificatorBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpeEntryMappingContextImpl implements GpeEntryMappingContext {

    private static final Logger LOG = LoggerFactory.getLogger(GpeEntryMappingContextImpl.class);

    private final KeyedInstanceIdentifier<GpeEntryIdentification, GpeEntryIdentificationKey>
            namingContextIid;

    /**
     * Create new naming context
     *
     * @param instanceName name of this context instance. Will be used as list item identifier within context data tree
     */
    public GpeEntryMappingContextImpl(@Nonnull final String instanceName) {
        namingContextIid = InstanceIdentifier.create(Contexts.class)
                .augmentation(GpeEntryIdentificationCtxAugmentation.class)
                .child(GpeEntryIdentificationContexts.class)
                .child(GpeEntryIdentification.class, new GpeEntryIdentificationKey(instanceName));
    }

    @Override
    public void addMapping(@Nonnull final String id,
                           @Nonnull final GpeEntryIdentifier identifier,
                           @Nonnull final MappingContext mappingContext) {
        LOG.debug("Adding mapping for gpe-entry[id={},entry-identifier={}]", id, identifier);
        mappingContext.put(getMappingId(id), getMapping(id, identifier));
        LOG.debug("Mapping for gp-entry[id={}] successfully added", id);
    }

    @Override
    public void removeMapping(@Nonnull final String id,
                              @Nonnull final MappingContext mappingContext) {
        LOG.debug("Removing mapping for gpe-entry[id={}]", id);
        mappingContext.delete(getMappingId(id));
        LOG.debug("Mapping for gpe-entry[id={}] removed", id);
    }

    @Override
    public GpeEntryIdentificator getIdentificatorById(@Nonnull final String id,
                                                      @Nonnull final MappingContext mappingContext) {
        final com.google.common.base.Optional<Mappings> read =
                mappingContext.read(namingContextIid.child(Mappings.class));

        if (read.isPresent()) {
            return Optional.of(read.get())
                    .map(Mappings::getMapping)
                    .map(Collection::stream)
                    .map(mappingStream -> mappingStream
                            .filter(mapping -> mapping.getId().equals(id))
                            .map(Mapping::getGpeEntryIdentificator)
                            .findAny().orElse(null))
                    .orElseThrow(() -> new IllegalStateException(format("No mapping for id %s", id)));

        }
        throw new IllegalStateException(format("No mapping for id %s", id));
    }

    @Override
    public String getIdByEntryIdentifier(@Nonnull final GpeEntryIdentifier identifier,
                                         @Nonnull final MappingContext mappingContext) {
        final com.google.common.base.Optional<Mappings> read =
                mappingContext.read(namingContextIid.child(Mappings.class));

        if (read.isPresent()) {
            return Optional.of(read.get())
                    .map(Mappings::getMapping)
                    .map(Collection::stream)
                    .map(mappingStream -> mappingStream
                            .filter(mapping -> identifier.isSame(mapping.getGpeEntryIdentificator()))
                            .map(Mapping::getId)
                            .findAny().orElse(null))
                    .orElse(addArtificialMapping(identifier, mappingContext));
        }

        return addArtificialMapping(identifier, mappingContext);
    }

    private String addArtificialMapping(@Nonnull final GpeEntryIdentifier identifier,
                                        @Nonnull final MappingContext mappingContext) {
        final String artificialName = buildArtificialName(identifier);
        addMapping(artificialName, identifier, mappingContext);
        return artificialName;
    }

    private String buildArtificialName(@Nonnull final GpeEntryIdentifier identifier) {
        return format("%s_%s_%s", identifier.getVni(), identifier.getLocalEid().getAddress(),
                identifier.getRemoteEid().getAddress());
    }

    private KeyedInstanceIdentifier<Mapping, MappingKey> getMappingId(final String id) {
        return namingContextIid.child(Mappings.class).child(Mapping.class, new MappingKey(id));
    }

    private Mapping getMapping(@Nonnull final String id,
                               @Nonnull final GpeEntryIdentifier identifier) {
        return new MappingBuilder()
                .setId(id)
                .setGpeEntryIdentificator(new GpeEntryIdentificatorBuilder()
                        .setLocalEid(identifier.getLocalEid())
                        .setRemoteEid(identifier.getRemoteEid())
                        .setVni(identifier.getVni())
                        .build())
                .build();
    }
}
