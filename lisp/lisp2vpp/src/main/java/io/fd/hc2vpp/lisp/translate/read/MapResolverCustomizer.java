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

package io.fd.hc2vpp.lisp.translate.read;

import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.read.init.LispInitPathsMapper;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.core.dto.LispMapResolverDetails;
import io.fd.vpp.jvpp.core.dto.LispMapResolverDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.LispMapResolverDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.resolvers.grouping.MapResolvers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.resolvers.grouping.MapResolversBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.resolvers.grouping.map.resolvers.MapResolverBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.resolvers.grouping.map.resolvers.MapResolverKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapResolverCustomizer extends CheckedLispCustomizer
        implements InitializingListReaderCustomizer<MapResolver, MapResolverKey, MapResolverBuilder>, AddressTranslator,
        JvppReplyConsumer, LispInitPathsMapper {

    private static final Logger LOG = LoggerFactory.getLogger(MapResolverCustomizer.class);

    private final DumpCacheManager<LispMapResolverDetailsReplyDump, Void> dumpManager;

    public MapResolverCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                                 @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJvpp, lispStateCheckService);
        this.dumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<LispMapResolverDetailsReplyDump, Void>()
                        .withExecutor((identifier, params) -> getReplyForRead(
                                futureJvpp.lispMapResolverDump(new LispMapResolverDump()).toCompletableFuture(),
                                identifier))
                        .acceptOnly(LispMapResolverDetailsReplyDump.class)
                        .build();
    }

    @Override
    public MapResolverBuilder getBuilder(InstanceIdentifier<MapResolver> id) {
        return new MapResolverBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<MapResolver> id, MapResolverBuilder builder, ReadContext ctx)
            throws ReadFailedException {
        if (!lispStateCheckService.lispEnabled(ctx)) {
            LOG.info("Lisp feature must be enabled first");
            return;
        }
        LOG.debug("Reading attributes...");

        final Optional<LispMapResolverDetailsReplyDump> dumpOptional =
                dumpManager.getDump(id, ctx.getModificationCache(), NO_PARAMS);

        if (!dumpOptional.isPresent() || dumpOptional.get().lispMapResolverDetails.isEmpty()) {
            LOG.warn("No data dumped");
            return;
        }

        final MapResolverKey key = id.firstKeyOf(MapResolver.class);
        final LispMapResolverDetails mapResolverDetails =
                dumpOptional.get().lispMapResolverDetails.stream()
                        .filter(a -> addressesEqual(key.getIpAddress(),
                                arrayToIpAddress(byteToBoolean(a.isIpv6), a.ipAddress)))
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
        if (!lispStateCheckService.lispEnabled(context)) {
            LOG.info("Lisp feature must be enabled first");
            return Collections.emptyList();
        }
        LOG.debug("Dumping MapResolver...");

        final Optional<LispMapResolverDetailsReplyDump> dumpOptional =
                dumpManager.getDump(id, context.getModificationCache(), NO_PARAMS);

        if (!dumpOptional.isPresent() || dumpOptional.get().lispMapResolverDetails.isEmpty()) {
            return Collections.emptyList();
        }

        return dumpOptional.get().lispMapResolverDetails.stream()
                .map(resolver -> new MapResolverKey(
                        arrayToIpAddress(byteToBoolean(resolver.isIpv6), resolver.ipAddress)))
                .collect(Collectors.toList());
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<MapResolver> readData) {
        ((MapResolversBuilder) builder).setMapResolver(readData);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<MapResolver> instanceIdentifier,
                                                  @Nonnull MapResolver mapResolver, @Nonnull ReadContext readContext) {
        return Initialized.create(lispFeaturesBasePath().child(MapResolvers.class)
                .child(MapResolver.class, instanceIdentifier.firstKeyOf(MapResolver.class)), mapResolver);
    }
}
