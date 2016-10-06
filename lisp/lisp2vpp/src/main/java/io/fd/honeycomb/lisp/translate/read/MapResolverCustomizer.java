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

import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;

import com.google.common.base.Optional;
import io.fd.honeycomb.lisp.translate.read.dump.executor.MapResolversDumpExecutor;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.vpp.util.AddressTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.vpp.jvpp.core.dto.LispMapResolverDetails;
import io.fd.vpp.jvpp.core.dto.LispMapResolverDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.MapResolversBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.map.resolvers.MapResolverBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.map.resolvers.MapResolverKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapResolverCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<MapResolver, MapResolverKey, MapResolverBuilder>, AddressTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(MapResolverCustomizer.class);
    private static final String MAP_RESOLVERS_CACHE_ID = MapResolverCustomizer.class.getName();

    private final DumpCacheManager<LispMapResolverDetailsReplyDump, Void> dumpManager;

    public MapResolverCustomizer(FutureJVppCore futureJvpp) {
        super(futureJvpp);
        this.dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<LispMapResolverDetailsReplyDump, Void>()
                .withExecutor(new MapResolversDumpExecutor((futureJvpp)))
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

        final Optional<LispMapResolverDetailsReplyDump> dumpOptional =
                dumpManager.getDump(id, MAP_RESOLVERS_CACHE_ID, ctx.getModificationCache(), NO_PARAMS);

        if (!dumpOptional.isPresent() || dumpOptional.get().lispMapResolverDetails.isEmpty()) {
            LOG.warn("No data dumped");
            return;
        }

        final MapResolverKey key = id.firstKeyOf(MapResolver.class);
        final LispMapResolverDetails mapResolverDetails =
                dumpOptional.get().lispMapResolverDetails.stream()
                        .filter(a -> addressesEqual(key.getIpAddress(),
                                arrayToIpAddressReversed(byteToBoolean(a.isIpv6), a.ipAddress)))
                        .collect(RWUtils.singleItemCollector());

        builder.setKey(key);
        builder.setIpAddress(
                arrayToIpAddress(byteToBoolean(mapResolverDetails.isIpv6), mapResolverDetails.ipAddress));
    }

    // safest way to compare addresses - prevents returning false while using different types from hierarchy
    // Ex. Key for MapResolver contains Ipv4Address as value but we translate addresses from binary data to Ipv4AddressNoZone
    private boolean addressesEqual(final IpAddress left, final IpAddress right) {
        return Arrays.equals(left.getValue(), right.getValue());
    }

    @Override
    public List<MapResolverKey> getAllIds(InstanceIdentifier<MapResolver> id, ReadContext context)
            throws ReadFailedException {
        LOG.debug("Dumping MapResolver...");

        final Optional<LispMapResolverDetailsReplyDump> dumpOptional =
                dumpManager.getDump(id, MAP_RESOLVERS_CACHE_ID, context.getModificationCache(), NO_PARAMS);

        if (!dumpOptional.isPresent() || dumpOptional.get().lispMapResolverDetails.isEmpty()) {
            return Collections.emptyList();
        }

        return dumpOptional.get().lispMapResolverDetails.stream()
                .map(resolver -> new MapResolverKey(
                        arrayToIpAddressReversed(byteToBoolean(resolver.isIpv6), resolver.ipAddress)))
                .collect(Collectors.toList());
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<MapResolver> readData) {
        ((MapResolversBuilder) builder).setMapResolver(readData);
    }
}
