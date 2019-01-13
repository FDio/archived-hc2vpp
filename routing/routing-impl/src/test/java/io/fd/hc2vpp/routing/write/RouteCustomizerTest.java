/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

import static io.fd.hc2vpp.routing.helpers.InterfaceTestHelper.INTERFACE_INDEX;
import static io.fd.hc2vpp.routing.helpers.InterfaceTestHelper.INTERFACE_NAME;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.hc2vpp.routing.helpers.ClassifyTableTestHelper;
import io.fd.hc2vpp.routing.helpers.RoutingRequestTestHelper;
import io.fd.hc2vpp.routing.helpers.SchemaContextTestHelper;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv4;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.routing.rev180319.RoutingProtocolVppAttr;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.routing.rev180319.RoutingProtocolVppAttrBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.routing.rev180319.routing.control.plane.protocols.control.plane.protocol.VppProtocolAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.ControlPlaneProtocols;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocolKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

abstract class RouteCustomizerTest extends WriterCustomizerTest implements RoutingRequestTestHelper,
        ClassifyTableTestHelper, SchemaContextTestHelper {
    static final Long TABLE_ID = 1L;
    static final Long INVALID_TABLE_ID = 3L;
    static final String ROUTE_PROTOCOL_INVALID_NAME = "tst-protocol-3";
    static final VniReference SEC_TABLE_ID = new VniReference(4L);
    private static final TableKey IPV4_TABLE_KEY = new TableKey(Ipv4.class, new VniReference(TABLE_ID));
    private static final TableKey IPV6_TABLE_KEY = new TableKey(Ipv6.class, new VniReference(TABLE_ID));
    private static final TableKey INVALID_IPV4_TABLE_KEY = new TableKey(Ipv4.class, new VniReference(INVALID_TABLE_ID));
    private static final TableKey INVALID_IPV6_TABLE_KEY = new TableKey(Ipv6.class, new VniReference(INVALID_TABLE_ID));
    private static final InstanceIdentifier<Table> TABLE_V4_IID =
            FibManagementIIds.FM_FIB_TABLES.child(Table.class, IPV4_TABLE_KEY);
    private static final InstanceIdentifier<Table> TABLE_V6_IID =
            FibManagementIIds.FM_FIB_TABLES.child(Table.class, IPV6_TABLE_KEY);
    private static final InstanceIdentifier<Table> INVALID_TABLE_V4_IID =
            FibManagementIIds.FM_FIB_TABLES.child(Table.class, INVALID_IPV4_TABLE_KEY);
    private static final InstanceIdentifier<Table> INVALID_TABLE_V6_IID =
            FibManagementIIds.FM_FIB_TABLES.child(Table.class, INVALID_IPV6_TABLE_KEY);
    private static final ControlPlaneProtocolKey
            CONTROL_PLANE_PROTOCOL_KEY = new ControlPlaneProtocolKey(ROUTE_PROTOCOL_NAME, Static.class);
    static final KeyedInstanceIdentifier<ControlPlaneProtocol, ControlPlaneProtocolKey>
            CONTROL_PROTOCOL_IID = InstanceIdentifier.create(ControlPlaneProtocols.class)
            .child(ControlPlaneProtocol.class, CONTROL_PLANE_PROTOCOL_KEY);
    private static final ControlPlaneProtocolKey
            CONTROL_PLANE_PROTOCOL_INVALID_KEY = new ControlPlaneProtocolKey(ROUTE_PROTOCOL_INVALID_NAME, Static.class);
    static final KeyedInstanceIdentifier<ControlPlaneProtocol, ControlPlaneProtocolKey>
            CONTROL_PROTOCOL_INVALID_IID = InstanceIdentifier.create(ControlPlaneProtocols.class)
            .child(ControlPlaneProtocol.class, CONTROL_PLANE_PROTOCOL_INVALID_KEY);

    @Mock
    VppClassifierContextManager classifyManager;

    @Mock
    MultiNamingContext routeHopContext;

    NamingContext routingProtocolContext;
    NamingContext interfaceContext;

    @Override
    protected void setUpTest() throws Exception {
        interfaceContext = new NamingContext("interface", "interface-context");
        routingProtocolContext = new NamingContext("routing-protocol", "routing-protocol-context");

        defineMapping(mappingContext, INTERFACE_NAME, INTERFACE_INDEX, "interface-context");
        defineMapping(mappingContext, ROUTE_PROTOCOL_NAME, TABLE_ID.intValue(), "routing-protocol-context");
        defineMapping(mappingContext, ROUTE_PROTOCOL_INVALID_NAME, INVALID_TABLE_ID.intValue(),
                "routing-protocol-context");
        addMapping(classifyManager, CLASSIFY_TABLE_NAME, CLASSIFY_TABLE_INDEX, mappingContext);
        whenAddRouteThenSuccess(api);

        when(writeContext.readAfter(CONTROL_PROTOCOL_IID)).thenReturn(Optional.of(new ControlPlaneProtocolBuilder()
                .withKey(CONTROL_PLANE_PROTOCOL_KEY).setName(ROUTE_PROTOCOL_NAME).setType(Static.class)
                .addAugmentation(RoutingProtocolVppAttr.class, new RoutingProtocolVppAttrBuilder()
                        .setVppProtocolAttributes(new VppProtocolAttributesBuilder()
                                .setPrimaryVrf(new VniReference(TABLE_ID))
                                .build()).build())
                .build()));

        when(writeContext.readAfter(CONTROL_PROTOCOL_INVALID_IID)).thenReturn(Optional.of(
                new ControlPlaneProtocolBuilder().withKey(CONTROL_PLANE_PROTOCOL_INVALID_KEY)
                        .setName(ROUTE_PROTOCOL_INVALID_NAME).setType(Static.class)
                        .addAugmentation(RoutingProtocolVppAttr.class, new RoutingProtocolVppAttrBuilder()
                                .setVppProtocolAttributes(new VppProtocolAttributesBuilder()
                                        .setPrimaryVrf(new VniReference(INVALID_TABLE_ID)).build()).build())
                        .build()));

        when(writeContext.readAfter(TABLE_V4_IID)).thenReturn(Optional.of(
                new TableBuilder().withKey(IPV4_TABLE_KEY).setAddressFamily(Ipv4.class)
                        .setTableId(IPV4_TABLE_KEY.getTableId()).build()));
        when(writeContext.readAfter(TABLE_V6_IID)).thenReturn(Optional.of(
                new TableBuilder().withKey(IPV6_TABLE_KEY).setAddressFamily(Ipv6.class)
                        .setTableId(IPV6_TABLE_KEY.getTableId()).build()));
        when(writeContext.readAfter(INVALID_TABLE_V4_IID)).thenReturn(Optional.absent());
        when(writeContext.readAfter(INVALID_TABLE_V6_IID)).thenReturn(Optional.absent());

    }
}
