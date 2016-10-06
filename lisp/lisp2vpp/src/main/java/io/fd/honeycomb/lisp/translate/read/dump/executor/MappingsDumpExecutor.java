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

package io.fd.honeycomb.lisp.translate.read.dump.executor;


import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.vpp.jvpp.core.dto.LispEidTableDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.LispEidTableDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Common dump executor for both local and remote mappings
 */
public class MappingsDumpExecutor extends AbstractJvppDumpExecutor
        implements EntityDumpExecutor<LispEidTableDetailsReplyDump, MappingsDumpParams>, JvppReplyConsumer {

    public MappingsDumpExecutor(@Nonnull FutureJVppCore vppApi) {
        super(vppApi);
    }


    @Override
    @Nonnull
    public LispEidTableDetailsReplyDump executeDump(final InstanceIdentifier<?> identifier,
                                                    final MappingsDumpParams params)
            throws ReadFailedException {
        checkNotNull(params, "Params for dump request not present");

        LispEidTableDump request = new LispEidTableDump();
        request.eid = params.getEid();
        request.eidSet = params.getEidSet();
        request.eidType = params.getEidType();
        request.prefixLength = params.getPrefixLength();
        request.vni = params.getVni();
        request.filter = params.getFilter();

        return getReplyForRead(vppApi.lispEidTableDump(request).toCompletableFuture(), identifier);
    }
}
