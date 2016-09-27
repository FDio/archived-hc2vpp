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


import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.DumpExecutionFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpCallFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpTimeoutException;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.LispEidTableMapDetailsReplyDump;
import org.openvpp.jvpp.core.dto.LispEidTableMapDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;


public class VniTableDumpExecutor extends AbstractDumpExecutor
        implements EntityDumpExecutor<LispEidTableMapDetailsReplyDump, Void>, JvppReplyConsumer {

    public VniTableDumpExecutor(@Nonnull FutureJVppCore api) {
        super(api);
    }

    @Override
    public LispEidTableMapDetailsReplyDump executeDump(Void params) throws DumpExecutionFailedException {
        try {
            return getReply(vppApi.lispEidTableMapDump(new LispEidTableMapDump()).toCompletableFuture());
        } catch (TimeoutException e) {
            throw DumpTimeoutException.wrapTimeoutException("Eid table map dump ended in timeout", e);
        } catch (VppBaseCallException e) {
            throw DumpCallFailedException.wrapFailedCallException("Eid table map dump failed", e);
        }
    }
}
