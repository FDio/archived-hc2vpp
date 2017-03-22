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

package io.fd.hc2vpp.lisp.translate.write;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.LispMapRequestMode;
import io.fd.vpp.jvpp.core.dto.LispMapRequestModeReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.request.mode.grouping.MapRequestMode;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MapRequestModeCustomizer extends CheckedLispCustomizer
        implements WriterCustomizer<MapRequestMode>, JvppReplyConsumer {

    public MapRequestModeCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                    @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJVppCore, lispStateCheckService);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull InstanceIdentifier<MapRequestMode> instanceIdentifier,
                                       @Nonnull MapRequestMode mapRequestMode,
                                       @Nonnull WriteContext writeContext) throws WriteFailedException {
        lispStateCheckService.checkLispEnabled(writeContext);
        getReplyForWrite(mapRequestModeRequestFuture(mapRequestMode), instanceIdentifier);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull InstanceIdentifier<MapRequestMode> instanceIdentifier,
                                        @Nonnull MapRequestMode mapRequestModeBefore,
                                        @Nonnull MapRequestMode mapRequestModeAfter, @Nonnull WriteContext writeContext)
            throws WriteFailedException {
        lispStateCheckService.checkLispEnabled(writeContext);
        getReplyForUpdate(mapRequestModeRequestFuture(mapRequestModeAfter), instanceIdentifier,
                mapRequestModeBefore, mapRequestModeAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull InstanceIdentifier<MapRequestMode> instanceIdentifier,
                                        @Nonnull MapRequestMode mapRequestMode,
                                        @Nonnull WriteContext writeContext) throws WriteFailedException {
        throw new UnsupportedOperationException("Map request mode cannot be deleted");
    }

    private CompletableFuture<LispMapRequestModeReply> mapRequestModeRequestFuture(
            @Nonnull final MapRequestMode mapRequestMode) {
        LispMapRequestMode request = new LispMapRequestMode();
        request.mode = (byte) checkNotNull(mapRequestMode.getMode(),
                "Mode not specified").getIntValue();
        return getFutureJVpp().lispMapRequestMode(request).toCompletableFuture();
    }
}
