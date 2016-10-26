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

package io.fd.honeycomb.translate.v3po.interfacesstate;

import static com.google.common.base.Preconditions.checkState;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.core.dto.VxlanTunnelDetails;
import io.fd.vpp.jvpp.core.dto.VxlanTunnelDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.VxlanTunnelDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VxlanTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces.state._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces.state._interface.VxlanBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VxlanCustomizer extends FutureJVppCustomizer
        implements InitializingReaderCustomizer<Vxlan, VxlanBuilder>, InterfaceDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(VxlanCustomizer.class);
    private final NamingContext interfaceContext;

    public VxlanCustomizer(@Nonnull final FutureJVppCore jvpp, @Nonnull final NamingContext interfaceContext) {
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
        final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
        if (!isInterfaceOfType(getFutureJVpp(), ctx.getModificationCache(), id, index, VxlanTunnel.class, LOG)) {
            return;
        }

        LOG.debug("Reading attributes for vxlan tunnel: {}", key.getName());
        // Dump just a single
        final VxlanTunnelDump request = new VxlanTunnelDump();
        request.swIfIndex = index;

        final CompletionStage<VxlanTunnelDetailsReplyDump> swInterfaceVxlanDetailsReplyDumpCompletionStage =
                getFutureJVpp().vxlanTunnelDump(request);
        final VxlanTunnelDetailsReplyDump reply =
                getReplyForRead(swInterfaceVxlanDetailsReplyDumpCompletionStage.toCompletableFuture(), id);

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
                "Unexpected number of returned vxlan tunnels: {} for tunnel: {}", reply.vxlanTunnelDetails,
                key.getName());
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
        builder.setVni(new VxlanVni((long) swInterfaceVxlanDetails.vni));
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

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.Vxlan> init(
            @Nonnull final InstanceIdentifier<Vxlan> id, @Nonnull final Vxlan readValue, @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.VxlanBuilder()
                        .setDst(readValue.getDst())
                        .setSrc(readValue.getSrc())
                        .setEncapVrfId(readValue.getEncapVrfId())
                        .setVni(new VxlanVni(readValue.getVni()))
                        .build());
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.Vxlan> getCfgId(
            final InstanceIdentifier<Vxlan> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                .augmentation(VppInterfaceAugmentation.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.Vxlan.class);
    }
}
