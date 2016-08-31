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
import static io.fd.honeycomb.lisp.translate.util.EidConverter.compareAddresses;
import static io.fd.honeycomb.lisp.translate.util.EidConverter.getArrayAsEidLocal;
import static io.fd.honeycomb.lisp.translate.util.EidConverter.getEidAsByteArray;
import static io.fd.honeycomb.lisp.translate.util.EidConverter.getEidType;
import static io.fd.honeycomb.lisp.translate.util.EidConverter.getPrefixLength;

import com.google.common.base.Optional;
import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.translate.read.dump.check.MappingsDumpCheck;
import io.fd.honeycomb.lisp.translate.read.dump.executor.MappingsDumpExecutor;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.cache.DumpCacheManager;
import io.fd.honeycomb.translate.v3po.util.cache.exceptions.execution.DumpExecutionFailedException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.LocalMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.LocalMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.LocalMappingKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.LispEidTableDetails;
import org.openvpp.jvpp.core.dto.LispEidTableDetailsReplyDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading {@code LocalMapping}<br> Currently unsupported by jvpp
 */
public class LocalMappingCustomizer
        extends FutureJVppCustomizer
        implements ListReaderCustomizer<LocalMapping, LocalMappingKey, LocalMappingBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(LocalMappingCustomizer.class);
    private static final String KEY = LocalMappingCustomizer.class.getName();

    private final DumpCacheManager<LispEidTableDetailsReplyDump, MappingsDumpParams> dumpManager;
    private final MappingsDumpExecutor dumpExecutor;
    private final NamingContext locatorSetContext;
    private final EidMappingContext localMappingContext;

    public LocalMappingCustomizer(@Nonnull FutureJVppCore futureJvpp, @Nonnull NamingContext locatorSetContext,
                                  @Nonnull EidMappingContext localMappingsContext) {
        super(futureJvpp);
        this.locatorSetContext = checkNotNull(locatorSetContext, "Locator Set Mapping Context cannot be null");
        this.localMappingContext = checkNotNull(localMappingsContext, "Local mappings context cannot be null");
        this.dumpExecutor = new MappingsDumpExecutor(futureJvpp);
        this.dumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<LispEidTableDetailsReplyDump, MappingsDumpParams>()
                        .withExecutor(dumpExecutor)
                        .withNonEmptyPredicate(new MappingsDumpCheck())
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
        Optional<LispEidTableDetailsReplyDump> replyOptional;

        try {
            replyOptional =
                    dumpManager.getDump(bindKey("SPECIFIC_" + localMappingId), ctx.getModificationCache(), dumpParams);
        } catch (DumpExecutionFailedException e) {
            throw new ReadFailedException(id, e);
        }

        if (replyOptional.isPresent()) {
            LOG.debug("Valid dump loaded");

            LispEidTableDetails details = replyOptional.get().lispEidTableDetails.stream()
                    .filter(a -> compareAddresses(eid.getAddress(),
                            getArrayAsEidLocal(valueOf(a.eidType), a.eid).getAddress()))
                    .collect(
                            RWUtils.singleItemCollector());

            //in case of local mappings,locator_set_index stands for interface index
            checkState(locatorSetContext.containsName(details.locatorSetIndex, ctx.getMappingContext()));
            builder.setLocatorSet(locatorSetContext.getName(details.locatorSetIndex, ctx.getMappingContext()));
            builder.setKey(new LocalMappingKey(new MappingId(id.firstKeyOf(LocalMapping.class).getId())));
            builder.setEid(getArrayAsEidLocal(valueOf(details.eidType), details.eid));
        } else {
            LOG.debug("No data dumped");
        }
    }

    @Override
    public List<LocalMappingKey> getAllIds(InstanceIdentifier<LocalMapping> id, ReadContext context)
            throws ReadFailedException {

        checkState(id.firstKeyOf(VniTable.class) != null, "Parent VNI table not specified");

        //request for all local mappings
        final MappingsDumpParams dumpParams = new MappingsDumpParamsBuilder()
                .setVni(Long.valueOf(id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier()).intValue())
                .setFilter(FilterType.LOCAL)
                .setEidSet(QuantityType.ALL)
                .build();

        LOG.debug("Dumping data for LocalMappings(id={})", id);
        Optional<LispEidTableDetailsReplyDump> replyOptional;
        try {
            replyOptional = dumpManager.getDump(bindKey("ALL_LOCAL"), context.getModificationCache(), dumpParams);
        } catch (DumpExecutionFailedException e) {
            throw new ReadFailedException(id, e);
        }

        if (replyOptional.isPresent()) {
            LOG.debug("Valid dump loaded");
            return replyOptional.get().lispEidTableDetails.stream().map(a -> new LocalMappingKey(
                    new MappingId(
                            localMappingContext.getId(
                                    getArrayAsEidLocal(valueOf(a.eidType), a.eid),
                                    context.getMappingContext()))))
                    .collect(Collectors.toList());
        } else {
            LOG.debug("No data dumped");
            return Collections.emptyList();
        }
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<LocalMapping> readData) {
        ((LocalMappingsBuilder) builder).setLocalMapping(readData);
    }

    private static String bindKey(String prefix) {
        return prefix + "_" + KEY;
    }
}
