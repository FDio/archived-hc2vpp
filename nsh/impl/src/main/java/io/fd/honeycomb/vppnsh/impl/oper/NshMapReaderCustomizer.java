/*
 * Copyright (c) 2016 Intel and/or its affiliates.
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

package io.fd.honeycomb.vppnsh.impl.oper;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.vppnsh.impl.util.FutureJVppNshCustomizer;
import io.fd.vpp.jvpp.nsh.dto.NshMapDetails;
import io.fd.vpp.jvpp.nsh.dto.NshMapDetailsReplyDump;
import io.fd.vpp.jvpp.nsh.dto.NshMapDump;
import io.fd.vpp.jvpp.nsh.future.FutureJVppNsh;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Pop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Push;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Swap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNsh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.NshMapsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.maps.NshMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.maps.NshMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.maps.NshMapKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reader customizer responsible for nsh map read.<br> to VPP.
 */
public class NshMapReaderCustomizer extends FutureJVppNshCustomizer
        implements InitializingListReaderCustomizer<NshMap, NshMapKey, NshMapBuilder>, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(NshMapReaderCustomizer.class);
    private final NamingContext nshMapContext;
    private final NamingContext interfaceContext;

    public NshMapReaderCustomizer(@Nonnull final FutureJVppNsh futureJVppNsh,
                                  @Nonnull final NamingContext nshMapContext,
                                  @Nonnull final NamingContext interfaceContext) {
        super(futureJVppNsh);
        this.nshMapContext = checkNotNull(nshMapContext, "nshMapContext should not be null");
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
    }


    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder,
                      @Nonnull final List<NshMap> readData) {
        ((NshMapsBuilder) builder).setNshMap(readData);
    }

    @Nonnull
    @Override
    public NshMapBuilder getBuilder(@Nonnull final InstanceIdentifier<NshMap> id) {
        return new NshMapBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<NshMap> id,
                                      @Nonnull final NshMapBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        LOG.debug("Reading attributes for nsh map: {}", id);
        final NshMapKey key = id.firstKeyOf(NshMap.class);
        checkArgument(key != null, "could not find NshMap key in {}", id);
        final NshMapDump request = new NshMapDump();

        final String mapName = key.getName();
        if (!nshMapContext.containsIndex(mapName, ctx.getMappingContext())) {
            LOG.debug("Could not find nsh map {} in the naming context", mapName);
            return;
        }
        request.mapIndex = nshMapContext.getIndex(mapName, ctx.getMappingContext());

        final CompletionStage<NshMapDetailsReplyDump> nshMapDetailsReplyDumpCompletionStage =
                getFutureJVppNsh().nshMapDump(request);
        final NshMapDetailsReplyDump reply =
                getReplyForRead(nshMapDetailsReplyDumpCompletionStage.toCompletableFuture(), id);

        if (reply == null || reply.nshMapDetails == null || reply.nshMapDetails.isEmpty()) {
            LOG.debug("Has no Nsh Map {} in VPP. ", key.getName());
            return;
        }

        LOG.trace("Nsh Map : {} attributes returned from VPP: {}", key.getName(), reply);

        final NshMapDetails nshMapDetails = reply.nshMapDetails.get(0);
        builder.setName(mapName);
        builder.setKey(key);

        builder.setNsp((long) ((nshMapDetails.nspNsi >> 8) & 0xFFFFFF));
        builder.setNsi((short) (nshMapDetails.nspNsi & 0xFF));

        builder.setMappedNsp((long) ((nshMapDetails.mappedNspNsi >> 8) & 0xFFFFFF));
        builder.setMappedNsi((short) (nshMapDetails.mappedNspNsi & 0xFF));

        switch (nshMapDetails.nshAction) {
        case 0:
            builder.setNshAction(Swap.class);
            break;
        case 1:
            builder.setNshAction(Push.class);
            break;
        case 2:
            builder.setNshAction(Pop.class);
            break;
        default:
            LOG.trace("Unsupported nsh_action for nsh map: {}", nshMapDetails.nshAction);
            return;
    }

        switch (nshMapDetails.nextNode) {
            case 2:
                builder.setEncapType(VxlanGpe.class);
                break;
            default:
                LOG.trace("Unsupported encap type for nsh map: {}", nshMapDetails.nextNode);
                return;
        }

        checkState(interfaceContext.containsName(nshMapDetails.swIfIndex, ctx.getMappingContext()),
                "Mapping does not contains mapping for provider interface Index ");
        final String interfaceName = interfaceContext.getName(nshMapDetails.swIfIndex, ctx.getMappingContext());
        builder.setEncapIfName(interfaceName);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Attributes for nsh map {} successfully read: {}", id, builder.build());
        }
    }

    @Nonnull
    @Override
    public List<NshMapKey> getAllIds(@Nonnull final InstanceIdentifier<NshMap> id,
                                     @Nonnull final ReadContext context) throws ReadFailedException {
        LOG.debug("Reading list of keys for nsh map: {}", id);

        final NshMapDump request = new NshMapDump();
        request.mapIndex = -1; // dump call

        NshMapDetailsReplyDump reply;
        try {
            reply = getFutureJVppNsh().nshMapDump(request).toCompletableFuture().get();
        } catch (Exception e) {
            throw new IllegalStateException("Nsh Map dump failed", e);
        }

        if (reply == null || reply.nshMapDetails == null) {
            return Collections.emptyList();
        }

        final int nIdsLength = reply.nshMapDetails.size();
        LOG.debug("vppstate.NshMapCustomizer.getAllIds: nIds.length={}", nIdsLength);
        if (nIdsLength == 0) {
            return Collections.emptyList();
        }

        final List<NshMapKey> allIds = new ArrayList<>(nIdsLength);
        for (NshMapDetails detail : reply.nshMapDetails) {
            final String nshName = nshMapContext.getName(detail.mapIndex, context.getMappingContext());
            LOG.debug("vppstate.NshMapCustomizer.getAllIds: nName={}", nshName);
            allIds.add(new NshMapKey(nshName));
        }

        return allIds;
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMap> init(
            @Nonnull final InstanceIdentifier<NshMap> id,
            @Nonnull final NshMap readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(
                InstanceIdentifier.create(VppNsh.class).child(
                        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.NshMaps.class).child(
                        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMap.class,
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMapKey(id.firstKeyOf(NshMap.class).getName())),
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMapBuilder(readValue).setName(readValue.getName()).build());
    }
}
