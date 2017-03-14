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

package io.fd.hc2vpp.lisp.translate.read;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.read.init.LispInitPathsMapper;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.ShowLispMapRequestMode;
import io.fd.vpp.jvpp.core.dto.ShowLispMapRequestModeReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.request.mode.grouping.MapRequestMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.request.mode.grouping.MapRequestModeBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MapRequestModeCustomizer extends FutureJVppCustomizer
        implements InitializingReaderCustomizer<MapRequestMode, MapRequestModeBuilder>,
        JvppReplyConsumer, LispInitPathsMapper {

    public MapRequestModeCustomizer(@Nonnull FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Nonnull
    @Override
    public MapRequestModeBuilder getBuilder(@Nonnull InstanceIdentifier<MapRequestMode> instanceIdentifier) {
        return new MapRequestModeBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<MapRequestMode> instanceIdentifier,
                                      @Nonnull MapRequestModeBuilder mapRequestModeBuilder,
                                      @Nonnull ReadContext readContext) throws ReadFailedException {
        final ShowLispMapRequestModeReply reply = getReplyForRead(
                getFutureJVpp().showLispMapRequestMode(new ShowLispMapRequestMode()).toCompletableFuture(),
                instanceIdentifier);

        if (reply != null) {
            mapRequestModeBuilder.setMode(
                    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.MapRequestMode
                            .forValue(reply.mode));
        }
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder,
                      @Nonnull MapRequestMode mapRequestMode) {
        LispFeatureDataBuilder.class.cast(builder).setMapRequestMode(mapRequestMode);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<MapRequestMode> instanceIdentifier,
                                                  @Nonnull MapRequestMode mapRequestMode,
                                                  @Nonnull ReadContext readContext) {
        return Initialized.create(lispFeaturesBasePath().child(MapRequestMode.class), mapRequestMode);
    }
}
