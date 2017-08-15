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

import static io.fd.hc2vpp.common.translate.util.JvppReplyConsumer.INSTANCE;
import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;
import static java.util.stream.Collectors.toMap;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetails;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for dump data of interfaces/sub-interfaces
 */
final class InterfaceCacheDumpManagerImpl implements InterfaceCacheDumpManager {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceCacheDumpManagerImpl.class);

    // byNameIndex must be cached, not held as reference here, to have it destroyed with cache after transaction
    static final String BY_NAME_INDEX_KEY = InterfaceCacheDumpManagerImpl.class.getName() + "_byNameIndex";
    private NamingContext namingContext;
    private final DumpCacheManager<SwInterfaceDetailsReplyDump, String> specificDumpManager;
    private final DumpCacheManager<SwInterfaceDetailsReplyDump, Void> fullDumpManager;

    InterfaceCacheDumpManagerImpl(@Nonnull final FutureJVppCore jvpp,
                                  @Nonnull final NamingContext namingContext) {
        this.namingContext = namingContext;
        specificDumpManager = specificInterfaceDumpManager(jvpp);
        fullDumpManager = fullInterfaceDumpManager(jvpp,
                new StaticCacheKeyFactory(StaticCacheKeyFactory.class.getName() + "_dump"));
    }

    @Override
    @Nonnull
    public synchronized Stream<SwInterfaceDetails> getInterfaces(@Nonnull final InstanceIdentifier<?> identifier,
                                                                 @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        LOG.debug("Reading all interfaces[{}]", identifier);
        return initMapAndGet(identifier, ctx).entrySet().stream().map(Map.Entry::getValue);
    }

    @Override
    @Nullable
    public synchronized SwInterfaceDetails getInterfaceDetail(@Nonnull final InstanceIdentifier<?> identifier,
                                                              @Nonnull final ReadContext ctx,
                                                              @Nonnull final String interfaceName)
            throws ReadFailedException {
        final Map<String, SwInterfaceDetails> interfaceIndex = getMap(ctx);

        // does not attempt to cover cases with concurrent updates, as tx should be atomic
        if (interfaceIndex != null) {
            // tries to find interface in map
            return interfaceIndex.get(interfaceName);
        } else {
            // if map is not present, use specific dump(it will be cached standard way, under key constructed from IID)
            return dumpSpecificDetail(identifier, ctx, interfaceName);
        }
    }

    private SwInterfaceDetails dumpSpecificDetail(@Nonnull final InstanceIdentifier<?> identifier,
                                                  @Nonnull final ReadContext ctx,
                                                  @Nonnull final String interfaceName)
            throws ReadFailedException {
        LOG.debug("Interface {} not present in cached data, performing specific dump[{}]", interfaceName,
                identifier);
        final SwInterfaceDetailsReplyDump reply =
                specificDumpManager.getDump(identifier, ctx.getModificationCache(), interfaceName)
                        .or(new SwInterfaceDetailsReplyDump());

        if (reply.swInterfaceDetails.isEmpty()) {
            return null;
        }

        return reply.swInterfaceDetails.get(0);
    }

    private Map<String, SwInterfaceDetails> initMapAndGet(final InstanceIdentifier<?> identifier, final ReadContext ctx)
            throws ReadFailedException {

        final ModificationCache cache = ctx.getModificationCache();
        if (!cache.containsKey(BY_NAME_INDEX_KEY)) {
            LOG.debug("Performing dump[{}]", identifier);
            final SwInterfaceDetailsReplyDump dump =
                    fullDumpManager.getDump(identifier, cache, NO_PARAMS)
                            .or(new SwInterfaceDetailsReplyDump());

            // naming context initialization must be done here, as it is uses getName in next step, therefore it would
            // create artificial mapping for every interface, because this happens before interface dump is processed
            dump.swInterfaceDetails.forEach((elt) -> {
                // Store interface name from VPP in context if not yet present
                if (!namingContext.containsName(elt.swIfIndex, ctx.getMappingContext())) {
                    namingContext.addName(elt.swIfIndex, ByteDataTranslator.INSTANCE.toString(elt.interfaceName),
                            ctx.getMappingContext());
                }
                LOG.trace("Interface with name: {}, VPP name: {} and index: {} found in VPP",
                        getInterfaceName(ctx, elt),
                        elt.interfaceName,
                        elt.swIfIndex);
            });

            final Map<String, SwInterfaceDetails> freshIndex = dump.swInterfaceDetails.stream()
                    .collect(toMap(detail -> getInterfaceName(ctx, detail),
                            detail -> detail));
            putMap(freshIndex, ctx);
        }

        return getMap(ctx);
    }

    private String getInterfaceName(final ReadContext ctx, final SwInterfaceDetails elt) {
        return namingContext.getName(elt.swIfIndex, ctx.getMappingContext());
    }

    private static Map<String, SwInterfaceDetails> getMap(final ReadContext ctx) {
        return (Map<String, SwInterfaceDetails>) ctx.getModificationCache().get(BY_NAME_INDEX_KEY);
    }

    private static void putMap(final Map<String, SwInterfaceDetails> map, final ReadContext ctx) {
        ctx.getModificationCache().put(BY_NAME_INDEX_KEY, map);
    }


    private static DumpCacheManager<SwInterfaceDetailsReplyDump, Void> fullInterfaceDumpManager(
            final FutureJVppCore jvpp,
            final StaticCacheKeyFactory cacheKeyFactory) {
        return new DumpCacheManager.DumpCacheManagerBuilder<SwInterfaceDetailsReplyDump, Void>()
                .withExecutor(fullInterfaceDumpExecutor(jvpp))
                .withCacheKeyFactory(cacheKeyFactory)
                .acceptOnly(SwInterfaceDetailsReplyDump.class)
                .build();
    }

    private static DumpCacheManager<SwInterfaceDetailsReplyDump, String> specificInterfaceDumpManager(
            final FutureJVppCore jvpp) {
        return new DumpCacheManager.DumpCacheManagerBuilder<SwInterfaceDetailsReplyDump, String>()
                .withExecutor(specificInterfaceDumpExecutor(jvpp))
                .acceptOnly(SwInterfaceDetailsReplyDump.class)
                .build();
    }

    private static EntityDumpExecutor<SwInterfaceDetailsReplyDump, Void> fullInterfaceDumpExecutor(
            final FutureJVppCore api) {
        return (identifier, params) -> {
            final SwInterfaceDump request = new SwInterfaceDump();
            request.nameFilter = "".getBytes();
            request.nameFilterValid = 0;

            final CompletableFuture<SwInterfaceDetailsReplyDump>
                    swInterfaceDetailsReplyDumpCompletableFuture = api.swInterfaceDump(request).toCompletableFuture();
            return INSTANCE.getReplyForRead(swInterfaceDetailsReplyDumpCompletableFuture, identifier);
        };
    }

    private static EntityDumpExecutor<SwInterfaceDetailsReplyDump, String> specificInterfaceDumpExecutor(
            final FutureJVppCore api) {
        return (identifier, ifaceName) -> {
            final SwInterfaceDump request = new SwInterfaceDump();
            request.nameFilter = ifaceName.getBytes();
            request.nameFilterValid = 1;

            final CompletableFuture<SwInterfaceDetailsReplyDump>
                    swInterfaceDetailsReplyDumpCompletableFuture = api.swInterfaceDump(request).toCompletableFuture();
            return INSTANCE.getReplyForRead(swInterfaceDetailsReplyDumpCompletableFuture, identifier);
        };
    }
}
