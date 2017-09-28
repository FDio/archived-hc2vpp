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

package io.fd.hc2vpp.routing.helpers;

import com.google.common.collect.ImmutableSet;
import io.fd.honeycomb.test.tools.annotations.InjectablesProcessor;
import io.fd.honeycomb.test.tools.annotations.SchemaContextProvider;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;

public interface SchemaContextTestHelper extends InjectablesProcessor {

    @SchemaContextProvider
    default ModuleInfoBackedContext getSchemaContext() {
        return provideSchemaContextFor(ImmutableSet.of(
                // Default ietf-ip
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.$YangModuleInfoImpl
                        .getInstance(),
                // Default ietf-routing
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.$YangModuleInfoImpl
                        .getInstance(),
                // Ipv4 augmentations
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.$YangModuleInfoImpl
                        .getInstance(),
                // Ipv4 augmentations
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.$YangModuleInfoImpl
                        .getInstance(),
                // Vpp routing
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.$YangModuleInfoImpl
                        .getInstance(),
                // Vpp routing RA
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.ra.rev170502.$YangModuleInfoImpl
                    .getInstance(),
                // Table lookup
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.table.lookup.rev170917.$YangModuleInfoImpl.getInstance()
                ));
    }
}
