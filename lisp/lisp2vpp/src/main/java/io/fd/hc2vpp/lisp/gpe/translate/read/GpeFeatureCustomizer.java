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

package io.fd.hc2vpp.lisp.gpe.translate.read;


import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.jvpp.core.dto.ShowLispStatus;
import io.fd.jvpp.core.dto.ShowLispStatusReply;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.Gpe;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.GpeStateBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.feature.data.grouping.GpeFeatureDataBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GpeFeatureCustomizer extends FutureJVppCustomizer
        implements InitializingReaderCustomizer<GpeFeatureData, GpeFeatureDataBuilder>, JvppReplyConsumer,
        ByteDataTranslator {

    public GpeFeatureCustomizer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }


    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<GpeFeatureData> id,
                                                  @Nonnull final GpeFeatureData readValue,
                                                  @Nonnull final ReadContext ctx) {
        return Initialized.create(InstanceIdentifier.create(Gpe.class).child(GpeFeatureData.class),
                new GpeFeatureDataBuilder().setEnable(readValue.isEnable()).build());
    }

    @Nonnull
    @Override
    public GpeFeatureDataBuilder getBuilder(@Nonnull final InstanceIdentifier<GpeFeatureData> id) {
        return new GpeFeatureDataBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<GpeFeatureData> id,
                                      @Nonnull final GpeFeatureDataBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        // same api as lispState
        final ShowLispStatusReply reply =
                getReplyForRead(getFutureJVpp().showLispStatus(new ShowLispStatus()).toCompletableFuture(), id);

        if (reply != null) {
            builder.setEnable(byteToBoolean(reply.gpeStatus));
        }
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final GpeFeatureData readValue) {
        ((GpeStateBuilder) parentBuilder).setGpeFeatureData(readValue);
    }
}
