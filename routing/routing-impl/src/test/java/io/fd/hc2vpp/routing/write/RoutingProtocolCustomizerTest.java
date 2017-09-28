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

import static io.fd.hc2vpp.routing.helpers.RoutingRequestTestHelper.ROUTE_PROTOCOL_NAME;
import static io.fd.hc2vpp.routing.helpers.RoutingRequestTestHelper.ROUTE_PROTOCOL_NAME_2;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Direct;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.RoutingProtocolVppAttr;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.RoutingProtocolVppAttrBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.VniReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol.VppProtocolAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RoutingProtocolCustomizerTest extends WriterCustomizerTest {

    private InstanceIdentifier<RoutingProtocol> validId;
    private RoutingProtocol validData;
    private RoutingProtocol validData2;
    private RoutingProtocol invalidData;
    private RoutingProtocolCustomizer customizer;
    private NamingContext routingProtocolContext;

    @Before
    public void init() {
        validId = InstanceIdentifier.create(RoutingProtocol.class);
        validData = new RoutingProtocolBuilder()
                .setName(ROUTE_PROTOCOL_NAME)
                .setType(Static.class)
                .addAugmentation(RoutingProtocolVppAttr.class, new RoutingProtocolVppAttrBuilder()
                        .setVppProtocolAttributes(new VppProtocolAttributesBuilder()
                                .setPrimaryVrf(new VniReference(1L))
                                .build())
                        .build())
                .build();

        validData2= new RoutingProtocolBuilder()
                .setName(ROUTE_PROTOCOL_NAME_2)
                .setType(Static.class)
                .addAugmentation(RoutingProtocolVppAttr.class, new RoutingProtocolVppAttrBuilder()
                        .setVppProtocolAttributes(new VppProtocolAttributesBuilder()
                                .setPrimaryVrf(new VniReference(1L))
                                .build())
                        .build())
                .build();

        invalidData = new RoutingProtocolBuilder()
                .setType(Direct.class)
                .build();

        routingProtocolContext = new NamingContext("routing-protocol", "routing-protocol-context");
        customizer = new RoutingProtocolCustomizer(routingProtocolContext);
    }

    @Test
    public void testWriteIsStatic() throws WriteFailedException {
        noMappingDefined(mappingContext, ROUTE_PROTOCOL_NAME, "routing-protocol-context");
        try {
            customizer.writeCurrentAttributes(validId, validData, writeContext);
        } catch (Exception e) {
            fail("Test should have passed without throwing exception");
        }
    }

    /**
     * Should not fail, just ignore re-mapping same name
     * */
    @Test
    public void testWriteIsStaticSameAllreadyExist() throws WriteFailedException {
        defineMapping(mappingContext, ROUTE_PROTOCOL_NAME, 1, "routing-protocol-context");
        try {
            customizer.writeCurrentAttributes(validId, validData, writeContext);
        } catch (Exception e) {
            fail("Test should have passed without throwing exception");
        }
    }

    /**
     * Should fail, because of attempt to map different name to same index
     * */
    @Test
    public void testWriteIsStaticOtherAllreadyExist() throws WriteFailedException {
        defineMapping(mappingContext, ROUTE_PROTOCOL_NAME, 1, "routing-protocol-context");
        try {
            customizer.writeCurrentAttributes(validId, validData2, writeContext);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
            return;
        }
        fail("Test should have thrown exception");
    }

    @Test
    public void testWriteIsntStatic() throws WriteFailedException {
        try {
            customizer.writeCurrentAttributes(validId, invalidData, writeContext);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            return;
        }
        fail("Test should have thrown exception");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws WriteFailedException {
        customizer.updateCurrentAttributes(validId, validData, validData, writeContext);
    }
}
