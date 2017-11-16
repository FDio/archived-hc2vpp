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
import static com.google.common.base.Preconditions.checkNotNull;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.StaticLspConfig.Operation.ImposeAndForward;

import com.google.common.annotations.VisibleForTesting;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.StaticLspConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.InSegment;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.OutSegment;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.in.segment.type.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.out.segment.PathList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.out.segment.SimplePath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.out.segment.path.list.Paths;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.routing.mpls._static.lsps.StaticLsp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.routing.mpls._static.lsps.StaticLspKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev170227.MplsLabel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StaticLspCustomizer extends FutureJVppCustomizer implements
    ListWriterCustomizer<StaticLsp, StaticLspKey>, JvppReplyConsumer, ByteDataTranslator, Ipv4Translator {
    private static final Logger LOG = LoggerFactory.getLogger(StaticLspCustomizer.class);

    /**
     * Maximum number of MPLS labels supported by VPP. Value is based on type of next_hop_n_out_labels value which is
     * u8.
     */
    private static final int MAX_LABELS = 255;

    /**
     * Constant used by VPP to disable optional parameters of mpls label type.
     */
    @VisibleForTesting
    static final int MPLS_LABEL_INVALID = 0x100000;

    private final NamingContext interfaceContext;

    StaticLspCustomizer(@Nonnull final FutureJVppCore vppApi, @Nonnull NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<StaticLsp> id,
                                       @Nonnull final StaticLsp dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Adding MPLS LSP: {}", dataAfter);
        imposeAndForward(id, validateConfig(dataAfter), writeContext, true);
        LOG.debug("MPLS LSP successfully configured: {}", dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<StaticLsp> id,
                                        @Nonnull final StaticLsp dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Removing MPLS LSP: {}", dataBefore);
        imposeAndForward(id, validateConfig(dataBefore), writeContext, false);
        LOG.debug("MPLS LSP successfully removed: {}", dataBefore);
    }

    private static Config validateConfig(@Nonnull final StaticLsp data) {
        final Config config = data.getConfig();
        checkArgument(config != null, "Config node of static-lsp is missing.");
        final StaticLspConfig.Operation operation = config.getOperation();
        checkArgument(ImposeAndForward.equals(operation),
            "Only impose-and-forward operation is supported, but %s given.", operation);
        return config;
    }

    private void imposeAndForward(@Nonnull final InstanceIdentifier<StaticLsp> id, @Nonnull final Config lspConfig,
                                  @Nonnull final WriteContext writeContext, final boolean isAdd)
        throws WriteFailedException {
        final IpAddDelRoute request = new IpAddDelRoute();
        request.isAdd = booleanToByte(isAdd);
        request.nextHopWeight = 1; // default value used in make test

        final InSegment inSegment = lspConfig.getInSegment();
        checkArgument(inSegment != null, "Configuring impose-and-forward, but in-segment is missing.");

        checkArgument(inSegment.getType() instanceof IpPrefix, "Only ip-prefix type is supported, but %s given.",
            inSegment.getType());
        final Ipv4Prefix prefix = ((IpPrefix) inSegment.getType()).getIpPrefix().getIpv4Prefix();

        // TODO(HC2VPP-264): add support for mpls + v6
        request.dstAddressLength = extractPrefix(prefix);
        request.dstAddress = ipv4AddressPrefixToArray(prefix);

        final OutSegment outSegment = lspConfig.getOutSegment();
        checkArgument(outSegment != null, "Configuring impose-and-forward, but out-segment is missing.");
        translateOutSegment(outSegment, request, writeContext.getMappingContext());
        getReplyForWrite(getFutureJVpp().ipAddDelRoute(request).toCompletableFuture(), id);
    }

    private void translateOutSegment(@Nonnull final OutSegment outSegment, @Nonnull final IpAddDelRoute request,
                                     @Nonnull final MappingContext mappingContext) {
        String outgoingInterface = null;
        if (outSegment instanceof SimplePath) {
            final SimplePath path = (SimplePath) outSegment;
            outgoingInterface = path.getOutgoingInterface();
            final IpAddress nextHop = path.getNextHop();
            checkArgument(nextHop != null, "Configuring impose-and-forward, but next-hop is missing.");
            // TODO(HC2VPP-264): add support for mpls + v6
            checkArgument(nextHop.getIpv4Address() != null, "Only IPv4 next-hop address is supported.");
            request.nextHopAddress = ipv4AddressNoZoneToArray(nextHop.getIpv4Address().getValue());
            request.nextHopNOutLabels = 1;
            checkArgument(path.getOutgoingLabel() != null,
                "Configuring impose-and-forward, but outgoing-label is missing.");
            request.nextHopOutLabelStack = new int[] {path.getOutgoingLabel().getValue().intValue()};
        } else if (outSegment instanceof PathList) {
            final PathList pathList = (PathList) outSegment;
            checkArgument(pathList.getPaths() != null && pathList.getPaths().size() == 1,
                "Only single path is supported");
            final Paths paths = pathList.getPaths().get(0);
            outgoingInterface = paths.getOutgoingInterface();
            // TODO(HC2VPP-264): add support for mpls + v6
            final IpAddress nextHop = paths.getNextHop();
            checkArgument(nextHop != null, "Configuring impose-and-forward, but next-hop is missing.");
            checkArgument(nextHop.getIpv4Address() != null, "Only IPv4 next-hop address is supported.");
            request.nextHopAddress = ipv4AddressNoZoneToArray(nextHop.getIpv4Address().getValue());
            final List<MplsLabel> outgoingLabels = paths.getOutgoingLabels();
            final int numberOfLabels = outgoingLabels.size();
            checkArgument(numberOfLabels > 0 && numberOfLabels < MAX_LABELS,
                "Number of labels (%s) not in range (0, %s].", numberOfLabels, MAX_LABELS, numberOfLabels);
            request.nextHopNOutLabels = (byte) numberOfLabels;
            request.nextHopOutLabelStack =
                outgoingLabels.stream().mapToInt(label -> label.getValue().intValue()).toArray();
        } else {
            throw new IllegalArgumentException("Unsupported out-segment type: " + outSegment);
        }

        checkArgument(outgoingInterface != null, "Configuring impose-and-forward, but outgoing-interface is missing.");
        request.nextHopSwIfIndex = interfaceContext.getIndex(outgoingInterface, mappingContext);
        request.nextHopViaLabel = MPLS_LABEL_INVALID;
    }
}
