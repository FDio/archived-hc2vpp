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
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.DumpExecutionFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpCallFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpTimeoutException;
import io.fd.honeycomb.translate.v3po.util.JvppReplyConsumer;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.LispEidTableDetailsReplyDump;
import org.openvpp.jvpp.core.dto.LispEidTableDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;


/**
 * Common dump executor for both local and remote mappings
 */
public class MappingsDumpExecutor extends AbstractDumpExecutor
        implements EntityDumpExecutor<LispEidTableDetailsReplyDump, MappingsDumpParams>, JvppReplyConsumer {

    public MappingsDumpExecutor(@Nonnull FutureJVppCore vppApi) {
        super(vppApi);
    }


    @Override
    public LispEidTableDetailsReplyDump executeDump(final MappingsDumpParams params)
            throws DumpExecutionFailedException {
        checkNotNull(params, "Params for dump request not present");

        LispEidTableDump request = new LispEidTableDump();
        request.eid = params.getEid();
        request.eidSet = params.getEidSet();
        request.eidType = params.getEidType();
        request.prefixLength = params.getPrefixLength();
        request.vni = params.getVni();
        request.filter = params.getFilter();

        try {
            return getReply(vppApi.lispEidTableDump(request).toCompletableFuture());
        } catch (TimeoutException e) {
            throw DumpTimeoutException
                    .wrapTimeoutException("Mappings dump execution timed out with params " + params.toString(), e);
        } catch (VppBaseCallException e) {
            throw DumpCallFailedException
                    .wrapFailedCallException("Mappings dump execution failed with params " + params.toString(), e);
        }
    }
}
