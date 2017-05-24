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
import java.util.Collections;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.GpeLocatorPairIdentificationCtxAugmentation;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.GpeLocatorPairIdentificationContexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.GpeLocatorPairIdentification;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.GpeLocatorPairIdentificationKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.mapping.LocatorPairMapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.mapping.LocatorPairMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.mapping.locator.pair.mapping.PairBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpeLocatorPairMappingContextImpl implements GpeLocatorPairMappingContext {

    private static final Logger LOG = LoggerFactory.getLogger(GpeLocatorPairMappingContextImpl.class);

    private final KeyedInstanceIdentifier<GpeLocatorPairIdentification, GpeLocatorPairIdentificationKey>
            namingContextIid;

    public GpeLocatorPairMappingContextImpl(@Nonnull final String instanceName) {
        namingContextIid = InstanceIdentifier.create(Contexts.class)
                .augmentation(GpeLocatorPairIdentificationCtxAugmentation.class)
                .child(GpeLocatorPairIdentificationContexts.class)
                .child(GpeLocatorPairIdentification.class, new GpeLocatorPairIdentificationKey(instanceName));
    }


    @Override
    public void addMapping(@Nonnull final String entryId,
                           @Nonnull final String locatorId,
                           @Nonnull final GpeLocatorPair pair,
                           @Nonnull final MappingContext mappingContext) {
        LOG.debug("Adding mapping for Gpe entry to locator id[entry-id={},locator-pair-id={}]", entryId, locatorId);
        mappingContext.merge(getMappingId(entryId), getMappingData(entryId, locatorId, pair));
        LOG.debug("Mapping for Gpe entry to locator id[entry-id={},locator-pair-id={}] successfully added", entryId,
                locatorId);
    }

    @Override
    public void removeMapping(@Nonnull final String entryId,
                              @Nonnull final MappingContext mappingContext) {
        LOG.debug("Removing all mappings for Gpe entry[id={}]", entryId);
        mappingContext.delete(getMappingId(entryId));
        LOG.debug("All mappings for Gpe entry[id={}] removed", entryId);
    }

    @Override
    public LocatorPairMapping getMapping(@Nonnull final String entryId,
                                         @Nonnull final GpeLocatorPair pair,
                                         @Nonnull final MappingContext mappingContext) {
        return mappingContext.read(getMappingId(entryId))
                .or(new MappingBuilder().setLocatorPairMapping(Collections.emptyList()).build())
                .getLocatorPairMapping()
                .stream()
                .filter(mapping -> pair.isSame(mapping.getPair()))
                .findAny().orElseGet(() -> {
                    final String artificialLocatorId = artificialLocatorPairId(entryId, pair);
                    addMapping(entryId, artificialLocatorId, pair, mappingContext);
                    return getMapping(entryId, artificialLocatorId, mappingContext);
                });
    }

    @Override
    public LocatorPairMapping getMapping(@Nonnull final String entryId,
                                         @Nonnull final String locatorId,
                                         @Nonnull final MappingContext mappingContext) {
        return mappingContext.read(getMappingId(entryId))
                .or(new MappingBuilder().setLocatorPairMapping(Collections.emptyList()).build())
                .getLocatorPairMapping()
                .stream()
                .filter(mapping -> mapping.getId().equals(locatorId))
                .findAny().orElseThrow(() -> new IllegalArgumentException(
                        format("No mapping for entry %s|locator %s", entryId, locatorId)));
    }

    private String artificialLocatorPairId(final String entryId, final GpeLocatorPair pair) {
        return format("%s_%s_%s", entryId, pair.getLocalAddress(), pair.getRemoteAddress());
    }

    private Mapping getMappingData(final String entryId,
                                   final String locatorId,
                                   final GpeLocatorPair identifier) {
        return new MappingBuilder()
                .setId(entryId)
                .setLocatorPairMapping(Collections.singletonList(new LocatorPairMappingBuilder()
                        .setId(locatorId)
                        .setPair(new PairBuilder()
                                .setLocalAddress(identifier.getLocalAddress())
                                .setRemoteAddress(identifier.getRemoteAddress())
                                .build())
                        .build())).build();
    }

    private KeyedInstanceIdentifier<Mapping, MappingKey> getMappingId(final String id) {
        return namingContextIid.child(Mappings.class)
                .child(Mapping.class, new MappingKey(id));
    }
}

