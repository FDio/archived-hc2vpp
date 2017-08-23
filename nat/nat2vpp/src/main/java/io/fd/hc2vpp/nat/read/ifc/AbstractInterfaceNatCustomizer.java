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

package io.fd.hc2vpp.nat.read.ifc;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.nat.dto.Nat44InterfaceDetailsReplyDump;
import io.fd.vpp.jvpp.nat.dto.Nat44InterfaceDump;
import io.fd.vpp.jvpp.nat.dto.Nat44InterfaceOutputFeatureDetailsReplyDump;
import io.fd.vpp.jvpp.nat.dto.Nat44InterfaceOutputFeatureDump;
import io.fd.vpp.jvpp.nat.dto.Nat64InterfaceDetailsReplyDump;
import io.fd.vpp.jvpp.nat.dto.Nat64InterfaceDump;
import io.fd.vpp.jvpp.nat.future.FutureJVppNatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;

abstract class AbstractInterfaceNatCustomizer<C extends DataObject, B extends Builder<C>>
        implements InitializingReaderCustomizer<C, B>, JvppReplyConsumer {

    private final DumpCacheManager<Nat44InterfaceDetailsReplyDump, Void> preRoutingNat44DumpMgr;
    private final DumpCacheManager<Nat64InterfaceDetailsReplyDump, Void> preRoutingNat64DumpMgr;
    private final DumpCacheManager<Nat44InterfaceOutputFeatureDetailsReplyDump, Void> postRoutingNat44DumpMgr;
    private final NamingContext ifcContext;
    private final VppAttributesBuilder vppAttributesBuilder;

    AbstractInterfaceNatCustomizer(@Nonnull final FutureJVppNatFacade jvppNat,
                                   @Nonnull final NamingContext ifcContext,
                                   @Nonnull final VppAttributesBuilder vppAttributesBuilder) {
        requireNonNull(jvppNat, "jvppNat should not be null");
        this.ifcContext = requireNonNull(ifcContext, "ifcContext should not be null");
        this.vppAttributesBuilder = requireNonNull(vppAttributesBuilder, "ifcContext should not be null");
        this.preRoutingNat44DumpMgr =
                new DumpCacheManager.DumpCacheManagerBuilder<Nat44InterfaceDetailsReplyDump, Void>()
                        .withExecutor((id, params) -> getReplyForRead(
                                jvppNat.nat44InterfaceDump(new Nat44InterfaceDump()).toCompletableFuture(), id))
                        .acceptOnly(Nat44InterfaceDetailsReplyDump.class)
                        .build();
        this.preRoutingNat64DumpMgr =
                new DumpCacheManager.DumpCacheManagerBuilder<Nat64InterfaceDetailsReplyDump, Void>()
                        .withExecutor((id, params) -> getReplyForRead(
                                jvppNat.nat64InterfaceDump(new Nat64InterfaceDump()).toCompletableFuture(), id))
                        .acceptOnly(Nat64InterfaceDetailsReplyDump.class)
                        .build();
        this.postRoutingNat44DumpMgr =
                new DumpCacheManager.DumpCacheManagerBuilder<Nat44InterfaceOutputFeatureDetailsReplyDump, Void>()
                        .withExecutor((id, params) -> getReplyForRead(
                                jvppNat.nat44InterfaceOutputFeatureDump(new Nat44InterfaceOutputFeatureDump())
                                        .toCompletableFuture(), id))
                        .acceptOnly(Nat44InterfaceOutputFeatureDetailsReplyDump.class)
                        .build();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<C> id,
                                      @Nonnull final B builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        final String ifcName = getName(id);
        getLog().debug("Reading NAT features on interface: {}", ifcName);
        final int index = ifcContext.getIndex(ifcName, ctx.getMappingContext());

        // Each of the following cases uses different VPP API, but we store them under single node.
        // Not all combinations are possible, but we don't validate on read and rely on VPP.
        readPreRoutingNat44(id, index, builder, ctx);
        readPreRoutingNat64(id, index, builder, ctx);
        readPostRoutingNat44(id, index, builder, ctx);
    }

    private void readPreRoutingNat44(final InstanceIdentifier<C> id, final int index, final B builder,
                                     final ReadContext ctx) throws ReadFailedException {
        final Optional<Nat44InterfaceDetailsReplyDump> dump =
                preRoutingNat44DumpMgr.getDump(id, ctx.getModificationCache(), null);

        dump.or(new Nat44InterfaceDetailsReplyDump()).nat44InterfaceDetails.stream()
                .filter(natIfcDetail -> natIfcDetail.swIfIndex == index)
                .filter(natIfcDetail -> isExpectedNatType(natIfcDetail.isInside))
                .findAny()
                .ifPresent(natIfcDetail -> vppAttributesBuilder.enableNat44(builder));
        // do not modify builder is feature is absent (inbound/outbound are presence containers)
    }

    private void readPreRoutingNat64(final InstanceIdentifier<C> id, final int index, final B builder,
                                     final ReadContext ctx) throws ReadFailedException {
        final Optional<Nat64InterfaceDetailsReplyDump> dump =
                preRoutingNat64DumpMgr.getDump(id, ctx.getModificationCache(), null);

        dump.or(new Nat64InterfaceDetailsReplyDump()).nat64InterfaceDetails.stream()
                .filter(natIfcDetail -> natIfcDetail.swIfIndex == index)
                .filter(natIfcDetail -> isExpectedNatType(natIfcDetail.isInside))
                .findAny()
                .ifPresent(natIfcDetail -> vppAttributesBuilder.enableNat64(builder));
        // do not modify builder is feature is absent (inbound/outbound are presence containers)
    }

    private void readPostRoutingNat44(final InstanceIdentifier<C> id, final int index, final B builder,
                                      final ReadContext ctx) throws ReadFailedException {
        final Optional<Nat44InterfaceOutputFeatureDetailsReplyDump> dump =
                postRoutingNat44DumpMgr.getDump(id, ctx.getModificationCache(), null);

        dump.or(new Nat44InterfaceOutputFeatureDetailsReplyDump()).nat44InterfaceOutputFeatureDetails
                .stream()
                .filter(natIfcDetail -> natIfcDetail.swIfIndex == index)
                .filter(natIfcDetail -> isExpectedNatType(natIfcDetail.isInside))
                .findAny()
                .ifPresent(natIfcDetail -> vppAttributesBuilder.enablePostRouting(builder));
        // do not modify builder is feature is absent (inbound/outbound are presence containers)
    }

    protected String getName(final InstanceIdentifier<C> id) {
        return id.firstKeyOf(Interface.class).getName();
    }

    abstract Logger getLog();

    abstract boolean isExpectedNatType(final int isInside);
}
