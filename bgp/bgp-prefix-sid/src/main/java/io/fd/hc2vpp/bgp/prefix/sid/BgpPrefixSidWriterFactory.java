/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.bgp.prefix.sid;

import com.google.inject.Inject;
import io.fd.honeycomb.translate.bgp.RibWriter;
import io.fd.honeycomb.translate.bgp.RouteWriterFactory;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;

final class BgpPrefixSidWriterFactory implements RouteWriterFactory {
    @Inject
    private FutureJVppCore vppApi;

    @Override
    public void init(@Nonnull final RibWriter registry) {
        registry.register(new BgpPrefixSidMplsWriter(vppApi));
    }
}
