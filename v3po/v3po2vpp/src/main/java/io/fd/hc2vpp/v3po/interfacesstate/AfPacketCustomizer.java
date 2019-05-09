/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.StaticCacheKeyFactory;
import io.fd.jvpp.core.dto.AfPacketDetails;
import io.fd.jvpp.core.dto.AfPacketDetailsReplyDump;
import io.fd.jvpp.core.dto.AfPacketDump;
import io.fd.jvpp.core.dto.SwInterfaceDetails;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces.state._interface.AfPacket;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces.state._interface.AfPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AfPacketCustomizer implements InitializingReaderCustomizer<AfPacket, AfPacketBuilder>,
    InterfaceDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AfPacketCustomizer.class);
    private NamingContext interfaceContext;
    private final InterfaceCacheDumpManager dumpManager;
    private final DumpCacheManager<AfPacketDetailsReplyDump, Void> afPacketDumpManager;

    public AfPacketCustomizer(@Nonnull final FutureJVppCore jvpp,
                              @Nonnull final NamingContext interfaceContext,
                              @Nonnull final InterfaceCacheDumpManager dumpManager) {
        this.interfaceContext = interfaceContext;
        this.dumpManager = dumpManager;
        this.afPacketDumpManager =
            new DumpCacheManager.DumpCacheManagerBuilder<AfPacketDetailsReplyDump, Void>()
                .withCacheKeyFactory(new StaticCacheKeyFactory(AfPacketCustomizer.class.getName() + "_dump",
                    AfPacketDetailsReplyDump.class))
                .withExecutor((identifier, params) -> {
                    final CompletionStage<AfPacketDetailsReplyDump> cs = jvpp.afPacketDump(new AfPacketDump());
                    return getReplyForRead(cs.toCompletableFuture(), identifier);
                }).build();
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder, @Nonnull AfPacket readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setAfPacket(readValue);
    }

    @Nonnull
    @Override
    public AfPacketBuilder getBuilder(@Nonnull InstanceIdentifier<AfPacket> id) {
        return new AfPacketBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<AfPacket> id,
                                      @Nonnull final AfPacketBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
        final SwInterfaceDetails ifcDetails = dumpManager.getInterfaceDetail(id, ctx, key.getName());

        if (!isInterfaceOfType(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.AfPacket.class,
            ifcDetails)) {
            return;
        }

        final AfPacketDetailsReplyDump dump = afPacketDumpManager.getDump(id, ctx.getModificationCache())
            .orElse(new AfPacketDetailsReplyDump());
        // Relying here that parent InterfaceCustomizer was invoked first to fill in the context with initial ifc mapping
        final AfPacketDetails afPacketDetails = dump.afPacketDetails.stream()
            .filter(detail -> detail.swIfIndex == index)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(format("AfPacket interface %s not found", key.getName())));
        LOG.trace("AfPacket interface: {} attributes returned from VPP: {}", key.getName(), afPacketDetails);

        builder.setMac(new PhysAddress(vppPhysAddrToYang(ifcDetails.l2Address)));
        builder.setHostInterfaceName(toString(afPacketDetails.hostIfName));

        LOG.debug("AfPacket interface: {}, id: {} attributes read as: {}", key.getName(), index, builder);
    }

    @Nonnull
    @Override
    public Initialized<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.AfPacket> init(
        @Nonnull final InstanceIdentifier<AfPacket> id,
        @Nonnull final AfPacket readValue,
        @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
            new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.AfPacketBuilder()
                .setHostInterfaceName(readValue.getHostInterfaceName())
                .setMac(readValue.getMac())
                .build());
    }

    @VisibleForTesting
    static InstanceIdentifier<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.AfPacket> getCfgId(
        final InstanceIdentifier<AfPacket> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
            .augmentation(VppInterfaceAugmentation.class)
            .child(
                org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.AfPacket.class);
    }
}
