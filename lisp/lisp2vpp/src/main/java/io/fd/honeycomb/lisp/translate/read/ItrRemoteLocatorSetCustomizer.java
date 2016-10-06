/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.lisp.translate.read;


import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;

import com.google.common.base.Optional;
import io.fd.honeycomb.lisp.translate.read.dump.executor.ItrRemoteLocatorSetDumpExecutor;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.vpp.jvpp.core.dto.LispGetMapRequestItrRlocsReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.itr.remote.locator.sets.grouping.ItrRemoteLocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.itr.remote.locator.sets.grouping.ItrRemoteLocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ItrRemoteLocatorSetCustomizer extends FutureJVppCustomizer
        implements ReaderCustomizer<ItrRemoteLocatorSet, ItrRemoteLocatorSetBuilder>, ByteDataTranslator {

    private static final String CACHE_KEY = ItrRemoteLocatorSetCustomizer.class.getName();

    private final DumpCacheManager<LispGetMapRequestItrRlocsReply, Void> dumpCacheManager;

    public ItrRemoteLocatorSetCustomizer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
        dumpCacheManager = new DumpCacheManagerBuilder<LispGetMapRequestItrRlocsReply, Void>()
                .withExecutor(new ItrRemoteLocatorSetDumpExecutor(futureJVppCore)).build();
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

        final Optional<LispGetMapRequestItrRlocsReply> reply =
                dumpCacheManager.getDump(id, CACHE_KEY, ctx.getModificationCache(), NO_PARAMS);
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
}
