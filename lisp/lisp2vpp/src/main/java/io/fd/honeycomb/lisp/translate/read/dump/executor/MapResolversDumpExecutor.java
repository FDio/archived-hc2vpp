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
import io.fd.honeycomb.translate.v3po.util.JvppReplyConsumer;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.LispMapResolverDetailsReplyDump;
import org.openvpp.jvpp.core.dto.LispMapResolverDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;


public class MapResolversDumpExecutor extends AbstractDumpExecutor
        implements EntityDumpExecutor<LispMapResolverDetailsReplyDump, Void>, JvppReplyConsumer {

    public MapResolversDumpExecutor(@Nonnull FutureJVppCore api) {
        super(api);
    }

    @Override
    public LispMapResolverDetailsReplyDump executeDump(final Void params) throws DumpExecutionFailedException {
        try {
            return getReply(vppApi.lispMapResolverDump(new LispMapResolverDump()).toCompletableFuture());
        } catch (TimeoutException e) {
            throw DumpTimeoutException
                    .wrapTimeoutException("Map resolver dump execution ended in timeout", e);
        } catch (VppBaseCallException e) {
            throw DumpCallFailedException.wrapFailedCallException("Map resolver dump execution failed", e);
        }
    }
}
