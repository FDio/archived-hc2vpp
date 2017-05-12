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

package io.fd.hc2vpp.v3po.interfaces.span;

import static io.fd.honeycomb.translate.util.RWUtils.cutId;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSpanEnableDisable;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.SpanState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.span.attributes.MirroredInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.span.attributes.mirrored.interfaces.MirroredInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.span.attributes.mirrored.interfaces.MirroredInterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MirroredInterfaceCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<MirroredInterface, MirroredInterfaceKey>, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MirroredInterfaceCustomizer.class);

    private final NamingContext ifcContext;
    private final Function<InstanceIdentifier<MirroredInterfaces>, String> destinationInterfaceNameExtractor;

    public MirroredInterfaceCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                       @Nonnull final NamingContext ifcContext,
                                       @Nonnull final Function<InstanceIdentifier<MirroredInterfaces>, String> destinationInterfaceNameExtractor) {
        super(futureJVppCore);
        this.ifcContext = ifcContext;
        this.destinationInterfaceNameExtractor = destinationInterfaceNameExtractor;
    }


    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<MirroredInterface> id,
                                       @Nonnull final MirroredInterface mirroredInterface,
                                       @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String destinationInterfaceName =
                destinationInterfaceNameExtractor.apply(cutId(id, MirroredInterfaces.class));
        final String sourceInterfaceName = mirroredInterface.getIfaceRef();
        final SpanState spanState = mirroredInterface.getState();

        LOG.debug("Enabling span for source interface {} | destination interface {} | state {}", sourceInterfaceName,
                destinationInterfaceName, spanState);

        getReplyForWrite(getFutureJVpp().swInterfaceSpanEnableDisable(
                getSpanAddDelRequest(
                        interfaceId(writeContext, ifcContext, destinationInterfaceName),
                        interfaceId(writeContext, ifcContext, sourceInterfaceName),
                        true,
                        spanState))
                .toCompletableFuture(), id);
        LOG.debug("Span for source interface {} | destination interface {} | state {} successfully enabled",
                sourceInterfaceName, destinationInterfaceName, spanState);
    }


    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<MirroredInterface> instanceIdentifier,
                                        @Nonnull final MirroredInterface mirroredInterfaceBefore,
                                        @Nonnull final MirroredInterface mirroredInterfaceAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        deleteCurrentAttributes(instanceIdentifier, mirroredInterfaceBefore, writeContext);
        writeCurrentAttributes(instanceIdentifier, mirroredInterfaceAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<MirroredInterface> id,
                                        @Nonnull final MirroredInterface mirroredInterface,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String destinationInterfaceName =
                destinationInterfaceNameExtractor.apply(cutId(id, MirroredInterfaces.class));
        final String sourceInterfaceName = mirroredInterface.getIfaceRef();
        LOG.debug("Disabling span for source interface {} | destination interface {} ", sourceInterfaceName,
                destinationInterfaceName);

        getReplyForWrite(getFutureJVpp().swInterfaceSpanEnableDisable(
                getSpanAddDelRequest(
                        interfaceId(writeContext, ifcContext, destinationInterfaceName),
                        interfaceId(writeContext, ifcContext, sourceInterfaceName),
                        false,
                        null))
                .toCompletableFuture(), id);
        LOG.debug("Span for source interface {} | destination interface {} successfully disabled",
                sourceInterfaceName, destinationInterfaceName);
    }

    private SwInterfaceSpanEnableDisable getSpanAddDelRequest(final int dstId, final Integer srcId, final boolean isAdd,
                                                              @Nullable final SpanState state) {
        final SwInterfaceSpanEnableDisable spanAddDel = new SwInterfaceSpanEnableDisable();
        spanAddDel.state = (byte) (isAdd
                ? state != null
                ? state.getIntValue()
                : 0
                : 0);// either one of 1(rx),2(tx),3(both) or 0 for disable/delete
        spanAddDel.swIfIndexFrom = srcId;
        spanAddDel.swIfIndexTo = dstId;
        return spanAddDel;
    }

    private static int interfaceId(final WriteContext writeContext, final NamingContext ifcContext, final String name) {
        return ifcContext.getIndex(name, writeContext.getMappingContext());
    }
}
