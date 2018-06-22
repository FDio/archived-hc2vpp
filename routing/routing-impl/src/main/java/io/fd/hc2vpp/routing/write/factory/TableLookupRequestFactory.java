/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.routing.write.factory;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.hc2vpp.routing.write.factory.base.BasicHopRequestFactory;
import io.fd.hc2vpp.routing.write.trait.RouteRequestProducer;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.TableLookupCase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.ipv4.unicast.routing.rev180319.VppIpv4RouteAttributesAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.ipv4.unicast.routing.rev180319.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.VppIpv4Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.ipv6.unicast.routing.rev180319.VppIpv6RouteAttributesAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.ipv6.unicast.routing.rev180319.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.route.VppIpv6Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev180319.VppRouteAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class TableLookupRequestFactory extends BasicHopRequestFactory implements RouteRequestProducer {

    // must be all zeros
    private static final byte[] DEAGRAGATION_ADDRESS = new byte[4];

    public TableLookupRequestFactory(@Nonnull final VppClassifierContextManager classifierContextManager,
                                     @Nonnull final NamingContext interfaceContext,
                                     @Nonnull final NamingContext routingProtocolContext) {
        super(classifierContextManager, interfaceContext, routingProtocolContext);
    }

    public IpAddDelRoute createV4TableLookupRouteRequest(final boolean add,
                                                         @Nonnull final String parentProtocolName,
                                                         @Nonnull final Route route,
                                                         @Nonnull final WriteContext writeContext) {

        final Ipv4Prefix prefix = route.getDestinationPrefix();
        final byte[] destinationAddress = ipv4AddressPrefixToArray(prefix);
        final byte destinationPrefix = extractPrefix(prefix);
        final int primaryTableId = getRoutingProtocolContext().getIndex(parentProtocolName,
                writeContext.getMappingContext());
        final Long secondaryTableId = Optional.ofNullable(route.getNextHop().getNextHopOptions())
                .filter(nextHopOptions -> nextHopOptions instanceof TableLookupCase)
                .map(TableLookupCase.class::cast)
                .map(TableLookupCase::getSecondaryVrf)
                .map(VniReference::getValue)
                .orElseThrow(() -> new IllegalArgumentException("Table lookup option not specified correctly"));

        TableKey key = new TableKey(Ipv4.class, new VniReference(secondaryTableId));
        KeyedInstanceIdentifier<Table, TableKey> fibIid = FibManagementIIds.FM_FIB_TABLES.child(Table.class, key);
        if (!writeContext.readAfter(fibIid).isPresent()) {
            throw new IllegalArgumentException(
                    String.format("Lookup table: %s not found for route: %s", secondaryTableId, route));
        }

        VppIpv4Route vppIpv4Route =
            route.getAugmentation(VppIpv4RouteAttributesAugmentation.class) != null ? route.getAugmentation(
                VppIpv4RouteAttributesAugmentation.class).getVppIpv4Route() : null;

        final Optional<String> optClassifyTable = Optional.ofNullable(vppIpv4Route)
                .map(VppRouteAttributes::getClassifyTable);
        final byte classifyTableSet = booleanToByte(optClassifyTable.isPresent());
        final byte classifyTableIndex = optClassifyTable
                .map(tableName -> classifyTableIndex(tableName, getVppClassifierContextManager(),
                        writeContext.getMappingContext()))
                .map(Integer::byteValue)
                .orElse(DEFAULT_CLASSIFY_TABLE_INDEX);

        return flaglessAddDelRouteRequest(booleanToByte(add), ~0, DEAGRAGATION_ADDRESS, (byte) 0, (byte) 0,
                destinationAddress, destinationPrefix, (byte) 0, primaryTableId, secondaryTableId.intValue(),
                classifyTableIndex, classifyTableSet);
    }

    public IpAddDelRoute createV6TableLookupRouteRequest(final boolean add,
                                                         @Nonnull final String parentProtocolName,
                                                         @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.Route route,
                                                         @Nonnull final WriteContext writeContext) {

        final Ipv6Prefix prefix = route.getDestinationPrefix();
        final byte[] destinationAddress = ipv6AddressPrefixToArray(prefix);
        final byte destinationPrefix = extractPrefix(prefix);
        final int primaryTableId =
                getRoutingProtocolContext().getIndex(parentProtocolName, writeContext.getMappingContext());
        final Long secondaryTableId = Optional.ofNullable(route.getNextHop().getNextHopOptions())
                .filter(nextHopOptions -> nextHopOptions instanceof TableLookupCase)
                .map(TableLookupCase.class::cast)
                .map(TableLookupCase::getSecondaryVrf)
                .map(VniReference::getValue)
                .orElseThrow(() -> new IllegalArgumentException("Table lookup option not specified correctly"));

        TableKey key = new TableKey(Ipv6.class, new VniReference(secondaryTableId));
        KeyedInstanceIdentifier<Table, TableKey> fibIid = FibManagementIIds.FM_FIB_TABLES.child(Table.class, key);
        if (!writeContext.readAfter(fibIid).isPresent()) {
            throw new IllegalArgumentException(
                    String.format("Lookup table: %s not found for route: %s", secondaryTableId, route));
        }

        VppIpv6Route vppIpv6Route = route.getAugmentation(VppIpv6RouteAttributesAugmentation.class) != null
            ? route.getAugmentation(VppIpv6RouteAttributesAugmentation.class).getVppIpv6Route() : null;

        final Optional<String> optClassifyTable = Optional.ofNullable(vppIpv6Route)
                .map(VppRouteAttributes::getClassifyTable);
        final byte classifyTableSet = booleanToByte(optClassifyTable.isPresent());
        final byte classifyTableIndex = optClassifyTable
                .map(tableName -> classifyTableIndex(tableName, getVppClassifierContextManager(),
                        writeContext.getMappingContext())).map(Integer::byteValue).orElse(DEFAULT_CLASSIFY_TABLE_INDEX);

        return flaglessAddDelRouteRequest(booleanToByte(add), ~0, DEAGRAGATION_ADDRESS, (byte) 0, (byte) 1,
                destinationAddress, destinationPrefix, (byte) 0, primaryTableId, secondaryTableId.intValue(),
                classifyTableIndex, classifyTableSet);
    }
}
