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
import io.fd.hc2vpp.lisp.translate.read.dump.executor.params.SubtableDumpParams;
import io.fd.hc2vpp.lisp.translate.read.init.LispInitPathsMapper;
import io.fd.hc2vpp.lisp.translate.read.trait.SubtableReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.vpp.jvpp.core.dto.OneEidTableMapDetails;
import io.fd.vpp.jvpp.core.dto.OneEidTableMapDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.VrfSubtableBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VrfSubtableCustomizer extends FutureJVppCustomizer
        implements InitializingReaderCustomizer<VrfSubtable, VrfSubtableBuilder>, SubtableReader, LispInitPathsMapper {

    private static final Logger LOG = LoggerFactory.getLogger(VrfSubtableCustomizer.class);

    private final DumpCacheManager<OneEidTableMapDetailsReplyDump, SubtableDumpParams> dumpManager;

    public VrfSubtableCustomizer(@Nonnull final FutureJVppCore futureJvpp) {
        super(futureJvpp);
        dumpManager = new DumpCacheManagerBuilder<OneEidTableMapDetailsReplyDump, SubtableDumpParams>()
                .withExecutor(createExecutor(futureJvpp))
                .acceptOnly(OneEidTableMapDetailsReplyDump.class)
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

        final Optional<OneEidTableMapDetailsReplyDump> reply = dumpManager.getDump(id, ctx.getModificationCache(), L3_PARAMS);

        if (!reply.isPresent() || reply.get().oneEidTableMapDetails.isEmpty()) {
            return;
        }

        // Single item collector cant be used in this case,because vrf-subtable is container
        // so read is invoked every time parent is defined
        final List<OneEidTableMapDetails> details =
                reply.get().oneEidTableMapDetails.stream().filter(a -> a.vni == vni)
                        .collect(Collectors.toList());
        if (details.size() == 1) {
            final OneEidTableMapDetails detail = details.get(0);
            builder.setTableId(Integer.toUnsignedLong(detail.dpTable));

            LOG.debug("Attributes for {} successfully loaded", id);
        }
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final VrfSubtable readValue) {
        ((VniTableBuilder) parentBuilder).setVrfSubtable(readValue);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<VrfSubtable> instanceIdentifier, @Nonnull VrfSubtable vrfSubtable, @Nonnull ReadContext readContext) {
        return Initialized.create((InstanceIdentifier<VrfSubtable>) vniSubtablePath(instanceIdentifier), vrfSubtable);
    }
}
