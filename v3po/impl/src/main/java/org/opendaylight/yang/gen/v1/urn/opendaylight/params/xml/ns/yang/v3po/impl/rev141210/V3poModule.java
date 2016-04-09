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

package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210;

import io.fd.honeycomb.v3po.impl.NorthboundFacadeHoneycombDOMBroker;
import org.opendaylight.controller.sal.core.api.AbstractProvider;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.model.SchemaService;

public class V3poModule extends
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210.AbstractV3poModule {

    public V3poModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                      org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public V3poModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                      org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                      org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.impl.rev141210.V3poModule oldModule,
                      java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        final Broker.ProviderSession providerSession =
            getDomBrokerDependency().registerProvider(new AbstractProvider() {
                @Override
                public void onSessionInitiated(final Broker.ProviderSession providerSession) {
                    // NOOP
                }
            });
        final SchemaService schemaBiService = providerSession.getService(SchemaService.class);

        return new NorthboundFacadeHoneycombDOMBroker(getHoneycombDomDataBrokerDependency(), schemaBiService,
            getHoneycombDomNotificationServiceDependency());
    }

}
