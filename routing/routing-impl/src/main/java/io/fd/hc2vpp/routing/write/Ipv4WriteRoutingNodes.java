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

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.StaticRoutes1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.StaticRoutes2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.RouteKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.NextHopOptions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.VppIpv4Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.VppIpv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.NextHopList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.NextHopListBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SimpleNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SimpleNextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SpecialNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SpecialNextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.next.hop.list.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.next.hop.list.NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.next.hop.list.NextHopKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol.StaticRoutes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface Ipv4WriteRoutingNodes {

    Ipv4WriteRoutingNodes INSTANCE = new Ipv4WriteRoutingNodes() {
    };

    Class<StaticRoutes1> CONFIG_IPV4_AUG_CLASS = StaticRoutes1.class;
    Class<StaticRoutes2> STATE_IPV4_AUG_CLASS = StaticRoutes2.class;

    static Ipv4 mapIpv4(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv4 state) {
        return new Ipv4Builder()
                .setRoute(state.getRoute()
                        .stream()
                        .map(route -> new RouteBuilder()
                                .setKey(new RouteKey(route.getKey().getId()))
                                .setDestinationPrefix(route.getDestinationPrefix())
                                .setDescription(route.getDescription())
                                .setId(route.getId())
                                .setVppIpv4Route(mapVppIpv4Route(route.getVppIpv4RouteState()))
                                .setNextHopOptions(mapNextHopOptions(route.getNextHopOptions()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    static VppIpv4Route mapVppIpv4Route(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.VppIpv4RouteState state) {

        return state == null
                ? null
                : new VppIpv4RouteBuilder()
                        // false is default
                        .setSecondaryVrf(state.getSecondaryVrf())
                        .setClassifyTable(state.getClassifyTable())
                        .build();
    }

    static NextHopOptions mapNextHopOptions(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.NextHopOptions state) {
        if (state instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SimpleNextHop) {
            return mapSimpleNextHop(
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SimpleNextHop.class
                            .cast(state));
        } else if (state instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.NextHopList) {
            return mapNextHopList(
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.NextHopList.class
                            .cast(state));
        } else if (state instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SpecialNextHop) {
            return mapSpecialNextHop(
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SpecialNextHop.class
                            .cast(state));
        } else {
            throw new UnsupportedOperationException("Unsupported next hop type");
        }
    }

    static SimpleNextHop mapSimpleNextHop(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SimpleNextHop hop) {
        return new SimpleNextHopBuilder()
                .setNextHop(hop.getNextHop())
                .setOutgoingInterface(hop.getOutgoingInterface())
                .build();
    }

    static NextHopList mapNextHopList(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.NextHopList state) {
        return new NextHopListBuilder()
                .setNextHopList(mapNextHopListList(state.getNextHopList()))
                .build();
    }

    static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.NextHopList mapNextHopListList(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.NextHopList state) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.NextHopListBuilder()
                .setNextHop(state.getNextHop()
                        .stream()
                        .map(nextHop -> new NextHopBuilder()
                                .setId(nextHop.getId())
                                .setOutgoingInterface(nextHop.getOutgoingInterface())
                                .setWeight(nextHop.getWeight())
                                .setPriority(nextHop.getPriority())
                                .setAddress(nextHop.getAddress())
                                .setKey(new NextHopKey(nextHop.getKey().getId()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    static SpecialNextHop mapSpecialNextHop(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.SpecialNextHop hop) {
        return new SpecialNextHopBuilder()
                .setSpecialNextHop(hop.getSpecialNextHop())
                .build();
    }

    default InstanceIdentifier<Route> ipv4RouteIdentifier(
            final InstanceIdentifier<StaticRoutes> staticRoutesIdentifier) {
        return staticRoutesIdentifier
                .augmentation(StaticRoutes1.class)
                .child(Ipv4.class)
                .child(Route.class);
    }

    default Set<InstanceIdentifier<?>> ipv4RoutingHandledChildren(final InstanceIdentifier<Route> parent) {
        return ImmutableSet.of(parent
                        .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.NextHopList.class),
                parent.child(
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.NextHopList.class)
                        .child(NextHop.class),
                parent.child(VppIpv4Route.class));
    }

    default InstanceIdentifier<Route> ipv4RouteSubtree() {
        return InstanceIdentifier.create(Route.class);
    }

    default StaticRoutes1 mapIpv4Augmentation(final StaticRoutes2 state) {
        if (state == null) {
            return null;
        }
        return new StaticRoutes1Builder()
                .setIpv4(mapIpv4(state.getIpv4()))
                .build();
    }
}
