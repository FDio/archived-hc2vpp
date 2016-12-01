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

package io.fd.hc2vpp.routing.read;

import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.trait.RouteMapper;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.core.dto.Ip6FibDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.IpFibDetailsReplyDump;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.RoutingProtocolsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.RoutingProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.RoutingProtocolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.RoutingProtocolStateVppAttr;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.RoutingProtocolStateVppAttrBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.VniReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.routing.state.routing.instance.routing.protocols.routing.protocol.VppProtocolStateAttributesBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class RoutingProtocolCustomizer
        implements ListReaderCustomizer<RoutingProtocol, RoutingProtocolKey, RoutingProtocolBuilder>, RouteMapper {

    private final NamingContext routingProtocolContext;
    private final DumpCacheManager<IpFibDetailsReplyDump, Void> ipv4RoutesDumpManager;
    private final DumpCacheManager<Ip6FibDetailsReplyDump, Void> ipv6RoutesDumpManager;


    public RoutingProtocolCustomizer(@Nonnull final NamingContext routingProtocolContext,
                                     @Nonnull final DumpCacheManager<IpFibDetailsReplyDump, Void> ipv4RoutesDumpManager,
                                     @Nonnull final DumpCacheManager<Ip6FibDetailsReplyDump, Void> ipv6RoutesDumpManager) {
        this.routingProtocolContext = routingProtocolContext;
        this.ipv4RoutesDumpManager = ipv4RoutesDumpManager;
        this.ipv6RoutesDumpManager = ipv6RoutesDumpManager;
    }

    @Nonnull
    @Override
    public List<RoutingProtocolKey> getAllIds(@Nonnull final InstanceIdentifier<RoutingProtocol> instanceIdentifier,
                                              @Nonnull final ReadContext readContext) throws ReadFailedException {

        final ModificationCache modificationCache = readContext.getModificationCache();

        // builds keys from routing protocol prefix and unique set of table ids
        return Stream.of(
                ipv4TableIds(instanceIdentifier, modificationCache),
                ipv6TableIds(instanceIdentifier, modificationCache))
                .flatMap(Collection::stream)
                .map(tableId -> routingProtocolContext.getName(tableId, readContext.getMappingContext()))
                .distinct()
                .map(RoutingProtocolKey::new)
                .collect(Collectors.toList());
    }

    private List<Integer> ipv4TableIds(final InstanceIdentifier<RoutingProtocol> instanceIdentifier,
                                       final ModificationCache modificationCache) throws ReadFailedException {
        final Optional<IpFibDetailsReplyDump>
                ipv4Routes = ipv4RoutesDumpManager.getDump(instanceIdentifier, modificationCache, NO_PARAMS);

        if (ipv4Routes.isPresent()) {
            return ipv4Routes.get().ipFibDetails.stream()
                    .map(ipFibDetails -> ipFibDetails.tableId)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<Integer> ipv6TableIds(final InstanceIdentifier<RoutingProtocol> instanceIdentifier,
                                       final ModificationCache modificationCache) throws ReadFailedException {
        final Optional<Ip6FibDetailsReplyDump>
                ipv6Routes = ipv6RoutesDumpManager.getDump(instanceIdentifier, modificationCache, NO_PARAMS);

        if (ipv6Routes.isPresent()) {
            return ipv6Routes.get().ip6FibDetails.stream()
                    .map(ipFibDetails -> ipFibDetails.tableId)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<RoutingProtocol> list) {
        RoutingProtocolsBuilder.class.cast(builder).setRoutingProtocol(list);
    }

    @Nonnull
    @Override
    public RoutingProtocolBuilder getBuilder(@Nonnull final InstanceIdentifier<RoutingProtocol> instanceIdentifier) {
        return new RoutingProtocolBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<RoutingProtocol> instanceIdentifier,
                                      @Nonnull final RoutingProtocolBuilder routingProtocolBuilder,
                                      @Nonnull final ReadContext readContext) throws ReadFailedException {

        final RoutingProtocolKey key = instanceIdentifier.firstKeyOf(RoutingProtocol.class);
        routingProtocolBuilder.setName(key.getName()).setKey(key).setType(Static.class)
                .addAugmentation(RoutingProtocolStateVppAttr.class, new RoutingProtocolStateVppAttrBuilder()
                        .setVppProtocolStateAttributes(new VppProtocolStateAttributesBuilder()
                                .setPrimaryVrf(new VniReference(Long.valueOf(routingProtocolContext
                                        .getIndex(key.getName(), readContext.getMappingContext()))))
                                .build())
                        .build());
    }
}
