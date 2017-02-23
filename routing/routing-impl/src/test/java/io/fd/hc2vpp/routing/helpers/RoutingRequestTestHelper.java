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

package io.fd.hc2vpp.routing.helpers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.routing.trait.RouteMapper;
import io.fd.hc2vpp.routing.write.trait.RouteRequestProducer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import io.fd.vpp.jvpp.core.dto.IpAddDelRouteReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.next.hop.list.next.hop.list.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol.StaticRoutes;

public interface RoutingRequestTestHelper extends ByteDataTranslator, FutureProducer, RouteMapper {

    String ROUTE_PROTOCOL_NAME = "tst-protocol";
    String ROUTE_PROTOCOL_NAME_2 = "tst-protocol-2";
    String ROUTE_NAME = "tst-route";
    String STATIC_ROUTE_PATH = "/hc2vpp-ietf-routing:routing" +
            "/hc2vpp-ietf-routing:routing-instance[hc2vpp-ietf-routing:name='" + ROUTE_PROTOCOL_NAME + "']" +
            "/hc2vpp-ietf-routing:routing-protocols" +
            "/hc2vpp-ietf-routing:routing-protocol[hc2vpp-ietf-routing:name='" + ROUTE_NAME + "']" +
            "/hc2vpp-ietf-routing:static-routes";

    default IpAddDelRoute desiredFlaglessResult(final int add, final int ipv6, final int isMultipath,
                                                final byte[] destinationAddress,
                                                final int destinationPrefixLength,
                                                final byte[] nextHopAddr,
                                                final int nextHopIndex,
                                                final int nextHopWeight,
                                                final int vrfId,
                                                final int createVrfIfNeeded,
                                                final int secondaryVrfId,
                                                final int classifyTableIndex,
                                                final int classifyTableIndexSet) {

        // verification of flagless request, so setting them to 0
        return desiredResult(add, ipv6, isMultipath, destinationAddress, destinationPrefixLength, nextHopAddr,
                nextHopIndex, nextHopWeight, vrfId, createVrfIfNeeded, secondaryVrfId, classifyTableIndex,
                classifyTableIndexSet, 0, 0, 0, 0);
    }

    default IpAddDelRoute desiredSpecialResult(final int add, final int ipv6,
                                               final byte[] destinationAddress,
                                               final int destinationPrefixLength,
                                               final int isDrop,
                                               final int isReceive,
                                               final int isUnreach,
                                               final int isProhibit,
                                               final int protocolTableId,
                                               final int secondaryTableId) {
        // verifiaction of special request that has only destination address and flag
        return desiredResult(add, ipv6, 0, destinationAddress, destinationPrefixLength, null, 0, 0, protocolTableId, 1, secondaryTableId, 0, 0,
                isDrop, isReceive, isUnreach, isProhibit);
    }

    default IpAddDelRoute desiredResult(final int add, final int ipv6, final int isMultipath,
                                        final byte[] destinationAddress,
                                        final int destinationPrefixLength,
                                        final byte[] nextHopAddr,
                                        final int nextHopIndex,
                                        final int nextHopWeight,
                                        final int vrfId,
                                        final int createVrfIfNeeded,
                                        final int secondaryVrfId,
                                        final int classifyTableIndex,
                                        final int classifyTableIndexSet,
                                        final int isDrop,
                                        final int isLocal,
                                        final int isUnreach,
                                        final int isProhibit) {
        final IpAddDelRoute request = new IpAddDelRoute();

        request.isAdd = toByte(add);
        request.isIpv6 = toByte(ipv6);
        request.isMultipath = toByte(isMultipath);
        request.dstAddress = destinationAddress;
        request.dstAddressLength = toByte(destinationPrefixLength);
        request.nextHopAddress = nextHopAddr;
        request.nextHopSwIfIndex = nextHopIndex;
        request.nextHopWeight = toByte(nextHopWeight);
        request.classifyTableIndex = classifyTableIndexSet;
        request.tableId = vrfId;
        request.nextHopTableId = secondaryVrfId;
        request.createVrfIfNeeded = toByte(createVrfIfNeeded);
        request.classifyTableIndex = classifyTableIndex;
        request.isClassify = toByte(classifyTableIndexSet);
        // special hop flags
        request.isDrop = toByte(isDrop);
        request.isLocal = toByte(isLocal);
        request.isUnreach = toByte(isUnreach);
        request.isProhibit = toByte(isProhibit);
        request.nextHopViaLabel = RouteRequestProducer.MPLS_LABEL_INVALID;
        return request;
    }

    default void verifyInvocation(final int nrOfInvocations, final List<IpAddDelRoute> desiredRequests, final
    FutureJVppCore api, final ArgumentCaptor<IpAddDelRoute> requestCaptor) {
        verify(api, times(nrOfInvocations)).ipAddDelRoute(requestCaptor.capture());

        final List<IpAddDelRoute> actualRequests = requestCaptor.getAllValues();

        for (int i = 0; i < nrOfInvocations; i++) {
            assertEquals(actualRequests.get(i), desiredRequests.get(i));
        }
    }

    default void verifyNotInvoked(final FutureJVppCore api) {
        verify(api, times(0)).ipAddDelRoute(any());
    }

    default void whenAddRouteThenSuccess(final FutureJVppCore api) {
        when(api.ipAddDelRoute(any())).thenReturn(future(new IpAddDelRouteReply()));
    }

    default Route getIpv4RouteWithId(final StaticRoutes staticRoutes, final long id) {
        return staticRoutes.getAugmentation(StaticRoutes1.class)
                .getIpv4()
                .getRoute()
                .stream()
                .filter(route -> route.getId() == id)
                .collect(RWUtils.singleItemCollector());
    }

    default NextHop getHopWithId(
            final Route route, final int id) {
        return org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv4.route.next.hop.options.NextHopList.class
                .cast(route.getNextHopOptions())
                .getNextHopList()
                .getNextHop()
                .stream()
                .filter(nextHop -> nextHop.getKey().getId().intValue() == id)
                .collect(RWUtils.singleItemCollector());
    }

    default org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.routing.routing.instance.routing.protocols.routing.protocol._static.routes.ipv6.Route getIpv6RouteWithId(
            final StaticRoutes staticRoutes, final long id) {
        return staticRoutes.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev140525.StaticRoutes1.class)
                .getIpv6()
                .getRoute()
                .stream()
                .filter(route -> route.getId() == id)
                .collect(RWUtils.singleItemCollector());
    }

}
