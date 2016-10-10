/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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
import io.fd.vpp.jvpp.core.dto.LispEidTableMapDetails;
import io.fd.vpp.jvpp.core.dto.LispEidTableMapDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.VrfSubtableBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VrfSubtableCustomizer extends FutureJVppCustomizer
        implements ReaderCustomizer<VrfSubtable, VrfSubtableBuilder>, SubtableReader {

    private static final Logger LOG = LoggerFactory.getLogger(VrfSubtableCustomizer.class);
    private static final String CACHE_KEY = VrfSubtableCustomizer.class.getName();

    private final DumpCacheManager<LispEidTableMapDetailsReplyDump, SubtableDumpParams> dumpManager;

    public VrfSubtableCustomizer(@Nonnull final FutureJVppCore futureJvpp) {
        super(futureJvpp);
        dumpManager = new DumpCacheManagerBuilder<LispEidTableMapDetailsReplyDump, SubtableDumpParams>()
                .withExecutor(createExecutor(futureJvpp))
                .build();
    }

    @Nonnull
    @Override
    public VrfSubtableBuilder getBuilder(@Nonnull final InstanceIdentifier<VrfSubtable> id) {
        return new VrfSubtableBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<VrfSubtable> id,
                                      @Nonnull final VrfSubtableBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        LOG.debug("Read attributes for {}", id);
        final int vni = checkNotNull(id.firstKeyOf(VniTable.class), "Cannot find parent VNI Table")
                .getVirtualNetworkIdentifier().intValue();

        final Optional<LispEidTableMapDetailsReplyDump> reply =
                readSubtable(dumpManager, CACHE_KEY, ctx.getModificationCache(), id, L3_PARAMS);

        if (!reply.isPresent() || reply.get().lispEidTableMapDetails.isEmpty()) {
            return;
        }

        // Single item collector cant be used in this case,because vrf-subtable is container
        // so read is invoked every time parent is defined
        final List<LispEidTableMapDetails> details =
                reply.get().lispEidTableMapDetails.stream().filter(a -> a.vni == vni)
                        .collect(Collectors.toList());
        if (details.size() == 1) {
            final LispEidTableMapDetails detail = details.get(0);
            builder.setTableId(Integer.valueOf(detail.dpTable).longValue());

            LOG.debug("Attributes for {} successfully loaded", id);
        }
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final VrfSubtable readValue) {
        ((VniTableBuilder) parentBuilder).setVrfSubtable(readValue);
    }
}
