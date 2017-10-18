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

import com.google.common.primitives.UnsignedInts;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.read.init.LispInitPathsMapper;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.ShowOneMapRegisterFallbackThreshold;
import io.fd.vpp.jvpp.core.dto.ShowOneMapRegisterFallbackThresholdReply;
import io.fd.vpp.jvpp.core.dto.ShowOneMapRegisterState;
import io.fd.vpp.jvpp.core.dto.ShowOneMapRegisterStateReply;
import io.fd.vpp.jvpp.core.dto.ShowOneMapRegisterTtl;
import io.fd.vpp.jvpp.core.dto.ShowOneMapRegisterTtlReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.register.grouping.MapRegister;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.register.grouping.MapRegisterBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapRegisterCustomizer extends CheckedLispCustomizer
        implements InitializingReaderCustomizer<MapRegister, MapRegisterBuilder>, ByteDataTranslator,
        JvppReplyConsumer, LispInitPathsMapper {

    private static final Logger LOG = LoggerFactory.getLogger(MapRegisterCustomizer.class);

    public MapRegisterCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                 @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJVppCore, lispStateCheckService);
    }

    @Nonnull
    @Override
    public MapRegisterBuilder getBuilder(@Nonnull InstanceIdentifier<MapRegister> instanceIdentifier) {
        return new MapRegisterBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<MapRegister> instanceIdentifier,
                                      @Nonnull MapRegisterBuilder mapRegisterBuilder,
                                      @Nonnull ReadContext readContext) throws ReadFailedException {
        if (!lispStateCheckService.lispEnabled(readContext)) {
            LOG.debug("Failed to read {}. Lisp feature must be enabled first", instanceIdentifier);
            return;
        }

        final ShowOneMapRegisterStateReply read = getReplyForRead(getFutureJVpp()
                .showOneMapRegisterState(new ShowOneMapRegisterState()).toCompletableFuture(), instanceIdentifier);

        if (read != null) {
            mapRegisterBuilder.setEnabled(byteToBoolean(read.isEnabled));

            final ShowOneMapRegisterTtlReply ttlRead = getReplyForRead(getFutureJVpp()
                    .showOneMapRegisterTtl(new ShowOneMapRegisterTtl()).toCompletableFuture(), instanceIdentifier);
            if (ttlRead != null) {
                mapRegisterBuilder.setTtl(UnsignedInts.toLong(ttlRead.ttl));
            }

            final ShowOneMapRegisterFallbackThresholdReply fallbackRead = getReplyForRead(
                    getFutureJVpp().showOneMapRegisterFallbackThreshold(new ShowOneMapRegisterFallbackThreshold())
                            .toCompletableFuture(), instanceIdentifier);
            if (fallbackRead != null) {
                mapRegisterBuilder.setFallbackThreshold(UnsignedInts.toLong(fallbackRead.value));
            }
        }
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull MapRegister mapRegister) {
        LispFeatureDataBuilder.class.cast(builder).setMapRegister(mapRegister);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<MapRegister> instanceIdentifier,
                                                  @Nonnull final MapRegister mapRegister,
                                                  @Nonnull final ReadContext readContext) {
        return Initialized.create(lispFeaturesBasePath().child(MapRegister.class), mapRegister);
    }
}
