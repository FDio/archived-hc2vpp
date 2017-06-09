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

package io.fd.hc2vpp.l3.write.ipv4.subinterface;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.utils.ip.write.IpWriter;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpNeighborAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.sub._interface.ip4.attributes.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.sub._interface.ip4.attributes.ipv4.NeighborKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubInterfaceIpv4NeighbourCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<Neighbor, NeighborKey>, ByteDataTranslator, AddressTranslator, IpWriter,
        JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(SubInterfaceIpv4NeighbourCustomizer.class);
    private final NamingContext interfaceContext;

    public SubInterfaceIpv4NeighbourCustomizer(final FutureJVppCore futureJVppCore,
                                               final NamingContext interfaceContext) {
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
            request.swIfIndex = subInterfaceIndex(id, interfaceContext, writeContext.getMappingContext());
            // we don't have support for sub-interface routing, so not setting vrf

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
            request.swIfIndex = subInterfaceIndex(id, interfaceContext, writeContext.getMappingContext());

            //TODO HONEYCOMB-182 if it is necessary for future use ,make adjustments to be able to set vrfid
            //request.vrfId
            return request;
        }, getFutureJVpp());
        LOG.debug("Neighbour {} successfully deleted", id);
    }

}
