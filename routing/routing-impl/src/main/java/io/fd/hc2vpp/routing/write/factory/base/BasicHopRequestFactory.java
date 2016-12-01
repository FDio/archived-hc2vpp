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

package io.fd.hc2vpp.routing.write.factory.base;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.vppclassifier.VppClassifierContextManager;
import javax.annotation.Nonnull;

/**
 * Extension to {@code ClassifierContextHolder} to hold also {@code NamingContext}
 */
public abstract class BasicHopRequestFactory extends ClassifierContextHolder {

    private final NamingContext interfaceNamingContext;
    private final NamingContext routingProtocolContext;

    protected BasicHopRequestFactory(
            @Nonnull final VppClassifierContextManager classifierContextManager,
            @Nonnull final NamingContext interfaceContext,
            @Nonnull final NamingContext routingProtocolContext) {
        super(classifierContextManager);
        this.interfaceNamingContext = checkNotNull(interfaceContext, "Interface context cannot be null");
        this.routingProtocolContext = checkNotNull(routingProtocolContext, "Routing protocol context cannot be null");
    }

    protected NamingContext getInterfaceNamingContext() {
        return interfaceNamingContext;
    }

    protected NamingContext getRoutingProtocolContext() {
        return routingProtocolContext;
    }
}
