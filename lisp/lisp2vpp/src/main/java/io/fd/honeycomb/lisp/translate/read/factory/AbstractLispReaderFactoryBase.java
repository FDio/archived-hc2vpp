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

package io.fd.honeycomb.lisp.translate.read.factory;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.LispState;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;


/**
 * Basic attributes for lisp reader factories
 */
abstract class AbstractLispReaderFactoryBase {

    protected final InstanceIdentifier<LispState> lispStateId;
    protected final FutureJVppCore vppApi;
    protected NamingContext interfaceContext;
    protected NamingContext locatorSetContext;
    protected EidMappingContext localMappingContext;
    protected EidMappingContext remoteMappingContext;

    protected AbstractLispReaderFactoryBase(@Nonnull final InstanceIdentifier<LispState> lispStateId,
                                            @Nonnull final FutureJVppCore vppApi) {
        this.lispStateId = checkNotNull(lispStateId, "Lisp state identifier is null");
        this.vppApi = checkNotNull(vppApi, "VPP api refference is null");
    }

    protected AbstractLispReaderFactoryBase(@Nonnull final InstanceIdentifier<LispState> lispStateId,
                                            @Nonnull final FutureJVppCore vppApi,
                                            @Nonnull final NamingContext interfaceContext,
                                            @Nonnull final EidMappingContext localMappingContext,
                                            @Nonnull final EidMappingContext remoteMappingContext) {
        this.lispStateId = checkNotNull(lispStateId, "Lisp state identifier is null");
        this.vppApi = checkNotNull(vppApi, "VPP api reference is null");
        this.interfaceContext = checkNotNull(interfaceContext,
                "Interface naming context is null,for readers that don't need this dependency,use different constructor");
        this.localMappingContext = checkNotNull(localMappingContext,
                "Local mappings reference is null,for readers that don't need this dependency use different constructor");
        this.remoteMappingContext = checkNotNull(remoteMappingContext,
                "Remote mappings reference is null,for readers that don't need this dependency use different constructor");
    }

    protected AbstractLispReaderFactoryBase(@Nonnull final InstanceIdentifier<LispState> lispStateId,
                                            @Nonnull final FutureJVppCore vppApi,
                                            @Nonnull final NamingContext interfaceContext,
                                            @Nonnull final NamingContext locatorSetContext,
                                            @Nonnull final EidMappingContext localMappingContext,
                                            @Nonnull final EidMappingContext remoteMappingContext) {
        this.lispStateId = checkNotNull(lispStateId, "Lisp state identifier is null");
        this.vppApi = checkNotNull(vppApi, "VPP api reference is null");
        this.interfaceContext = checkNotNull(interfaceContext,
                "Interface naming context is null,for readers that don't need this dependency,use different constructor");
        this.locatorSetContext = checkNotNull(locatorSetContext,
                "Locator set naming context is null,for readers that don't need this dependency,use different constructor");
        this.localMappingContext = checkNotNull(localMappingContext,
                "Local mappings reference is null,for readers that don't need this dependency use different constructor");
        this.remoteMappingContext = checkNotNull(remoteMappingContext,
                "Remote mappings reference is null,for readers that don't need this dependency use different constructor");
    }
}
