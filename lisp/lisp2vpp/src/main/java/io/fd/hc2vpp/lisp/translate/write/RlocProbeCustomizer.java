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

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.LispRlocProbeEnableDisable;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.rloc.probing.grouping.RlocProbe;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;

public class RlocProbeCustomizer extends FutureJVppCustomizer
        implements WriterCustomizer<RlocProbe>, ByteDataTranslator, JvppReplyConsumer {

    public RlocProbeCustomizer(@Nonnull FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull InstanceIdentifier<RlocProbe> instanceIdentifier,
                                       @Nonnull RlocProbe rlocProbe,
                                       @Nonnull WriteContext writeContext) throws WriteFailedException {
        enableDisableRlocProbe(rlocProbe.isEnabled(), instanceIdentifier);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull InstanceIdentifier<RlocProbe> instanceIdentifier,
                                        @Nonnull RlocProbe rlocProbeBefore,
                                        @Nonnull RlocProbe rlocProbeAfter,
                                        @Nonnull WriteContext writeContext) throws WriteFailedException {
        enableDisableRlocProbe(rlocProbeAfter.isEnabled(), instanceIdentifier);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull InstanceIdentifier<RlocProbe> instanceIdentifier,
                                        @Nonnull RlocProbe rlocProbe,
                                        @Nonnull WriteContext writeContext) throws WriteFailedException {
        enableDisableRlocProbe(false, instanceIdentifier);
    }

    private void enableDisableRlocProbe(final boolean enable, @Nonnull final InstanceIdentifier<RlocProbe> id) throws WriteFailedException {
        LispRlocProbeEnableDisable request = new LispRlocProbeEnableDisable();

        request.isEnabled = booleanToByte(enable);

        getReplyForWrite(getFutureJVpp().lispRlocProbeEnableDisable(request).toCompletableFuture(), id);
    }
}
