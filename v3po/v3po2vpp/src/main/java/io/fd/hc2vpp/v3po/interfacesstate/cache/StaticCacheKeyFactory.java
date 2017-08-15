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

package io.fd.hc2vpp.v3po.interfacesstate.cache;

import io.fd.honeycomb.translate.util.read.cache.CacheKeyFactory;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetailsReplyDump;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class StaticCacheKeyFactory implements CacheKeyFactory<Void> {

    private final String key;

    public StaticCacheKeyFactory(@Nonnull final String key) {
        this.key = key;
    }

    @Nonnull
    @Override
    public String createKey(@Nonnull final InstanceIdentifier<?> actualContextIdentifier,
                            @Nullable final Void dumpParams) {
        return key;
    }

    @Nonnull
    @Override
    public Class<?> getCachedDataType() {
        return SwInterfaceDetailsReplyDump.class;
    }
}
