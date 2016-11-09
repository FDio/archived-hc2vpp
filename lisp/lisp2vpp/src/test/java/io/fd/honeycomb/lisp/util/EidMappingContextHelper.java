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


import static org.mockito.Mockito.doReturn;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.MappingContext;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.Contexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.EidMappingContext;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.EidMappingContextKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.MappingId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Utility class to stub {@link EidMappingContext}
 *
 * TODO - HONEYCOMB-237 - generalize logic for naming context and eid mapping context helper utils if possible
 */
public interface EidMappingContextHelper {

    /**
     * Creates {@link Mapping} for given data.
     *
     * @param eid to be mapped
     * @param id  to be mapped  @return eid to id mapping
     */
    default Optional<Mapping> mapping(@Nonnull final Eid eid, final MappingId id) {
        return Optional.of(new MappingBuilder().setEid(eid).setId(id).build());
    }

    /**
     * Creates {@link KeyedInstanceIdentifier} for {@link Mapping} in {@link EidMappingContext}.
     *
     * @param id                 identificator of the mapping
     * @param mappingContextName identificator of the mapping context
     * @return identifier for the mapping
     */
    default KeyedInstanceIdentifier<Mapping, MappingKey> mappingIid(@Nonnull final MappingId id,
                                                                    @Nonnull final String mappingContextName) {
        return InstanceIdentifier.create(Contexts.class).child(EidMappingContext.class,
                new EidMappingContextKey(mappingContextName)).child(Mappings.class)
                .child(Mapping.class, new MappingKey(id));
    }

    /**
     * Stubs {@link MappingContext#read} to include given mapping in {@link EidMappingContext}.
     *
     * @param mappingContext    mock instance of {@link MappingContext}
     * @param eid               name of the mapping
     * @param mappingName       index to be mapped
     * @param namingContextName name of the naming context
     */
    default void defineEidMapping(@Nonnull final MappingContext mappingContext, @Nonnull final Eid eid,
                                  final MappingId mappingName, @Nonnull final String namingContextName) {
        final KeyedInstanceIdentifier<Mapping, MappingKey> mappingIid = mappingIid(mappingName, namingContextName);
        final InstanceIdentifier<Mappings> mappingsIid = mappingIid.firstIdentifierOf(Mappings.class);

        final Optional<Mapping> singleMapping = mapping(eid, mappingName);
        final List<Mapping> list = Common.getMappingList(mappingContext, mappingsIid);
        list.add(singleMapping.get());

        doReturn(Optional.of(new MappingsBuilder().setMapping(list).build())).when(mappingContext).read(mappingsIid);
        doReturn(singleMapping).when(mappingContext).read(mappingIid);
    }

    default void noEidMappingDefined(@Nonnull final MappingContext mappingContext, @Nonnull final String name,
                                     @Nonnull final String namingContextName) {
        final InstanceIdentifier<Mappings> iid =
                mappingIid(new MappingId(name), namingContextName).firstIdentifierOf(Mappings.class);
        final List<Mapping> list = Common.getMappingList(mappingContext, iid);

        doReturn(Optional.of(new MappingsBuilder().setMapping(list).build())).when(mappingContext).read(iid);
        doReturn(Optional.absent()).when(mappingContext).read(mappingIid(new MappingId(name), namingContextName));
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
