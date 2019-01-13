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

import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.MplsRouteAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.types.FibMplsLabel;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.mpls.rev171120.LookupType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.mpls.rev171120.StaticLspVppLookupAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.top.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls._static.lsps.StaticLsp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Translates {@link StaticLspConfig.Operation#PopAndLookup} operation with {@link LookupType#Ipv4} to
 * mpls_route_add_del API.
 *
 * @see <a href="https://git.fd.io/vpp/tree/src/vnet/mpls/mpls.api">mpls_route_add_del</a> definition
 */
final class Ipv4LookupWriter implements LspWriter, MplsInSegmentTranslator {
    private static final byte IPV4_PROTOCOL = (byte) LookupType.Ipv4.getIntValue();

    private final FutureJVppCore vppApi;

    Ipv4LookupWriter(@Nonnull final FutureJVppCore vppApi) {
        this.vppApi = vppApi;
    }

    @Override
    public void write(@Nonnull final InstanceIdentifier<StaticLsp> id, @Nonnull final StaticLsp data,
                      @Nonnull final MappingContext ctx, final boolean isAdd) throws WriteFailedException {
        final Config config = data.getConfig();
        final MplsRouteAddDel request = new MplsRouteAddDel();

        request.mrIsAdd = booleanToByte(isAdd);

        translate(config.getInSegment(), request);
        translate(config.augmentation(StaticLspVppLookupAugmentation.class), request);

        // default values based on inspecting VPP's CLI and make test code
        request.mrClassifyTableIndex = -1;
        request.mrNextHopProto = IPV4_PROTOCOL;
        request.mrNextHopWeight = 1;
        request.mrNextHop = new byte[0]; // no next hop since we POP
        request.mrNextHopOutLabelStack = new FibMplsLabel[0]; // no new labels
        request.mrNextHopSwIfIndex = -1;
        request.mrNextHopViaLabel = MPLS_LABEL_INVALID;

        getReplyForWrite(vppApi.mplsRouteAddDel(request).toCompletableFuture(), id);
    }

    private void translate(@Nonnull final StaticLspVppLookupAugmentation vppLabelLookup,
                           @Nonnull final MplsRouteAddDel request) {
        // IPv4 lookup should only happen if there is no more labels to process, so setting the EOS bit:
        request.mrEos = 1;
        final Long lookupTable = vppLabelLookup.getLabelLookup().getIp4LookupInTable();
        checkArgument(lookupTable != null, "Configuring pop and ipv4 lookup, but ipv4 lookup table was not given");
        request.mrNextHopTableId = lookupTable.intValue();
    }
}
