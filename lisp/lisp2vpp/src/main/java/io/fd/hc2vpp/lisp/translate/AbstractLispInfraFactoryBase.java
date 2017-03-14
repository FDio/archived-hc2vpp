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

package io.fd.hc2vpp.lisp.translate;

import static io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration.ADJACENCIES_IDENTIFICATION_CONTEXT;
import static io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration.INTERFACE_CONTEXT;
import static io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration.LOCAL_MAPPING_CONTEXT;
import static io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration.LOCATOR_SET_CONTEXT;
import static io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration.REMOTE_MAPPING_CONTEXT;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.lisp.context.util.AdjacenciesMappingContext;
import io.fd.hc2vpp.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.LispState;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;


/**
 * Basic attributes for lisp writer factories
 */
public abstract class AbstractLispInfraFactoryBase {

    protected static final InstanceIdentifier<Lisp> LISP_CONFIG_IDENTIFIER = InstanceIdentifier.create(Lisp.class);
    protected static final InstanceIdentifier<LispState> LISP_OPERATIONAL_IDENTIFIER =
            InstanceIdentifier.create(LispState.class);

    @Inject
    protected FutureJVppCore vppApi;

    @Inject
    @Named(INTERFACE_CONTEXT)
    protected NamingContext interfaceContext;

    @Inject
    @Named(LOCATOR_SET_CONTEXT)
    protected NamingContext locatorSetContext;

    @Inject
    @Named("bridge-domain-context")
    protected NamingContext bridgeDomainContext;

    @Inject
    @Named(LOCAL_MAPPING_CONTEXT)
    protected EidMappingContext localMappingContext;

    @Inject
    @Named(REMOTE_MAPPING_CONTEXT)
    protected EidMappingContext remoteMappingContext;

    @Inject
    @Named(ADJACENCIES_IDENTIFICATION_CONTEXT)
    protected AdjacenciesMappingContext adjacenciesMappingContext;

    @Nonnull
    protected <D extends DataObject> GenericWriter<D> writer(@Nonnull final InstanceIdentifier<D> type,
                                                             @Nonnull final WriterCustomizer<D> customizer) {
        return new GenericWriter<>(type, customizer);
    }
}
