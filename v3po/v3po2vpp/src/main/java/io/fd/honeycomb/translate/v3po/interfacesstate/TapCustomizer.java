/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.honeycomb.translate.v3po.interfacesstate;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.TapBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.SwInterfaceTapDetails;
import org.openvpp.jvpp.core.dto.SwInterfaceTapDetailsReplyDump;
import org.openvpp.jvpp.core.dto.SwInterfaceTapDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TapCustomizer extends FutureJVppCustomizer
        implements ReaderCustomizer<Tap, TapBuilder>, InterfaceDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TapCustomizer.class);
    public static final String DUMPED_TAPS_CONTEXT_KEY = TapCustomizer.class.getName() + "dumpedTapsDuringGetAllIds";
    private NamingContext interfaceContext;

    public TapCustomizer(@Nonnull final FutureJVppCore jvpp, @Nonnull final NamingContext interfaceContext) {
        super(jvpp);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder, @Nonnull Tap readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setTap(readValue);
    }

    @Nonnull
    @Override
    public TapBuilder getBuilder(@Nonnull InstanceIdentifier<Tap> id) {
        return new TapBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Tap> id,
                                      @Nonnull final TapBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        try {
            final InterfaceKey key = id.firstKeyOf(Interface.class);
            final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
            if (!isInterfaceOfType(getFutureJVpp(), ctx.getModificationCache(), id, index,
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Tap.class, LOG)) {
                return;
            }

            LOG.debug("Reading attributes for tap interface: {}", key.getName());

            @SuppressWarnings("unchecked")
            Map<Integer, SwInterfaceTapDetails> mappedTaps =
                    (Map<Integer, SwInterfaceTapDetails>) ctx.getModificationCache().get(DUMPED_TAPS_CONTEXT_KEY);

            if (mappedTaps == null) {
                // Full Tap dump has to be performed here, no filter or anything is here to help so at least we cache it
                final SwInterfaceTapDump request = new SwInterfaceTapDump();
                final CompletionStage<SwInterfaceTapDetailsReplyDump> swInterfaceTapDetailsReplyDumpCompletionStage =
                        getFutureJVpp().swInterfaceTapDump(request);
                final SwInterfaceTapDetailsReplyDump reply =
                        getReplyForRead(swInterfaceTapDetailsReplyDumpCompletionStage.toCompletableFuture(), id);

                if (null == reply || null == reply.swInterfaceTapDetails) {
                    mappedTaps = Collections.emptyMap();
                } else {
                    final List<SwInterfaceTapDetails> swInterfaceTapDetails = reply.swInterfaceTapDetails;
                    // Cache interfaces dump in per-tx context to later be used in readCurrentAttributes
                    mappedTaps = swInterfaceTapDetails.stream()
                            .collect(Collectors.toMap(t -> t.swIfIndex, swInterfaceDetails -> swInterfaceDetails));
                }

                ctx.getModificationCache().put(DUMPED_TAPS_CONTEXT_KEY, mappedTaps);
            }

            final SwInterfaceTapDetails swInterfaceTapDetails = mappedTaps.get(index);
            LOG.trace("Tap interface: {} attributes returned from VPP: {}", key.getName(), swInterfaceTapDetails);

            builder.setTapName(toString(swInterfaceTapDetails.devName));
            LOG.debug("Tap interface: {}, id: {} attributes read as: {}", key.getName(), index, builder);
        } catch (VppBaseCallException e) {
            LOG.warn("Failed to readCurrentAttributes for: {}", id, e);
            throw new ReadFailedException(id, e);
        }
    }
}
