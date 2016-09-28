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


import io.fd.honeycomb.lisp.translate.read.InterfaceCustomizer;
import io.fd.honeycomb.lisp.translate.read.LocatorSetCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.LispState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.LocatorSetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;


/**
 * Produces reader for {@link LocatorSets} <br> and all its inhired child readers.
 */
public class LocatorSetsReaderFactory extends AbstractLispReaderFactoryBase implements ReaderFactory {


    private LocatorSetsReaderFactory(final InstanceIdentifier<LispState> lispStateId,
                                     final FutureJVppCore vppApi,
                                     final NamingContext interfaceContext,
                                     final NamingContext locatorSetContext) {
        super(lispStateId, vppApi);
        this.interfaceContext = interfaceContext;
        this.locatorSetContext = locatorSetContext;
    }

    public static final LocatorSetsReaderFactory newInstance(@Nonnull final InstanceIdentifier<LispState> lispStateId,
                                                             @Nonnull final FutureJVppCore vppApi,
                                                             final NamingContext interfaceContext,
                                                             @Nonnull final NamingContext locatorSetContext) {
        return new LocatorSetsReaderFactory(lispStateId, vppApi, interfaceContext, locatorSetContext);
    }

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        InstanceIdentifier<LocatorSets> locatorSetsInstanceIdentifier = lispStateId.child(LocatorSets.class);
        InstanceIdentifier<LocatorSet> locatorSetInstanceIdentifier =
                locatorSetsInstanceIdentifier.child(LocatorSet.class);

        registry.addStructuralReader(locatorSetsInstanceIdentifier, LocatorSetsBuilder.class);
        registry.add(new GenericListReader<>(locatorSetInstanceIdentifier,
                new LocatorSetCustomizer(vppApi, locatorSetContext)));
        registry.add(new GenericListReader<>(locatorSetInstanceIdentifier.child(Interface.class),
                new InterfaceCustomizer(vppApi, interfaceContext, locatorSetContext)));
    }
}
