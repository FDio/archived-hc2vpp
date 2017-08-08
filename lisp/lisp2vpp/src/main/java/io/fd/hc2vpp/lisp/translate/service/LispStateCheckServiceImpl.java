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

package io.fd.hc2vpp.lisp.translate.service;

import static com.google.common.base.Preconditions.checkState;
import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;

import com.google.inject.Inject;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.vpp.jvpp.core.dto.ShowOneStatus;
import io.fd.vpp.jvpp.core.dto.ShowOneStatusReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.LispBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class LispStateCheckServiceImpl implements LispStateCheckService, JvppReplyConsumer, ByteDataTranslator {

    private static final Lisp STATIC_LISP_INSTANCE = new LispBuilder().setEnable(false).build();
    private static final ShowOneStatusReply DEFAULT_REPLY = new ShowOneStatusReply();
    private static final InstanceIdentifier<Lisp> IDENTIFIER = InstanceIdentifier.create(Lisp.class);

    private final DumpCacheManager<ShowOneStatusReply, Void> dumpManager;

    @Inject
    public LispStateCheckServiceImpl(@Nonnull final FutureJVppCore vppApi) {
        dumpManager = new DumpCacheManagerBuilder<ShowOneStatusReply, Void>()
                .withExecutor((instanceIdentifier, aVoid) -> getReplyForRead(vppApi.showOneStatus(new ShowOneStatus())
                        .toCompletableFuture(), instanceIdentifier))
                .acceptOnly(ShowOneStatusReply.class)
                .build();
    }

    @Override
    public void checkLispEnabledBefore(@Nonnull final WriteContext ctx) {
        // no need to dump here, can be read directly from context
        checkState(ctx.readBefore(InstanceIdentifier.create(Lisp.class))
                .or(STATIC_LISP_INSTANCE).isEnable(), "Lisp feature not enabled");
    }

    @Override
    public void checkLispEnabledAfter(@Nonnull final WriteContext ctx) {
        // no need to dump here, can be read directly from context
        checkState(ctx.readAfter(InstanceIdentifier.create(Lisp.class))
                .or(STATIC_LISP_INSTANCE).isEnable(), "Lisp feature not enabled");
    }

    public boolean lispEnabled(@Nonnull final ReadContext ctx) {
        // in this case it must be dumped
        try {
            return byteToBoolean(dumpManager.getDump(IDENTIFIER, ctx.getModificationCache(), NO_PARAMS)
                    .or(DEFAULT_REPLY).featureStatus);
        } catch (ReadFailedException e) {
            throw new IllegalStateException("Unable to read Lisp Feature status", e);
        }
    }
}
