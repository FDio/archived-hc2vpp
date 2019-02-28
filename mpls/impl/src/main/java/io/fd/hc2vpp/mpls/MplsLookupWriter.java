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
import io.fd.jvpp.core.dto.MplsRouteAddDel;
import io.fd.jvpp.core.future.FutureJVppCore;
import io.fd.jvpp.core.types.FibMplsLabel;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.mpls.rev171120.LookupType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.mpls.rev171120.StaticLspVppLookupAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.MplsOperationsType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.top.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls._static.lsps.StaticLsp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Translates {@link MplsOperationsType#PopAndLookup} operation with {@link LookupType#Mpls} to
 * mpls_route_add_del API.
 *
 * @see <a href="https://git.fd.io/vpp/tree/src/vnet/mpls/mpls.api">mpls_route_add_del</a> definition
 */
final class MplsLookupWriter implements LspWriter, MplsInSegmentTranslator {
    private static final byte MPLS_PROTOCOL = (byte) LookupType.Mpls.getIntValue();

    private final FutureJVppCore vppApi;

    MplsLookupWriter(@Nonnull final FutureJVppCore vppApi) {
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
        request.mrNextHopProto = MPLS_PROTOCOL;
        request.mrNextHopWeight = 1;
        request.mrNextHop = new byte[0]; // no next hop since we POP
        request.mrNextHopOutLabelStack = new FibMplsLabel[0]; // no new labels
        request.mrNextHopSwIfIndex = -1;
        request.mrNextHopViaLabel = MPLS_LABEL_INVALID;

        getReplyForWrite(vppApi.mplsRouteAddDel(request).toCompletableFuture(), id);
    }

    private void translate(@Nonnull final StaticLspVppLookupAugmentation vppLabelLookup,
                           @Nonnull final MplsRouteAddDel request) {
        // MPLS lookup for the last label is not valid operation (there is no next label to lookup),
        // so match only labels without EOS bit set:
        request.mrEos = 0;
        final Long lookupTable = vppLabelLookup.getLabelLookup().getMplsLookupInTable();
        checkArgument(lookupTable != null, "Configuring pop and mpls lookup, but MPLS lookup table was not given");
        request.mrNextHopTableId = lookupTable.intValue();
    }
}
