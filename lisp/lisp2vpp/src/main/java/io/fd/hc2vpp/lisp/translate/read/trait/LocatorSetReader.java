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

package io.fd.hc2vpp.lisp.translate.read.trait;

import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.vpp.jvpp.core.dto.OneLocatorSetDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.OneLocatorSetDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.locator.sets.LocatorSet;

/**
 * Provides common logic for reading {@link LocatorSet}
 */
public interface LocatorSetReader extends JvppReplyConsumer {

    default EntityDumpExecutor<OneLocatorSetDetailsReplyDump, Void> createExecutor(
            @Nonnull final FutureJVppCore vppApi) {
        return (identifier, params) -> {
            final OneLocatorSetDump request = new OneLocatorSetDump();
            //only local
            request.filter = 1;
            return getReplyForRead(vppApi.oneLocatorSetDump(request).toCompletableFuture(), identifier);
        };
    }
}
