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

package io.fd.hc2vpp.v3po.read;

import static java.lang.String.format;

import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
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
import io.fd.jvpp.core.dto.SwInterfaceVhostUserDetails;
import io.fd.jvpp.core.dto.SwInterfaceVhostUserDetailsReplyDump;
import io.fd.jvpp.core.dto.SwInterfaceVhostUserDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.math.BigInteger;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VhostUserRole;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.VhostUser;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.VhostUserBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VhostUserCustomizer implements InitializingReaderCustomizer<VhostUser, VhostUserBuilder>,
        InterfaceDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(VhostUserCustomizer.class);
    private NamingContext interfaceContext;
    private final InterfaceCacheDumpManager dumpManager;
    private final DumpCacheManager<SwInterfaceVhostUserDetailsReplyDump, Void> vhostDumpManager;

    public VhostUserCustomizer(@Nonnull final FutureJVppCore jvpp,
                               @Nonnull final NamingContext interfaceContext,
                               @Nonnull final InterfaceCacheDumpManager dumpManager) {
        this.interfaceContext = interfaceContext;
        this.dumpManager = dumpManager;
        this.vhostDumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<SwInterfaceVhostUserDetailsReplyDump, Void>()
                        .withCacheKeyFactory(new StaticCacheKeyFactory(VhostUserCustomizer.class.getName() + "_dump",
                                SwInterfaceVhostUserDetailsReplyDump.class))
                        .withExecutor((identifier, params) -> {
                            final CompletionStage<SwInterfaceVhostUserDetailsReplyDump>
                                    swInterfaceVhostUserDetailsReplyDumpCompletionStage =
                                    jvpp.swInterfaceVhostUserDump(new SwInterfaceVhostUserDump());
                            return getReplyForRead(
                                    swInterfaceVhostUserDetailsReplyDumpCompletionStage.toCompletableFuture(),
                                    identifier);
                        }).build();
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder, @Nonnull VhostUser readValue) {
        ((VppInterfaceAugmentationBuilder) parentBuilder).setVhostUser(readValue);
    }

    @Nonnull
    @Override
    public VhostUserBuilder getBuilder(@Nonnull InstanceIdentifier<VhostUser> id) {
        return new VhostUserBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<VhostUser> id,
                                      @Nonnull final VhostUserBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {

        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
        final SwInterfaceDetails ifcDetails = dumpManager.getInterfaceDetail(id, ctx, key.getName());


        if (!isInterfaceOfType(
                org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VhostUser.class,
                ifcDetails)) {
            return;
        }

        LOG.debug("Reading attributes for vhpost user interface: {}", key.getName());


        final SwInterfaceVhostUserDetailsReplyDump dump =
                vhostDumpManager.getDump(id, ctx.getModificationCache())
                        .orElse(new SwInterfaceVhostUserDetailsReplyDump());

        // Relying here that parent InterfaceCustomizer was invoked first to fill in the context with initial ifc mapping
        final SwInterfaceVhostUserDetails swInterfaceVhostUserDetails = dump.swInterfaceVhostUserDetails.stream()
                .filter(detail -> detail.swIfIndex == index)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        format("Vhost user for interface %s not found", key.getName())));
        LOG.trace("Vhost user interface: {} attributes returned from VPP: {}", key.getName(),
                swInterfaceVhostUserDetails);

        builder.setRole(swInterfaceVhostUserDetails.isServer == 1
                ? VhostUserRole.Server
                : VhostUserRole.Client);
        builder.setFeatures(BigInteger.valueOf(swInterfaceVhostUserDetails.features));
        builder.setNumMemoryRegions((long) swInterfaceVhostUserDetails.numRegions);
        builder.setSocket(toString(swInterfaceVhostUserDetails.sockFilename));
        builder.setVirtioNetHdrSize((long) swInterfaceVhostUserDetails.virtioNetHdrSz);
        // TODO: map error code to meaningful message after VPP-436 is done
        builder.setConnectError(Integer.toString(swInterfaceVhostUserDetails.sockErrno));
        if (ifcDetails.tag[0] != 0) { // tag supplied
            builder.setTag(toString(ifcDetails.tag));
        }

        LOG.debug("Vhost user interface: {}, id: {} attributes read as: {}", key.getName(), index, builder);
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.VhostUser> init(
            @Nonnull final InstanceIdentifier<VhostUser> id,
            @Nonnull final VhostUser readValue,
            @Nonnull final ReadContext ctx) {
        // The tag is set from interface details, those details are retrieved from cache
        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final SwInterfaceDetails ifcDetails;
        try {
            ifcDetails = dumpManager.getInterfaceDetail(id, ctx, key.getName());
        } catch (ReadFailedException e) {
            throw new IllegalStateException(format("Unable to find VHost interface %s", key.getName()), e);
        }
        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.VhostUserBuilder()
                        .setRole(readValue.getRole())
                        .setSocket(readValue.getSocket())
                        .setTag(ifcDetails.tag[0] == 0
                                ? null
                                : toString(ifcDetails.tag))
                        .build());
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.VhostUser> getCfgId(
            final InstanceIdentifier<VhostUser> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                .augmentation(VppInterfaceAugmentation.class)
                .child(org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.VhostUser.class);
    }
}
