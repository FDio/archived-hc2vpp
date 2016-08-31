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

package io.fd.honeycomb.lisp.translate.read;

import static io.fd.honeycomb.translate.v3po.util.cache.EntityDumpExecutor.NO_PARAMS;

import com.google.common.base.Optional;
import io.fd.honeycomb.lisp.translate.read.dump.check.MapResolverDumpCheck;
import io.fd.honeycomb.lisp.translate.read.dump.executor.MapResolversDumpExecutor;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.v3po.util.cache.DumpCacheManager;
import io.fd.honeycomb.translate.v3po.util.cache.exceptions.execution.DumpExecutionFailedException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.MapResolversBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.map.resolvers.MapResolverBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.map.resolvers.MapResolverKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.LispMapResolverDetails;
import org.openvpp.jvpp.core.dto.LispMapResolverDetailsReplyDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapResolverCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<MapResolver, MapResolverKey, MapResolverBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(MapResolverCustomizer.class);
    private static final String MAP_RESOLVERS_CACHE_ID = MapResolverCustomizer.class.getName();

    private final DumpCacheManager<LispMapResolverDetailsReplyDump, Void> dumpManager;

    public MapResolverCustomizer(FutureJVppCore futureJvpp) {
        super(futureJvpp);
        this.dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<LispMapResolverDetailsReplyDump, Void>()
                .withExecutor(new MapResolversDumpExecutor((futureJvpp)))
                .withNonEmptyPredicate(new MapResolverDumpCheck())
                .build();
    }

    @Override
    public MapResolverBuilder getBuilder(InstanceIdentifier<MapResolver> id) {
        return new MapResolverBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<MapResolver> id, MapResolverBuilder builder, ReadContext ctx)
            throws ReadFailedException {
        LOG.debug("Reading attributes...");

        Optional<LispMapResolverDetailsReplyDump> dumpOptional = null;
        try {
            dumpOptional = dumpManager.getDump(MAP_RESOLVERS_CACHE_ID, ctx.getModificationCache(), NO_PARAMS);
        } catch (DumpExecutionFailedException e) {
            throw new ReadFailedException(id, e);
        }

        if (!dumpOptional.isPresent()) {
            LOG.warn("No data dumped");
            return;
        }

        MapResolverKey key = id.firstKeyOf(MapResolver.class);
        LispMapResolverDetailsReplyDump dump = dumpOptional.get();

        //cannot use RWUtils.singleItemCollector(),there is some problem with generic params binding
        java.util.Optional<LispMapResolverDetails> mapResolverOptional =
                dump.lispMapResolverDetails.stream().filter(key::equals).findFirst();

        if (mapResolverOptional.isPresent()) {
            LispMapResolverDetails details = mapResolverOptional.get();

            builder.setKey(key);
            builder.setIpAddress(
                    TranslateUtils.arrayToIpAddress(TranslateUtils.byteToBoolean(details.isIpv6), details.ipAddress));
        } else {
            LOG.warn("No data found with matching key");
        }

    }

    @Override
    public List<MapResolverKey> getAllIds(InstanceIdentifier<MapResolver> id, ReadContext context)
            throws ReadFailedException {
        LOG.debug("Dumping MapResolver...");

        Optional<LispMapResolverDetailsReplyDump> dumpOptional = null;
        try {
            dumpOptional = dumpManager.getDump(MAP_RESOLVERS_CACHE_ID, context.getModificationCache(), NO_PARAMS);
        } catch (DumpExecutionFailedException e) {
            throw new ReadFailedException(id, e);
        }

        if (!dumpOptional.isPresent()) {
            LOG.warn("No data dumped");
            return Collections.emptyList();
        }

        return dumpOptional.get().lispMapResolverDetails.stream()
                .map(resolver -> new MapResolverKey(
                        TranslateUtils
                                .arrayToIpAddress(TranslateUtils.byteToBoolean(resolver.isIpv6), resolver.ipAddress)))
                .collect(Collectors.toList());
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<MapResolver> readData) {
        ((MapResolversBuilder) builder).setMapResolver(readData);
    }
}
