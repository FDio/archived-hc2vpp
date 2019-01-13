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
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.MplsOperationsType.ImposeAndForward;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.MplsOperationsType.PopAndLookup;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.MplsOperationsType.SwapAndForward;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.mpls.rev171120.LookupType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.mpls.rev171120.StaticLspVppLookupAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.mpls.rev171120.VppLabelLookupAttributes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.MplsOperationsType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.top.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls._static.lsps.StaticLsp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls._static.lsps.StaticLspKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selects specific {@link LspWriter} based on {@link MplsOperationsType} and {@link LookupType}.
 */
final class StaticLspCustomizer implements ListWriterCustomizer<StaticLsp, StaticLspKey>, LspWriter {
    private static final Logger LOG = LoggerFactory.getLogger(StaticLspCustomizer.class);
    private final ImposeAndForwardWriter imposeAndForward;
    private final MplsLookupWriter mplsLookup;
    private final Ipv4LookupWriter ipv4Lookup;
    private final MplsSwapWriter mplsSwap;

    StaticLspCustomizer(@Nonnull final FutureJVppCore vppApi, @Nonnull NamingContext interfaceContext) {
        checkNotNull(vppApi, "vppApi should not be null");
        checkNotNull(interfaceContext, "interfaceContext should not be null");
        this.imposeAndForward = new ImposeAndForwardWriter(vppApi, interfaceContext);
        this.mplsLookup = new MplsLookupWriter(vppApi);
        this.ipv4Lookup = new Ipv4LookupWriter(vppApi);
        this.mplsSwap = new MplsSwapWriter(vppApi, interfaceContext);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<StaticLsp> id,
                                       @Nonnull final StaticLsp dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Adding MPLS LSP: {}", dataAfter);
        write(id, dataAfter, writeContext.getMappingContext(), true);
        LOG.debug("MPLS LSP successfully configured: {}", dataAfter);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<StaticLsp> id,
                                        @Nonnull final StaticLsp dataBefore,
                                        @Nonnull final StaticLsp dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Updating MPLS LSP: before={} after={}", dataBefore, dataAfter);
        write(id, dataBefore, writeContext.getMappingContext(), false);
        write(id, dataAfter, writeContext.getMappingContext(), true);
        LOG.debug("MPLS LSP successfully configured: {}", dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<StaticLsp> id,
                                        @Nonnull final StaticLsp dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Removing MPLS LSP: {}", dataBefore);
        write(id, dataBefore, writeContext.getMappingContext(), false);
        LOG.debug("MPLS LSP successfully removed: {}", dataBefore);
    }

    @Override
    public void write(@Nonnull final InstanceIdentifier<StaticLsp> id, @Nonnull final StaticLsp data,
                      @Nonnull final MappingContext ctx, final boolean isAdd) throws WriteFailedException {
        final Config config = data.getConfig();
        checkArgument(config != null, "Config node of static-lsp is missing.");
        final MplsOperationsType operation = config.getOperation();
        if (ImposeAndForward.equals(operation)) {
            imposeAndForward.write(id, data, ctx, isAdd);
        } else if (PopAndLookup.equals(operation)) {
            final VppLabelLookupAttributes vppAttributes = config.augmentation(StaticLspVppLookupAugmentation.class);
            checkArgument(vppAttributes != null && vppAttributes.getLabelLookup() != null,
                "Configuring pop-and-lookup operation but label-lookup leaf is missing");
            final LookupType type = vppAttributes.getLabelLookup().getType();
            if (LookupType.Ipv4.equals(type)) {
                ipv4Lookup.write(id, data, ctx, isAdd);
            } else if (LookupType.Mpls.equals(type)) {
                mplsLookup.write(id, data, ctx, isAdd);
            } else {
                throw new IllegalArgumentException("Unsupported lookup type: " + type);
            }
        } else if (SwapAndForward.equals(operation)) {
            mplsSwap.write(id, data, ctx, isAdd);
        } else {
            throw new IllegalArgumentException("Unsupported operation: " + operation);
        }
    }
}
