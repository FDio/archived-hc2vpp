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

package io.fd.hc2vpp.bgp.inet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.bgp.RouteWriter;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Ipv4Writer implements RouteWriter<Ipv4Route>, Ipv4Translator, JvppReplyConsumer, RouteRequestProducer {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4Writer.class);

    @SuppressWarnings("unchecked")
    private static final InstanceIdentifier<Ipv4Route> ID = InstanceIdentifier.create(BgpRib.class).child(Rib.class)
        .child(LocRib.class).child(Tables.class).child((Class) Ipv4Routes.class)
        .child(Ipv4Route.class);

    private final FutureJVppCore vppApi;

    Ipv4Writer(@Nonnull final FutureJVppCore vppApi) {
        this.vppApi = checkNotNull(vppApi, "vppApi should not be null");
    }

    @Override
    public void create(@Nonnull final InstanceIdentifier<Ipv4Route> id,
                       @Nonnull final Ipv4Route route)
        throws WriteFailedException.CreateFailedException {
        final IpAddDelRoute request = request(route, true);
        LOG.debug("Translating id={}, route={} to {}", id, route, request);
        getReplyForCreate(vppApi.ipAddDelRoute(request).toCompletableFuture(), id, route);
        LOG.debug("VPP FIB updated successfully (added id={}).", id);
    }

    @Override
    public void delete(@Nonnull final InstanceIdentifier<Ipv4Route> id,
                       @Nonnull final Ipv4Route route)
        throws WriteFailedException.DeleteFailedException {
        LOG.debug("Removing id={}, route={}", id, route);
        getReplyForDelete(vppApi.ipAddDelRoute(request(route, false)).toCompletableFuture(), id);
        LOG.debug("VPP FIB updated successfully (removed id={}).", id);
    }

    @Override
    public void update(@Nonnull final InstanceIdentifier<Ipv4Route> id,
                       @Nonnull final Ipv4Route routeBefore,
                       @Nonnull final Ipv4Route routeAfter)
        throws WriteFailedException.UpdateFailedException {
        throw new WriteFailedException.UpdateFailedException(id, routeBefore, routeAfter,
            new UnsupportedOperationException("Operation not supported"));
    }

    private IpAddDelRoute request(final Ipv4Route route, boolean isAdd) {
        // TODO(HC2VPP-177): add support for request.nextHopWeight for multiple path case

        final CNextHop cNextHop = route.getAttributes().getCNextHop();
        checkArgument(cNextHop instanceof Ipv4NextHopCase, "only ipv4 next hop is supported, but was %s (route = %s)",
            cNextHop, route);

        final IpAddDelRoute request = ipAddDelRoute(isAdd);

        final Ipv4Address nextHop = ((Ipv4NextHopCase) cNextHop).getIpv4NextHop().getGlobal();
        request.nextHopAddress = ipv4AddressNoZoneToArray(nextHop.getValue());

        final Ipv4Prefix destinationAddress = route.getPrefix();
        request.dstAddress = ipv4AddressPrefixToArray(destinationAddress);
        request.dstAddressLength = extractPrefix(destinationAddress);

        return request;
    }

    @Nonnull
    @Override
    public InstanceIdentifier<Ipv4Route> getManagedDataObjectType() {
        return ID;
    }
}
