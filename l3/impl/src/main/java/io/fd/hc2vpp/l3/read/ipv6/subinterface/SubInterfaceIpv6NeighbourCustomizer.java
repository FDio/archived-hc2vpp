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

package io.fd.hc2vpp.l3.read.ipv6.subinterface;

import java.util.Optional;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.utils.ip.read.IfaceDumpFilter;
import io.fd.hc2vpp.l3.utils.ip.read.IpNeighbourReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.jvpp.core.dto.IpNeighborDetails;
import io.fd.jvpp.core.dto.IpNeighborDetailsReplyDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.Ipv6Builder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.ipv6.Neighbor;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.ipv6.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.ipv6.NeighborKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceIpv6NeighbourCustomizer extends IpNeighbourReader
        implements ListReaderCustomizer<Neighbor, NeighborKey, NeighborBuilder> {

    public SubInterfaceIpv6NeighbourCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                               @Nonnull final NamingContext interfaceContext) {
        super(interfaceContext, true, new DumpCacheManagerBuilder<IpNeighborDetailsReplyDump, IfaceDumpFilter>()
                .withExecutor(createNeighbourDumpExecutor(futureJVppCore))
                // cached with parent interface scope
                .withCacheKeyFactory(subInterfaceScopedCacheKeyFactory(IpNeighborDetailsReplyDump.class))
                .build());
    }

    @Override
    public NeighborBuilder getBuilder(InstanceIdentifier<Neighbor> id) {
        return new NeighborBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<Neighbor> id, NeighborBuilder builder, ReadContext ctx)
            throws ReadFailedException {

        final Ipv6AddressNoZone ip = id.firstKeyOf(Neighbor.class).getIp();

        final Optional<IpNeighborDetailsReplyDump> dumpOpt = subInterfaceNeighboursDump(id, ctx);

        if (dumpOpt.isPresent()) {
            dumpOpt.get().ipNeighborDetails
                    .stream()
                    .filter(ipNeighborDetails -> ip.equals(arrayToIpv6AddressNoZone(
                            ipNeighborDetails.neighbor.ipAddress.un.getIp6().ip6Address)))
                    .findFirst()
                    .ifPresent(ipNeighborDetails -> builder.setIp(arrayToIpv6AddressNoZone(
                            ipNeighborDetails.neighbor.ipAddress.un.getIp6().ip6Address))
                            .withKey(keyMapper().apply(ipNeighborDetails))
                            .setLinkLayerAddress(toPhysAddress(ipNeighborDetails.neighbor.macAddress.macaddress)));
        }
    }

    @Override
    public List<NeighborKey> getAllIds(InstanceIdentifier<Neighbor> id, ReadContext context)
            throws ReadFailedException {
        return getNeighborKeys(subInterfaceNeighboursDump(id, context), keyMapper());
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<Neighbor> readData) {
        ((Ipv6Builder) builder).setNeighbor(readData);
    }

    private Function<IpNeighborDetails, NeighborKey> keyMapper() {
        return ipNeighborDetails -> new NeighborKey(
                arrayToIpv6AddressNoZone(ipNeighborDetails.neighbor.ipAddress.un.getIp6().ip6Address));
    }
}
