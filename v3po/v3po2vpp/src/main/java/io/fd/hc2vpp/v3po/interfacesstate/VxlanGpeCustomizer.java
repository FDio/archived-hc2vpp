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
import io.fd.jvpp.core.dto.VxlanGpeTunnelDetails;
import io.fd.jvpp.core.dto.VxlanGpeTunnelDetailsReplyDump;
import io.fd.jvpp.core.dto.VxlanGpeTunnelDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VxlanGpeNextProtocol;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VxlanGpeTunnel;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VxlanGpeVni;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces.state._interface.VxlanGpe;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces.state._interface.VxlanGpeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VxlanGpeCustomizer extends FutureJVppCustomizer
    implements InitializingReaderCustomizer<VxlanGpe, VxlanGpeBuilder>, InterfaceDataTranslator, JvppReplyConsumer,
    Ipv4Translator, Ipv6Translator {

    private static final Logger LOG = LoggerFactory.getLogger(VxlanGpeCustomizer.class);
    private final NamingContext interfaceContext;
    private final InterfaceCacheDumpManager dumpManager;

    public VxlanGpeCustomizer(@Nonnull final FutureJVppCore jvpp,
                              @Nonnull final NamingContext interfaceContext,
                              @Nonnull final InterfaceCacheDumpManager dumpManager) {
        super(jvpp);
        this.interfaceContext = interfaceContext;
        this.dumpManager = dumpManager;
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder,
                      @Nonnull VxlanGpe readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setVxlanGpe(readValue);
    }

    @Nonnull
    @Override
    public VxlanGpeBuilder getBuilder(@Nonnull InstanceIdentifier<VxlanGpe> id) {
        return new VxlanGpeBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<VxlanGpe> id,
                                      @Nonnull final VxlanGpeBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {

        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
        if (!isInterfaceOfType(dumpManager, id, ctx, VxlanGpeTunnel.class)) {
            return;
        }

        LOG.debug("Reading attributes for VxlanGpe tunnel: {}", key.getName());
        // Dump just a single
        final VxlanGpeTunnelDump request = new VxlanGpeTunnelDump();
        request.swIfIndex = index;

        final CompletionStage<VxlanGpeTunnelDetailsReplyDump> swInterfaceVxlanGpeDetailsReplyDumpCompletionStage =
                getFutureJVpp().vxlanGpeTunnelDump(request);
        final VxlanGpeTunnelDetailsReplyDump reply =
                getReplyForRead(swInterfaceVxlanGpeDetailsReplyDumpCompletionStage.toCompletableFuture(),
                        id);

        // VPP keeps VxlanGpe tunnel interfaces even after they were deleted (optimization)
        // However there are no longer any VxlanGpe tunnel specific fields assigned to it and this call
        // returns nothing
        if (reply == null || reply.vxlanGpeTunnelDetails == null || reply.vxlanGpeTunnelDetails.isEmpty()) {
            LOG.debug(
                    "VxlanGpe tunnel {}, id {} has no attributes assigned in VPP. Probably is a leftover interface placeholder" +
                            "after delete", key.getName(), index);
            return;
        }

        checkState(reply.vxlanGpeTunnelDetails.size() == 1,
                "Unexpected number of returned VxlanGpe tunnels: {} for tunnel: {}", reply.vxlanGpeTunnelDetails,
                key.getName());
        LOG.trace("VxlanGpe tunnel: {} attributes returned from VPP: {}", key.getName(), reply);

        final VxlanGpeTunnelDetails swInterfaceVxlanGpeDetails = reply.vxlanGpeTunnelDetails.get(0);
        if (swInterfaceVxlanGpeDetails.isIpv6 == 1) {
            builder.setRemote(new IpAddressNoZone(arrayToIpv6AddressNoZone(swInterfaceVxlanGpeDetails.remote)));
            builder.setLocal(new IpAddressNoZone(arrayToIpv6AddressNoZone(swInterfaceVxlanGpeDetails.local)));
        } else {
            builder.setRemote(new IpAddressNoZone(arrayToIpv4AddressNoZone(swInterfaceVxlanGpeDetails.remote)));
            builder.setLocal(new IpAddressNoZone(arrayToIpv4AddressNoZone(swInterfaceVxlanGpeDetails.local)));
        }
        builder.setVni(new VxlanGpeVni((long) swInterfaceVxlanGpeDetails.vni));
        builder.setNextProtocol(VxlanGpeNextProtocol.forValue(swInterfaceVxlanGpeDetails.protocol));
        builder.setEncapVrfId((long) swInterfaceVxlanGpeDetails.encapVrfId);
        builder.setDecapVrfId((long) swInterfaceVxlanGpeDetails.decapVrfId);
        LOG.debug("VxlanGpe tunnel: {}, id: {} attributes read as: {}", key.getName(), index, builder);
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.VxlanGpe> init(
            @Nonnull final InstanceIdentifier<VxlanGpe> id, @Nonnull final VxlanGpe readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.VxlanGpeBuilder()
                        .setLocal(readValue.getLocal())
                        .setRemote(readValue.getRemote())
                        .setVni(new VxlanGpeVni(readValue.getVni()))
                        .setNextProtocol(readValue.getNextProtocol())
                        .setEncapVrfId(readValue.getEncapVrfId())
                        .setDecapVrfId(readValue.getDecapVrfId())
                        .build());
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.VxlanGpe> getCfgId(
            final InstanceIdentifier<VxlanGpe> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                .augmentation(VppInterfaceAugmentation.class)
                .child(org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.VxlanGpe.class);
    }
}
