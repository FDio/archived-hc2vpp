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

package io.fd.honeycomb.vppnsh.impl.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vppnsh.impl.util.FutureJVppNshCustomizer;
import io.fd.vpp.jvpp.nsh.dto.NshAddDelMap;
import io.fd.vpp.jvpp.nsh.dto.NshAddDelMapReply;
import io.fd.vpp.jvpp.nsh.future.FutureJVppNsh;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMapKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer customizer responsible for NshMap create/delete.
 */
public class NshMapWriterCustomizer extends FutureJVppNshCustomizer
        implements ListWriterCustomizer<NshMap, NshMapKey>, ByteDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(NshMapWriterCustomizer.class);
    private final NamingContext nshMapContext;
    private final NamingContext interfaceContext;

    public NshMapWriterCustomizer(@Nonnull final FutureJVppNsh futureJVppNsh,
                                  @Nonnull final NamingContext nshMapContext,
                                  @Nonnull final NamingContext interfaceContext) {
        super(futureJVppNsh);
        this.nshMapContext = checkNotNull(nshMapContext, "nshMapContext should not be null");
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<NshMap> id,
                                       @Nonnull final NshMap dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        LOG.debug("Creating nsh map: iid={} dataAfter={}", id, dataAfter);
        final int newMapIndex =
                nshAddDelMap(true, id, dataAfter, ~0 /* value not present */, writeContext.getMappingContext());

        // Add nsh map name <-> vpp index mapping to the naming context:
        nshMapContext.addName(newMapIndex, dataAfter.getName(), writeContext.getMappingContext());
        LOG.debug("Successfully created nsh map(id={]): iid={} dataAfter={}", newMapIndex, id, dataAfter);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<NshMap> id,
                                        @Nonnull final NshMap dataBefore, @Nonnull final NshMap dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new UnsupportedOperationException("Nsh map update is not supported");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<NshMap> id,
                                        @Nonnull final NshMap dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Removing nsh map: iid={} dataBefore={}", id, dataBefore);
        final String mapName = dataBefore.getName();
        checkState(nshMapContext.containsIndex(mapName, writeContext.getMappingContext()),
                "Removing nsh map {}, but index could not be found in the nsh map context", mapName);

        final int mapIndex = nshMapContext.getIndex(mapName, writeContext.getMappingContext());
        nshAddDelMap(false, id, dataBefore, mapIndex, writeContext.getMappingContext());

        // Remove deleted interface from interface context:
        nshMapContext.removeName(dataBefore.getName(), writeContext.getMappingContext());
        LOG.debug("Successfully removed nsh map(id={]): iid={} dataAfter={}", mapIndex, id, dataBefore);
    }

    private int nshAddDelMap(final boolean isAdd, @Nonnull final InstanceIdentifier<NshMap> id,
                             @Nonnull final NshMap map, final int mapId, final MappingContext ctx)
            throws WriteFailedException {
        final CompletionStage<NshAddDelMapReply> createNshMapReplyCompletionStage =
                getFutureJVppNsh().nshAddDelMap(getNshAddDelMapRequest(isAdd, mapId, map, ctx));

        final NshAddDelMapReply reply = getReplyForWrite(createNshMapReplyCompletionStage.toCompletableFuture(), id);
        return reply.mapIndex;

    }

    private NshAddDelMap getNshAddDelMapRequest(final boolean isAdd, final int mapIndex,
                                                @Nonnull final NshMap map,
                                                @Nonnull final MappingContext ctx) {
        final NshAddDelMap request = new NshAddDelMap();
        request.isAdd = booleanToByte(isAdd);

        request.nspNsi = (map.getNsp().intValue() << 8) | map.getNsi();
        request.mappedNspNsi = (map.getMappedNsp().intValue() << 8) | map.getMappedNsi();

        if (map.getEncapType() == VxlanGpe.class) {
            request.nextNode = 2;
        }

        checkState(interfaceContext.containsIndex(map.getEncapIfName(), ctx),
                "Mapping does not contains mapping for provider interface Name ".concat(map.getEncapIfName()));
        request.swIfIndex = interfaceContext.getIndex(map.getEncapIfName(), ctx);

        return request;
    }
}
