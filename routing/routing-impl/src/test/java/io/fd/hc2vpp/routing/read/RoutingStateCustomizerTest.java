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

package io.fd.hc2vpp.routing.read;


import static org.junit.Assert.assertEquals;

import io.fd.hc2vpp.routing.helpers.SchemaContextTestHelper;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.spi.read.Initialized;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.RoutingState;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class RoutingStateCustomizerTest implements SchemaContextTestHelper {

    @InjectTestData(resourcePath = "/init/config-data.json")
    private Routing config;

    @InjectTestData(resourcePath = "/init/state-data.json")
    private RoutingState state;

    @Mock
    private ReadContext readContext;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInit() {
        final InstanceIdentifier<RoutingState> identifier = InstanceIdentifier.create(RoutingState.class);
        final Initialized<? extends DataObject> initilized =
                new RoutingStateCustomizer().init(identifier, state, readContext);

        final Routing initializedRouting = Routing.class.cast(initilized.getData());

        assertEquals(config, initializedRouting);
    }
}
