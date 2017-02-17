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

package io.fd.hc2vpp.v3po.interfaces.ip.v4;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfaces.ip.IpWriter;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpNeighborAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.NeighborKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.RoutingBaseAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Customizer for writing {@link Neighbor} for {@link Ipv4}.
 */
public class Ipv4NeighbourCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<Neighbor, NeighborKey>, ByteDataTranslator, AddressTranslator,
        IpWriter, JvppReplyConsumer {


    private static final Logger LOG = LoggerFactory.getLogger(Ipv4NeighbourCustomizer.class);
    private final NamingContext interfaceContext;

    public Ipv4NeighbourCustomizer(final FutureJVppCore futureJVppCore, final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull InstanceIdentifier<Neighbor> id, @Nonnull Neighbor data,
                                       @Nonnull WriteContext writeContext)
            throws WriteFailedException {

        LOG.debug("Processing request for Neighbour {} write", id);

        addDelNeighbour(id, () -> {
            IpNeighborAddDel request = preBindIpv4Request(true);

            request.dstAddress = ipv4AddressNoZoneToArray(data.getIp());
            request.macAddress = parseMac(data.getLinkLayerAddress().getValue());
            request.swIfIndex = interfaceContext
                    .getIndex(id.firstKeyOf(Interface.class).getName(), writeContext.getMappingContext());

            bindVrfIfSpecified(writeContext, id, request);

            return request;
        }, getFutureJVpp());
        LOG.debug("Neighbour {} successfully written", id);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull InstanceIdentifier<Neighbor> id, @Nonnull Neighbor dataBefore,
                                        @Nonnull Neighbor dataAfter,
                                        @Nonnull WriteContext writeContext) throws WriteFailedException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull InstanceIdentifier<Neighbor> id, @Nonnull Neighbor data,
                                        @Nonnull WriteContext writeContext)
            throws WriteFailedException {

        LOG.debug("Processing request for Neighbour {} delete", id);

        addDelNeighbour(id, () -> {
            IpNeighborAddDel request = preBindIpv4Request(false);

            request.dstAddress = ipv4AddressNoZoneToArray(data.getIp());
            request.macAddress = parseMac(data.getLinkLayerAddress().getValue());
            request.swIfIndex = interfaceContext
                    .getIndex(id.firstKeyOf(Interface.class).getName(), writeContext.getMappingContext());

            bindVrfIfSpecified(writeContext, id, request);

            return request;
        }, getFutureJVpp());
        LOG.debug("Neighbour {} successfully deleted", id);
    }

    private void bindVrfIfSpecified(final WriteContext writeContext,
                                    final InstanceIdentifier<Neighbor> id,
                                    IpNeighborAddDel request) {
        final Optional<Interface> optIface = writeContext.readBefore(id.firstIdentifierOf(Interface.class));

        // if routing set, reads vrf-id
        // uses java.util.Optional(its internal behaviour suites this use better than guava one)
        if (optIface.isPresent()) {
            java.util.Optional.of(optIface.get())
                    .map(iface -> iface.getAugmentation(VppInterfaceAugmentation.class))
                    .map(VppInterfaceAugmentation::getRouting)
                    .map(RoutingBaseAttributes::getIpv4VrfId)
                    .ifPresent(vrf -> request.vrfId = vrf.byteValue());
        }
    }
}