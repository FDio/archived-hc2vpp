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

package io.fd.hc2vpp.v3po.interfaces.span;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSpanEnableDisable;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSpanEnableDisableReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.span.attributes.MirroredInterfaces;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MirroredInterfacesCustomizer
        extends FutureJVppCustomizer
        implements WriterCustomizer<MirroredInterfaces>, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MirroredInterfacesCustomizer.class);

    private final NamingContext ifcContext;

    public MirroredInterfacesCustomizer(@Nonnull final FutureJVppCore futureJVppCore, final NamingContext ifcContext) {
        super(futureJVppCore);
        this.ifcContext = ifcContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<MirroredInterfaces> id,
                                       @Nonnull final MirroredInterfaces dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.trace("Writing mirrored interfaces for: {} as: {}", id, dataAfter);

        final int dstId =
                ifcContext.getIndex(id.firstKeyOf(Interface.class).getName(), writeContext.getMappingContext());

        // Collect source IDs
        final List<Integer> srcIds = dataAfter.getMirroredInterface().stream()
                .map(ifcName -> ifcContext.getIndex(ifcName, writeContext.getMappingContext()))
                // Collecting before executing to verify we have all the IDs first, failing fast...
                .collect(Collectors.toList());

        Preconditions.checkArgument(!srcIds.contains(dstId),
                "Source and Destination interface for port mirroring detected: %s at %s", dstId, id);

        // Invoke addition for each source ID
        final List<CompletableFuture<SwInterfaceSpanEnableDisableReply>> futures = srcIds.stream()
                .map(srcId -> getSpanAddDelRequest(dstId, srcId, true))
                .map(request -> getFutureJVpp().swInterfaceSpanEnableDisable(request).toCompletableFuture())
                .collect(Collectors.toList());

        // Wait for success/exception
        for (final CompletableFuture<SwInterfaceSpanEnableDisableReply> future : futures) {
            getReplyForWrite(future, id);
        }

        LOG.trace("Mirrored interfaces for: {} written successfully", id);
    }

    private SwInterfaceSpanEnableDisable getSpanAddDelRequest(final int dstId, final Integer srcId, final boolean isAdd) {
        final SwInterfaceSpanEnableDisable spanAddDel = new SwInterfaceSpanEnableDisable();
        spanAddDel.state = (byte) (isAdd ? 3 : 0); // 3 - enable rx & tx : 0 - disabled
        spanAddDel.swIfIndexFrom = srcId;
        spanAddDel.swIfIndexTo = dstId;
        return spanAddDel;
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<MirroredInterfaces> id,
                                        @Nonnull final MirroredInterfaces dataBefore,
                                        @Nonnull final MirroredInterfaces dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        deleteCurrentAttributes(id, dataBefore, writeContext);
        writeCurrentAttributes(id, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<MirroredInterfaces> id,
                                        @Nonnull final MirroredInterfaces dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.trace("Removing mirrored interfaces for: {} as: {}", id, dataBefore);

        final int dstId =
                ifcContext.getIndex(id.firstKeyOf(Interface.class).getName(), writeContext.getMappingContext());

        final List<Integer> srcIds = dataBefore.getMirroredInterface().stream()
                .map(ifcName -> ifcContext.getIndex(ifcName, writeContext.getMappingContext()))
                // Collecting before executing to verify we have all the IDs first, failing fast...
                .collect(Collectors.toList());

        final List<CompletableFuture<SwInterfaceSpanEnableDisableReply>> futures = srcIds.stream()
                .map(srcId -> getSpanAddDelRequest(dstId, srcId, false))
                .map(request -> getFutureJVpp().swInterfaceSpanEnableDisable(request).toCompletableFuture())
                .collect(Collectors.toList());

        for (final CompletableFuture<SwInterfaceSpanEnableDisableReply> future : futures) {
            getReplyForWrite(future, id);
        }

        LOG.trace("Mirrored interfaces for: {} removed successfully", id);
    }
}
