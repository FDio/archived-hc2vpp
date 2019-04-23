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

package io.fd.hc2vpp.v3po.interfacesstate;

import static com.google.common.base.Preconditions.checkState;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.jvpp.core.dto.GreTunnelDetails;
import io.fd.jvpp.core.dto.GreTunnelDetailsReplyDump;
import io.fd.jvpp.core.dto.GreTunnelDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.GreTunnel;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.interfaces.state._interface.Gre;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.interfaces.state._interface.GreBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreCustomizer extends FutureJVppCustomizer
    implements InitializingReaderCustomizer<Gre, GreBuilder>, InterfaceDataTranslator, Ipv4Translator,
    Ipv6Translator {

    private static final Logger LOG = LoggerFactory.getLogger(GreCustomizer.class);
    private final NamingContext interfaceContext;
    private final InterfaceCacheDumpManager dumpManager;

    public GreCustomizer(@Nonnull final FutureJVppCore jvpp,
                         @Nonnull final NamingContext interfaceContext,
                         @Nonnull final InterfaceCacheDumpManager dumpManager) {
        super(jvpp);
        this.interfaceContext = interfaceContext;
        this.dumpManager = dumpManager;
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder,
                      @Nonnull Gre readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setGre(readValue);
    }

    @Nonnull
    @Override
    public GreBuilder getBuilder(@Nonnull InstanceIdentifier<Gre> id) {
        return new GreBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Gre> id,
                                      @Nonnull final GreBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
        if (!isInterfaceOfType(dumpManager, id, ctx, GreTunnel.class)) {
            return;
        }

        LOG.debug("Reading attributes for gre tunnel: {}", key.getName());
        // Dump just a single
        final GreTunnelDump request = new GreTunnelDump();
        request.swIfIndex = index;

        final CompletionStage<GreTunnelDetailsReplyDump> swInterfaceGreDetailsReplyDumpCompletionStage =
                getFutureJVpp().greTunnelDump(request);
        final GreTunnelDetailsReplyDump reply =
                getReplyForRead(swInterfaceGreDetailsReplyDumpCompletionStage.toCompletableFuture(), id);

        // VPP keeps gre tunnel interfaces even after they were deleted (optimization)
        // However there ar no longer any gre tunnel specific fields assigned to it and this call
        // returns nothing
        if (reply == null || reply.greTunnelDetails == null || reply.greTunnelDetails.isEmpty()) {
            LOG.debug(
                    "Gre tunnel {}, id {} has no attributes assigned in VPP. Probably is a leftover interface placeholder" +
                            "after delete", key.getName(), index);
            return;
        }

        checkState(reply.greTunnelDetails.size() == 1,
                "Unexpected number of returned gre tunnels: {} for tunnel: {}", reply.greTunnelDetails,
                key.getName());
        LOG.trace("Gre tunnel: {} attributes returned from VPP: {}", key.getName(), reply);

        final GreTunnelDetails swInterfaceGreDetails = reply.greTunnelDetails.get(0);
        if (swInterfaceGreDetails.tunnel.isIpv6 == 1) {
            builder.setDst(new IpAddressNoZone(
                    arrayToIpv4AddressNoZone(swInterfaceGreDetails.tunnel.dst.un.getIp6().ip6Address)));
            builder.setSrc(new IpAddressNoZone(
                    arrayToIpv6AddressNoZone(swInterfaceGreDetails.tunnel.src.un.getIp6().ip6Address)));
        } else {
            builder.setDst(new IpAddressNoZone(
                    arrayToIpv4AddressNoZone(swInterfaceGreDetails.tunnel.dst.un.getIp4().ip4Address)));
            builder.setSrc(new IpAddressNoZone(
                    arrayToIpv4AddressNoZone(swInterfaceGreDetails.tunnel.src.un.getIp4().ip4Address)));
        }
        builder.setOuterFibId((long) swInterfaceGreDetails.tunnel.outerFibId);
        LOG.debug("Gre tunnel: {}, id: {} attributes read as: {}", key.getName(), index, builder);
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.interfaces._interface.Gre> init(
            @Nonnull final InstanceIdentifier<Gre> id, @Nonnull final Gre readValue, @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.interfaces._interface.GreBuilder()
                        .setDst(readValue.getDst())
                        .setSrc(readValue.getSrc())
                        .setOuterFibId(readValue.getOuterFibId())
                        .build());
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.interfaces._interface.Gre> getCfgId(
            final InstanceIdentifier<Gre> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                .augmentation(VppInterfaceAugmentation.class)
                .child(org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.interfaces._interface.Gre.class);
    }
}
