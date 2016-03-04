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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.v3po.impl.V3poProvider;
import javax.management.ObjectName;
import org.junit.Test;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;

public class V3poModuleTest {
    @Test
    public void testCustomValidation() {
        V3poModule module = new V3poModule(mock(ModuleIdentifier.class), mock(DependencyResolver.class));

        // ensure no exceptions on validation
        // currently this method is empty
        module.customValidation();
    }


    // @Test
    public void testCreateInstance() throws Exception {
        // configure mocks
        DependencyResolver dependencyResolver = mock(DependencyResolver.class);
        BindingAwareBroker broker = mock(BindingAwareBroker.class);
        when(dependencyResolver.resolveInstance(eq(BindingAwareBroker.class), any(ObjectName.class), any(JmxAttribute.class)))
            .thenReturn(broker);
        final org.opendaylight.controller.sal.core.api.Broker domBroker = mock(org.opendaylight.controller.sal.core.api.Broker.class);
        when(dependencyResolver.resolveInstance(eq(org.opendaylight.controller.sal.core.api.Broker.class), any(ObjectName.class), any(JmxAttribute.class)))
            .thenReturn(domBroker);

        // create instance of module with injected mocks
        V3poModule module = new V3poModule(mock(ModuleIdentifier.class), dependencyResolver);

        // getInstance calls resolveInstance to get the broker dependency and then calls createInstance
        AutoCloseable closeable = module.getInstance();

        // verify that the module registered the returned provider with the broker
        verify(broker).registerProvider((V3poProvider)closeable);

        // ensure no exceptions on close
        closeable.close();
    }
}
