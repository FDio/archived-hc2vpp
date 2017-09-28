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
import io.fd.hc2vpp.routing.write.factory.base.BasicHopRequestFactory;
import io.fd.hc2vpp.routing.write.trait.RouteRequestProducer;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.TableLookup;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.table.lookup.TableLookupParams;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.VniReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.VppRouteAttributes;


import javax.annotation.Nonnull;
import java.util.Optional;

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
                                                         @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.Route route,
                                                         @Nonnull final MappingContext mappingContext) {

        final Ipv4Prefix prefix = route.getDestinationPrefix();
        final byte[] destinationAddress = ipv4AddressPrefixToArray(prefix);
        final byte destinationPrefix = extractPrefix(prefix);
        final int primaryTableId = getRoutingProtocolContext().getIndex(parentProtocolName, mappingContext);
        final byte secondaryTableId = Optional.ofNullable(route.getNextHopOptions())
                .filter(nextHopOptions -> nextHopOptions instanceof TableLookup)
                .map(TableLookup.class::cast)
                .map(TableLookup::getTableLookupParams)
                .map(TableLookupParams::getSecondaryVrf)
                .map(VniReference::getValue)
                .map(Long::byteValue)
                .orElseThrow(() -> new IllegalArgumentException("Table lookup option not specified correctly"));

        final Optional<String> optClassifyTable = Optional.ofNullable(route.getVppIpv4Route())
                .map(VppRouteAttributes::getClassifyTable);
        final byte classifyTableSet = booleanToByte(optClassifyTable.isPresent());
        final byte classifyTableIndex = optClassifyTable.map(tableName -> classifyTableIndex(tableName, getVppClassifierContextManager(), mappingContext))
                .map(Integer::byteValue)
                .orElse(DEFAULT_CLASSIFY_TABLE_INDEX);

        return flaglessAddDelRouteRequest(booleanToByte(add), ~0, DEAGRAGATION_ADDRESS, (byte) 0, (byte) 0, destinationAddress,
                destinationPrefix, (byte) 0, primaryTableId, secondaryTableId, classifyTableIndex, classifyTableSet);
    }

    public IpAddDelRoute createV6TableLookupRouteRequest(final boolean add,
                                                         @Nonnull final String parentProtocolName,
                                                         @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.Route route,
                                                         @Nonnull final MappingContext mappingContext) {

        final Ipv6Prefix prefix = route.getDestinationPrefix();
        final byte[] destinationAddress = ipv6AddressPrefixToArray(prefix);
        final byte destinationPrefix = extractPrefix(prefix);
        final int primaryTableId = getRoutingProtocolContext().getIndex(parentProtocolName, mappingContext);
        final byte secondaryTableId = Optional.ofNullable(route.getNextHopOptions())
                .filter(nextHopOptions -> nextHopOptions instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.TableLookup)
                .map(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.TableLookup.class::cast)
                .map(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.TableLookup::getTableLookupParams)
                .map(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.table.lookup.TableLookupParams::getSecondaryVrf)
                .map(VniReference::getValue)
                .map(Long::byteValue)
                .orElseThrow(() -> new IllegalArgumentException("Table lookup option not specified correctly"));

        final Optional<String> optClassifyTable = Optional.ofNullable(route.getVppIpv6Route())
                .map(VppRouteAttributes::getClassifyTable);
        final byte classifyTableSet = booleanToByte(optClassifyTable.isPresent());
        final byte classifyTableIndex = optClassifyTable.map(tableName -> classifyTableIndex(tableName, getVppClassifierContextManager(), mappingContext))
                .map(Integer::byteValue)
                .orElse(DEFAULT_CLASSIFY_TABLE_INDEX);

        return flaglessAddDelRouteRequest(booleanToByte(add), ~0, DEAGRAGATION_ADDRESS, (byte) 0, (byte) 1, destinationAddress,
                destinationPrefix, (byte) 0, primaryTableId, secondaryTableId, classifyTableIndex, classifyTableSet);
    }
}
