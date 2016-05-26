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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate;

import static com.google.common.base.Preconditions.checkState;

import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VxlanBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.VxlanTunnelDetails;
import org.openvpp.jvpp.dto.VxlanTunnelDetailsReplyDump;
import org.openvpp.jvpp.dto.VxlanTunnelDump;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VxlanCustomizer extends FutureJVppCustomizer
        implements ChildReaderCustomizer<Vxlan, VxlanBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(VxlanCustomizer.class);
    private NamingContext interfaceContext;

    public VxlanCustomizer(@Nonnull final FutureJVpp jvpp, @Nonnull final NamingContext interfaceContext) {
        super(jvpp);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder,
                      @Nonnull Vxlan readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setVxlan(readValue);
    }

    @Nonnull
    @Override
    public VxlanBuilder getBuilder(@Nonnull InstanceIdentifier<Vxlan> id) {
        return new VxlanBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Vxlan> id,
                                      @Nonnull final VxlanBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        final InterfaceKey key = id.firstKeyOf(Interface.class);
        // Relying here that parent InterfaceCustomizer was invoked first (PREORDER)
        // to fill in the context with initial ifc mapping
        final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
        if (!InterfaceUtils.isInterfaceOfType(ctx.getModificationCache(), index, VxlanTunnel.class)) {
            return;
        }

        LOG.debug("Reading attributes for vxlan tunnel: {}", key.getName());
        // Dump just a single
        final VxlanTunnelDump request = new VxlanTunnelDump();
        request.swIfIndex = index;

        final CompletionStage<VxlanTunnelDetailsReplyDump> swInterfaceVxlanDetailsReplyDumpCompletionStage =
            getFutureJVpp().vxlanTunnelDump(request);
        final VxlanTunnelDetailsReplyDump reply =
            TranslateUtils.getReply(swInterfaceVxlanDetailsReplyDumpCompletionStage.toCompletableFuture());

        // VPP keeps vxlan tunnel interfaces even after they were deleted (optimization)
        // However there ar no longer any vxlan tunnel specific fields assigned to it and this call
        // returns nothing
        if (reply == null || reply.vxlanTunnelDetails == null || reply.vxlanTunnelDetails.isEmpty()) {
            LOG.debug(
                "Vxlan tunnel {}, id {} has no attributes assigned in VPP. Probably is a leftover interface placeholder" +
                    "after delete", key.getName(), index);
            return;
        }

        checkState(reply.vxlanTunnelDetails.size() == 1,
            "Unexpected number of returned vxlan tunnels: {} for tunnel: {}", reply.vxlanTunnelDetails, key.getName());
        LOG.trace("Vxlan tunnel: {} attributes returned from VPP: {}", key.getName(), reply);

        final VxlanTunnelDetails swInterfaceVxlanDetails = reply.vxlanTunnelDetails.get(0);
        if (swInterfaceVxlanDetails.isIpv6 == 1) {
            final Ipv6Address dstIpv6 =
                new Ipv6Address(parseAddress(swInterfaceVxlanDetails.dstAddress).getHostAddress());
            builder.setDst(new IpAddress(dstIpv6));
            final Ipv6Address srcIpv6 =
                new Ipv6Address(parseAddress(swInterfaceVxlanDetails.srcAddress).getHostAddress());
            builder.setSrc(new IpAddress(srcIpv6));
        } else {
            final byte[] dstBytes = Arrays.copyOfRange(swInterfaceVxlanDetails.dstAddress, 0, 4);
            final Ipv4Address dstIpv4 = new Ipv4Address(parseAddress(dstBytes).getHostAddress());
            builder.setDst(new IpAddress(dstIpv4));
            final byte[] srcBytes = Arrays.copyOfRange(swInterfaceVxlanDetails.srcAddress, 0, 4);
            final Ipv4Address srcIpv4 = new Ipv4Address(parseAddress(srcBytes).getHostAddress());
            builder.setSrc(new IpAddress(srcIpv4));
        }
        builder.setEncapVrfId((long) swInterfaceVxlanDetails.encapVrfId);
        builder.setVni((long) swInterfaceVxlanDetails.vni);
        LOG.debug("Vxlan tunnel: {}, id: {} attributes read as: {}", key.getName(), index, builder);
    }

    @Nonnull
    private static InetAddress parseAddress(@Nonnull final byte[] addr) {
        try {
            return InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot create InetAddress from " + Arrays.toString(addr), e);
        }
    }
}
