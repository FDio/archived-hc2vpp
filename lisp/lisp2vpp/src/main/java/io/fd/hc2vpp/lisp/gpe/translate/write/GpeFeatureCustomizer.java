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

package io.fd.hc2vpp.lisp.gpe.translate.write;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.GpeEnableDisable;
import io.fd.jvpp.core.dto.GpeEnableDisableReply;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpeFeatureCustomizer extends FutureJVppCustomizer
        implements WriterCustomizer<GpeFeatureData>, JvppReplyConsumer, ByteDataTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(GpeFeatureCustomizer.class);

    public GpeFeatureCustomizer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<GpeFeatureData> id,
                                       @Nonnull final GpeFeatureData dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Writing gpe feature(enabled={})", dataAfter.isEnable());
        getReplyForWrite(enableDisableGpeFeature(dataAfter.isEnable()), id);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<GpeFeatureData> id,
                                        @Nonnull final GpeFeatureData dataBefore,
                                        @Nonnull final GpeFeatureData dataAfter,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        LOG.debug("Updating gpe feature(enabled={})", dataAfter.isEnable());
        getReplyForUpdate(enableDisableGpeFeature(dataAfter.isEnable()), id, dataBefore, dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<GpeFeatureData> id,
                                        @Nonnull final GpeFeatureData dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Removing gpe feature");
        getReplyForDelete(enableDisableGpeFeature(false), id);
    }

    private CompletableFuture<GpeEnableDisableReply> enableDisableGpeFeature(final boolean enable) {
        final GpeEnableDisable request = new GpeEnableDisable();
        request.isEn = booleanToByte(enable);
        LOG.debug("gpeEnableDisable({})", request);
        return getFutureJVpp().gpeEnableDisable(request).toCompletableFuture();
    }
}
