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

package io.fd.honeycomb.v3po.translate.v3po.test;

import com.google.common.base.Optional;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContextKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class ContextTestUtils {

    public static Optional<Mapping> getMapping(final String name, final int index) {
        return Optional.of(new MappingBuilder().setName(name).setIndex(index).build());
    }

    public static KeyedInstanceIdentifier<Mapping, MappingKey> getMappingIid(final String name, final String namingContextName) {
        return InstanceIdentifier.create(Contexts.class).child(
                org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContext.class,
                new NamingContextKey(namingContextName)).child(Mappings.class).child(Mapping.class, new MappingKey(name));
    }
}
