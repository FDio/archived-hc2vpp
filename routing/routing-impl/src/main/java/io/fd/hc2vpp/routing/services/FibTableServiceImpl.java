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

package io.fd.hc2vpp.routing.services;

import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.routing.RoutingIIds;
import io.fd.hc2vpp.routing.write.factory.FibTableRequest;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.Ip6FibDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.Ip6FibDump;
import io.fd.vpp.jvpp.core.dto.IpFibDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.IpFibDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.stream.Stream;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Todo HC2VPP-317: FibTableService was created as a temporary workaround to write Fib tables in VPP.
// We need to implement proper support for Fib table management.
public class FibTableServiceImpl extends FutureJVppCustomizer implements FibTableService, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(FibTableServiceImpl.class);
    private final DumpCacheManager<IpFibDetailsReplyDump, Void> v4DumpManager;
    private final DumpCacheManager<Ip6FibDetailsReplyDump, Void> v6DumpManager;
    private ModificationCache modificationCache;

    public FibTableServiceImpl(@Nonnull FutureJVppCore futureJVppCore, ModificationCache modificationCache) {
        super(futureJVppCore);
        this.modificationCache = modificationCache;

        v4DumpManager = new DumpCacheManager.DumpCacheManagerBuilder<IpFibDetailsReplyDump, Void>()
                .acceptOnly(IpFibDetailsReplyDump.class)
                .withExecutor((identifier, params) -> getReplyForRead(
                        futureJVppCore.ipFibDump(new IpFibDump()).toCompletableFuture(), identifier))
                .build();
        v6DumpManager = new DumpCacheManager.DumpCacheManagerBuilder<Ip6FibDetailsReplyDump, Void>()
                .acceptOnly(Ip6FibDetailsReplyDump.class)
                .withExecutor((identifier, params) -> getReplyForRead(
                        futureJVppCore.ip6FibDump(new Ip6FibDump()).toCompletableFuture(), identifier))
                .build();
    }

    @Override
    public void write(InstanceIdentifier<?> identifier, @Nonnegative int tableId, @Nonnull String tableName,
                      boolean isIpv6) throws WriteFailedException {
        // Register fib table in VPP
        FibTableRequest fibTableRequest = new FibTableRequest(getFutureJVpp(), modificationCache);
        fibTableRequest.setFibName(tableName);
        fibTableRequest.setIpv6(isIpv6);
        fibTableRequest.setFibTable(tableId);
        fibTableRequest.checkValid();
        try {
            fibTableRequest.write(identifier);
            LOG.debug("Fib table written successfully. table-name: {}, table-id: {}, request: {}", tableName, tableId,
                    fibTableRequest);
        } catch (WriteFailedException e) {
            LOG.warn("Fib table write failed. request: {}", fibTableRequest);
            throw new WriteFailedException(identifier, "Failed to write fib table to VPP.", e);
        }
    }

    @Override
    public void checkTableExist(@Nonnegative final int index,
                                @Nonnull final ModificationCache cache)
            throws ReadFailedException, FibTableService.FibTableDoesNotExistException {

        if (Stream.concat(dumpV4FibTableIdsStream(cache), dumpV6FibTableIdsStream(cache))
                .noneMatch(id -> id == index)) {
            throw new FibTableService.FibTableDoesNotExistException(index);
        }
    }

    private Stream<Integer> dumpV6FibTableIdsStream(final ModificationCache cache) throws ReadFailedException {
        return v6DumpManager.getDump(RoutingIIds.ROUTING, cache, NO_PARAMS)
                .toJavaUtil()
                .map(ip6FibDetailsReplyDump -> ip6FibDetailsReplyDump.ip6FibDetails)
                .orElse(Collections.emptyList())
                .stream()
                .map(ip6FibDetails -> ip6FibDetails.tableId);
    }

    private Stream<Integer> dumpV4FibTableIdsStream(final ModificationCache cache) throws ReadFailedException {
        return v4DumpManager.getDump(RoutingIIds.ROUTING, cache, NO_PARAMS)
                .toJavaUtil()
                .map(ipFibDetailsReplyDump -> ipFibDetailsReplyDump.ipFibDetails)
                .orElse(Collections.emptyList())
                .stream()
                .map(ipFibDetails -> ipFibDetails.tableId);
    }
}
