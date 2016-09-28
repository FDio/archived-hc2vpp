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

package io.fd.honeycomb.lisp.translate.read.factory;


import io.fd.honeycomb.lisp.translate.read.MapResolverCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.LispState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.MapResolvers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.MapResolversBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;


/**
 * Builds reader for {@link MapResolvers}<br> and all its inhired child readers
 */
public class MapResolversReaderFactory extends AbstractLispReaderFactoryBase implements ReaderFactory {

    private MapResolversReaderFactory(final InstanceIdentifier<LispState> lispStateId, final FutureJVppCore vppApi) {
        super(lispStateId, vppApi);
    }

    public static MapResolversReaderFactory newInstance(@Nonnull final InstanceIdentifier<LispState> lispStateId,
                                                        @Nonnull final FutureJVppCore vppApi) {
        return new MapResolversReaderFactory(lispStateId, vppApi);
    }

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {

        InstanceIdentifier<MapResolvers> mapResolversInstanceIdentifier = lispStateId.child(MapResolvers.class);

        registry.addStructuralReader(mapResolversInstanceIdentifier, MapResolversBuilder.class);
        registry.add(new GenericListReader<>(mapResolversInstanceIdentifier.child(MapResolver.class),
                new MapResolverCustomizer(vppApi)));
    }
}
