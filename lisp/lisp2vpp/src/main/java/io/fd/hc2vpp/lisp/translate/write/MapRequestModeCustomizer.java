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

import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.OneMapRequestMode;
import io.fd.vpp.jvpp.core.dto.OneMapRequestModeReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.map.request.mode.grouping.MapRequestMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.map.request.mode.grouping.MapRequestModeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.MapRequestMode.DestinationOnly;

public class MapRequestModeCustomizer extends CheckedLispCustomizer
        implements WriterCustomizer<MapRequestMode>, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MapRequestModeCustomizer.class);
    private static final MapRequestMode DEFAULT_MODE = new MapRequestModeBuilder().setMode(DestinationOnly).build();

    public MapRequestModeCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                    @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJVppCore, lispStateCheckService);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull InstanceIdentifier<MapRequestMode> instanceIdentifier,
                                       @Nonnull MapRequestMode mapRequestMode,
                                       @Nonnull WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Setting map request mode to [{}]", mapRequestMode);
        lispStateCheckService.checkLispEnabledAfter(writeContext);
        getReplyForWrite(mapRequestModeRequestFuture(mapRequestMode), instanceIdentifier);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull InstanceIdentifier<MapRequestMode> instanceIdentifier,
                                        @Nonnull MapRequestMode mapRequestModeBefore,
                                        @Nonnull MapRequestMode mapRequestModeAfter, @Nonnull WriteContext writeContext)
            throws WriteFailedException {
        lispStateCheckService.checkLispEnabledAfter(writeContext);
        LOG.debug("Setting map request mode to [{}]", mapRequestModeAfter);
        getReplyForUpdate(mapRequestModeRequestFuture(mapRequestModeAfter), instanceIdentifier,
                mapRequestModeBefore, mapRequestModeAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull InstanceIdentifier<MapRequestMode> instanceIdentifier,
                                        @Nonnull MapRequestMode mapRequestMode,
                                        @Nonnull WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Setting map request mode to default[{}]", DEFAULT_MODE);
        // there is no delete, just set to default. also prevents failing on delete of parent node
        lispStateCheckService.checkLispEnabledBefore(writeContext);
        getReplyForWrite(mapRequestModeRequestFuture(DEFAULT_MODE), instanceIdentifier);
    }

    private CompletableFuture<OneMapRequestModeReply> mapRequestModeRequestFuture(
            @Nonnull final MapRequestMode mapRequestMode) {
        OneMapRequestMode request = new OneMapRequestMode();
        request.mode = (byte) checkNotNull(mapRequestMode.getMode(),
                "Mode not specified").getIntValue();
        return getFutureJVpp().oneMapRequestMode(request).toCompletableFuture();
    }
}
