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
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.honeycomb.translate.write.WriteFailedException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Direct;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.ControlPlaneProtocols;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev180319.RoutingProtocolVppAttr;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev180319.RoutingProtocolVppAttrBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev180319.routing.control.plane.protocols.control.plane.protocol.VppProtocolAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class ControlPlaneProtocolCustomizerTest extends WriterCustomizerTest {

    private static final VniReference VRF = new VniReference(1L);
    private static final VniReference INVALID_VRF = new VniReference(3L);
    private static final String INVALID_TABLE_PROTOCOL_1 = "invalid-table-protocol-1";
    private InstanceIdentifier<ControlPlaneProtocol> validId;
    private InstanceIdentifier<ControlPlaneProtocol> invalidIid;
    private ControlPlaneProtocol validData;
    private ControlPlaneProtocol validData2;
    private ControlPlaneProtocol invalidData;
    private ControlPlaneProtocol invalidTableData;
    private ControlPlaneProtocolCustomizer customizer;
    private NamingContext routingProtocolContext;

    @Before
    public void init() {
        validId = InstanceIdentifier.create(ControlPlaneProtocol.class);
        invalidIid = InstanceIdentifier.create(ControlPlaneProtocols.class)
                .child(ControlPlaneProtocol.class,
                        new ControlPlaneProtocolKey(INVALID_TABLE_PROTOCOL_1, Static.class));
        invalidTableData = new ControlPlaneProtocolBuilder()
                .setName(INVALID_TABLE_PROTOCOL_1)
                .setType(Static.class)
                .addAugmentation(RoutingProtocolVppAttr.class, new RoutingProtocolVppAttrBuilder()
                        .setVppProtocolAttributes(new VppProtocolAttributesBuilder()
                                .setPrimaryVrf(INVALID_VRF)
                                .build())
                        .build())
                .build();
        validData = new ControlPlaneProtocolBuilder()
                .setName(ROUTE_PROTOCOL_NAME)
                .setType(Static.class)
                .addAugmentation(RoutingProtocolVppAttr.class, new RoutingProtocolVppAttrBuilder()
                        .setVppProtocolAttributes(new VppProtocolAttributesBuilder()
                                .setPrimaryVrf(VRF)
                                .build())
                        .build())
                .build();

        validData2= new ControlPlaneProtocolBuilder()
                .setName(ROUTE_PROTOCOL_NAME_2)
                .setType(Static.class)
                .addAugmentation(RoutingProtocolVppAttr.class, new RoutingProtocolVppAttrBuilder()
                        .setVppProtocolAttributes(new VppProtocolAttributesBuilder()
                                .setPrimaryVrf(VRF)
                                .build())
                        .build())
                .build();

        invalidData = new ControlPlaneProtocolBuilder()
                .setType(Direct.class)
                .build();

        routingProtocolContext = new NamingContext("routing-protocol", "routing-protocol-context");
        customizer = new ControlPlaneProtocolCustomizer(routingProtocolContext);
        TableKey keyV4 = new TableKey(Ipv4.class, VRF);
        TableKey keyV6 = new TableKey(Ipv6.class, VRF);
        KeyedInstanceIdentifier<Table, TableKey> vrfIidV4 = FibManagementIIds.FM_FIB_TABLES.child(Table.class, keyV4);
        KeyedInstanceIdentifier<Table, TableKey> vrfIidV6 = FibManagementIIds.FM_FIB_TABLES.child(Table.class, keyV6);
        TableKey invalidKeyV4 = new TableKey(Ipv4.class, INVALID_VRF);
        TableKey invalidKeyV6 = new TableKey(Ipv6.class, INVALID_VRF);
        KeyedInstanceIdentifier<Table, TableKey> invalidVrfIidV4 =
                FibManagementIIds.FM_FIB_TABLES.child(Table.class, invalidKeyV4);
        KeyedInstanceIdentifier<Table, TableKey> invalidVrfIidV6 =
                FibManagementIIds.FM_FIB_TABLES.child(Table.class, invalidKeyV6);
        when(writeContext.readAfter(vrfIidV4)).thenReturn(Optional.of(
                new TableBuilder().setKey(keyV4).setAddressFamily(keyV4.getAddressFamily())
                        .setTableId(keyV4.getTableId()).setName("VRF-IPV4-1").build()));
        when(writeContext.readAfter(vrfIidV6)).thenReturn(Optional.of(
                new TableBuilder().setKey(keyV6).setAddressFamily(keyV6.getAddressFamily())
                        .setTableId(keyV6.getTableId()).setName("VRF-IPV6-1").build()));
        when(writeContext.readAfter(invalidVrfIidV4)).thenReturn(Optional.absent());
        when(writeContext.readAfter(invalidVrfIidV6)).thenReturn(Optional.absent());
    }

    @Test(expected = WriteFailedException.class)
    public void testWriteInvalid() throws WriteFailedException {
        noMappingDefined(mappingContext, INVALID_TABLE_PROTOCOL_1, "routing-protocol-context");
        customizer.writeCurrentAttributes(invalidIid, invalidTableData, writeContext);

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
