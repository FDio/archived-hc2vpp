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

package io.fd.hc2vpp.lisp.gpe.translate.service;

import static com.google.common.base.Preconditions.checkState;

import com.google.inject.Inject;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.vpp.jvpp.core.dto.ShowLispStatus;
import io.fd.vpp.jvpp.core.dto.ShowLispStatusReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.Gpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.GpeState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.feature.data.grouping.GpeFeatureDataBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class GpeStateCheckServiceImpl implements GpeStateCheckService, JvppReplyConsumer, ByteDataTranslator {

    private static final GpeFeatureData DISABLED_GPE = new GpeFeatureDataBuilder().build();
    private static final InstanceIdentifier<GpeFeatureData>
            GPE_FEATURE_CONFIG_ID = InstanceIdentifier.create(Gpe.class)
            .child(GpeFeatureData.class);
    private static final InstanceIdentifier<GpeFeatureData>
            GPE_FEATURE_STATE_ID = InstanceIdentifier.create(GpeState.class).child(GpeFeatureData.class);
    private static final ShowLispStatusReply DEFAULT_REPLY = new ShowLispStatusReply();
    private final DumpCacheManager<ShowLispStatusReply, Void> dumpCacheManager;

    @Inject
    public GpeStateCheckServiceImpl(@Nonnull final FutureJVppCore api) {
        dumpCacheManager = new DumpCacheManagerBuilder<ShowLispStatusReply, Void>()
                .acceptOnly(ShowLispStatusReply.class)
                .withExecutor((identifier, params) -> getReplyForRead(
                        api.showLispStatus(new ShowLispStatus()).toCompletableFuture(), identifier))
                .build();
    }

    @Override
    public void checkGpeEnabledBefore(@Nonnull final WriteContext writeContext) {
        checkState(writeContext.readBefore(GPE_FEATURE_CONFIG_ID).or(DISABLED_GPE).isEnable(),
                "Gpe feature is disabled");
    }

    @Override
    public void checkGpeEnabledAfter(@Nonnull final WriteContext writeContext) {
        checkState(writeContext.readAfter(GPE_FEATURE_CONFIG_ID).or(DISABLED_GPE).isEnable(),
                "Gpe feature is disabled");
    }

    @Override
    public boolean isGpeEnabled(@Nonnull final ReadContext readContext) {
        try {
            return byteToBoolean(
                    dumpCacheManager.getDump(GPE_FEATURE_STATE_ID, readContext.getModificationCache())
                            .or(DEFAULT_REPLY).gpeStatus);
        } catch (ReadFailedException e) {
            throw new IllegalStateException("Unable to read Gpe feature status", e);
        }
    }
}
