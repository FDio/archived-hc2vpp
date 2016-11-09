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

package io.fd.honeycomb.lisp.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.Mockito.doReturn;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.MappingContext;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.AdjacenciesIdentificationContexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.AdjacenciesIdentification;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.AdjacenciesIdentificationKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.mapping.EidIdentificatorPair;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.mapping.EidIdentificatorPairBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.MappingId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Utility for mocking adjacency mappings
 */
public interface AdjacencyMappingContextTestHelper {

    /**
     * Creates {@link Mapping} for given data.
     *
     * @param pair to be mapped
     * @param id   to be mapped
     * @return eid to id mapping
     */
    default Optional<Mapping> mapping(@Nonnull final EidIdentificatorPair pair, final String id) {
        return Optional.of(new MappingBuilder().setEidIdentificatorPair(pair).setId(id).build());
    }

    /**
     * Creates {@link KeyedInstanceIdentifier} for {@link Mapping} in {@link AdjacenciesIdentificationContexts}.
     *
     * @param id                 identificator of the mapping
     * @param mappingContextName identificator of the mapping context
     * @return identifier for the mapping
     */
    static KeyedInstanceIdentifier<Mapping, MappingKey> mappingIid(@Nonnull final String id,
                                                                   @Nonnull final String mappingContextName) {
        return InstanceIdentifier.create(AdjacenciesIdentificationContexts.class).child(AdjacenciesIdentification.class,
                new AdjacenciesIdentificationKey(mappingContextName)).child(Mappings.class)
                .child(Mapping.class, new MappingKey(id));
    }

    static EidIdentificatorPair pairOf(@Nonnull final String local, @Nonnull final String remote) {
        return new EidIdentificatorPairBuilder()
                .setLocalEidId(new MappingId(checkNotNull(local, "Local id cannot be null")))
                .setRemoteEidId(new MappingId(checkNotNull(remote, "Remote id cannot be null")))
                .build();
    }

    /**
     * Stubs {@link MappingContext#read} to include given mapping in {@link AdjacenciesIdentification}.
     *
     * @param mappingContext    mock instance of {@link MappingContext}
     * @param localEidId        local id for identification pair
     * @param remoteEidId       remote id for identification pair
     * @param mappingName       index to be mapped
     * @param namingContextName name of the naming context
     */
    default void defineAdjacencyMapping(@Nonnull final MappingContext mappingContext, @Nonnull final String localEidId,
                                        @Nonnull final String remoteEidId, @Nonnull final String mappingName,
                                        @Nonnull final String namingContextName) {
        final KeyedInstanceIdentifier<Mapping, MappingKey> mappingIid = mappingIid(mappingName, namingContextName);
        final InstanceIdentifier<Mappings> mappingsIid = mappingIid.firstIdentifierOf(Mappings.class);

        final Optional<Mapping> singleMapping = mapping(pairOf(localEidId, remoteEidId), mappingName);
        final List<Mapping> list = Common.getMappingList(mappingContext, mappingsIid);
        list.add(singleMapping.get());

        doReturn(Optional.of(new MappingsBuilder().setMapping(list).build())).when(mappingContext).read(mappingsIid);
        doReturn(singleMapping).when(mappingContext).read(mappingIid);
    }

    default void noAdjacencyMappingDefined(@Nonnull final MappingContext mappingContext, @Nonnull final String name,
                                           @Nonnull final String namingContextName) {
        final InstanceIdentifier<Mappings> iid =
                mappingIid(name, namingContextName).firstIdentifierOf(Mappings.class);
        final List<Mapping> list = Common.getMappingList(mappingContext, iid);

        doReturn(Optional.of(new MappingsBuilder().setMapping(list).build())).when(mappingContext).read(iid);
        doReturn(Optional.absent()).when(mappingContext).read(mappingIid(name, namingContextName));
    }

    final class Common {
        private static List<Mapping> getMappingList(@Nonnull final MappingContext mappingContext,
                                                    @Nonnull final InstanceIdentifier<Mappings> mappingsIid) {
            final Optional<Mappings> previousMappings = mappingContext.read(mappingsIid);
            final MappingsBuilder mappingsBuilder;
            if (previousMappings != null && previousMappings.isPresent()) {
                mappingsBuilder = new MappingsBuilder(previousMappings.get());
            } else {
                mappingsBuilder = new MappingsBuilder();
                mappingsBuilder.setMapping(Lists.newArrayList());
            }
            return mappingsBuilder.getMapping();
        }
    }

}
