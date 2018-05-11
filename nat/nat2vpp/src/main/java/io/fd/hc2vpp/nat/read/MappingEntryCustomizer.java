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

package io.fd.hc2vpp.nat.read;

import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.nat.util.MappingEntryContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.vpp.jvpp.nat.dto.Nat44StaticMappingDetails;
import io.fd.vpp.jvpp.nat.dto.Nat44StaticMappingDetailsReplyDump;
import io.fd.vpp.jvpp.nat.dto.Nat44StaticMappingDump;
import io.fd.vpp.jvpp.nat.dto.Nat64BibDetails;
import io.fd.vpp.jvpp.nat.dto.Nat64BibDetailsReplyDump;
import io.fd.vpp.jvpp.nat.dto.Nat64BibDump;
import io.fd.vpp.jvpp.nat.future.FutureJVppNatFacade;
import java.util.List;
import java.util.Optional;
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

final class MappingEntryCustomizer implements Ipv4Translator, Ipv6Translator,
        InitializingListReaderCustomizer<MappingEntry, MappingEntryKey, MappingEntryBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(MappingEntryCustomizer.class);

    private final DumpCacheManager<Nat44StaticMappingDetailsReplyDump, Void> nat44DumpManager;
    private final DumpCacheManager<Nat64BibDetailsReplyDump, Void> nat64DumpManager;
    private final MappingEntryContext mappingEntryContext;

    MappingEntryCustomizer(
            final DumpCacheManager<Nat44StaticMappingDetailsReplyDump, Void> nat44DumpManager,
            final DumpCacheManager<Nat64BibDetailsReplyDump, Void> nat64DumpManager,
            final MappingEntryContext mappingEntryContext) {
        this.nat44DumpManager = nat44DumpManager;
        this.nat64DumpManager = nat64DumpManager;
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
        final List<Nat44StaticMappingDetails> nat44Details =
                nat44DumpManager.getDump(id, ctx.getModificationCache())
                        .or(new Nat44StaticMappingDetailsReplyDump()).nat44StaticMappingDetails;
        final Optional<Nat44StaticMappingDetails> nat44StaticMappingDetails =
                mappingEntryContext.findDetailsNat44(nat44Details, natInstanceId, idx, ctx.getMappingContext());

        if (nat44StaticMappingDetails.isPresent()) {
            readNat44Entry(builder, idx, nat44StaticMappingDetails.get());
        } else {
            final List<Nat64BibDetails> nat64Details =
                    nat64DumpManager.getDump(id, ctx.getModificationCache())
                            .or(new Nat64BibDetailsReplyDump()).nat64BibDetails;

            final Optional<Nat64BibDetails> nat64StaticMappingDetails =
                    mappingEntryContext.findDetailsNat64(nat64Details, natInstanceId, idx, ctx.getMappingContext());

            if (nat64StaticMappingDetails.isPresent()) {
                readNat64Entry(builder, idx, nat64StaticMappingDetails.get());
            }
        }


        LOG.trace("Mapping-entry read as: {}", builder);
    }

    private void readNat44Entry(@Nonnull final MappingEntryBuilder builder,
                                final int index, final Nat44StaticMappingDetails detail) {
        builder.setIndex((long) index);
        builder.setType(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.MappingEntry.Type.Static);
        builder.setExternalSrcAddress(arrayToIpv4AddressNoZone(detail.externalIpAddress));
        builder.setInternalSrcAddress(
                new IpAddress(arrayToIpv4AddressNoZone(detail.localIpAddress)));

        if (detail.addrOnly == 0) {
            builder.setExternalSrcPort(new ExternalSrcPortBuilder()
                    .setPortType(new SinglePortNumberBuilder().setSinglePortNumber(new PortNumber(
                            Short.toUnsignedInt(detail.externalPort)))
                            .build())
                    .build());
            builder.setInternalSrcPort(new InternalSrcPortBuilder()
                    .setPortType(new SinglePortNumberBuilder().setSinglePortNumber(new PortNumber(
                            Short.toUnsignedInt(detail.localPort)))
                            .build())
                    .build());
        }
    }

    private void readNat64Entry(@Nonnull final MappingEntryBuilder builder,
                                final int index, final Nat64BibDetails detail) {
        builder.setIndex((long) index);
        if (detail.isStatic == 1) {
            builder.setType(
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.MappingEntry.Type.Static);
        } else {
            builder.setType(
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.MappingEntry.Type.Dynamic);
        }
        builder.setExternalSrcAddress(arrayToIpv4AddressNoZone(detail.oAddr));
        builder.setInternalSrcAddress(
                new IpAddress(arrayToIpv6AddressNoZone(detail.iAddr)));

        builder.setExternalSrcPort(new ExternalSrcPortBuilder()
                .setPortType(new SinglePortNumberBuilder().setSinglePortNumber(new PortNumber(
                        Short.toUnsignedInt(detail.oPort)))
                        .build())
                .build());
        builder.setInternalSrcPort(new InternalSrcPortBuilder()
                .setPortType(new SinglePortNumberBuilder().setSinglePortNumber(new PortNumber(
                        Short.toUnsignedInt(detail.iPort)))
                        .build())
                .build());
    }

    @Nonnull
    @Override
    public List<MappingEntryKey> getAllIds(@Nonnull final InstanceIdentifier<MappingEntry> id,
                                           @Nonnull final ReadContext context) throws ReadFailedException {
        final Long natInstanceId = id.firstKeyOf(NatInstance.class).getId();
        LOG.trace("Listing IDs for all mapping-entries within nat-instance(vrf):{}", natInstanceId);

        final List<MappingEntryKey> entryKeys =
                nat44DumpManager.getDump(id, context.getModificationCache())
                        .or(new Nat44StaticMappingDetailsReplyDump()).nat44StaticMappingDetails.stream()
                        .filter(detail -> natInstanceId == detail.vrfId)
                        .map(detail -> mappingEntryContext
                                .getStoredOrArtificialIndex(natInstanceId, detail, context.getMappingContext()))
                        .map(MappingEntryKey::new)
                        .collect(Collectors.toList());


        final List<MappingEntryKey> nat64Keys =
                nat64DumpManager.getDump(id, context.getModificationCache())
                        .or(new Nat64BibDetailsReplyDump()).nat64BibDetails.stream()
                        .filter(detail -> natInstanceId == detail.vrfId)
                        .map(detail -> mappingEntryContext
                                .getStoredOrArtificialIndex(natInstanceId, detail, context.getMappingContext()))
                        .map(MappingEntryKey::new)
                        .collect(Collectors.toList());
        entryKeys.addAll(nat64Keys);
        LOG.debug("List of mapping-entry keys within nat-instance(vrf):{} : {}", natInstanceId, entryKeys);

        return entryKeys;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder,
                      @Nonnull final List<MappingEntry> readData) {
        ((MappingTableBuilder) builder).setMappingEntry(readData);
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntry> init(
            @Nonnull final InstanceIdentifier<MappingEntry> id,
            @Nonnull final MappingEntry readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryBuilder(
                        readValue)
                        .build());
    }

    static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntry> getCfgId(
            final @Nonnull InstanceIdentifier<MappingEntry> id) {
        return NatInstanceCustomizer.getCfgId(RWUtils.cutId(id, NatInstance.class))
                .child(MappingTable.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntry.class,
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryKey(
                                id.firstKeyOf(MappingEntry.class).getIndex()));
    }

    static final class MappingEntryNat44DumpExecutor
            implements EntityDumpExecutor<Nat44StaticMappingDetailsReplyDump, Void>, JvppReplyConsumer {

        private final FutureJVppNatFacade jvppNat;

        MappingEntryNat44DumpExecutor(final FutureJVppNatFacade jvppNat) {
            this.jvppNat = jvppNat;
        }

        @Nonnull
        @Override
        public Nat44StaticMappingDetailsReplyDump executeDump(final InstanceIdentifier<?> identifier, final Void params)
                throws ReadFailedException {
            return getReplyForRead(jvppNat.nat44StaticMappingDump(new Nat44StaticMappingDump()).toCompletableFuture(),
                    identifier);
        }
    }

    static final class MappingEntryNat64DumpExecutor
            implements EntityDumpExecutor<Nat64BibDetailsReplyDump, Void>, JvppReplyConsumer {

        private final FutureJVppNatFacade jvppNat;

        MappingEntryNat64DumpExecutor(final FutureJVppNatFacade jvppNat) {
            this.jvppNat = jvppNat;
        }

        @Nonnull
        @Override
        public Nat64BibDetailsReplyDump executeDump(final InstanceIdentifier<?> identifier, final Void params)
                throws ReadFailedException {
            final Nat64BibDump dump = new Nat64BibDump();
            dump.proto = -1; // dump entries for all protocols
            return getReplyForRead(jvppNat.nat64BibDump(dump).toCompletableFuture(), identifier);
        }
    }
}
