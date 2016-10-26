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

package io.fd.honeycomb.nat.read;

import io.fd.honeycomb.nat.util.MappingEntryContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.vpp.util.Ipv4Translator;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.vpp.jvpp.snat.dto.SnatStaticMappingDetails;
import io.fd.vpp.jvpp.snat.dto.SnatStaticMappingDetailsReplyDump;
import io.fd.vpp.jvpp.snat.dto.SnatStaticMappingDump;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.mapping.entry.ExternalSrcPortBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.mapping.entry.InternalSrcPortBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.MappingTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.MappingTableBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.mapping.table.MappingEntryKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.port.number.port.type.SinglePortNumberBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MappingEntryCustomizer implements Ipv4Translator,
        InitializingListReaderCustomizer<MappingEntry, MappingEntryKey, MappingEntryBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(MappingEntryCustomizer.class);

    private final DumpCacheManager<SnatStaticMappingDetailsReplyDump, Void> dumpCacheManager;
    private final MappingEntryContext mappingEntryContext;

    MappingEntryCustomizer(final DumpCacheManager<SnatStaticMappingDetailsReplyDump, Void> dumpCacheManager,
                           final MappingEntryContext mappingEntryContext) {
        this.dumpCacheManager = dumpCacheManager;
        this.mappingEntryContext = mappingEntryContext;
    }

    @Nonnull
    @Override
    public MappingEntryBuilder getBuilder(@Nonnull final InstanceIdentifier<MappingEntry> id) {
        return new MappingEntryBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<MappingEntry> id,
                                      @Nonnull final MappingEntryBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        LOG.trace("Reading current attributes for mapping-entry: {}", id);

        final int idx = id.firstKeyOf(MappingEntry.class).getIndex().intValue();
        final int natInstanceId = id.firstKeyOf(NatInstance.class).getId().intValue();
        final List<SnatStaticMappingDetails> details =
                dumpCacheManager.getDump(id, getClass().getName(), ctx.getModificationCache(), null)
                        .or(new SnatStaticMappingDetailsReplyDump()).snatStaticMappingDetails;
        final SnatStaticMappingDetails snatStaticMappingDetails =
                mappingEntryContext.findDetails(details, natInstanceId, idx, ctx.getMappingContext());

        builder.setIndex((long) idx);
        builder.setType(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.MappingEntry.Type.Static);
        // Snat only supports ipv4 for now
        builder.setExternalSrcAddress(arrayToIpv4AddressNoZoneReversed(snatStaticMappingDetails.externalIpAddress));
        builder.setInternalSrcAddress(
                new IpAddress(arrayToIpv4AddressNoZoneReversed(snatStaticMappingDetails.localIpAddress)));

        if (snatStaticMappingDetails.addrOnly == 0) {
            builder.setExternalSrcPort(new ExternalSrcPortBuilder()
                    .setPortType(new SinglePortNumberBuilder().setSinglePortNumber(new PortNumber(
                            (int) snatStaticMappingDetails.externalPort))
                            .build())
                    .build());
            builder.setInternalSrcPort(new InternalSrcPortBuilder()
                    .setPortType(new SinglePortNumberBuilder().setSinglePortNumber(new PortNumber(
                            (int) snatStaticMappingDetails.localPort))
                            .build())
                    .build());
        }

        LOG.trace("Mapping-entry read as: {}", builder);
    }

    @Nonnull
    @Override
    public List<MappingEntryKey> getAllIds(@Nonnull final InstanceIdentifier<MappingEntry> id,
                                           @Nonnull final ReadContext context) throws ReadFailedException {
        final Long natInstanceId = id.firstKeyOf(NatInstance.class).getId();
        LOG.trace("Listing IDs for all mapping-entries within nat-instance(vrf):{}", natInstanceId);

        final List<MappingEntryKey> entryKeys =
                dumpCacheManager.getDump(id, getClass().getName(), context.getModificationCache(), null)
                        .or(new SnatStaticMappingDetailsReplyDump()).snatStaticMappingDetails.stream()
                        .filter(detail -> natInstanceId == detail.vrfId)
                        .map(detail -> mappingEntryContext
                                .getStoredOrArtificialIndex(natInstanceId, detail, context.getMappingContext()))
                        .map(MappingEntryKey::new)
                        .collect(Collectors.toList());
        LOG.debug("List of mapping-entry keys within nat-instance(vrf):{} : {}", natInstanceId, entryKeys);

        return entryKeys;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder,
                      @Nonnull final List<MappingEntry> readData) {
        ((MappingTableBuilder) builder).setMappingEntry(readData);
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntry> init(@Nonnull final InstanceIdentifier<MappingEntry> id,
                                                                                                                                                                          @Nonnull final MappingEntry readValue,
                                                                                                                                                                          @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryBuilder(readValue)
                        .build());
    }

    static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntry> getCfgId(final @Nonnull InstanceIdentifier<MappingEntry> id) {
        return NatInstanceCustomizer.getCfgId(RWUtils.cutId(id, NatInstance.class))
                .child(MappingTable.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntry.class,
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryKey(id.firstKeyOf(MappingEntry.class).getIndex()));
    }

    static final class MappingEntryDumpExecutor
            implements EntityDumpExecutor<SnatStaticMappingDetailsReplyDump, Void>, JvppReplyConsumer {

        private final FutureJVppSnatFacade jvppSnat;

        MappingEntryDumpExecutor(final FutureJVppSnatFacade jvppSnat) {
            this.jvppSnat = jvppSnat;
        }

        @Nonnull
        @Override
        public SnatStaticMappingDetailsReplyDump executeDump(final InstanceIdentifier<?> identifier, final Void params)
                throws ReadFailedException {
            return getReplyForRead(jvppSnat.snatStaticMappingDump(new SnatStaticMappingDump()).toCompletableFuture(),
                    identifier);
        }
    }
}
