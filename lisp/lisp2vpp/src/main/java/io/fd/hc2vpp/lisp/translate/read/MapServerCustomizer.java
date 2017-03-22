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

package io.fd.hc2vpp.lisp.translate.read;

import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.vpp.jvpp.core.dto.LispMapServerDetails;
import io.fd.vpp.jvpp.core.dto.LispMapServerDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.LispMapServerDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.MapServers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.MapServersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.map.servers.MapServer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.map.servers.MapServerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.map.servers.MapServerKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapServerCustomizer extends CheckedLispCustomizer
        implements InitializingListReaderCustomizer<MapServer, MapServerKey, MapServerBuilder>, JvppReplyConsumer,
        AddressTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(MapServerCustomizer.class);

    private final DumpCacheManager<LispMapServerDetailsReplyDump, Void> dumpManager;

    public MapServerCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                               @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJVppCore, lispStateCheckService);
        dumpManager = new DumpCacheManagerBuilder<LispMapServerDetailsReplyDump, Void>()
                .acceptOnly(LispMapServerDetailsReplyDump.class)
                .withExecutor((instanceIdentifier, aVoid) ->
                        getReplyForRead(getFutureJVpp()
                                .lispMapServerDump(new LispMapServerDump()).toCompletableFuture(), instanceIdentifier))
                .build();
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<MapServer> instanceIdentifier,
                                                  @Nonnull MapServer mapServer,
                                                  @Nonnull ReadContext readContext) {
        final InstanceIdentifier<MapServer> configId = InstanceIdentifier.create(Lisp.class)
                .child(LispFeatureData.class)
                .child(MapServers.class)
                .child(MapServer.class, instanceIdentifier.firstKeyOf(MapServer.class));

        return Initialized.create(configId, mapServer);
    }

    @Nonnull
    @Override
    public List<MapServerKey> getAllIds(@Nonnull InstanceIdentifier<MapServer> instanceIdentifier,
                                        @Nonnull ReadContext readContext) throws ReadFailedException {
        if (!lispStateCheckService.lispEnabled(readContext)) {
            LOG.info("Lisp feature must be enabled first");
            return Collections.emptyList();
        }

        final Optional<LispMapServerDetailsReplyDump> dump =
                dumpManager.getDump(instanceIdentifier, readContext.getModificationCache(), NO_PARAMS);

        if (dump.isPresent() && dump.get().lispMapServerDetails != null) {
            return dump.get().lispMapServerDetails.stream()
                    .map(detail -> arrayToIpAddress(byteToBoolean(detail.isIpv6), detail.ipAddress))
                    .map(MapServerKey::new)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull List<MapServer> list) {
        MapServersBuilder.class.cast(builder).setMapServer(list);
    }

    @Nonnull
    @Override
    public MapServerBuilder getBuilder(@Nonnull InstanceIdentifier<MapServer> instanceIdentifier) {
        return new MapServerBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<MapServer> instanceIdentifier,
                                      @Nonnull MapServerBuilder mapServerBuilder, @Nonnull ReadContext readContext)
            throws ReadFailedException {
        if (!lispStateCheckService.lispEnabled(readContext)) {
            LOG.info("Lisp feature must be enabled first");
            return;
        }
        final Optional<LispMapServerDetailsReplyDump> dump =
                dumpManager.getDump(instanceIdentifier, readContext.getModificationCache(), NO_PARAMS);

        if (dump.isPresent() && dump.get().lispMapServerDetails != null) {
            final IpAddress currentAddress = instanceIdentifier.firstKeyOf(MapServer.class).getIpAddress();

            final LispMapServerDetails currentDetail = dump.get().lispMapServerDetails.stream()
                    .filter(detail -> Arrays.equals(currentAddress.getValue(),
                            arrayToIpAddress(byteToBoolean(detail.isIpv6), detail.ipAddress).getValue()))
                    .collect(RWUtils.singleItemCollector());

            mapServerBuilder
                    .setIpAddress(arrayToIpAddress(byteToBoolean(currentDetail.isIpv6), currentDetail.ipAddress));

        }
    }
}
