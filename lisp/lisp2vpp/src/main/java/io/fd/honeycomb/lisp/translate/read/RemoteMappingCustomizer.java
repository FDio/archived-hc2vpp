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
import static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.MapReplyAction.NoAction;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.LocatorDumpParams;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.LocatorDumpParams.LocatorDumpParamsBuilder;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.QuantityType;
import io.fd.honeycomb.lisp.translate.read.trait.LocatorReader;
import io.fd.honeycomb.lisp.translate.read.trait.MappingReader;
import io.fd.honeycomb.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.IdentifierCacheKeyFactory;
import io.fd.honeycomb.translate.vpp.util.AddressTranslator;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.core.dto.LispEidTableDetails;
import io.fd.vpp.jvpp.core.dto.LispEidTableDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.LispLocatorDetails;
import io.fd.vpp.jvpp.core.dto.LispLocatorDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.MapReplyAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.RemoteMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.RemoteMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.RemoteMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.NegativeMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.PositiveMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.negative.mapping.MapReplyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.RlocsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.Locator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.LocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.LocatorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading {@code RemoteMapping}<br>
 */
public class RemoteMappingCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<RemoteMapping, RemoteMappingKey, RemoteMappingBuilder>,
        EidTranslator, AddressTranslator, ByteDataTranslator, MappingReader, LocatorReader {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteMappingCustomizer.class);

    private final DumpCacheManager<LispEidTableDetailsReplyDump, MappingsDumpParams> dumpManager;
    private final DumpCacheManager<LispLocatorDetailsReplyDump, LocatorDumpParams> locatorsDumpManager;
    private final NamingContext locatorSetContext;
    private final EidMappingContext remoteMappingContext;

    public RemoteMappingCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                                   @Nonnull final NamingContext locatorSetContext,
                                   @Nonnull final EidMappingContext remoteMappingContext) {
        super(futureJvpp);
        this.locatorSetContext = checkNotNull(locatorSetContext, "Locator sets context not present");
        this.remoteMappingContext = checkNotNull(remoteMappingContext, "Remote mappings not present");
        // this one should have default scope == RemoteMapping
        this.dumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<LispEidTableDetailsReplyDump, MappingsDumpParams>()
                        .withExecutor(createMappingDumpExecutor(futureJvpp))
                        .build();

        // cache key needs to have locator set scope to not mix with cached data
        this.locatorsDumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<LispLocatorDetailsReplyDump, LocatorDumpParams>()
                        .withExecutor(createLocatorDumpExecutor(futureJvpp))
                        .withCacheKeyFactory(new IdentifierCacheKeyFactory(ImmutableSet.of(LocatorSet.class)))
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
                .setEidType(getEidType(eid))
                .setEid(getEidAsByteArray(eid))
                .setPrefixLength(getPrefixLength(eid))
                .setFilter(FilterType.REMOTE)
                .build();

        LOG.debug("Dumping data for LocalMappings(id={})", id);
        final Optional<LispEidTableDetailsReplyDump> replyOptional =
                dumpManager.getDump(id, ctx.getModificationCache(), dumpParams);

        if (!replyOptional.isPresent() || replyOptional.get().lispEidTableDetails.isEmpty()) {
            return;
        }

        LOG.debug("Valid dump loaded");

        LispEidTableDetails details = replyOptional.get().lispEidTableDetails.stream()
                .filter(subtableFilterForRemoteMappings(id))
                .filter(a -> compareAddresses(eid.getAddress(),
                        getArrayAsEidLocal(valueOf(a.eidType), a.eid, a.vni).getAddress()))
                .collect(
                        RWUtils.singleItemCollector());

        builder.setEid(getArrayAsEidRemote(valueOf(details.eidType), details.eid, details.vni));
        builder.setKey(new RemoteMappingKey(new MappingId(id.firstKeyOf(RemoteMapping.class).getId())));
        builder.setTtl(resolveTtl(details.ttl));
        builder.setAuthoritative(
                new RemoteMapping.Authoritative(byteToBoolean(details.authoritative)));
        resolveMappings(id, builder, details, ctx.getModificationCache(), ctx.getMappingContext());
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
        final int vni = id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier().intValue();

        if (vni == 0) {
            // ignoring default vni mapping
            // it's not relevant for us and we also don't store mapping for such eid's
            // such mapping is used to create helper local mappings to process remote ones
            return Collections.emptyList();
        }

        //requesting all remote with specific vni
        final MappingsDumpParams dumpParams = new MappingsDumpParamsBuilder()
                .setEidSet(QuantityType.ALL)
                .setFilter(FilterType.REMOTE)
                .build();

        LOG.debug("Dumping data for LocalMappings(id={})", id);
        final Optional<LispEidTableDetailsReplyDump> replyOptional =
                dumpManager.getDump(id, context.getModificationCache(), dumpParams);

        if (!replyOptional.isPresent() || replyOptional.get().lispEidTableDetails.isEmpty()) {
            return Collections.emptyList();
        }

        return replyOptional.get()
                .lispEidTableDetails
                .stream()
                .filter(a -> a.vni == vni)
                .filter(subtableFilterForRemoteMappings(id))
                .map(detail -> getArrayAsEidRemote(valueOf(detail.eidType), detail.eid, detail.vni))
                .map(remoteEid -> remoteMappingContext.getId(remoteEid, context.getMappingContext()))
                .map(MappingId::new)
                .map(RemoteMappingKey::new)
                .collect(Collectors.toList());
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<RemoteMapping> readData) {
        ((RemoteMappingsBuilder) builder).setRemoteMapping(readData);
    }

    private void resolveMappings(final InstanceIdentifier id, final RemoteMappingBuilder builder,
                                 final LispEidTableDetails details,
                                 final ModificationCache cache,
                                 final MappingContext mappingContext) throws ReadFailedException {

        if (details.action != 0) {
            // in this case ,negative action was defined
            bindNegativeMapping(builder, MapReplyAction.forValue(details.action));
        } else {
            // in this case, there is no clear determination whether negative action with NO_ACTION(value == 0) was defined,
            // or if its default value and remote locators, are defined, so only chance to determine so, is to dump locators for this mapping

            // cache key needs to have locator set scope to not mix with cached data
            final Optional<LispLocatorDetailsReplyDump> reply;

            // this will serve to achieve that locators have locator set scope
            final InstanceIdentifier<Interface> locatorIfaceIdentifier = InstanceIdentifier.create(LocatorSets.class)
                    .child(LocatorSet.class,
                            new LocatorSetKey(locatorSetContext.getName(details.locatorSetIndex, mappingContext)))
                    .child(Interface.class);
            try {
                reply = locatorsDumpManager.getDump(locatorIfaceIdentifier, cache,
                        new LocatorDumpParamsBuilder().setLocatorSetIndex(details.locatorSetIndex).build());
            } catch (ReadFailedException e) {
                throw new ReadFailedException(id,
                        new IllegalStateException("Unable to resolve Positive/Negative mapping for RemoteMapping",
                                e.getCause()));
            }

            if (!reply.isPresent() || reply.get().lispLocatorDetails.isEmpty()) {
                // no remote locators exist, therefore there was NO_ACTION defined
                bindNegativeMapping(builder, NoAction);
            } else {
                // bind remote locators
                bindPositiveMapping(builder, reply.get());
            }
        }
    }

    private void bindNegativeMapping(final RemoteMappingBuilder builder,
                                     final MapReplyAction action) {
        builder.setLocatorList(
                new NegativeMappingBuilder().setMapReply(new MapReplyBuilder().setMapReplyAction(action).build())
                        .build());
    }

    private void bindPositiveMapping(final RemoteMappingBuilder builder, final LispLocatorDetailsReplyDump reply) {
        builder.setLocatorList(
                new PositiveMappingBuilder()
                        .setRlocs(
                                new RlocsBuilder()
                                        .setLocator(reply
                                                .lispLocatorDetails
                                                .stream()
                                                .map(this::detailsToLocator)
                                                .collect(Collectors.toList()))
                                        .build()
                        )
                        .build()
        );
    }

    private Locator detailsToLocator(final LispLocatorDetails details) {
        final IpAddress address = arrayToIpAddressReversed(byteToBoolean(details.isIpv6), details.ipAddress);
        return new LocatorBuilder()
                .setAddress(address)
                .setKey(new LocatorKey(address))
                .setPriority((short) details.priority)
                .setWeight((short) details.weight)
                .build();
    }
}
