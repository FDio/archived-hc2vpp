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

package io.fd.hc2vpp.nat.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.nat.dto.Nat44StaticMappingDetails;
import io.fd.vpp.jvpp.nat.dto.Nat64BibDetails;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.nat.context.rev161214.NatMappingEntryCtxAugmentation;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.nat.context.rev161214.mapping.entry.context.attributes.NatMappingEntryContext;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.nat.context.rev161214.mapping.entry.context.attributes.nat.mapping.entry.context.NatInstance;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.nat.context.rev161214.mapping.entry.context.attributes.nat.mapping.entry.context.NatInstanceKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.nat.context.rev161214.mapping.entry.context.attributes.nat.mapping.entry.context.nat.instance.MappingTable;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.nat.context.rev161214.mapping.entry.context.attributes.nat.mapping.entry.context.nat.instance.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.nat.context.rev161214.mapping.entry.context.attributes.nat.mapping.entry.context.nat.instance.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.nat.context.rev161214.mapping.entry.context.attributes.nat.mapping.entry.context.nat.instance.mapping.table.MappingEntryKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context tracker for Nat Mapping entries.
 */
@ThreadSafe
public class MappingEntryContext implements Ipv4Translator, Ipv6Translator {

    private static final Logger LOG = LoggerFactory.getLogger(MappingEntryContext.class);

    /**
     * Add mapping entry to index mapping to context.
     */
    public synchronized void addEntry(final long natInstanceId,
                                      final long entryId,
                                      @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntry entry,
                                      @Nonnull final MappingContext mappingContext) {
        final InstanceIdentifier<MappingEntry> id = getId(natInstanceId, entryToKey(entry));
        checkArgument(!containsEntry(natInstanceId, entry, mappingContext), "Mapping for %s already present", id);
        mappingContext.put(id, toCtxMapEntry(entry, entryId));
    }

    /**
     * Check whether mapping entry to index mapping already exists in context.
     */
    public synchronized boolean containsEntry(final long natInstanceId,
                                              @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntry entry,
                                              @Nonnull final MappingContext mappingContext) {
        final InstanceIdentifier<MappingEntry> id = getId(natInstanceId, entryToKey(entry));
        return mappingContext.read(id).isPresent();
    }

    @VisibleForTesting
    static InstanceIdentifier<MappingEntry> getId(final Long natInstanceId, final MappingEntryKey key) {
        return getTableId(natInstanceId).child(MappingEntry.class, key);
    }

    @VisibleForTesting
    static InstanceIdentifier<MappingTable> getTableId(final long natInstanceId) {
        return InstanceIdentifier.create(Contexts.class)
                .augmentation(NatMappingEntryCtxAugmentation.class)
                .child(NatMappingEntryContext.class)
                .child(NatInstance.class, new NatInstanceKey(natInstanceId))
                .child(MappingTable.class);
    }

    @VisibleForTesting
    static MappingEntryKey entryToKey(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntry entry) {
        // Only IPv4
        return new MappingEntryKey(entry.getExternalSrcAddress(), entry.getInternalSrcAddress());
    }

    private MappingEntryKey entryToKey(final Nat44StaticMappingDetails entry) {
        // Only IPv4
        return new MappingEntryKey(
            new IpPrefix(new Ipv4Prefix(toIpv4Prefix(entry.externalIpAddress,32))),
            new IpPrefix(new Ipv4Prefix(toIpv4Prefix(entry.localIpAddress, 32))));
    }

    private MappingEntryKey entryToKey(final Nat64BibDetails entry) {
        return new MappingEntryKey(
                new IpPrefix(new Ipv4Prefix(toIpv4Prefix(entry.oAddr, 32))),
                new IpPrefix(new Ipv6Prefix(toIpv6Prefix(entry.iAddr, 128))));
    }

    private boolean equalEntries(final Nat44StaticMappingDetails detail, final MappingEntry ctxMappingEntry) {
        // Only IPv4
        final IpPrefix internalAddrFromDetails = new IpPrefix(toIpv4Prefix(detail.localIpAddress, 32));
        if (!ctxMappingEntry.getInternal().equals(internalAddrFromDetails)) {
            return false;
        }
        // Only IPv4
        final IpPrefix externalAddrFromDetails = new IpPrefix(toIpv4Prefix(detail.externalIpAddress, 32));
        if (!ctxMappingEntry.getExternal().equals(externalAddrFromDetails)) {
            return false;
        }
        return true;
    }

    private boolean equalEntries(final Nat64BibDetails detail, final MappingEntry ctxMappingEntry) {
        // Only IPv6
        final IpPrefix internalAddrFromDetails = new IpPrefix(toIpv6Prefix(detail.iAddr, 128));
        if (!ctxMappingEntry.getInternal().equals(internalAddrFromDetails)) {
            return false;
        }
        // Only IPv4
        final IpPrefix externalAddrFromDetails = new IpPrefix(toIpv4Prefix(detail.oAddr, 32));
        if (!ctxMappingEntry.getExternal().equals(externalAddrFromDetails)) {
            return false;
        }
        return true;
    }

    @VisibleForTesting
    static MappingEntry toCtxMapEntry(
            @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntry entry,
            final long entryId) {
        return new MappingEntryBuilder()
                .setKey(entryToKey(entry))
                .setIndex(entryId)
                .build();
    }

