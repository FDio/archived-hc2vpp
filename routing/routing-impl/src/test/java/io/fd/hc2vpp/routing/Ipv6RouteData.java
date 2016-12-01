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

package io.fd.hc2vpp.routing;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.RouteBuilder;

public class Ipv6RouteData {

    public static final Ipv6Prefix
            FIRST_ADDRESS_AS_V6_PREFIX = new Ipv6Prefix("2001:0db8:0a0b:12f0:0000:0000:0000:0001/64");

    public static final byte[] FIRST_ADDRESS_AS_ARRAY = {32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 0, 1};

    public static final Ipv6Address
            SECOND_ADDRESS_AS_ADDRESS = new Ipv6Address("2001:0db8:0a0b:12f0:0000:0000:0000:0002");

    public static final byte[] SECOND_ADDRESS_AS_ARRAY = {32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 0, 2};


    public static final Route IPV6_ROUTE_WITH_CLASSIFIER_BLACKHOLE_HOP = new RouteBuilder()
            .setDestinationPrefix(FIRST_ADDRESS_AS_V6_PREFIX)
            /*.setNextHopOptions(new SpecialNextHopBuilder()
                    .setSpecialNextHop(SpecialNextHopGrouping.SpecialNextHop.Blackhole)
                    /*.addAugmentation(SpecialNextHop2.class, new SpecialNextHop2Builder()
                            .setPrimaryVrf(new VniReference(2L))
                            .setAutoCreateVrf(true)
                            .setClassifyTable(CLASSIFY_TABLE_NAME)
                            .build())
                    .build())*/
            .build();

    public static final Route IPV6_ROUTE_WITHOUT_CLASSIFIER_BLACKHOLE_HOP = new RouteBuilder()
            .setDestinationPrefix(FIRST_ADDRESS_AS_V6_PREFIX)
            /*.setNextHopOptions(new SpecialNextHopBuilder()
                    .setSpecialNextHop(SpecialNextHopGrouping.SpecialNextHop.Blackhole)
                   /* .addAugmentation(SpecialNextHop2.class, new SpecialNextHop2Builder()
                            .setPrimaryVrf(new VniReference(2L))
                            .setAutoCreateVrf(true)
                            .build())
                    .build())*/
            .build();

    public static final Route IPV6_ROUTE_WITH_CLASSIFIER_RECEIVE_HOP = new RouteBuilder()
            .setDestinationPrefix(FIRST_ADDRESS_AS_V6_PREFIX)
            /*.setNextHopOptions(new SpecialNextHopBuilder()
                    .setSpecialNextHop(SpecialNextHopGrouping.SpecialNextHop.Blackhole)
                    /*.addAugmentation(SpecialNextHop2.class, new SpecialNextHop2Builder()
                            .setPrimaryVrf(new VniReference(2L))
                            .setAutoCreateVrf(true)
                            .setClassifyTable(CLASSIFY_TABLE_NAME)
                            .build())
                    .build())*/
            .build();

    public static final Route IPV6_ROUTE_WITHOUT_CLASSIFIER_RECEIVE_HOP = new RouteBuilder()
            .setDestinationPrefix(FIRST_ADDRESS_AS_V6_PREFIX)
            /*.setNextHopOptions(new SpecialNextHopBuilder()
                    .setSpecialNextHop(SpecialNextHopGrouping.SpecialNextHop.Blackhole)
                    /*.addAugmentation(SpecialNextHop2.class, new SpecialNextHop2Builder()
                            .setPrimaryVrf(new VniReference(2L))
                            .setAutoCreateVrf(true)
                            .build())
                    .build())*/
            .build();
}
