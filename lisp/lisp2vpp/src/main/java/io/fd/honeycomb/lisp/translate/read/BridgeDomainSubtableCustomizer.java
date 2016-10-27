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

package io.fd.honeycomb.lisp.translate.read;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.SubtableDumpParams;
import io.fd.honeycomb.lisp.translate.read.trait.SubtableReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.core.dto.LispEidTableMapDetails;
import io.fd.vpp.jvpp.core.dto.LispEidTableMapDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtableBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeDomainSubtableCustomizer extends FutureJVppCustomizer implements
        ReaderCustomizer<BridgeDomainSubtable, BridgeDomainSubtableBuilder>, SubtableReader {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomainSubtableCustomizer.class);
    private static final String CACHE_KEY = BridgeDomainSubtableCustomizer.class.getName();

    private final DumpCacheManager<LispEidTableMapDetailsReplyDump, SubtableDumpParams> dumpManager;
    private final NamingContext bridgeDomainContext;

    public BridgeDomainSubtableCustomizer(@Nonnull final FutureJVppCore futureJvppCore,
                                          @Nonnull final NamingContext bridgeDomainContext) {
        super(futureJvppCore);
        dumpManager = new DumpCacheManagerBuilder<LispEidTableMapDetailsReplyDump, SubtableDumpParams>()
                .withExecutor(createExecutor(futureJvppCore))
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
        final Optional<LispEidTableMapDetailsReplyDump> reply =
                readSubtable(dumpManager, CACHE_KEY, ctx.getModificationCache(), id, L2_PARAMS);

        if (!reply.isPresent() || reply.get().lispEidTableMapDetails.isEmpty()) {
            return;
        }

        // Single item collector cant be used in this case,because bridge-domain-subtable is container
        // so read is invoked every time parent is defined
        final List<LispEidTableMapDetails>
                details = reply.get().lispEidTableMapDetails.stream().filter(a -> a.vni == vni)
                .collect(Collectors.toList());
        if (details.size() == 1) {
            final LispEidTableMapDetails detail = details.get(0);
            builder.setBridgeDomainRef(bridgeDomainContext.getName(detail.dpTable, ctx.getMappingContext()));
            LOG.debug("Attributes for {} successfully loaded", id);
        }
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final BridgeDomainSubtable readValue) {
        ((VniTableBuilder) parentBuilder).setBridgeDomainSubtable(readValue);
    }
}