    private MappingEntry toCtxMapEntry(@Nonnull final Nat44StaticMappingDetails details, final long entryId) {
        return new MappingEntryBuilder()
                .setKey(entryToKey(details))
                .setIndex(entryId)
                .build();
    }

    private MappingEntry toCtxMapEntry(@Nonnull final Nat64BibDetails details, final long entryId) {
        return new MappingEntryBuilder()
                .setKey(entryToKey(details))
                .setIndex(entryId)
                .build();
    }

    /**
     * Delete mapping of mapping entry to index from context.
     */
    public synchronized void removeEntry(final long natInstanceId,
                                         @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntry entry,
                                         @Nonnull final MappingContext mappingContext) {
        mappingContext.delete(getId(natInstanceId, entryToKey(entry)));
    }

    /**
     * Find specific details in provided collection identified with provided index.
     */
    public synchronized java.util.Optional<Nat44StaticMappingDetails> findDetailsNat44(@Nonnull final List<Nat44StaticMappingDetails> details,
                                                                                       final long natInstanceId, final long idx,
                                                                                       @Nonnull final MappingContext mappingContext) {
        // Find mapping entry for Index
        final MappingEntry ctxMappingEntry = mappingContext.read(getTableId(natInstanceId))
                .transform(MappingTable::getMappingEntry)
                .or(Collections.emptyList())
                .stream()
                .filter(entry -> entry.getIndex() == idx)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to find context mapping for nat-instance: "
                        + natInstanceId + " and ID: " + idx));

        // Find which details matches the context stored entry under index
        return details.stream()
                .filter(detail -> equalEntries(detail, ctxMappingEntry))
                .findFirst();
    }

    /**
     * Find specific details in provided collection identified with provided index.
     */
    public synchronized java.util.Optional<Nat64BibDetails> findDetailsNat64(@Nonnull final List<Nat64BibDetails> details,
                                                                             final long natInstanceId, final long idx,
                                                                             @Nonnull final MappingContext mappingContext) {
        // Find mapping entry for Index
        final MappingEntry ctxMappingEntry = mappingContext.read(getTableId(natInstanceId))
                .transform(MappingTable::getMappingEntry)
                .or(Collections.emptyList())
                .stream()
                .filter(entry -> entry.getIndex() == idx)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to find context mapping for nat-instance: "
                        + natInstanceId + " and ID: " + idx));

        // Find which details matches the context stored entry under index
        return details.stream()
                .filter(detail -> equalEntries(detail, ctxMappingEntry))
                .findFirst();
    }

    /**
     * Get index for a mapping entry details or create an artificial one.
     */
    public synchronized long getStoredOrArtificialIndex(final Long natInstanceId,
                                                        @Nonnull final Nat44StaticMappingDetails details,
                                                        @Nonnull final MappingContext mappingContext) {
        return mappingContext.read(getId(natInstanceId, entryToKey(details)))
                .transform(MappingEntry::getIndex)
                .or(() -> getArtificialId(details, natInstanceId, mappingContext));
    }

    /**
     * Get index for a mapping entry details or create an artificial one.
     */
    public synchronized long getStoredOrArtificialIndex(final Long natInstanceId,
                                                        @Nonnull final Nat64BibDetails details,
                                                        @Nonnull final MappingContext mappingContext) {
        return mappingContext.read(getId(natInstanceId, entryToKey(details)))
                .transform(MappingEntry::getIndex)
                .or(() -> getArtificialId(details, natInstanceId, mappingContext));
    }

    /**
     * Get index for a stored mapping entry.
     */
    public synchronized Optional<Long> getStoredIndex(final long natInstanceId,
                                                      @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntry entry,
                                                      @Nonnull final MappingContext mappingContext) {
        return mappingContext.read(getId(natInstanceId, entryToKey(entry)))
                .transform(MappingEntry::getIndex);
    }

    private long getArtificialId(final Nat44StaticMappingDetails details, final Long natInstanceId,
                                 final MappingContext mappingContext) {
        LOG.trace("Assigning artificial ID for {}", details);
        final long artificialIdx = findFreeIndex(natInstanceId, mappingContext);
        LOG.debug("Artificial ID for {} assigned as: {}", details, artificialIdx);
        mappingContext.put(getId(natInstanceId, entryToKey(details)), toCtxMapEntry(details, artificialIdx));
        return artificialIdx;
    }

    private long getArtificialId(final Nat64BibDetails details, final Long natInstanceId,
                                 final MappingContext mappingContext) {
        LOG.trace("Assigning artificial ID for {}", details);
        final long artificialIdx = findFreeIndex(natInstanceId, mappingContext);
        LOG.debug("Artificial ID for {} assigned as: {}", details, artificialIdx);
        mappingContext.put(getId(natInstanceId, entryToKey(details)), toCtxMapEntry(details, artificialIdx));
        return artificialIdx;
    }

    private long findFreeIndex(final long natInstanceId, final MappingContext mappingContext) {
        return mappingContext.read(getTableId(natInstanceId))
                .transform(MappingTable::getMappingEntry)
                .or(Collections.emptyList())
                .stream()
                .map(MappingEntry::getIndex)
                .max(Comparator.naturalOrder())
                .map(i -> i + 1)
                .orElse(0L);
    }
}
