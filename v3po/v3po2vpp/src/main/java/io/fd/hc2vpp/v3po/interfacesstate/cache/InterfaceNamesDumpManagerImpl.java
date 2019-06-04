/*
 * Copyright (c) 2019 Cisco and/or its affiliates.
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
import static java.util.stream.Collectors.toMap;

import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.util.read.cache.StaticCacheKeyFactory;
import io.fd.jvpp.stats.dto.InterfaceName;
import io.fd.jvpp.stats.dto.InterfaceNamesDetailsReplyDump;
import io.fd.jvpp.stats.dto.InterfaceNamesDump;
import io.fd.jvpp.stats.future.FutureJVppStatsFacade;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceNamesDumpManagerImpl implements InterfaceNamesDumpManager {
    // byNameIndex must be cached, not held as reference here, to have it destroyed with cache after transaction
    static final String BY_NAME_INDEX_KEY = InterfaceNamesDumpManagerImpl.class.getName() + "_byNameIndex";
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceNamesDumpManagerImpl.class);
    private final FutureJVppStatsFacade jvppStats;

    private final DumpCacheManager<InterfaceNamesDetailsReplyDump, Void> fullDumpManager;

    public InterfaceNamesDumpManagerImpl(final FutureJVppStatsFacade jvppStats) {
        this.jvppStats = jvppStats;
        fullDumpManager = fullInterfaceDumpManager(this.jvppStats,
                new StaticCacheKeyFactory(InterfaceNamesDumpManagerImpl.class.getName() + "_dump",
                        InterfaceNamesDetailsReplyDump.class));
    }

    private static DumpCacheManager<InterfaceNamesDetailsReplyDump, Void> fullInterfaceDumpManager(
            final FutureJVppStatsFacade jvppStats,
            final StaticCacheKeyFactory cacheKeyFactory) {
        return new DumpCacheManager.DumpCacheManagerBuilder<InterfaceNamesDetailsReplyDump, Void>()
                .withExecutor(fullInterfaceDumpExecutor(jvppStats))
                .withCacheKeyFactory(cacheKeyFactory)
                .acceptOnly(InterfaceNamesDetailsReplyDump.class)
                .build();
    }

    private static EntityDumpExecutor<InterfaceNamesDetailsReplyDump, Void> fullInterfaceDumpExecutor(
            final FutureJVppStatsFacade api) {
        return (identifier, params) -> {
            final InterfaceNamesDump request = new InterfaceNamesDump();

            final CompletableFuture<InterfaceNamesDetailsReplyDump>
                    interfaceNamesDetailsReplyDumpCompletableFuture =
                    api.interfaceNamesDump(request).toCompletableFuture();
            return INSTANCE.getReplyForRead(interfaceNamesDetailsReplyDumpCompletableFuture, identifier);
        };
    }

    @Nonnull
    @Override
    public List<InterfaceName> getInterfaceNames(@Nonnull final InstanceIdentifier<?> identifier,
                                                 @Nonnull final ModificationCache cache)
            throws ReadFailedException {
        LOG.debug("Reading all interface names[{}]", identifier);
        return new ArrayList<>(initMapAndGet(identifier, cache).values());
    }

    private Map<String, InterfaceName> initMapAndGet(final InstanceIdentifier<?> identifier,
                                                     final ModificationCache cache)
            throws ReadFailedException {

        if (!cache.containsKey(BY_NAME_INDEX_KEY)) {
            LOG.debug("Performing dump[{}]", identifier);
            final InterfaceNamesDetailsReplyDump dump =
                    fullDumpManager.getDump(identifier, cache).orElse(new InterfaceNamesDetailsReplyDump());

            // naming context initialization must be done here, as it is uses getName in next step, therefore it would
            // create artificial mapping for every interface, because this happens before interface dump is processed
            Arrays.stream(dump.interfaceNamesDetails.interfaceNames).forEach((elt) -> {
                // Store interface name from VPP in context if not yet present
                LOG.trace("Interface with VPP name: {} and index: {} found in VPP", elt.name, elt.swIfIndex);
            });

            final Map<String, InterfaceName> freshIndex = Arrays.stream(dump.interfaceNamesDetails.interfaceNames)
                    .collect(toMap(detail -> detail.name, detail -> detail));
            putMap(freshIndex, cache);
        }

        return getMap(cache);
    }

    private static Map<String, InterfaceName> getMap(final ModificationCache cache) {
        return (Map<String, InterfaceName>) cache.get(BY_NAME_INDEX_KEY);
    }

    private static void putMap(final Map<String, InterfaceName> map, final ModificationCache cache) {
        cache.put(BY_NAME_INDEX_KEY, map);
    }
}
