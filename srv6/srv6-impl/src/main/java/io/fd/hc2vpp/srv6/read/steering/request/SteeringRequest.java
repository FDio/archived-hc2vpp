/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6.read.steering.request;

import io.fd.hc2vpp.srv6.util.JVppRequest;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.jvpp.core.dto.SrSteeringPolDetailsReplyDump;
import io.fd.jvpp.core.dto.SrSteeringPolDump;
import io.fd.jvpp.core.future.FutureJVppCore;

/**
 * General template for steering requests
 */
abstract class SteeringRequest extends JVppRequest {

    static final SrSteeringPolDetailsReplyDump STATIC_EMPTY_REPLY = new SrSteeringPolDetailsReplyDump();
    private static final SrSteeringPolDump STATIC_DUMP_REQUEST = new SrSteeringPolDump();
    final DumpCacheManager<SrSteeringPolDetailsReplyDump, Void> dumpManager;

    SteeringRequest(final FutureJVppCore api) {
        super(api);
        this.dumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<SrSteeringPolDetailsReplyDump, Void>().acceptOnly(
                        SrSteeringPolDetailsReplyDump.class)
                        .withExecutor((identifier, params) -> getReplyForRead(
                                getApi().srSteeringPolDump(STATIC_DUMP_REQUEST).toCompletableFuture(), identifier))
                        .build();
    }
}
