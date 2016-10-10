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
import static com.google.common.base.Preconditions.checkState;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.valueOf;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.FilterType;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.MappingsDumpParamsBuilder;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.QuantityType;

import com.google.common.base.Optional;
import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams;
import io.fd.honeycomb.lisp.translate.read.trait.MappingReader;
import io.fd.honeycomb.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.core.dto.LispEidTableDetails;
import io.fd.vpp.jvpp.core.dto.LispEidTableDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.LocalMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.LocalMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.LocalMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading {@code LocalMapping}<br> Currently unsupported by jvpp
 */
public class LocalMappingCustomizer
        extends FutureJVppCustomizer
        implements ListReaderCustomizer<LocalMapping, LocalMappingKey, LocalMappingBuilder>, EidTranslator,
        MappingReader {

    private static final Logger LOG = LoggerFactory.getLogger(LocalMappingCustomizer.class);
    private static final String KEY = LocalMappingCustomizer.class.getName();

    private final DumpCacheManager<LispEidTableDetailsReplyDump, MappingsDumpParams> dumpManager;
    private final NamingContext locatorSetContext;
    private final EidMappingContext localMappingContext;

    public LocalMappingCustomizer(@Nonnull FutureJVppCore futureJvpp, @Nonnull NamingContext locatorSetContext,
                                  @Nonnull EidMappingContext localMappingsContext) {
        super(futureJvpp);
        this.locatorSetContext = checkNotNull(locatorSetContext, "Locator Set Mapping Context cannot be null");
        this.localMappingContext = checkNotNull(localMappingsContext, "Local mappings context cannot be null");
        this.dumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<LispEidTableDetailsReplyDump, MappingsDumpParams>()
                        .withExecutor(createMappingDumpExecutor(futureJvpp))
                        .build();
    }

    @Override
    public LocalMappingBuilder getBuilder(InstanceIdentifier<LocalMapping> id) {
        return new LocalMappingBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<LocalMapping> id, LocalMappingBuilder builder,
                                      ReadContext ctx) throws ReadFailedException {
        checkState(id.firstKeyOf(LocalMapping.class) != null, "No key present for id({})", id);
        checkState(id.firstKeyOf(VniTable.class) != null, "Parent VNI table not specified");

        //checks whether there is an existing mapping
        final MappingId mappingId = id.firstKeyOf(LocalMapping.class).getId();
        checkState(localMappingContext.containsEid(mappingId, ctx.getMappingContext()));

        final long vni = id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier();

        final String localMappingId = id.firstKeyOf(LocalMapping.class).getId().getValue();
        final Eid eid = localMappingContext.getEid(mappingId, ctx.getMappingContext());

        //Requesting for specific mapping dump,only from local mappings with specified eid/vni/eid type
        final MappingsDumpParams dumpParams = new MappingsDumpParams.MappingsDumpParamsBuilder()
                .setEidSet(QuantityType.SPECIFIC)
                .setVni(Long.valueOf(vni).intValue())
                .setEid(getEidAsByteArray(eid))
                .setEidType(getEidType(eid))
                .setPrefixLength(getPrefixLength(eid))
                .build();

        LOG.debug("Dumping data for LocalMappings(id={})", id);
        final Optional<LispEidTableDetailsReplyDump> replyOptional =
                dumpManager.getDump(id, bindKey("SPECIFIC_" + localMappingId), ctx.getModificationCache(), dumpParams);

        if (!replyOptional.isPresent() || replyOptional.get().lispEidTableDetails.isEmpty()) {
            return;
        }

        LispEidTableDetails details = replyOptional.get().lispEidTableDetails.stream()
                .filter(subtableFilterForLocalMappings(id))
                .filter(detail -> compareAddresses(eid.getAddress(), getAddressFromDumpDetail(detail)))
                .collect(RWUtils.singleItemCollector());

        //in case of local mappings,locator_set_index stands for interface index
        checkState(locatorSetContext.containsName(details.locatorSetIndex, ctx.getMappingContext()),
                "No Locator Set name found for index %s", details.locatorSetIndex);
        builder.setLocatorSet(locatorSetContext.getName(details.locatorSetIndex, ctx.getMappingContext()));
        builder.setKey(new LocalMappingKey(new MappingId(id.firstKeyOf(LocalMapping.class).getId())));
        builder.setEid(getArrayAsEidLocal(valueOf(details.eidType), details.eid, details.vni));
    }

    private Address getAddressFromDumpDetail(final LispEidTableDetails detail) {
        return getArrayAsEidLocal(valueOf(detail.eidType), detail.eid, detail.vni).getAddress();
    }

    @Override
    public List<LocalMappingKey> getAllIds(InstanceIdentifier<LocalMapping> id, ReadContext context)
            throws ReadFailedException {

        checkState(id.firstKeyOf(VniTable.class) != null, "Parent VNI table not specified");
        final long vni = id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier();

        if (vni == 0) {
            // ignoring default vni mapping
            // its not relevant for us and we also don't store mapping for such eid's
            // such mapping is used to create helper local mappings to process remote ones
            return Collections.emptyList();
        }

        //request for all local mappings
        final MappingsDumpParams dumpParams = new MappingsDumpParamsBuilder()
                .setFilter(FilterType.LOCAL)
                .setEidSet(QuantityType.ALL)
                .build();

        LOG.debug("Dumping data for LocalMappings(id={})", id);
        final Optional<LispEidTableDetailsReplyDump> replyOptional =
                dumpManager.getDump(id, bindKey("ALL_LOCAL"), context.getModificationCache(), dumpParams);

        if (!replyOptional.isPresent() || replyOptional.get().lispEidTableDetails.isEmpty()) {
            return Collections.emptyList();
        }


        return replyOptional.get().lispEidTableDetails.stream()
                .filter(a -> a.vni == vni)
                .filter(subtableFilterForLocalMappings(id))
                .map(detail -> getArrayAsEidLocal(valueOf(detail.eidType), detail.eid, detail.vni))
                .map(localEid -> localMappingContext.getId(localEid, context.getMappingContext()))
                .map(MappingId::new)
                .map(LocalMappingKey::new)
                .collect(Collectors.toList());
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<LocalMapping> readData) {
        ((LocalMappingsBuilder) builder).setLocalMapping(readData);
    }

    private static String bindKey(String prefix) {
        return prefix + "_" + KEY;
    }

}
