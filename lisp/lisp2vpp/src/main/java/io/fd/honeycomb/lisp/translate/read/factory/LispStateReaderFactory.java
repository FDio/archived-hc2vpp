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

import static io.fd.honeycomb.lisp.cfgattrs.LispConfiguration.INTERFACE_CONTEXT;
import static io.fd.honeycomb.lisp.cfgattrs.LispConfiguration.LOCAL_MAPPING_CONTEXT;
import static io.fd.honeycomb.lisp.cfgattrs.LispConfiguration.LOCATOR_SET_CONTEXT;
import static io.fd.honeycomb.lisp.cfgattrs.LispConfiguration.REMOTE_MAPPING_CONTEXT;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.translate.read.LispStateCustomizer;
import io.fd.honeycomb.lisp.translate.read.PitrCfgCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.LispState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.pitr.cfg.grouping.PitrCfg;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Initialize readers for {@link LispState}
 */
public class LispStateReaderFactory extends AbstractLispReaderFactoryBase implements ReaderFactory {

    @Inject
    public LispStateReaderFactory(final FutureJVppCore vppApi,
                                  @Named(INTERFACE_CONTEXT) final NamingContext interfaceContext,
                                  @Named(LOCATOR_SET_CONTEXT) final NamingContext locatorSetContext,
                                  @Named("bridge-domain-context") final NamingContext bridgeDomainContext,
                                  @Named(LOCAL_MAPPING_CONTEXT) final EidMappingContext localMappingContext,
                                  @Named(REMOTE_MAPPING_CONTEXT) final EidMappingContext remoteMappingContext) {
        super(InstanceIdentifier.create(LispState.class), vppApi, interfaceContext, locatorSetContext,
                bridgeDomainContext, localMappingContext, remoteMappingContext);
    }

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {

        registry.add(new GenericReader<>(lispStateId, new LispStateCustomizer(vppApi)));
        registry.addStructuralReader(lispStateId.child(LispFeatureData.class), LispFeatureDataBuilder.class);

        LocatorSetsReaderFactory.newInstance(lispStateId, vppApi, interfaceContext, locatorSetContext).init(registry);
        MapResolversReaderFactory.newInstance(lispStateId, vppApi).init(registry);
        EidTableReaderFactory
                .newInstance(lispStateId, vppApi, interfaceContext, locatorSetContext, bridgeDomainContext,
                        localMappingContext, remoteMappingContext)
                .init(registry);

        registry.add(new GenericReader<>(lispStateId.child(LispFeatureData.class).child(PitrCfg.class),
                new PitrCfgCustomizer(vppApi)));
    }
}
