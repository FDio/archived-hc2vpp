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

package io.fd.hc2vpp.bgp.prefix.sid;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.istack.internal.Nullable;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.bgp.RouteWriter;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.MplsRouteAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.list.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Programs VPP according to draft-ietf-idr-bgp-prefix-sid.
 *
 * @see <a href="https://tools.ietf.org/html/draft-ietf-idr-bgp-prefix-sid-07#section-4.1">Receiving BGP-Prefix-SID
 * attribute</a>
 */
final class BgpPrefixSidMplsWriter
    implements RouteWriter<LabeledUnicastRoute>, MplsRouteRequestProducer, IpRouteRequestProducer, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(BgpPrefixSidMplsWriter.class);

    @SuppressWarnings("unchecked")
    private static final InstanceIdentifier<LabeledUnicastRoute> ID =
        InstanceIdentifier.create(BgpRib.class).child(Rib.class)
            .child(LocRib.class).child(Tables.class).child((Class) LabeledUnicastRoutes.class)
            .child(LabeledUnicastRoute.class);

    private final FutureJVppCore vppApi;

    BgpPrefixSidMplsWriter(@Nonnull final FutureJVppCore vppApi) {
        this.vppApi = checkNotNull(vppApi, "vppApi should not be null");
    }

    @Override
    public void create(@Nonnull final InstanceIdentifier<LabeledUnicastRoute> id,
                       @Nullable final LabeledUnicastRoute route)
        throws WriteFailedException.CreateFailedException {
        LOG.debug("Translating id={}, route={}", id, route);
        // Compute label based on BGP Prefix SID TLVs and add following VPP FIB entries
        // (see: https://tools.ietf.org/html/draft-ietf-spring-segment-routing-msdc-08#section-4.2.2):
        //
        // 1) non-eos VPP MPLS FIB entry (for MPLS packets with derived label in the middle of label stack)
        final MplsRouteAddDel mplsRequest = mplsRouteAddDelFor(route, true, LOG);
        getReplyForCreate(vppApi.mplsRouteAddDel(mplsRequest).toCompletableFuture(), id, route);

        // 2) eos VPP MPLS FIB entry (for MPLS packets with derived label at the end of the label stack)
        mplsRequest.mrEos = 1;
        getReplyForCreate(vppApi.mplsRouteAddDel(mplsRequest).toCompletableFuture(), id, route);

        // 3) VPP IP FIB entry (impose received outbound label on IP packets destined to the BGP prefix)
        getReplyForCreate(vppApi.ipAddDelRoute(ipAddDelRouteFor(route, true)).toCompletableFuture(), id, route);

        LOG.debug("VPP FIB updated successfully (added id={}).", id);
    }

    @Override
    public void delete(@Nonnull final InstanceIdentifier<LabeledUnicastRoute> id,
                       @Nullable final LabeledUnicastRoute route)
        throws WriteFailedException.DeleteFailedException {
        LOG.debug("Removing id={}, route={}", id, route);
        // Remove non-eos VPP MPLS FIB entry:
        final MplsRouteAddDel mplsRequest = mplsRouteAddDelFor(route, false, LOG);
        getReplyForDelete(vppApi.mplsRouteAddDel(mplsRequest).toCompletableFuture(), id);

        // Remove eos VPP MPLS FIB entry:
        mplsRequest.mrEos = 1;
        getReplyForDelete(vppApi.mplsRouteAddDel(mplsRequest).toCompletableFuture(), id);

        // Remove VPP IP FIB entry:
        getReplyForDelete(vppApi.ipAddDelRoute(ipAddDelRouteFor(route, false)).toCompletableFuture(), id);

        LOG.debug("VPP FIB updated successfully (removed id={}).", id);
    }

    @Override
    public void update(@Nonnull final InstanceIdentifier<LabeledUnicastRoute> id,
                       @Nullable final LabeledUnicastRoute routeBefore,
                       @Nullable final LabeledUnicastRoute routeAfter)
        throws WriteFailedException.UpdateFailedException {
        throw new WriteFailedException.UpdateFailedException(id, routeBefore, routeAfter,
            new UnsupportedOperationException("Operation not supported"));
    }

    // TODO(HC2VPP-268): add test which checks if ID is serializable
    @Nonnull
    @Override
    public InstanceIdentifier<LabeledUnicastRoute> getManagedDataObjectType() {
        return ID;
    }
}
