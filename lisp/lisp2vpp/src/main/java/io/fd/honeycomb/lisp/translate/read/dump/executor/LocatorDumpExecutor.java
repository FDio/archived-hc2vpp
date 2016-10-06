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
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.vpp.jvpp.core.dto.LispLocatorDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.LispLocatorDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Executor for dumping of locators
 */
public class LocatorDumpExecutor extends AbstractJvppDumpExecutor
        implements EntityDumpExecutor<LispLocatorDetailsReplyDump, LocatorDumpParams>, JvppReplyConsumer {


    public LocatorDumpExecutor(@Nonnull final FutureJVppCore vppApi) {
        super(vppApi);
    }

    @Override
    @Nonnull
    public LispLocatorDetailsReplyDump executeDump(final InstanceIdentifier<?> identifier,
                                                   final LocatorDumpParams params) throws
            ReadFailedException {
        checkNotNull(params, "Params for dump request not present");

        LispLocatorDump request = new LispLocatorDump();
        request.lsIndex = params.getLocatorSetIndex();
        //flag that lsIndex is set
        request.isIndexSet = (byte) 1;

        return getReplyForRead(vppApi.lispLocatorDump(request).toCompletableFuture(), identifier);
    }
}
