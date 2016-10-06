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
import io.fd.vpp.jvpp.core.dto.LispEidTableVniDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.LispEidTableVniDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class VniTableDumpExecutor extends AbstractJvppDumpExecutor
        implements EntityDumpExecutor<LispEidTableVniDetailsReplyDump, Void>, JvppReplyConsumer {

    public VniTableDumpExecutor(@Nonnull FutureJVppCore api) {
        super(api);
    }

    @Override
    public LispEidTableVniDetailsReplyDump executeDump(final InstanceIdentifier<?> identifier, Void params)
            throws ReadFailedException {
        return getReplyForRead(vppApi.lispEidTableVniDump(new LispEidTableVniDump()).toCompletableFuture(), identifier);
    }
}
