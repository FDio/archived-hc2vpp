/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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


import java.util.Optional;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.read.init.LispInitPathsMapper;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.jvpp.core.dto.OneGetMapRequestItrRlocs;
import io.fd.jvpp.core.dto.OneGetMapRequestItrRlocsReply;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.itr.remote.locator.sets.grouping.ItrRemoteLocatorSet;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.itr.remote.locator.sets.grouping.ItrRemoteLocatorSetBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItrRemoteLocatorSetCustomizer extends CheckedLispCustomizer
        implements InitializingReaderCustomizer<ItrRemoteLocatorSet, ItrRemoteLocatorSetBuilder>, ByteDataTranslator,
        JvppReplyConsumer, LispInitPathsMapper {

    private static final Logger LOG = LoggerFactory.getLogger(ItrRemoteLocatorSetCustomizer.class);

    private final DumpCacheManager<OneGetMapRequestItrRlocsReply, Void> dumpCacheManager;

    public ItrRemoteLocatorSetCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                         @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJVppCore, lispStateCheckService);
        dumpCacheManager = new DumpCacheManagerBuilder<OneGetMapRequestItrRlocsReply, Void>()
                .withExecutor(((identifier, params) -> getReplyForRead(
                        futureJVppCore.oneGetMapRequestItrRlocs(new OneGetMapRequestItrRlocs()).toCompletableFuture(),
                        identifier)))
                .acceptOnly(OneGetMapRequestItrRlocsReply.class)
                .build();
    }

    @Nonnull
    @Override
    public ItrRemoteLocatorSetBuilder getBuilder(@Nonnull final InstanceIdentifier<ItrRemoteLocatorSet> id) {
        return new ItrRemoteLocatorSetBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<ItrRemoteLocatorSet> id,
                                      @Nonnull final ItrRemoteLocatorSetBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {

        if (!lispStateCheckService.lispEnabled(ctx)) {
            LOG.debug("Failed to read {}. Lisp feature must be enabled first", id);
            return;
        }

        final Optional<OneGetMapRequestItrRlocsReply> reply = dumpCacheManager.getDump(id, ctx.getModificationCache());
        if (!reply.isPresent() || reply.get().locatorSetName == null) {
            return;
        }

        builder.setRemoteLocatorSetName(toString(reply.get().locatorSetName));
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final ItrRemoteLocatorSet readValue) {
        ((LispFeatureDataBuilder) parentBuilder).setItrRemoteLocatorSet(readValue);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<ItrRemoteLocatorSet> instanceIdentifier, @Nonnull ItrRemoteLocatorSet itrRemoteLocatorSet, @Nonnull ReadContext readContext) {
        return Initialized.create(lispFeaturesBasePath().child(ItrRemoteLocatorSet.class), itrRemoteLocatorSet);
    }
}
