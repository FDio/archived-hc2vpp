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

package io.fd.honeycomb.vpp.test.util;

import static org.mockito.Mockito.doReturn;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.MappingContext;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContext;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContextKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Utility that helps stubbing {@link io.fd.honeycomb.translate.v3po.util.NamingContext} methods.
 */
// TODO(HONEYCOMB-226): the class needs to be refactored or even removed after extracting interface from NamingContext
public interface NamingContextHelper {

    /**
     * Creates {@link Mapping} for given data.
     *
     * @param name  to be mapped
     * @param index to be mapped
     * @return name to index mapping
     */
    default Optional<Mapping> mapping(@Nonnull final String name, final int index) {
        return Optional.of(new MappingBuilder().setName(name).setIndex(index).build());
    }

    /**
     * Creates {@link KeyedInstanceIdentifier} for {@link Mapping} in {@link NamingContext}.
     *
     * @param name              name of the mapping
     * @param namingContextName name of the naming context
     * @return identifier for the mapping
     */
    default KeyedInstanceIdentifier<Mapping, MappingKey> mappingIid(@Nonnull final String name,
                                                                    @Nonnull final String namingContextName) {
        return InstanceIdentifier.create(Contexts.class).child(NamingContext.class,
            new NamingContextKey(namingContextName)).child(Mappings.class).child(Mapping.class, new MappingKey(name));
    }

    /**
     * Stubs {@link MappingContext#read} to include given mapping in {@link NamingContext}.
     *
     * @param mappingContext    mock instance of {@link MappingContext}
     * @param name              name of the mapping
     * @param index             index to be mapped
     * @param namingContextName name of the naming context
     */
    default void defineMapping(@Nonnull final MappingContext mappingContext, @Nonnull final String name,
                               final int index, @Nonnull final String namingContextName) {
        final KeyedInstanceIdentifier<Mapping, MappingKey> mappingIid = mappingIid(name, namingContextName);
        final InstanceIdentifier<Mappings> mappingsIid = mappingIid.firstIdentifierOf(Mappings.class);

        final Optional<Mapping> singleMapping = mapping(name, index);
        final List<Mapping> list = Common.getMappingList(mappingContext, mappingsIid);
        list.add(singleMapping.get());

        doReturn(Optional.of(new MappingsBuilder().setMapping(list).build())).when(mappingContext).read(mappingsIid);
        doReturn(singleMapping).when(mappingContext).read(mappingIid);
    }

    /**
     * Stubs {@link MappingContext#read} for given {@link NamingContext} to return {@link Optional#absent} for provided
     * name.
     *
     * @param mappingContext    mock instance of {@link MappingContext}
     * @param name              name of the mapping
     * @param namingContextName name of the naming context
     */
    default void noMappingDefined(@Nonnull final MappingContext mappingContext, @Nonnull final String name,
                                  @Nonnull final String namingContextName) {
        final InstanceIdentifier<Mappings> iid = mappingIid(name, namingContextName).firstIdentifierOf(Mappings.class);
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
