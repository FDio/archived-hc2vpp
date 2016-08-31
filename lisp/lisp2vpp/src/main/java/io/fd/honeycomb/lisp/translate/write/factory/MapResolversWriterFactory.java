/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.lisp.translate.write.factory;


import io.fd.honeycomb.lisp.translate.write.MapResolverCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.MapResolvers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.future.FutureJVppCore;


/**
 * Factory responsible for producing writers for {@code MapResolvers}
 */
final class MapResolversWriterFactory extends AbstractLispWriterFactoryBase implements WriterFactory {

    private MapResolversWriterFactory(final InstanceIdentifier<Lisp> lispInstanceIdentifier,
                                      final FutureJVppCore vppApi) {
        super(lispInstanceIdentifier, vppApi, null);
    }

    public static MapResolversWriterFactory newInstance(
            @Nonnull final InstanceIdentifier<Lisp> lispInstanceIdentifier,
            @Nonnull final FutureJVppCore vppApi) {
        return new MapResolversWriterFactory(lispInstanceIdentifier, vppApi);
    }

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        registry.add(new GenericListWriter<>(lispInstanceIdentifier.child(MapResolvers.class).child(MapResolver.class),
                new MapResolverCustomizer(vppApi)));
    }
}
