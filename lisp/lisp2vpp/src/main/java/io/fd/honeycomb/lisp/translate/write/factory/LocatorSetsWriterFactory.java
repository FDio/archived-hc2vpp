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

import io.fd.honeycomb.lisp.translate.write.InterfaceCustomizer;
import io.fd.honeycomb.lisp.translate.write.LocatorSetCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.future.FutureJVppCore;


/**
 * Factory producing writers for {@code LocatorSets}
 */
final class LocatorSetsWriterFactory extends AbstractLispWriterFactoryBase implements WriterFactory {

    private LocatorSetsWriterFactory(final InstanceIdentifier<Lisp> lispInstanceIdentifier,
                                     final FutureJVppCore vppApi,
                                     final NamingContext interfaceContext,
                                     final NamingContext locatorSetContext) {
        super(lispInstanceIdentifier, vppApi, interfaceContext, locatorSetContext);
    }

    public static LocatorSetsWriterFactory newInstance(
            @Nonnull final InstanceIdentifier<Lisp> lispInstanceIdentifier,
            @Nonnull final FutureJVppCore vppApi,
            @Nonnull final NamingContext interfaceContext,
            @Nonnull final NamingContext locatorSetContext) {
        return new LocatorSetsWriterFactory(lispInstanceIdentifier, vppApi, interfaceContext, locatorSetContext);
    }

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        InstanceIdentifier<LocatorSet> locatorSetId =
                lispInstanceIdentifier.child(LocatorSets.class).child(LocatorSet.class);

        registry.add(new GenericListWriter<>(locatorSetId, new LocatorSetCustomizer(vppApi, locatorSetContext)));
        registry.add(new GenericListWriter<>(locatorSetId.child(Interface.class),
                new InterfaceCustomizer(vppApi, interfaceContext)));
    }
}
