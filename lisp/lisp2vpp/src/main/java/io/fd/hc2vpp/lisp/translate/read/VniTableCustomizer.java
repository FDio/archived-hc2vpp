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

import static com.google.common.base.Preconditions.checkState;
import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.core.dto.OneEidTableVniDetails;
import io.fd.vpp.jvpp.core.dto.OneEidTableVniDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.OneEidTableVniDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.EidTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the reads of {@link VniTable} nodes
 */
public class VniTableCustomizer extends CheckedLispCustomizer
        implements InitializingListReaderCustomizer<VniTable, VniTableKey, VniTableBuilder>, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(VniTableCustomizer.class);

    private final DumpCacheManager<OneEidTableVniDetailsReplyDump, Void> dumpManager;

    public VniTableCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                              @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJvpp, lispStateCheckService);
        this.dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<OneEidTableVniDetailsReplyDump, Void>()
                .withExecutor(((identifier, params) -> getReplyForRead(
                        futureJvpp.oneEidTableVniDump(new OneEidTableVniDump()).toCompletableFuture(), identifier)))
                .acceptOnly(OneEidTableVniDetailsReplyDump.class)
                .build();
    }

    private static VniTableKey detailsToKey(final OneEidTableVniDetails lispEidTableMapDetails) {
        return new VniTableKey(Integer.valueOf(lispEidTableMapDetails.vni).longValue());

    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<VniTable> readData) {
        ((EidTableBuilder) builder).setVniTable(readData);
    }

    @Nonnull
    @Override
    public VniTableBuilder getBuilder(@Nonnull final InstanceIdentifier<VniTable> id) {
        return new VniTableBuilder();
    }

    @Nonnull
    @Override
    public List<VniTableKey> getAllIds(@Nonnull final InstanceIdentifier<VniTable> id,
                                       @Nonnull final ReadContext context)
            throws ReadFailedException {
        if (!lispStateCheckService.lispEnabled(context)) {
            LOG.info("Lisp feature must be enabled first");
            return Collections.emptyList();
        }
        LOG.trace("Reading all IDS...");

        final Optional<OneEidTableVniDetailsReplyDump> optionalReply =
                dumpManager.getDump(id, context.getModificationCache(), NO_PARAMS);

        if (!optionalReply.isPresent() || optionalReply.get().oneEidTableVniDetails.isEmpty()) {
            return Collections.emptyList();
        }

        return optionalReply.get().oneEidTableVniDetails.stream().map(VniTableCustomizer::detailsToKey)
                .collect(Collectors.toList());
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<VniTable> id,
                                      @Nonnull final VniTableBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        if (!lispStateCheckService.lispEnabled(ctx)) {
            LOG.info("Lisp feature must be enabled first");
            return;
        }

        checkState(id.firstKeyOf(VniTable.class) != null, "No VNI present");
        VniTableKey key = new VniTableKey(id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier());

        final Optional<OneEidTableVniDetailsReplyDump> optionalReply =
                dumpManager.getDump(id, ctx.getModificationCache(), NO_PARAMS);

        if (!optionalReply.isPresent() || optionalReply.get().oneEidTableVniDetails.isEmpty()) {
            return;
        }

        //transforming right away to single detail(specific request should do the magic)
        final OneEidTableVniDetails details = optionalReply.get()
                .oneEidTableVniDetails
                .stream()
                .filter(a -> a.vni == key.getVirtualNetworkIdentifier().intValue())
                .collect(RWUtils.singleItemCollector());

        builder.setVirtualNetworkIdentifier((long) details.vni);
        builder.setKey(new VniTableKey(Long.valueOf(details.vni)));
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<VniTable> instanceIdentifier, @Nonnull VniTable vniTable, @Nonnull ReadContext readContext) {
        final KeyedInstanceIdentifier<VniTable, VniTableKey> identifier = InstanceIdentifier.create(Lisp.class)
                .child(LispFeatureData.class)
                .child(EidTable.class)
                .child(VniTable.class, instanceIdentifier.firstKeyOf(VniTable.class));

        return Initialized.create(identifier, vniTable);
    }
}
