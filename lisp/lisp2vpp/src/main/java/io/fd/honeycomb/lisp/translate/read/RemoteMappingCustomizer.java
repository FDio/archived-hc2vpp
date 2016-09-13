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
import static io.fd.honeycomb.lisp.translate.util.EidConverter.compareAddresses;
import static io.fd.honeycomb.lisp.translate.util.EidConverter.getArrayAsEidLocal;

import com.google.common.base.Optional;
import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.translate.read.dump.check.MappingsDumpCheck;
import io.fd.honeycomb.lisp.translate.read.dump.executor.MappingsDumpExecutor;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.QuantityType;
import io.fd.honeycomb.lisp.translate.util.EidConverter;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.DumpExecutionFailedException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.RemoteMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.RemoteMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.RemoteMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.EidBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.LispEidTableDetails;
import org.openvpp.jvpp.core.dto.LispEidTableDetailsReplyDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading {@code RemoteMapping}<br>
 */
public class RemoteMappingCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<RemoteMapping, RemoteMappingKey, RemoteMappingBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteMappingCustomizer.class);
    private static final String KEY = RemoteMappingCustomizer.class.getName();

    private final DumpCacheManager<LispEidTableDetailsReplyDump, MappingsDumpParams> dumpManager;
    private final EidMappingContext remoteMappingContext;

    public RemoteMappingCustomizer(@Nonnull FutureJVppCore futureJvpp,
                                   @Nonnull EidMappingContext remoteMappingContext) {
        super(futureJvpp);
        this.remoteMappingContext = checkNotNull(remoteMappingContext, "Remote mappings not present");
        this.dumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<LispEidTableDetailsReplyDump, MappingsDumpParams>()
                        .withExecutor(new MappingsDumpExecutor(futureJvpp))
                        .withNonEmptyPredicate(new MappingsDumpCheck())
                        .build();
    }


    @Override
    public RemoteMappingBuilder getBuilder(InstanceIdentifier<RemoteMapping> id) {
        return new RemoteMappingBuilder();
    }

    private Eid copyEid(
            org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid eid) {
        return new EidBuilder().setAddress(eid.getAddress()).setAddressType(eid.getAddressType())
                .setVirtualNetworkId(eid.getVirtualNetworkId()).build();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<RemoteMapping> id, RemoteMappingBuilder builder,
                                      ReadContext ctx)
            throws ReadFailedException {
        checkState(id.firstKeyOf(RemoteMapping.class) != null, "No key present for id({})", id);
        checkState(id.firstKeyOf(VniTable.class) != null, "Parent VNI table not specified");

        final MappingId mappingId = id.firstKeyOf(RemoteMapping.class).getId();
        checkState(remoteMappingContext.containsEid(mappingId, ctx.getMappingContext()),
                "No mapping stored for id %s", mappingId);

        final long vni = id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier();
        final String remoteMappingId = id.firstKeyOf(RemoteMapping.class).getId().getValue();
        final Eid eid = copyEid(remoteMappingContext.getEid(mappingId, ctx.getMappingContext()));
        final MappingsDumpParams dumpParams = new MappingsDumpParamsBuilder()
                .setVni(Long.valueOf(vni).intValue())
                .setEidSet(QuantityType.SPECIFIC)
                .setEidType(EidConverter.getEidType(eid))
                .setEid(EidConverter.getEidAsByteArray(eid))
                .setPrefixLength(EidConverter.getPrefixLength(eid))
                .build();

        LOG.debug("Dumping data for LocalMappings(id={})", id);
        Optional<LispEidTableDetailsReplyDump> replyOptional;
        try {
            replyOptional =
                    dumpManager.getDump(bindKey("SPECIFIC_" + remoteMappingId), ctx.getModificationCache(), dumpParams);
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

            builder.setEid(EidConverter.getArrayAsEidRemote(valueOf(details.eidType), details.eid));
            builder.setKey(new RemoteMappingKey(new MappingId(id.firstKeyOf(RemoteMapping.class).getId())));
            builder.setTtl(resolveTtl(details.ttl));
            builder.setAuthoritative(
                    new RemoteMapping.Authoritative(TranslateUtils.byteToBoolean(details.authoritative)));

        } else {
            LOG.debug("No data dumped");
        }
    }

    //compensate ~0 as default value of ttl
    private static long resolveTtl(final int ttlValue) {
        return ttlValue == -1
                ? Integer.MAX_VALUE
                : ttlValue;
    }

    @Override
    public List<RemoteMappingKey> getAllIds(InstanceIdentifier<RemoteMapping> id, ReadContext context)
            throws ReadFailedException {

        checkState(id.firstKeyOf(VniTable.class) != null, "Parent VNI table not specified");

        //requesting all remote with specific vni
        final MappingsDumpParams dumpParams = new MappingsDumpParamsBuilder()
                .setVni(Long.valueOf(id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier()).intValue())
                .setEidSet(QuantityType.ALL)
                .setFilter(FilterType.REMOTE)
                .build();

        LOG.debug("Dumping data for LocalMappings(id={})", id);
        Optional<LispEidTableDetailsReplyDump> replyOptional;
        try {
            replyOptional = dumpManager.getDump(bindKey("ALL_REMOTE"), context.getModificationCache(), dumpParams);
        } catch (DumpExecutionFailedException e) {
            throw new ReadFailedException(id, e);
        }

        if (replyOptional.isPresent()) {
            LOG.debug("Valid dump loaded");
            return replyOptional.get()
                    .lispEidTableDetails
                    .stream()
                    .map(detail -> new RemoteMappingKey(
                            new MappingId(
                                    remoteMappingContext.getId(
                                            EidConverter.getArrayAsEidRemote(
                                                    valueOf(detail.eidType), detail.eid),
                                            context.getMappingContext()))))
                    .collect(Collectors.toList());
        } else {
            LOG.debug("No data dumped");
            return Collections.emptyList();
        }
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<RemoteMapping> readData) {
        ((RemoteMappingsBuilder) builder).setRemoteMapping(readData);
    }

    private String bindKey(String prefix) {
        return prefix + "_" + KEY;
    }
}
