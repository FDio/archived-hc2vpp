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

package io.fd.honeycomb.translate.v3po.test;

import static org.mockito.Mockito.doReturn;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.MappingContext;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContextKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class ContextTestUtils {

    public static Optional<Mapping> getMapping(final String name, final int index) {
        return Optional.of(new MappingBuilder().setName(name).setIndex(index).build());
    }

    public static KeyedInstanceIdentifier<Mapping, MappingKey> getMappingIid(final String name,
                                                                             final String namingContextName) {
        return InstanceIdentifier.create(Contexts.class).child(
            org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContext.class,
            new NamingContextKey(namingContextName)).child(Mappings.class).child(Mapping.class, new MappingKey(name));
    }

    private static List<Mapping> getMappingList(final MappingContext mappingContext, final InstanceIdentifier<Mappings> mappingsIid) {
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

    public static void mockMapping(final MappingContext mappingContext, final String name, final int id,
                                   final String namingContextName) {
        final InstanceIdentifier<Mappings> mappingsIid =
            getMappingIid(name, namingContextName).firstIdentifierOf(Mappings.class);

        final Optional<Mapping> singleMapping = getMapping(name, id);
        final List<Mapping> mappingList = getMappingList(mappingContext, mappingsIid);
        mappingList.add(singleMapping.get());

        doReturn(Optional.of(new MappingsBuilder().setMapping(mappingList).build()))
            .when(mappingContext).read(mappingsIid);
        doReturn(singleMapping).when(mappingContext).read(getMappingIid(name, namingContextName));
    }

    public static void mockEmptyMapping(final MappingContext mappingContext, final String name,
                                        final String namingContextName) {
        final InstanceIdentifier<Mappings> mappingsIid =
            getMappingIid(name, namingContextName).firstIdentifierOf(Mappings.class);

        final List<Mapping> mappingList = getMappingList(mappingContext, mappingsIid);

        doReturn(Optional.of(new MappingsBuilder().setMapping(mappingList).build()))
            .when(mappingContext).read(mappingsIid);
        doReturn(Optional.absent()).when(mappingContext).read(getMappingIid(name, namingContextName));
    }
}
