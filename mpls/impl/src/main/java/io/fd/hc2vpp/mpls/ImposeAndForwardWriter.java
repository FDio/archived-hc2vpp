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
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.types.FibMplsLabel;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.MplsOperationsType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.OutSegment;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.MultiplePaths;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.SimplePath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.multiple.paths.Paths;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.top.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment.InSegment;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment_config.Type;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment_config.type.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.path.outgoing.labels.outgoing.labels.OutgoingLabels;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls._static.lsps.StaticLsp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.MplsLabel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Translates {@link MplsOperationsType#ImposeAndForward ImposeAndForward} operation to ip_add_del_route API.
 *
 * @see <a href="https://git.fd.io/vpp/tree/src/vnet/ip/ip.api">ip_add_del_route</a> definition
 */
final class ImposeAndForwardWriter implements LspWriter, Ipv4Translator, MplsLabelTranslator {
    private final FutureJVppCore vppApi;
    private final NamingContext interfaceContext;

    ImposeAndForwardWriter(@Nonnull final FutureJVppCore vppApi, @Nonnull final NamingContext interfaceContext) {
        this.vppApi = vppApi;
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void write(@Nonnull final InstanceIdentifier<StaticLsp> id, @Nonnull final StaticLsp data,
                      @Nonnull final MappingContext ctx, final boolean isAdd) throws WriteFailedException {
        final Config config = data.getConfig();
        final IpAddDelRoute request = new IpAddDelRoute();
        request.isAdd = booleanToByte(isAdd);

        translate(config.getInSegment(), request);
        translate(data.getOutSegment(), request, ctx);

        // default values based on inspecting VPP's CLI and make test code
        request.nextHopWeight = 1;
        request.nextHopViaLabel = MPLS_LABEL_INVALID;

        getReplyForWrite(vppApi.ipAddDelRoute(request).toCompletableFuture(), id);
    }

    private void translate(@Nonnull final InSegment inSegment, @Nonnull final IpAddDelRoute request) {
        checkArgument(inSegment.getConfig() != null,
                "Configuring impose-and-forward, but in-segment config is missing.");
        final Type type = inSegment.getConfig().getType();
        checkArgument(type instanceof IpPrefix, "Only ip-prefix type is supported, but %s given.", type);

        // TODO(HC2VPP-264): add support for mpls + v6
        final Ipv4Prefix prefix = ((IpPrefix) type).getIpPrefix().getIpv4Prefix();
        request.dstAddressLength = extractPrefix(prefix);
        request.dstAddress = ipv4AddressPrefixToArray(prefix);
    }

    private void translate(@Nonnull final OutSegment outSegment, @Nonnull final IpAddDelRoute request,
                           @Nonnull final MappingContext ctx) {
        checkArgument(outSegment != null, "Configuring impose-and-forward, but out-segment is missing.");

        final String outgoingInterface;
        if (outSegment instanceof SimplePath) {
            outgoingInterface = translate((SimplePath) outSegment, request);
        } else if (outSegment instanceof MultiplePaths) {
            outgoingInterface = translate((MultiplePaths) outSegment, request);
        } else {
            throw new IllegalArgumentException("Unsupported out-segment type: " + outSegment);
        }

        checkArgument(outgoingInterface != null, "Configuring impose-and-forward, but outgoing-interface is missing.");
        request.nextHopSwIfIndex = interfaceContext.getIndex(outgoingInterface, ctx);
    }

    private String translate(@Nonnull final SimplePath path, @Nonnull final IpAddDelRoute request) {
        final IpAddress nextHop = path.getSimplePath().getConfig().getNextHop();

        // TODO(HC2VPP-264): add support for mpls + v6
        if (nextHop != null) {
            final Ipv4Address address = nextHop.getIpv4Address();
            checkArgument(address != null, "Only IPv4 next-hop address is supported.");
            request.nextHopAddress = ipv4AddressNoZoneToArray(address.getValue());
        } else {
            request.nextHopAddress = new byte[0];
        }

        final MplsLabel outgoingLabel = path.getSimplePath().getConfig().getOutgoingLabel();
        checkArgument(outgoingLabel != null, "Configuring impose-and-forward, but outgoing-label is missing.");
        request.nextHopOutLabelStack = new FibMplsLabel[] {translate(outgoingLabel.getValue())};
        request.nextHopNOutLabels = 1;

        return path.getSimplePath().getConfig().getOutgoingInterface();
    }

    private String translate(@Nonnull final MultiplePaths pathList, @Nonnull final IpAddDelRoute request) {
        checkArgument(pathList.getPaths() != null && pathList.getPaths().getPath().size() == 1,
                      "Only single path is supported");
        final Paths paths = pathList.getPaths();
        final IpAddress nextHop = paths.getPath().get(0).getConfig().getNextHop();

        // TODO(HC2VPP-264): add support for mpls + v6
        if (nextHop != null) {
            final Ipv4Address address = nextHop.getIpv4Address();
            checkArgument(address != null, "Only IPv4 next-hop address is supported.");
            request.nextHopAddress = ipv4AddressNoZoneToArray(address.getValue());
        } else {
            request.nextHopAddress = new byte[0];
        }

        final List<OutgoingLabels> labels = paths.getOutgoingLabels().getOutgoingLabels();
        final int numberOfLabels = labels.size();
        checkArgument(numberOfLabels > 0 && numberOfLabels < MAX_LABELS, "Number of labels (%s) not in range (0, %s].",
            numberOfLabels, MAX_LABELS, numberOfLabels);
        request.nextHopNOutLabels = (byte) numberOfLabels;
        request.nextHopOutLabelStack = labels.stream().map(label -> translate(label.getConfig().getLabel().getValue()))
                .toArray(FibMplsLabel[]::new);

        return paths.getPath().get(0).getConfig().getOutgoingInterface();
    }
}
