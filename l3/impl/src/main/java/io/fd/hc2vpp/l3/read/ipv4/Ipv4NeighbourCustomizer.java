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

package io.fd.hc2vpp.l3.read.ipv4;

import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.NeighborOrigin.Dynamic;
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.NeighborOrigin.Static;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.utils.ip.read.IfaceDumpFilter;
import io.fd.hc2vpp.l3.utils.ip.read.IpNeighbourReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.vpp.jvpp.core.dto.IpNeighborDetails;
import io.fd.vpp.jvpp.core.dto.IpNeighborDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.types.IpNeighborFlags;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.NeighborKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Operational data read operation customizer for {@link Neighbor}<br>
 * Currently not supported in jvpp, so this is only dummy implementation<br>
 */
public class Ipv4NeighbourCustomizer extends IpNeighbourReader
        implements ListReaderCustomizer<Neighbor, NeighborKey, NeighborBuilder> {

    public Ipv4NeighbourCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                   @Nonnull final NamingContext interfaceContext) {
        super(interfaceContext, false, new DumpCacheManagerBuilder<IpNeighborDetailsReplyDump, IfaceDumpFilter>()
                .withExecutor(createNeighbourDumpExecutor(futureJVppCore))
                // cached with parent interface scope
                .withCacheKeyFactory(interfaceScopedCacheKeyFactory(IpNeighborDetailsReplyDump.class))
                .build());
    }

    @Override
    public NeighborBuilder getBuilder(InstanceIdentifier<Neighbor> id) {
        return new NeighborBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<Neighbor> id, NeighborBuilder builder, ReadContext ctx)
            throws ReadFailedException {

        final Ipv4AddressNoZone ip = id.firstKeyOf(Neighbor.class).getIp();

        final Optional<IpNeighborDetailsReplyDump> dumpOpt = interfaceNeighboursDump(id, ctx);

        if (dumpOpt.isPresent()) {
            dumpOpt.get().ipNeighborDetails
                    .stream()
                    .filter(ipNeighborDetails -> ip.equals(arrayToIpv4AddressNoZone(
                            ipNeighborDetails.neighbor.ipAddress.un.getIp4().ip4Address)))
                    .findFirst()
                    .ifPresent(ipNeighborDetails -> builder.setIp(arrayToIpv4AddressNoZone(
                            ipNeighborDetails.neighbor.ipAddress.un.getIp4().ip4Address))
                            .withKey(keyMapper().apply(ipNeighborDetails))
                            .setLinkLayerAddress(toPhysAddress(ipNeighborDetails.neighbor.macAddress.macaddress))
                            .setOrigin(ipNeighborDetails.neighbor.flags
                                    .contains(IpNeighborFlags.IpNeighborFlagsOptions.IP_API_NEIGHBOR_FLAG_STATIC)
                                    ? Static
                                    : Dynamic));
        }
    }

    @Override
    public List<NeighborKey> getAllIds(InstanceIdentifier<Neighbor> id, ReadContext context)
            throws ReadFailedException {
        return getNeighborKeys(interfaceNeighboursDump(id, context), keyMapper());
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<Neighbor> readData) {
        ((Ipv4Builder) builder).setNeighbor(readData);
    }

    private Function<IpNeighborDetails, NeighborKey> keyMapper() {
        return ipNeighborDetails -> new NeighborKey(
                arrayToIpv4AddressNoZone(ipNeighborDetails.neighbor.ipAddress.un.getIp4().ip4Address));
    }
}