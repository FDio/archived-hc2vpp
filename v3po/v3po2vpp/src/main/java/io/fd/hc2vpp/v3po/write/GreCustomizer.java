/*
 * Copyright (c) 2016 Intel and/or its affiliates.
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

package io.fd.hc2vpp.v3po.write;

import io.fd.hc2vpp.common.translate.util.AbstractInterfaceTypeCustomizer;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.GreTunnelAddDel;
import io.fd.jvpp.core.dto.GreTunnelAddDelReply;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.GreTunnel;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Gre;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreCustomizer extends AbstractInterfaceTypeCustomizer<Gre> implements JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(GreCustomizer.class);
    private final NamingContext interfaceContext;

    public GreCustomizer(final FutureJVppCore vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    private static GreTunnelAddDel getGreTunnelRequest(final byte isAdd, final IpAddressNoZone srcAddr,
                                                       final IpAddressNoZone dstAddr, final int outerFibId,
                                                       final byte isIpv6) {
        final GreTunnelAddDel greTunnelAddDel = new GreTunnelAddDel();
        greTunnelAddDel.isAdd = isAdd;
        greTunnelAddDel.tunnel = new io.fd.jvpp.core.types.GreTunnel();
        if (isIpv6==0) {
            greTunnelAddDel.tunnel.src =
                    AddressTranslator.INSTANCE.ipv4AddressNoZoneToAddress(srcAddr.getIpv4AddressNoZone());
            greTunnelAddDel.tunnel.dst =
                    AddressTranslator.INSTANCE.ipv4AddressNoZoneToAddress(dstAddr.getIpv4AddressNoZone());
        } else {
            greTunnelAddDel.tunnel.src =
                    AddressTranslator.INSTANCE.ipv6AddressToAddress(srcAddr.getIpv6AddressNoZone());
            greTunnelAddDel.tunnel.dst =
                    AddressTranslator.INSTANCE.ipv6AddressToAddress(dstAddr.getIpv6AddressNoZone());
        }
        greTunnelAddDel.tunnel.outerFibId = outerFibId;
        greTunnelAddDel.tunnel.isIpv6 = isIpv6;
        return greTunnelAddDel;
    }

    @Override
    protected Class<? extends InterfaceType> getExpectedInterfaceType() {
        return GreTunnel.class;
    }

    @Override
    protected final void writeInterface(@Nonnull final InstanceIdentifier<Gre> id, @Nonnull final Gre dataAfter,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String swIfName = id.firstKeyOf(Interface.class).getName();

        createGreTunnel(id, swIfName, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Gre> id, @Nonnull final Gre dataBefore,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String swIfName = id.firstKeyOf(Interface.class).getName();
        deleteGreTunnel(id, swIfName, dataBefore, writeContext);
    }

    private void createGreTunnel(final InstanceIdentifier<Gre> id, final String swIfName, final Gre gre,
                                 final WriteContext writeContext) throws WriteFailedException {
        final byte isIpv6 = (byte) (isIpv6(gre)
                ? 1
                : 0);
        int outerFibId = gre.getOuterFibId().intValue();

        LOG.debug("Setting gre tunnel for interface: {}. Gre: {}", swIfName, gre);
        final CompletionStage<GreTunnelAddDelReply> greAddDelTunnelReplyCompletionStage =
            getFutureJVpp().greTunnelAddDel(getGreTunnelRequest((byte) 1 /* is add */, gre.getSrc(),
                gre.getDst(), outerFibId, isIpv6));

        final GreTunnelAddDelReply reply =
                getReplyForCreate(greAddDelTunnelReplyCompletionStage.toCompletableFuture(), id, gre);
        LOG.debug("Gre tunnel set successfully for: {}, gre: {}", swIfName, gre);
        if (interfaceContext.containsName(reply.swIfIndex, writeContext.getMappingContext())) {
            // VPP keeps gre tunnels present even after they are delete(reserving ID for next tunnel)
            // This may cause inconsistencies in mapping context when configuring tunnels like this:
            // 1. Add tunnel 2. Delete tunnel 3. Read interfaces (reserved mapping e.g. gre_tunnel0 -> 6
            // will get into mapping context) 4. Add tunnel (this will add another mapping with the same
            // reserved ID and context is invalid)
            // That's why a check has to be performed here removing mapping gre_tunnel0 -> 6 mapping and storing
            // new name for that ID
            final String formerName = interfaceContext.getName(reply.swIfIndex, writeContext.getMappingContext());
            LOG.debug("Removing updated mapping of a gre tunnel, id: {}, former name: {}, new name: {}",
                    reply.swIfIndex, formerName, swIfName);
            interfaceContext.removeName(formerName, writeContext.getMappingContext());
        }
        // Add new interface to our interface context
        interfaceContext.addName(reply.swIfIndex, swIfName, writeContext.getMappingContext());
    }

    private boolean isIpv6(final Gre gre) {
        return gre.getSrc().getIpv4AddressNoZone() == null;
    }

    private void deleteGreTunnel(final InstanceIdentifier<Gre> id, final String swIfName, final Gre gre,
                                 final WriteContext writeContext) throws WriteFailedException {
        final byte isIpv6 = (byte) (isIpv6(gre)
                ? 1
                : 0);

        int outerFibId = gre.getOuterFibId().intValue();

        LOG.debug("Deleting gre tunnel for interface: {}. Gre: {}", swIfName, gre);
        final CompletionStage<GreTunnelAddDelReply> greAddDelTunnelReplyCompletionStage =
            getFutureJVpp().greTunnelAddDel(getGreTunnelRequest((byte) 0 /* is add */, gre.getSrc(),
                gre.getDst(), outerFibId, isIpv6));

        getReplyForDelete(greAddDelTunnelReplyCompletionStage.toCompletableFuture(), id);
        LOG.debug("Gre tunnel deleted successfully for: {}, gre: {}", swIfName, gre);
        // Remove interface from our interface context
        interfaceContext.removeName(swIfName, writeContext.getMappingContext());
    }
}
