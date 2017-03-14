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
import io.fd.vpp.jvpp.core.dto.ShowLispRlocProbeState;
import io.fd.vpp.jvpp.core.dto.ShowLispRlocProbeStateReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.rloc.probing.grouping.RlocProbe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.rloc.probing.grouping.RlocProbeBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RlocProbeCustomizer extends FutureJVppCustomizer
        implements InitializingReaderCustomizer<RlocProbe, RlocProbeBuilder>, JvppReplyConsumer, ByteDataTranslator,
        LispInitPathsMapper {

    public RlocProbeCustomizer(@Nonnull FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Nonnull
    @Override
    public RlocProbeBuilder getBuilder(@Nonnull InstanceIdentifier<RlocProbe> instanceIdentifier) {
        return new RlocProbeBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<RlocProbe> instanceIdentifier,
                                      @Nonnull RlocProbeBuilder rlocProbeBuilder,
                                      @Nonnull ReadContext readContext) throws ReadFailedException {
        final ShowLispRlocProbeStateReply read = getReplyForRead(getFutureJVpp()
                .showLispRlocProbeState(new ShowLispRlocProbeState()).toCompletableFuture(), instanceIdentifier);

        if (read != null) {
            rlocProbeBuilder.setEnabled(byteToBoolean(read.isEnabled));
        }
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull RlocProbe rlocProbe) {
        LispFeatureDataBuilder.class.cast(builder).setRlocProbe(rlocProbe);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<RlocProbe> instanceIdentifier,
                                                  @Nonnull RlocProbe rlocProbe,
                                                  @Nonnull ReadContext readContext) {
        return Initialized.create(lispFeaturesBasePath().child(RlocProbe.class), rlocProbe);
    }
}
