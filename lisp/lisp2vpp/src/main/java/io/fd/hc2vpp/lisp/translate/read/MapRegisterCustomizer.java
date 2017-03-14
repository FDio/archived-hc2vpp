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

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.read.init.LispInitPathsMapper;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.ShowLispMapRegisterState;
import io.fd.vpp.jvpp.core.dto.ShowLispMapRegisterStateReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.register.grouping.MapRegister;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.register.grouping.MapRegisterBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MapRegisterCustomizer extends FutureJVppCustomizer
        implements InitializingReaderCustomizer<MapRegister, MapRegisterBuilder>, ByteDataTranslator,
        JvppReplyConsumer, LispInitPathsMapper {

    public MapRegisterCustomizer(@Nonnull FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
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
        final ShowLispMapRegisterStateReply read = getReplyForRead(getFutureJVpp()
                .showLispMapRegisterState(new ShowLispMapRegisterState()).toCompletableFuture(), instanceIdentifier);

        if (read != null) {
            mapRegisterBuilder.setEnabled(byteToBoolean(read.isEnabled));
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
