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

package io.fd.honeycomb.lisp.translate.read.trait;


import static com.google.common.base.Preconditions.checkNotNull;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.SubtableDumpParams.MapLevel.L2;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.SubtableDumpParams.MapLevel.L3;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.SubtableDumpParams.SubtableDumpParamsBuilder;

import com.google.common.base.Optional;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.SubtableDumpParams;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.vpp.jvpp.core.dto.LispEidTableMapDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.LispEidTableMapDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Provides common logic for reading Eid subtables
 */
public interface SubtableReader extends JvppReplyConsumer {

    SubtableDumpParams L2_PARAMS = new SubtableDumpParamsBuilder().setL2(L2).build();
    SubtableDumpParams L3_PARAMS = new SubtableDumpParamsBuilder().setL2(L3).build();

    default Optional<LispEidTableMapDetailsReplyDump> readSubtable(
            @Nonnull final DumpCacheManager<LispEidTableMapDetailsReplyDump, SubtableDumpParams> dumpManager,
            @Nonnull final String cacheKey,
            @Nonnull final ModificationCache cache,
            @Nonnull final InstanceIdentifier<? extends ChildOf<VniTable>> id,
            @Nonnull final SubtableDumpParams params) throws ReadFailedException {
        return dumpManager.getDump(id, cacheKey, cache, params);
    }

    default EntityDumpExecutor<LispEidTableMapDetailsReplyDump, SubtableDumpParams> createExecutor(
            @Nonnull final FutureJVppCore vppApi) {
        return (identifier, params) -> {
            final LispEidTableMapDump request = new LispEidTableMapDump();
            request.isL2 = checkNotNull(params, "Cannot bind null params").isL2();
            return getReplyForRead(vppApi.lispEidTableMapDump(request).toCompletableFuture(), identifier);
        };
    }
}
