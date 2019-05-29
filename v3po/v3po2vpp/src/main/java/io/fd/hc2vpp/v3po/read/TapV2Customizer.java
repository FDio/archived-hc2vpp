/*
 * Copyright (c) 2018 Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.v3po.read;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.MacTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.read.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.StaticCacheKeyFactory;
import io.fd.jvpp.core.dto.SwInterfaceDetails;
import io.fd.jvpp.core.dto.SwInterfaceTapV2Details;
import io.fd.jvpp.core.dto.SwInterfaceTapV2DetailsReplyDump;
import io.fd.jvpp.core.dto.SwInterfaceTapV2Dump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.TapV2;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.TapV2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TapV2Customizer extends FutureJVppCustomizer
        implements InitializingReaderCustomizer<TapV2, TapV2Builder>, InterfaceDataTranslator, JvppReplyConsumer,
        MacTranslator, Ipv4Translator, Ipv6Translator{

    private static final Logger LOG = LoggerFactory.getLogger(TapV2Customizer.class);
    private NamingContext interfaceContext;
    private final InterfaceCacheDumpManager dumpManager;
    private final DumpCacheManager<SwInterfaceTapV2DetailsReplyDump, Void> tapV2DumpManager;

    public TapV2Customizer(@Nonnull final FutureJVppCore jvpp,
                           @Nonnull final NamingContext interfaceContext,
                           @Nonnull final InterfaceCacheDumpManager dumpManager) {
        super(jvpp);
        this.interfaceContext = interfaceContext;
        this.dumpManager = dumpManager;
        this.tapV2DumpManager = new DumpCacheManager.DumpCacheManagerBuilder<SwInterfaceTapV2DetailsReplyDump, Void>()
                .withCacheKeyFactory(new StaticCacheKeyFactory(TapV2Customizer.class.getName() + "_dump",
                        SwInterfaceTapV2DetailsReplyDump.class))
                .withExecutor((identifier, params) -> {
                    // Full TapV2 dump has to be performed here, no filter or anything is here to help so at least we cache it
                    return getReplyForRead(getFutureJVpp()
                            .swInterfaceTapV2Dump(new SwInterfaceTapV2Dump()).toCompletableFuture(), identifier);
                }).build();
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder, @Nonnull TapV2 readValue) {
        ((VppInterfaceAugmentationBuilder) parentBuilder).setTapV2(readValue);
    }

    @Nonnull
    @Override
    public TapV2Builder getBuilder(@Nonnull InstanceIdentifier<TapV2> id) {
        return new TapV2Builder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<TapV2> id,
                                      @Nonnull final TapV2Builder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {

        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
        if (!isInterfaceOfType(dumpManager, id, ctx,
                org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.TapV2.class)) {
            return;
        }

        LOG.debug("Reading attributes for tapV2 interface: {}", key.getName());
        final SwInterfaceTapV2DetailsReplyDump reply = tapV2DumpManager.getDump(id, ctx.getModificationCache())
                .orElse(new SwInterfaceTapV2DetailsReplyDump());

        final Optional<SwInterfaceTapV2Details> detail = reply.swInterfaceTapV2Details.stream()
                .filter(d -> d.swIfIndex == index)
                .findAny();

        checkState(detail.isPresent(), "TapV2 interface for index %s not found", index);
        final SwInterfaceTapV2Details swInterfaceTapV2Details = detail.get();

        LOG.trace("TapV2 interface: {} attributes returned from VPP: {}", key.getName(), swInterfaceTapV2Details);
        if (swInterfaceTapV2Details.devName != null && swInterfaceTapV2Details.devName[0] != 0) {
            builder.setDeviceName(toString(swInterfaceTapV2Details.devName));
        } else {
            builder.setDeviceName(null);
        }

        if (swInterfaceTapV2Details.hostBridge != null && swInterfaceTapV2Details.hostBridge[0] != 0) {
            builder.setHostBridge(toString(swInterfaceTapV2Details.hostBridge));
        } else {
            builder.setHostBridge(null);
        }

        if (swInterfaceTapV2Details.hostMacAddr != null &&
                !ByteDataTranslator.INSTANCE.isArrayZeroed(swInterfaceTapV2Details.hostMacAddr)) {
            builder.setHostMac(toPhysAddress(swInterfaceTapV2Details.hostMacAddr));
        } else {
            builder.setHostMac(null);
        }

        if (swInterfaceTapV2Details.hostIfName != null && swInterfaceTapV2Details.hostIfName[0] != 0) {
            builder.setHostInterfaceName(toString(swInterfaceTapV2Details.hostIfName));
        } else {
            builder.setHostInterfaceName(null);
        }

        if (swInterfaceTapV2Details.hostIp4Addr != null && swInterfaceTapV2Details.hostIp4PrefixLen != 0) {
            builder.setHostIpv4Address(
                    toIpv4Prefix(swInterfaceTapV2Details.hostIp4Addr, swInterfaceTapV2Details.hostIp4PrefixLen));
        } else {
            builder.setHostIpv4Address(null);
        }

        if (swInterfaceTapV2Details.hostIp6Addr != null && swInterfaceTapV2Details.hostIp6PrefixLen != 0) {
            builder.setHostIpv6Address(
                    toIpv6Prefix(swInterfaceTapV2Details.hostIp6Addr,
                            Byte.toUnsignedInt(swInterfaceTapV2Details.hostIp6PrefixLen)));
        } else {
            builder.setHostIpv6Address(null);
        }

        if (swInterfaceTapV2Details.hostNamespace != null && swInterfaceTapV2Details.hostNamespace[0] != 0) {
            builder.setHostNamespace(toString(swInterfaceTapV2Details.hostNamespace));
        } else {
            builder.setHostNamespace(null);
        }


        builder.setRxRingSize(Short.toUnsignedInt(swInterfaceTapV2Details.rxRingSz));
        builder.setTxRingSize(Short.toUnsignedInt(swInterfaceTapV2Details.txRingSz));
        final SwInterfaceDetails ifcDetails = dumpManager.getInterfaceDetail(id, ctx, key.getName());

        if (ifcDetails.tag[0] != 0) { // tag supplied
            builder.setTag(toString(ifcDetails.tag));
        }
        LOG.debug("TapV2 interface: {}, id: {} attributes read as: {}", key.getName(), index, builder);
    }

    @Override
    public Initialized<TapV2> init(@Nonnull final InstanceIdentifier<TapV2> id, @Nonnull final TapV2 readValue,
                                   @Nonnull final ReadContext ctx) {
        // The MAC address & tag is set from interface details, those details are retrieved from cache
        final InterfaceKey key = id.firstKeyOf(Interface.class);

        final SwInterfaceDetails ifcDetails;
        try {
            ifcDetails = dumpManager.getInterfaceDetail(id, ctx, key.getName());
        } catch (ReadFailedException e) {
            throw new IllegalStateException(format("Unable to read interface %s", key.getName()), e);
        }

        return Initialized.create(getCfgId(id),
                new TapV2Builder()
                        .setMac(new PhysAddress(vppPhysAddrToYang(ifcDetails.l2Address)))
                        .setHostInterfaceName(readValue.getHostInterfaceName())
                        .setTag(ifcDetails.tag[0] == 0
                                ? null
                                : toString(ifcDetails.tag))
                        .setHostBridge(readValue.getHostBridge())
                        .setHostIpv4Address(readValue.getHostIpv4Address())
                        .setHostIpv6Address(readValue.getHostIpv6Address())
                        .setRxRingSize(readValue.getRxRingSize())
                        .setTxRingSize(readValue.getTxRingSize())
                        .setHostMac(readValue.getHostMac())
                        .setHostNamespace(readValue.getHostNamespace())
                        .build());
    }

    private InstanceIdentifier<TapV2> getCfgId(final InstanceIdentifier<TapV2> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                .augmentation(VppInterfaceAugmentation.class)
                .child(TapV2.class);
    }
}
