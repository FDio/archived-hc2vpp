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

import io.fd.honeycomb.lisp.translate.read.dump.executor.params.LocatorDumpParams;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.DumpExecutionFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpCallFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.i.DumpTimeoutException;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.LispLocatorDetailsReplyDump;
import org.openvpp.jvpp.core.dto.LispLocatorDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;


/**
 * Executor for dumping of locators
 */
public class LocatorDumpExecutor extends AbstractDumpExecutor
        implements EntityDumpExecutor<LispLocatorDetailsReplyDump, LocatorDumpParams>, JvppReplyConsumer {


    public LocatorDumpExecutor(@Nonnull final FutureJVppCore vppApi) {
        super(vppApi);
    }

    @Override
    public LispLocatorDetailsReplyDump executeDump(final LocatorDumpParams params) throws DumpExecutionFailedException {
        checkNotNull(params, "Params for dump request not present");

        LispLocatorDump request = new LispLocatorDump();
        request.lsIndex = params.getLocatorSetIndex();
        //flag that lsIndex is set
        request.isIndexSet = (byte) 1;

        try {
            return getReply(vppApi.lispLocatorDump(request).toCompletableFuture());
        } catch (TimeoutException e) {
            throw DumpTimeoutException
                    .wrapTimeoutException("Locator dump ended in timeout with params" + params.toString(), e);
        } catch (VppBaseCallException e) {
            throw DumpCallFailedException
                    .wrapFailedCallException("Locator dump failed with params" + params.toString(), e);
        }
    }
}
