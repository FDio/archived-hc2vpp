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

package io.fd.hc2vpp.lisp.translate.read;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.lisp.translate.read.dump.executor.params.SubtableDumpParams;
import io.fd.hc2vpp.lisp.translate.read.init.LispInitPathsMapper;
import io.fd.hc2vpp.lisp.translate.read.trait.SubtableReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.jvpp.core.dto.OneEidTableMapDetails;
import io.fd.jvpp.core.dto.OneEidTableMapDetailsReplyDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtableBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeDomainSubtableCustomizer extends FutureJVppCustomizer implements
        InitializingReaderCustomizer<BridgeDomainSubtable, BridgeDomainSubtableBuilder>, SubtableReader, LispInitPathsMapper {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomainSubtableCustomizer.class);

    private final DumpCacheManager<OneEidTableMapDetailsReplyDump, SubtableDumpParams>
            dumpManager;
    private final NamingContext bridgeDomainContext;

    public BridgeDomainSubtableCustomizer(@Nonnull final FutureJVppCore futureJvppCore,
                                          @Nonnull final NamingContext bridgeDomainContext) {
        super(futureJvppCore);
        dumpManager =
                new DumpCacheManagerBuilder<OneEidTableMapDetailsReplyDump, SubtableDumpParams>()
                        .withExecutor(createExecutor(futureJvppCore))
                        .acceptOnly(OneEidTableMapDetailsReplyDump.class)
                        .build();
        this.bridgeDomainContext = checkNotNull(bridgeDomainContext, "Bridge domain context cannot be null");
    }

    @Nonnull
    @Override
    public BridgeDomainSubtableBuilder getBuilder(@Nonnull final InstanceIdentifier<BridgeDomainSubtable> id) {
        return new BridgeDomainSubtableBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomainSubtable> id,
                                      @Nonnull final BridgeDomainSubtableBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        final int vni = checkNotNull(id.firstKeyOf(VniTable.class), "Cannot find parent VNI Table")
                .getVirtualNetworkIdentifier().intValue();
        LOG.debug("Read attributes for id {}", id);
        //dumps only L2(bridge domains)
        final Optional<OneEidTableMapDetailsReplyDump> reply =
                dumpManager.getDump(id, ctx.getModificationCache(), L2_PARAMS);

        if (!reply.isPresent() || reply.get().oneEidTableMapDetails.isEmpty()) {
            return;
        }

        // Single item collector cant be used in this case,because bridge-domain-subtable is container
        // so read is invoked every time parent is defined
        final List<OneEidTableMapDetails>
                details = reply.get().oneEidTableMapDetails.stream().filter(a -> a.vni == vni)
                .collect(Collectors.toList());
        if (details.size() == 1) {
            final OneEidTableMapDetails detail = details.get(0);
            builder.setBridgeDomainRef(bridgeDomainContext.getName(detail.dpTable, ctx.getMappingContext()));
            LOG.debug("Attributes for {} successfully loaded", id);
        }
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final BridgeDomainSubtable readValue) {
        ((VniTableBuilder) parentBuilder).setBridgeDomainSubtable(readValue);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<BridgeDomainSubtable> instanceIdentifier, @Nonnull BridgeDomainSubtable bridgeDomainSubtable, @Nonnull ReadContext readContext) {
        return Initialized.create((InstanceIdentifier<BridgeDomainSubtable>) vniSubtablePath(instanceIdentifier), bridgeDomainSubtable);
    }
}
