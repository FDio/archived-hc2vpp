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


import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.vpp.jvpp.core.dto.LispLocatorSetDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.LispLocatorSetDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class LocatorSetsDumpExecutor extends AbstractJvppDumpExecutor
        implements EntityDumpExecutor<LispLocatorSetDetailsReplyDump, Void>, JvppReplyConsumer {

    public LocatorSetsDumpExecutor(@Nonnull FutureJVppCore api) {
        super(api);
    }

    @Override
    @Nonnull
    public LispLocatorSetDetailsReplyDump executeDump(final InstanceIdentifier<?> identifier, final Void params)
            throws ReadFailedException {

        LispLocatorSetDump request = new LispLocatorSetDump();
        //only local
        request.filter = 1;

        return getReplyForRead(vppApi.lispLocatorSetDump(request).toCompletableFuture(), identifier);
    }
}
