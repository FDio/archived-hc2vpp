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

package io.fd.honeycomb.lisp.context.util;

import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.BindingBrokerReader;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.Contexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.ContextsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * {@link ReaderFactory} initiating reader providing data from context data store for eid's.
 * Making them available over RESTCONF/NETCONF.
 */
public class ContextsReaderFactory implements ReaderFactory {

    private final DataBroker contextBindingBrokerDependency;

    public ContextsReaderFactory(final DataBroker contextBindingBrokerDependency) {
        this.contextBindingBrokerDependency = contextBindingBrokerDependency;
    }

    @Override
    public void init(final ModifiableReaderRegistryBuilder registry) {
        registry.add(new BindingBrokerReader<>(InstanceIdentifier.create(Contexts.class),
                contextBindingBrokerDependency,
                LogicalDatastoreType.OPERATIONAL, ContextsBuilder.class));
    }
}
