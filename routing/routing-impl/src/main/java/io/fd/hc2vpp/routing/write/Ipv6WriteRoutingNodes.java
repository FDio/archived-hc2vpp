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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.StaticRoutes1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.StaticRoutes2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.RouteKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.NextHopOptions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.VppIpv6Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.VppIpv6RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.NextHopListBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.SimpleNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.SimpleNextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.SpecialNextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.SpecialNextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.NextHopList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.next.hop.list.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.next.hop.list.NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.next.hop.list.NextHopKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol.StaticRoutes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public interface Ipv6WriteRoutingNodes {

    Ipv6WriteRoutingNodes INSTANCE = new Ipv6WriteRoutingNodes() {
    };

    Class<StaticRoutes1> CONFIG_IPV6_AUG_CLASS = StaticRoutes1.class;
    Class<StaticRoutes2> STATE_IPV6_AUG_CLASS = StaticRoutes2.class;

    static Ipv6 mapIpv6(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.Ipv6 state) {
        return new Ipv6Builder()
                .setRoute(state.getRoute()
                        .stream()
                        .map(route -> new RouteBuilder()
                                .setKey(new RouteKey(route.getKey().getId()))
                                .setDestinationPrefix(route.getDestinationPrefix())
                                .setDescription(route.getDescription())
                                .setId(route.getId())
                                .setVppIpv6Route(mapVppIpv6Route(route.getVppIpv6RouteState()))
                                .setNextHopOptions(mapNextHopOptions(route.getNextHopOptions()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    static VppIpv6Route mapVppIpv6Route(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.VppIpv6RouteState state) {
        return state == null
                ? null
                : new VppIpv6RouteBuilder()
                        .setSecondaryVrf(state.getSecondaryVrf())
                        .setClassifyTable(state.getClassifyTable())
                        .build();
    }

    static NextHopOptions mapNextHopOptions(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.NextHopOptions state) {
        if (state instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.SimpleNextHop) {
            return mapSimpleNextHop(
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.SimpleNextHop.class
                            .cast(state));
        } else if (state instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.NextHopList) {
            return mapNextHopList(
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.NextHopList.class
                            .cast(state));
        } else if (state instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.SpecialNextHop) {
            return mapSpecialNextHop(
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.SpecialNextHop.class
                            .cast(state));
        } else {
            throw new UnsupportedOperationException("Unsupported next hop type");
        }
    }

    static SimpleNextHop mapSimpleNextHop(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.SimpleNextHop hop) {
        return new SimpleNextHopBuilder()
                .setNextHop(hop.getNextHop())
                .setOutgoingInterface(hop.getOutgoingInterface())
                .build();
    }

    static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.NextHopList mapNextHopList(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.NextHopList state) {
        return new NextHopListBuilder()
                .setNextHopList(mapNextHopListList(state.getNextHopList()))
                .build();
    }

    static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.NextHopList mapNextHopListList(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.NextHopList state) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.next.hop.list.NextHopListBuilder()
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
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.state.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.route.next.hop.options.SpecialNextHop hop) {
        return new SpecialNextHopBuilder()
                .setSpecialNextHop(hop.getSpecialNextHop())
                .build();
    }

    default InstanceIdentifier<Route> ipv6RouteIdentifier(
            final InstanceIdentifier<StaticRoutes> staticRoutesIdentifier) {
        return staticRoutesIdentifier
                .augmentation(StaticRoutes1.class)
                .child(Ipv6.class)
                .child(Route.class);
    }

    default InstanceIdentifier<Route> ipv6RouteSubtree() {
        return InstanceIdentifier
                .create(Route.class);
    }

    default Set<InstanceIdentifier<?>> ipv6RoutingHandledChildren(
            final InstanceIdentifier<Route> parent) {
        return ImmutableSet.of(parent.child(NextHopList.class),
                parent.child(NextHopList.class).child(NextHop.class),
                parent.child(VppIpv6Route.class));
    }

    default org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.StaticRoutes1 mapIpv6Augmentation(
            final StaticRoutes2 state) {
        if (state == null) {
            return null;
        }
        return new StaticRoutes1Builder()
                .setIpv6(mapIpv6(state.getIpv6()))
                .build();
    }
}
