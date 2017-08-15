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
package io.fd.hc2vpp.routing.write;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.routing.RoutingConfiguration;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.RoutingInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.RoutingInstanceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RoutingInstanceCustomizerTest extends WriterCustomizerTest {

    private static final String VALID_NAME = "valid-name";
    private static final String INVALID_NAME = "invalid-name";

    @Mock
    private RoutingConfiguration configuration;

    private RoutingInstanceCustomizer customizer;
    private InstanceIdentifier<RoutingInstance> id;

    private RoutingInstance validData;
    private RoutingInstance invalidData;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new RoutingInstanceCustomizer(configuration);

        when(configuration.getDefaultRoutingInstanceName()).thenReturn(VALID_NAME);

        id = InstanceIdentifier.create(RoutingInstance.class);
        validData = new RoutingInstanceBuilder().setName(VALID_NAME).build();
        invalidData = new RoutingInstanceBuilder().setName(INVALID_NAME).build();
    }

    @Test
    public void writeCurrentAttributesValid() throws Exception {
        try {
            customizer.writeCurrentAttributes(id, validData, writeContext);
        } catch (Exception e) {
            fail("Test should passed without exception");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void writeCurrentAttributesInvalid() throws Exception {
        customizer.writeCurrentAttributes(id, invalidData, writeContext);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void updateCurrentAttributes() throws Exception {
        customizer.updateCurrentAttributes(id, validData, validData, writeContext);
    }

    @Test
    public void deleteCurrentAttributesValid() throws Exception {
        try {
            customizer.deleteCurrentAttributes(id, validData, writeContext);
        } catch (Exception e) {
            fail("Test should passed without exception");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteCurrentAttributesInvalid() throws Exception {
        customizer.deleteCurrentAttributes(id, invalidData, writeContext);
    }

}