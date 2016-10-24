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

package io.fd.honeycomb.nat.read.ifc;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceDetails;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceDetailsReplyDump;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;

abstract class AbstractInterfaceNatCustomizer<C extends DataObject, B extends Builder<C>>
        implements InitializingReaderCustomizer<C, B> {

    private final DumpCacheManager<SnatInterfaceDetailsReplyDump, Void> dumpMgr;
    private final NamingContext ifcContext;

    AbstractInterfaceNatCustomizer(@Nonnull final DumpCacheManager<SnatInterfaceDetailsReplyDump, Void> dumpMgr,
                                   @Nonnull final NamingContext ifcContext) {
        this.dumpMgr = dumpMgr;
        this.ifcContext = ifcContext;
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<C> id,
                                      @Nonnull final B builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();
        getLog().debug("Reading NAT features on interface: {}", ifcName);
        final int index = ifcContext.getIndex(ifcName, ctx.getMappingContext());

        // Cache dump for each interface under the same key since this is all ifc dump
        final Optional<SnatInterfaceDetailsReplyDump> dump =
                dumpMgr.getDump(id, getClass().getName(), ctx.getModificationCache(), null);

        // Find entries for current ifc and if is marked as inside set the builder to return presence container
        dump.or(new SnatInterfaceDetailsReplyDump()).snatInterfaceDetails.stream()
                .filter(snatIfcDetail -> snatIfcDetail.swIfIndex == index)
                .filter(this::isExpectedNatType)
                .findFirst()
                .ifPresent(snatIfcDetail -> setBuilderPresence(builder));
        // Not setting data, just marking the builder to propagate empty container to indicate presence
    }

    protected abstract Logger getLog();

    abstract void setBuilderPresence(@Nonnull final B builder);

    abstract boolean isExpectedNatType(final SnatInterfaceDetails snatInterfaceDetails);
}
