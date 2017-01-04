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

package io.fd.hc2vpp.v3po.interfacesstate.ip.v4.subinterface;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.ip.IpReader;
import io.fd.hc2vpp.v3po.interfacesstate.ip.dump.params.IfaceDumpFilter;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.honeycomb.translate.util.read.cache.DumpSupplier;
import io.fd.honeycomb.translate.util.read.cache.TypeAwareIdentifierCacheKeyFactory;
import io.fd.vpp.jvpp.core.dto.IpNeighborDetails;
import io.fd.vpp.jvpp.core.dto.IpNeighborDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.NeighborKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceIpv4NeighbourCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<Neighbor, NeighborKey, NeighborBuilder>, IpReader {

    private final DumpCacheManager<IpNeighborDetailsReplyDump, IfaceDumpFilter> dumpManager;
    private final NamingContext interfaceContext;

    public SubInterfaceIpv4NeighbourCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                               @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        dumpManager = new DumpCacheManagerBuilder<IpNeighborDetailsReplyDump, IfaceDumpFilter>()
                .withExecutor(createNeighbourDumpExecutor(futureJVppCore))
                // cached with parent interface scope
                .withCacheKeyFactory(new TypeAwareIdentifierCacheKeyFactory(IpNeighborDetailsReplyDump.class,
                        ImmutableSet.of(Interface.class)))
                .build();
        this.interfaceContext = interfaceContext;
    }

    @Override
    public NeighborBuilder getBuilder(InstanceIdentifier<Neighbor> id) {
        return new NeighborBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<Neighbor> id, NeighborBuilder builder, ReadContext ctx)
            throws ReadFailedException {

        final Ipv4AddressNoZone ip = id.firstKeyOf(Neighbor.class).getIp();

        final Optional<IpNeighborDetailsReplyDump> dumpOpt = dumpSupplier(id, ctx).get();

        if (dumpOpt.isPresent()) {
            dumpOpt.get().ipNeighborDetails
                    .stream()
                    .filter(ipNeighborDetails -> ip.equals(arrayToIpv4AddressNoZone(ipNeighborDetails.ipAddress)))
                    .findFirst()
                    .ifPresent(ipNeighborDetails -> builder.setIp(arrayToIpv4AddressNoZone(ipNeighborDetails.ipAddress))
                            .setKey(keyMapper().apply(ipNeighborDetails))
                            .setLinkLayerAddress(toPhysAddress(ipNeighborDetails.macAddress)));
        }
    }

    @Override
    public List<NeighborKey> getAllIds(InstanceIdentifier<Neighbor> id, ReadContext context)
            throws ReadFailedException {
        return getNeighborKeys(dumpSupplier(id, context), keyMapper());
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<Neighbor> readData) {
        ((Ipv4Builder) builder).setNeighbor(readData);
    }

    private Function<IpNeighborDetails, NeighborKey> keyMapper() {
        return ipNeighborDetails -> new NeighborKey(arrayToIpv4AddressNoZone(ipNeighborDetails.ipAddress));
    }

    private DumpSupplier<Optional<IpNeighborDetailsReplyDump>> dumpSupplier(final InstanceIdentifier<Neighbor> id,
                                                                            final ReadContext context) {
        return () -> dumpManager
                .getDump(id, context.getModificationCache(), new IfaceDumpFilter(interfaceContext
                        .getIndex(id.firstKeyOf(Interface.class).getName(), context.getMappingContext()),
                        false));
    }

}