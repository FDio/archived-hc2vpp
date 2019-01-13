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

package io.fd.hc2vpp.fib.management.read;

import com.google.inject.Inject;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.core.dto.Ip6FibDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.Ip6FibDump;
import io.fd.vpp.jvpp.core.dto.IpFibDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.IpFibDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.FibTableManagementBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.FibTablesBuilder;

/**
 * Factory producing readers for FIB management plugin's data.
 */
public final class FibManagementReaderFactory implements ReaderFactory, JvppReplyConsumer {

    @Inject
    private FutureJVppCore vppApi;

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        final DumpCacheManager<IpFibDetailsReplyDump, Void> ipv4DumpManager = newIpv4RoutesDumpManager(vppApi);
        final DumpCacheManager<Ip6FibDetailsReplyDump, Void> ipv6DumpManager = newIpv6RoutesDumpManager(vppApi);

        registry.addStructuralReader(FibManagementIIds.FIB_MNGMNT, FibTableManagementBuilder.class);
        registry.addStructuralReader(FibManagementIIds.FM_FIB_TABLES, FibTablesBuilder.class);
        registry.add(new GenericInitListReader<>(FibManagementIIds.FM_FTBLS_TABLE,
                new FibTableCustomizer(ipv4DumpManager, ipv6DumpManager)));
    }

    private DumpCacheManager<IpFibDetailsReplyDump, Void> newIpv4RoutesDumpManager(
            @Nonnull final FutureJVppCore vppApi) {
        return new DumpCacheManager.DumpCacheManagerBuilder<IpFibDetailsReplyDump, Void>()
                .withExecutor(
                        (identifier, params) -> getReplyForRead(vppApi.ipFibDump(new IpFibDump()).toCompletableFuture(),
                                identifier))
                .acceptOnly(IpFibDetailsReplyDump.class)
                .build();
    }

    private DumpCacheManager<Ip6FibDetailsReplyDump, Void> newIpv6RoutesDumpManager(
            @Nonnull final FutureJVppCore vppApi) {
        return new DumpCacheManager.DumpCacheManagerBuilder<Ip6FibDetailsReplyDump, Void>()
                .withExecutor(
                        (identifier, params) -> getReplyForRead(
                                vppApi.ip6FibDump(new Ip6FibDump()).toCompletableFuture(), identifier))
                .acceptOnly(Ip6FibDetailsReplyDump.class)
                .build();
    }
}
