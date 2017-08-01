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
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceDetailsReplyDump;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceDump;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceOutputFeatureDetailsReplyDump;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceOutputFeatureDump;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;

abstract class AbstractInterfaceNatCustomizer<C extends DataObject, B extends Builder<C>>
        implements InitializingReaderCustomizer<C, B>, JvppReplyConsumer {

    private final FutureJVppSnatFacade jvppSnat;
    private final DumpCacheManager<SnatInterfaceDetailsReplyDump, Void> preRoutingDumpMgr;
    private final DumpCacheManager<SnatInterfaceOutputFeatureDetailsReplyDump, Void> postRoutingDumpMgr;
    private final NamingContext ifcContext;

    AbstractInterfaceNatCustomizer(@Nonnull final FutureJVppSnatFacade jvppSnat,
                                   @Nonnull final NamingContext ifcContext) {
        this.jvppSnat = requireNonNull(jvppSnat, "jvppSnat should not be null");
        this.ifcContext = requireNonNull(ifcContext, "ifcContext should not be null");
        this.preRoutingDumpMgr = new DumpCacheManager.DumpCacheManagerBuilder<SnatInterfaceDetailsReplyDump, Void>()
                .withExecutor((id, params) -> getReplyForRead(
                        jvppSnat.snatInterfaceDump(new SnatInterfaceDump()).toCompletableFuture(), id))
                .acceptOnly(SnatInterfaceDetailsReplyDump.class)
                .build();
        this.postRoutingDumpMgr =
                new DumpCacheManager.DumpCacheManagerBuilder<SnatInterfaceOutputFeatureDetailsReplyDump, Void>()
                        .withExecutor((id, params) -> getReplyForRead(
                                jvppSnat.snatInterfaceOutputFeatureDump(new SnatInterfaceOutputFeatureDump())
                                        .toCompletableFuture(), id))
                        .acceptOnly(SnatInterfaceOutputFeatureDetailsReplyDump.class)
                        .build();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<C> id,
                                      @Nonnull final B builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        final String ifcName = getName(id);
        getLog().debug("Reading NAT features on interface: {}", ifcName);
        final int index = ifcContext.getIndex(ifcName, ctx.getMappingContext());

        // There are no additional attributes for pre routing NAT, so it is enough to read post routing ifc mapping:
        final Optional<SnatInterfaceOutputFeatureDetailsReplyDump> postRoutingDump =
                postRoutingDumpMgr.getDump(id, ctx.getModificationCache(), null);

        postRoutingDump.or(new SnatInterfaceOutputFeatureDetailsReplyDump()).snatInterfaceOutputFeatureDetails.stream()
                .filter(snatIfcDetail -> snatIfcDetail.swIfIndex == index)
                .filter(snatIfcDetail -> isExpectedNatType(snatIfcDetail.isInside))
                .findFirst()
                .ifPresent(snatIfcDetail -> setPostRouting(builder));
    }

    @Override
    public boolean isPresent(final InstanceIdentifier<C> id, final C built, final ReadContext ctx)
            throws ReadFailedException {
        // In the post routing case, we can reuse default implementation:
        if (InitializingReaderCustomizer.super.isPresent(id, built, ctx)) {
            // post routing was set
            return true;
        }
        // In the pre routing case, we need to inspect pre routing dump:
        final String ifcName = getName(id);
        getLog().debug("Checking NAT presence for interface: {}", ifcName);
        final int index = ifcContext.getIndex(ifcName, ctx.getMappingContext());

        // Cache dump for each interface under the same key since this is all ifc dump:
        final Optional<SnatInterfaceDetailsReplyDump> preRoutingDump =
                preRoutingDumpMgr.getDump(id, ctx.getModificationCache(), null);

        // Find entries for current ifc and if is marked as inside set the builder to return presence container:
        return preRoutingDump.or(new SnatInterfaceDetailsReplyDump()).snatInterfaceDetails.stream()
                .filter(snatIfcDetail -> snatIfcDetail.swIfIndex == index)
                .anyMatch(snatIfcDetail -> isExpectedNatType(snatIfcDetail.isInside));
        // Not setting data, just marking the builder to propagate empty container to indicate presence.
    }

    protected String getName(final InstanceIdentifier<C> id) {
        return id.firstKeyOf(Interface.class).getName();
    }

    abstract Logger getLog();

    abstract boolean isExpectedNatType(final int isInside);

    abstract void setPostRouting(final B builder);
}
