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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.naming.Ipv4RouteNamesFactory;
import io.fd.hc2vpp.routing.trait.RouteMapper;
import io.fd.hc2vpp.routing.write.factory.MultipathHopRequestFactory;
import io.fd.hc2vpp.routing.write.factory.SimpleHopRequestFactory;
import io.fd.hc2vpp.routing.write.factory.SpecialNextHopRequestFactory;
import io.fd.hc2vpp.routing.write.factory.TableLookupRequestFactory;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.IpAddDelRoute;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.RouteKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.NextHopList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.SimpleNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.SpecialNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.TableLookupCase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.next.hop.list.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocol;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for handling write operations for {@link Ipv4} according to ietf-ipv4-unicast-routing.yang
 */
public class Ipv4RouteCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<Route, RouteKey>, JvppReplyConsumer, RouteMapper {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4RouteCustomizer.class);

    private final NamingContext routesContext;
    private final MultiNamingContext routesHopsContext;
    /**
     * Request factories
     */
    private final SimpleHopRequestFactory simpleHopRequestFactory;
    private final MultipathHopRequestFactory multipathHopRequestFactory;
    private final SpecialNextHopRequestFactory specialNextHopRequestFactory;
    private final TableLookupRequestFactory tableLookupRequestFactory;

    /**
     * Names factory
     */
    private final Ipv4RouteNamesFactory routeNamesFactory;

    Ipv4RouteCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                        @Nonnull final NamingContext interfaceContext,
                        @Nonnull final NamingContext routesContext,
                        @Nonnull final NamingContext routingProtocolContext,
                        @Nonnull final MultiNamingContext routesHopsContext,
                        @Nonnull final VppClassifierContextManager classifierContextManager) {
        super(futureJVppCore);

        this.routesContext = routesContext;
        this.routesHopsContext = routesHopsContext;
        simpleHopRequestFactory =
                SimpleHopRequestFactory.forContexts(classifierContextManager, interfaceContext, routingProtocolContext);
        multipathHopRequestFactory = MultipathHopRequestFactory
                .forContexts(classifierContextManager, interfaceContext, routingProtocolContext);
        specialNextHopRequestFactory = SpecialNextHopRequestFactory.forContexts(classifierContextManager,
                interfaceContext, routingProtocolContext);
        routeNamesFactory = new Ipv4RouteNamesFactory(interfaceContext, routingProtocolContext);
        tableLookupRequestFactory =
            new TableLookupRequestFactory(classifierContextManager, interfaceContext, routingProtocolContext);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Route> instanceIdentifier,
                                       @Nonnull final Route route,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String parentProtocolName = instanceIdentifier.firstKeyOf(ControlPlaneProtocol.class).getName();
        final String routeName = routeNamesFactory.uniqueRouteName(parentProtocolName, route);
        Optional<ControlPlaneProtocol> protocolOptional =
                writeContext.readAfter(RWUtils.cutId(instanceIdentifier, ControlPlaneProtocol.class));
        Preconditions.checkArgument(protocolOptional.isPresent(), "Control protocol cannot be null for route: {}",
                instanceIdentifier);
        TableKey key = new TableKey(
                org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv4.class,
                new VniReference(ControlPlaneProtocolCustomizer.extractTableId(protocolOptional.get())));

        if (!ControlPlaneProtocolCustomizer.isTablePresent(key, writeContext)) {
            throw new WriteFailedException(instanceIdentifier, "IPv4 FIB table does not exist!");
        }

        writeRoute(instanceIdentifier, parentProtocolName, routeName, route, writeContext, true);

        // maps new route by next available index,
        routesContext.addName(routeName, writeContext.getMappingContext());
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Route> instanceIdentifier,
                                        @Nonnull final Route routeBefore,
                                        @Nonnull final Route routeAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        throw new WriteFailedException.UpdateFailedException(instanceIdentifier, routeBefore, routeAfter,
                new UnsupportedOperationException("Operation not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Route> instanceIdentifier,
                                        @Nonnull final Route route,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String parentProtocolName = instanceIdentifier.firstKeyOf(ControlPlaneProtocol.class).getName();
        final String routeName = routeNamesFactory.uniqueRouteName(parentProtocolName, route);
        writeRoute(instanceIdentifier, parentProtocolName, routeName, route, writeContext, false);
        routesContext.removeName(routeName, writeContext.getMappingContext());
    }

    private void writeRoute(@Nonnull final InstanceIdentifier<Route> identifier,
                            @Nonnull final String parentProtocolName,
                            @Nonnull final String routeName,
                            @Nonnull final Route route,
                            @Nonnull final WriteContext writeContext,
                            final boolean isAdd) throws WriteFailedException {
        if (route.getNextHop().getNextHopOptions() instanceof SimpleNextHop) {
            writeRoute(
                    simpleHopRequestFactory.createIpv4SimpleHopRequest(isAdd, parentProtocolName, route,
                            writeContext.getMappingContext()),
                    identifier);
        } else if (route.getNextHop().getNextHopOptions() instanceof NextHopList) {
            final List<NextHop> createdHops =
                    writeMultihopRoute(identifier, parentProtocolName, route, writeContext, isAdd);

            // after all hops has been added, add mappings to preserve hop ids, or remove them
            if (isAdd) {
                addMappingForEachHop(routeName, writeContext, createdHops);
            } else {
                removeMappingForEachHop(routeName, writeContext, createdHops);
            }
        } else if (route.getNextHop().getNextHopOptions() instanceof SpecialNextHop) {
            writeSpecialHopRoute(identifier, route, parentProtocolName, writeContext, isAdd);
        } else if (route.getNextHop().getNextHopOptions() instanceof TableLookupCase) {
            writeRoute(tableLookupRequestFactory
                    .createV4TableLookupRouteRequest(isAdd, parentProtocolName, route, writeContext), identifier);
        } else {
            throw new IllegalArgumentException("Unsupported next-hop type");
        }
    }

    private void removeMappingForEachHop(final @Nonnull String routeName, final @Nonnull WriteContext writeContext,
                                         final List<NextHop> createdHops) {
        createdHops.forEach(nextHop -> routesHopsContext.removeChild(routeName,
                routeNamesFactory.uniqueRouteHopName(nextHop),
                writeContext.getMappingContext()));
    }

    private void addMappingForEachHop(final @Nonnull String routeName, final @Nonnull WriteContext writeContext,
                                      final List<NextHop> createdHops) {
        createdHops.forEach(nextHop -> routesHopsContext.addChild(routeName,
                Integer.valueOf(nextHop.getIndex()),
                routeNamesFactory.uniqueRouteHopName(nextHop),
                writeContext.getMappingContext()));
    }

    private List<NextHop> writeMultihopRoute(@Nonnull final InstanceIdentifier<Route> identifier,
                                             @Nonnull final String parentProtocolName, @Nonnull final Route route,
                                             @Nonnull final WriteContext writeContext, final boolean isAdd)
            throws WriteFailedException {
        // list of next hops
        final NextHopList hopList = NextHopList.class.cast(route.getNextHop().getNextHopOptions());
        final MappingContext mappingContext = writeContext.getMappingContext();
        LOG.debug("Writing hop list {} for route {}", hopList, identifier);

        // order hops to preserve order by ids(even that model is not ordered)
        final List<NextHop> orderedHops = hopList.getNextHopList().getNextHop().stream()
                .sorted(Comparator.comparing(NextHop::getIndex))
                .collect(Collectors.toList());

        for (NextHop hop : orderedHops) {
            LOG.debug("Writing hop {} for route {}", hop, identifier);

            final IpAddDelRoute request = multipathHopRequestFactory
                    .createIpv4MultipathHopRequest(isAdd, parentProtocolName, route, hop, mappingContext);

            writeRoute(request, identifier);
        }

        return orderedHops;
    }

    private void writeSpecialHopRoute(final @Nonnull InstanceIdentifier<Route> identifier, final @Nonnull Route route,
                                      final @Nonnull String parentProtocolName,
                                      final @Nonnull WriteContext writeContext, final boolean isAdd)
            throws WriteFailedException {
        final SpecialNextHop hop = SpecialNextHop.class.cast(route.getNextHop().getNextHopOptions());
        final MappingContext mappingContext = writeContext.getMappingContext();

        final IpAddDelRoute request =
            specialNextHopRequestFactory.createIpv4SpecialHopRequest(isAdd, parentProtocolName, route, mappingContext,
                                                                     hop.getSpecialNextHopEnum());

        writeRoute(request, identifier);
    }


    private void writeRoute(final IpAddDelRoute request, final InstanceIdentifier<Route> identifier)
            throws WriteFailedException {
        LOG.debug("Writing request {} for path {}", request, identifier);
        getReplyForWrite(getFutureJVpp().ipAddDelRoute(request).toCompletableFuture(), identifier);
    }
}
