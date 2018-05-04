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

package io.fd.hc2vpp.mpls;

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.MplsLabelTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.MplsRouteAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.types.FibMplsLabel;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.StaticLspConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.OutSegment;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.out.segment.SimplePath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.routing.mpls._static.lsps.StaticLsp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.MplsLabel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Translates {@link StaticLspConfig.Operation#SwapAndForward} operation to mpls_route_add_del API.
 *
 * @see <a href="https://git.fd.io/vpp/tree/src/vnet/mpls/mpls.api">mpls_route_add_del</a> definition
 */
final class MplsSwapWriter implements LspWriter, Ipv4Translator, MplsInSegmentTranslator, MplsLabelTranslator {

    private final FutureJVppCore vppApi;
    private final NamingContext interfaceContext;

    MplsSwapWriter(@Nonnull final FutureJVppCore vppApi, @Nonnull final NamingContext interfaceContext) {
        this.vppApi = vppApi;
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void write(@Nonnull final InstanceIdentifier<StaticLsp> id, @Nonnull final StaticLsp data,
                      @Nonnull final MappingContext ctx, final boolean isAdd) throws WriteFailedException {
        final Config config = data.getConfig();
        final MplsRouteAddDel request = new MplsRouteAddDel();
        request.mrIsAdd = booleanToByte(isAdd);
        request.mrEos = 1; // only SWAP for the last label in the stack is currently supported

        translate(config.getInSegment(), request);
        translate(config.getOutSegment(), request, ctx);

        // default values based on inspecting VPP's CLI and make test code
        request.mrClassifyTableIndex = -1;
        request.mrNextHopWeight = 1;
        request.mrNextHopViaLabel = MPLS_LABEL_INVALID;

        getReplyForWrite(vppApi.mplsRouteAddDel(request).toCompletableFuture(), id);
    }

    private void translate(@Nonnull final OutSegment outSegment, @Nonnull final MplsRouteAddDel request,
                           @Nonnull final MappingContext ctx) {
        checkArgument(outSegment instanceof SimplePath, "Unsupported out-segment type: %s", outSegment);
        final SimplePath path = (SimplePath) outSegment;
        final IpAddress nextHop = path.getNextHop();
        checkArgument(nextHop != null, "Configuring swap-and-forward, but next-hop is missing.");

        // TODO(HC2VPP-264): add support for mpls + v6
        final Ipv4Address address = nextHop.getIpv4Address();
        checkArgument(address != null, "Only IPv4 next-hop address is supported.");
        request.mrNextHop = ipv4AddressNoZoneToArray(address.getValue());

        final MplsLabel outgoingLabel = path.getOutgoingLabel();
        checkArgument(outgoingLabel != null, "Configuring swap-and-forward, but outgoing-label is missing.");
        request.mrNextHopOutLabelStack = new FibMplsLabel[] {translate(outgoingLabel.getValue())};
        request.mrNextHopNOutLabels = 1;

        final String outgoingInterface = path.getOutgoingInterface();
        checkArgument(outgoingInterface != null, "Configuring swap-and-forward, but outgoing-interface is missing.");
        request.mrNextHopSwIfIndex = interfaceContext.getIndex(outgoingInterface, ctx);
    }
}
