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

package io.fd.hc2vpp.v3po.interfacesstate;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.primitives.UnsignedInts;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.vpp.jvpp.core.dto.VxlanTunnelDetails;
import io.fd.vpp.jvpp.core.dto.VxlanTunnelDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.VxlanTunnelDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.L2Input;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.NshProxy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.VxlanTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.interfaces.state._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.interfaces.state._interface.VxlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VxlanCustomizer extends FutureJVppCustomizer
    implements InitializingReaderCustomizer<Vxlan, VxlanBuilder>, InterfaceDataTranslator, JvppReplyConsumer,
    Ipv4Translator, Ipv6Translator {

    private static final Logger LOG = LoggerFactory.getLogger(VxlanCustomizer.class);
    private final NamingContext interfaceContext;
    private final InterfaceCacheDumpManager dumpManager;

    public VxlanCustomizer(@Nonnull final FutureJVppCore jvpp, @Nonnull final NamingContext interfaceContext,
                           @Nonnull final InterfaceCacheDumpManager dumpManager) {
        super(jvpp);
        this.interfaceContext = interfaceContext;
        this.dumpManager = dumpManager;
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
        if (!isInterfaceOfType(dumpManager, id, ctx, VxlanTunnel.class)) {
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
            builder.setDst(new IpAddressNoZone(arrayToIpv6AddressNoZone(swInterfaceVxlanDetails.dstAddress)));
            builder.setSrc(new IpAddressNoZone(arrayToIpv6AddressNoZone(swInterfaceVxlanDetails.srcAddress)));
        } else {
            builder.setDst(new IpAddressNoZone(arrayToIpv4AddressNoZone(swInterfaceVxlanDetails.dstAddress)));
            builder.setSrc(new IpAddressNoZone(arrayToIpv4AddressNoZone(swInterfaceVxlanDetails.srcAddress)));
        }
        builder.setEncapVrfId(new VniReference(UnsignedInts.toLong(swInterfaceVxlanDetails.encapVrfId)));
        builder.setVni(new VxlanVni(UnsignedInts.toLong(swInterfaceVxlanDetails.vni)));
        switch (swInterfaceVxlanDetails.decapNextIndex) {
            case 1:
                builder.setDecapNext(L2Input.class);
                break;
            case 2:
                builder.setDecapNext(NshProxy.class);
                break;
            default:
                LOG.trace("Unsupported decap next index for vxlan: {}", swInterfaceVxlanDetails.decapNextIndex);
                return;
        }
        LOG.debug("Vxlan tunnel: {}, id: {} attributes read as: {}", key.getName(), index, builder);
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.interfaces._interface.Vxlan> init(
            @Nonnull final InstanceIdentifier<Vxlan> id, @Nonnull final Vxlan readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.interfaces._interface.VxlanBuilder()
                        .setDst(readValue.getDst())
                        .setSrc(readValue.getSrc())
                        .setEncapVrfId(readValue.getEncapVrfId())
                        .setVni(new VxlanVni(readValue.getVni()))
                        .setDecapNext(readValue.getDecapNext())
                        .build());
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.interfaces._interface.Vxlan> getCfgId(
            final InstanceIdentifier<Vxlan> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                .augmentation(VppInterfaceAugmentation.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.interfaces._interface.Vxlan.class);
    }
}
