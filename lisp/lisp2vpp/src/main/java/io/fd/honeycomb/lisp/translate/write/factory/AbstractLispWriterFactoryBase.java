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

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.Lisp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;


/**
 * Basic attributes for lisp writer factories
 */
abstract class AbstractLispWriterFactoryBase {

    protected final InstanceIdentifier<Lisp> lispInstanceIdentifier;
    protected final FutureJVppCore vppApi;
    protected NamingContext interfaceContext;
    protected NamingContext locatorSetContext;
    protected EidMappingContext localMappingContext;
    protected EidMappingContext remoteMappingContext;

    protected AbstractLispWriterFactoryBase(@Nonnull final InstanceIdentifier<Lisp> lispInstanceIdentifier,
                                            @Nonnull final FutureJVppCore vppApi,
                                            NamingContext interfaceContext) {
        this.lispInstanceIdentifier = checkNotNull(lispInstanceIdentifier, "Lisp identifier is null");
        this.vppApi = checkNotNull(vppApi, "VPP Api refference is null");
        this.interfaceContext = interfaceContext;
    }

    protected AbstractLispWriterFactoryBase(@Nonnull final InstanceIdentifier<Lisp> lispInstanceIdentifier,
                                            @Nonnull final FutureJVppCore vppApi,
                                            NamingContext interfaceContext,
                                            NamingContext locatorSetContext) {
        this.lispInstanceIdentifier = checkNotNull(lispInstanceIdentifier, "Lisp identifier is null");
        this.vppApi = checkNotNull(vppApi, "VPP Api refference is null");
        this.interfaceContext = interfaceContext;
        this.locatorSetContext = locatorSetContext;
    }

    protected AbstractLispWriterFactoryBase(@Nonnull final InstanceIdentifier<Lisp> lispInstanceIdentifier,
                                            @Nonnull final FutureJVppCore vppApi,
                                            EidMappingContext localMappingContext,
                                            EidMappingContext remoteMappingContext) {
        this.lispInstanceIdentifier = checkNotNull(lispInstanceIdentifier, "Lisp identifier is null");
        this.vppApi = checkNotNull(vppApi, "VPP Api refference is null");
        this.localMappingContext = localMappingContext;
        this.remoteMappingContext = remoteMappingContext;
    }

    protected AbstractLispWriterFactoryBase(@Nonnull final InstanceIdentifier<Lisp> lispInstanceIdentifier,
                                            @Nonnull final FutureJVppCore vppApi,
                                            NamingContext interfaceContext,
                                            NamingContext locatorSetContext,
                                            EidMappingContext localMappingContext,
                                            EidMappingContext remoteMappingContext) {
        this.lispInstanceIdentifier = checkNotNull(lispInstanceIdentifier, "Lisp identifier is null");
        this.vppApi = checkNotNull(vppApi, "VPP Api refference is null");
        this.interfaceContext = interfaceContext;
        this.locatorSetContext = locatorSetContext;
        this.localMappingContext = localMappingContext;
        this.remoteMappingContext = remoteMappingContext;
    }

    protected AbstractLispWriterFactoryBase(@Nonnull final InstanceIdentifier<Lisp> lispInstanceIdentifier,
                                            @Nonnull final FutureJVppCore vppApi,
                                            NamingContext interfaceContext,
                                            EidMappingContext localMappingContext,
                                            EidMappingContext remoteMappingContext) {
        this.lispInstanceIdentifier = checkNotNull(lispInstanceIdentifier, "Lisp identifier is null");
        this.vppApi = checkNotNull(vppApi, "VPP Api refference is null");
        this.interfaceContext = interfaceContext;
        this.localMappingContext = localMappingContext;
        this.remoteMappingContext = remoteMappingContext;
    }
}
