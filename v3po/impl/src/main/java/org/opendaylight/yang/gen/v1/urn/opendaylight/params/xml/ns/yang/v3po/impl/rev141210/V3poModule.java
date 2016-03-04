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

import io.fd.honeycomb.v3po.impl.V3poProvider;
import java.util.Collection;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

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
        final Broker domBroker = getDomBrokerDependency();
        domBroker.registerProvider(new InitializationProvider());
        final V3poProvider provider = new V3poProvider(domBroker);
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

    /**
     * Writes list parents as a workaround for ODL issue TODO remove (also remove from yang model and cfg) and fix ODL
     * bug-5382
     */
    private class InitializationProvider implements Provider {
        @Override
        public void onSessionInitiated(final Broker.ProviderSession providerSession) {
            final DOMDataBroker service = providerSession.getService(DOMDataBroker.class);
            final DOMDataWriteTransaction domDataWriteTransaction = service.newWriteOnlyTransaction();

            // Initialize interfaces list
            YangInstanceIdentifier.NodeIdentifier nodeId = getNodeId(Interfaces.QNAME);
            YangInstanceIdentifier interfacesYid = YangInstanceIdentifier.create(nodeId);
            domDataWriteTransaction.merge(LogicalDatastoreType.CONFIGURATION,
                    interfacesYid, Builders.containerBuilder().withNodeIdentifier(nodeId)
                            .withChild(Builders.mapBuilder().withNodeIdentifier(getNodeId(Interface.QNAME)).build())
                            .build());

            // Initialize bridge domains list
            nodeId = getNodeId(BridgeDomains.QNAME);
            interfacesYid = YangInstanceIdentifier.create(getNodeId(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp.QNAME), nodeId);
            domDataWriteTransaction.merge(LogicalDatastoreType.CONFIGURATION,
                    interfacesYid, Builders.containerBuilder().withNodeIdentifier(nodeId)
                            .withChild(Builders.mapBuilder().withNodeIdentifier(getNodeId(BridgeDomain.QNAME)).build())
                            .build());

            try {
                domDataWriteTransaction.submit().checkedGet();
            } catch (TransactionCommitFailedException e) {
                throw new IllegalStateException("Unable to initialize parent data structures", e);
            }
        }

        private YangInstanceIdentifier.NodeIdentifier getNodeId(final QName qname) {
            return new YangInstanceIdentifier.NodeIdentifier(qname);
        }

        @Override
        public Collection<ProviderFunctionality> getProviderFunctionality() {
            return null;
        }
    }


}
