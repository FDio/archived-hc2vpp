/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.ipsec.helpers;

import com.google.common.collect.ImmutableSet;
import io.fd.honeycomb.test.tools.annotations.InjectablesProcessor;
import io.fd.honeycomb.test.tools.annotations.SchemaContextProvider;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;

public interface SchemaContextTestHelper extends InjectablesProcessor {

    @SchemaContextProvider
    default ModuleInfoBackedContext getSchemaContext() {
        return provideSchemaContextFor(ImmutableSet.of(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.$YangModuleInfoImpl
                        .getInstance(),
                org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.$YangModuleInfoImpl
                        .getInstance()
        ));
    }
}
