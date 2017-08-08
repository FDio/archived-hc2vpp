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
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.context.util.AdjacenciesMappingContext;
import io.fd.hc2vpp.lisp.context.util.EidMappingContext;
import io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams;
import io.fd.hc2vpp.lisp.translate.read.init.LispInitPathsMapper;
import io.fd.hc2vpp.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.vpp.jvpp.core.dto.OneAdjacenciesGet;
import io.fd.vpp.jvpp.core.dto.OneAdjacenciesGetReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.types.OneAdjacency;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.mapping.EidIdentificatorPair;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.mapping.EidIdentificatorPairBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.adjacencies.grouping.Adjacencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.adjacencies.grouping.AdjacenciesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.adjacencies.grouping.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.adjacencies.grouping.adjacencies.AdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.adjacencies.grouping.adjacencies.AdjacencyKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class AdjacencyCustomizer extends FutureJVppCustomizer
        implements InitializingListReaderCustomizer<Adjacency, AdjacencyKey, AdjacencyBuilder>, JvppReplyConsumer,
        EidTranslator, LispInitPathsMapper {

    private final DumpCacheManager<OneAdjacenciesGetReply, AdjacencyDumpParams> dumpCacheManager;
    private final AdjacenciesMappingContext adjacenciesMappingContext;
    private final EidPairProducer eidPairProducer;


    public AdjacencyCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                               @Nonnull final EidMappingContext localMappingContext,
                               @Nonnull final EidMappingContext remoteMappingContext,
                               @Nonnull final AdjacenciesMappingContext adjacenciesMappingContext) {
        super(futureJvpp);
        dumpCacheManager = new DumpCacheManager.DumpCacheManagerBuilder<OneAdjacenciesGetReply, AdjacencyDumpParams>()
                .withExecutor(createExecutor())
                .acceptOnly(OneAdjacenciesGetReply.class)
                .build();

        this.adjacenciesMappingContext =
                checkNotNull(adjacenciesMappingContext, "Adjacencies mapping context cannot be null");
        this.eidPairProducer = new EidPairProducer(localMappingContext, remoteMappingContext);
    }

    @Nonnull
    @Override
    public List<AdjacencyKey> getAllIds(@Nonnull final InstanceIdentifier<Adjacency> id,
                                        @Nonnull final ReadContext context) throws ReadFailedException {

        final int vni = id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier().intValue();

        final Optional<OneAdjacenciesGetReply> optionalDump =
                dumpCacheManager.getDump(id, context.getModificationCache(), new AdjacencyDumpParams(vni));


        if (optionalDump.isPresent()) {
            return Arrays.stream(optionalDump.get().adjacencies)
                    .map(lispAdjacency -> eidPairProducer.createPair(lispAdjacency, vni, context.getMappingContext()))
                    .map(pair -> adjacenciesMappingContext
                            .getAdjacencyId(pair.getLocalEidId().getValue(), pair.getRemoteEidId().getValue(),
                                    context.getMappingContext()))
                    .map(AdjacencyKey::new)
                    .collect(Collectors.toList());
        }

        //does not throw exception to not disturb lisp state reading
        return Collections.emptyList();
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<Adjacency> readData) {
        ((AdjacenciesBuilder) builder).setAdjacency(readData);
    }

    @Nonnull
    @Override
    public AdjacencyBuilder getBuilder(@Nonnull final InstanceIdentifier<Adjacency> id) {
        return new AdjacencyBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Adjacency> id,
                                      @Nonnull final AdjacencyBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {

        final int vni = id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier().intValue();

        final Optional<OneAdjacenciesGetReply> optionalDump = dumpCacheManager
                .getDump(id, ctx.getModificationCache(), new AdjacencyDumpParams(vni));

        if (!optionalDump.isPresent() || optionalDump.get().adjacencies.length == 0) {
            return;
        }

        final String currentAdjacencyId = id.firstKeyOf(Adjacency.class).getId();
        final EidIdentificatorPair currentAdjacencyIdentificationPair =
                adjacenciesMappingContext.getEidPair(currentAdjacencyId, ctx.getMappingContext());

        final OneAdjacency currentAdjacency = Arrays.stream(optionalDump.get().adjacencies)
                .filter(lispAdjacency -> Objects.equals(currentAdjacencyIdentificationPair,
                        eidPairProducer.createPair(lispAdjacency, vni, ctx.getMappingContext())))
                .collect(RWUtils.singleItemCollector());

        builder.setId(currentAdjacencyId)
                .setKey(new AdjacencyKey(currentAdjacencyId))
                .setLocalEid(getArrayAsLocalEid(
                        MappingsDumpParams.EidType.valueOf(currentAdjacency.eidType), currentAdjacency.leid,
                        currentAdjacency.leidPrefixLen, vni))
                .setRemoteEid(getArrayAsRemoteEid(
                        MappingsDumpParams.EidType.valueOf(currentAdjacency.eidType), currentAdjacency.reid,
                        currentAdjacency.reidPrefixLen, vni));
    }

    private EntityDumpExecutor<OneAdjacenciesGetReply, AdjacencyDumpParams> createExecutor() {
        return (final InstanceIdentifier<?> identifier, final AdjacencyDumpParams params) -> {
            checkNotNull(params, "Dump parameters cannot be null");

            final OneAdjacenciesGet request = new OneAdjacenciesGet();
            request.vni = params.getVni();

            return getReplyForRead(getFutureJVpp().oneAdjacenciesGet(request).toCompletableFuture(), identifier);
        };
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<Adjacency> instanceIdentifier, @Nonnull Adjacency adjacency, @Nonnull ReadContext readContext) {
        final KeyedInstanceIdentifier<Adjacency, AdjacencyKey> identifier = remoteMappingPath(instanceIdentifier)
                .child(Adjacencies.class)
                .child(Adjacency.class, instanceIdentifier.firstKeyOf(Adjacency.class));

        return Initialized.create(identifier, adjacency);
    }

    private static final class AdjacencyDumpParams {

        private final int vni;

        AdjacencyDumpParams(final int vni) {
            this.vni = vni;
        }

        public int getVni() {
            return this.vni;
        }
    }

    private class EidPairProducer implements EidTranslator {

        private final EidMappingContext localMappingContext;
        private final EidMappingContext remoteMappingContext;

        EidPairProducer(final EidMappingContext localMappingContext,
                        final EidMappingContext remoteMappingContext) {
            this.localMappingContext = checkNotNull(localMappingContext, "Local mapping context cannot be null");
            this.remoteMappingContext = checkNotNull(remoteMappingContext, "Remote mapping context cannot be null");
        }

        EidIdentificatorPair createPair(final OneAdjacency data, final int vni,
                                        final MappingContext mappingContext) {
            return new EidIdentificatorPairBuilder()
                    .setLocalEidId(new MappingId(localMappingContext.getId(getArrayAsEidLocal(
                            MappingsDumpParams.EidType.valueOf(data.eidType), data.leid, data.leidPrefixLen, vni),
                            mappingContext)))
                    .setRemoteEidId(new MappingId(remoteMappingContext.getId(getArrayAsEidLocal(
                            MappingsDumpParams.EidType.valueOf(data.eidType), data.reid, data.reidPrefixLen, vni),
                            mappingContext)))
                    .build();
        }
    }
}
