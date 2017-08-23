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

package io.fd.hc2vpp.nat.write;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.nat.util.MappingEntryContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.nat.dto.Nat44AddDelStaticMapping;
import io.fd.vpp.jvpp.nat.dto.Nat64AddDelStaticBib;
import io.fd.vpp.jvpp.nat.future.FutureJVppNatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.port.number.PortType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.port.number.port.type.SinglePortNumber;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MappingEntryCustomizer implements ListWriterCustomizer<MappingEntry, MappingEntryKey>,
        JvppReplyConsumer, Ipv4Translator, Ipv6Translator, ByteDataTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(MappingEntryCustomizer.class);

    private final FutureJVppNatFacade jvppNat;
    private final MappingEntryContext mappingEntryContext;

    MappingEntryCustomizer(final FutureJVppNatFacade jvppNat, final MappingEntryContext mappingEntryContext) {
        this.jvppNat = jvppNat;
        this.mappingEntryContext = mappingEntryContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<MappingEntry> id,
                                       @Nonnull final MappingEntry dataAfter,
                                       @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        // Only static mapping supported by SNAT for now
        checkArgument(dataAfter.getType() ==
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.MappingEntry.Type.Static,
                "Only static NAT entries are supported currently. Trying to write: %s entry", dataAfter.getType());
        final Long natInstanceId = id.firstKeyOf(NatInstance.class).getId();
        final Long mappingEntryId = id.firstKeyOf(MappingEntry.class).getIndex();
        LOG.debug("Writing mapping entry: {} for nat-instance(vrf): {}", natInstanceId, mappingEntryId);

        configureMapping(id, dataAfter, natInstanceId, true);

        // Store context mapping only if not already present under the same exact mapping
        synchronized (mappingEntryContext) {
            if (shouldStoreContextMapping(natInstanceId, mappingEntryId, dataAfter, writeContext)) {
                mappingEntryContext
                        .addEntry(natInstanceId, mappingEntryId, dataAfter, writeContext.getMappingContext());
            }
        }
        LOG.trace("Mapping entry: {} for nat-instance(vrf): {} written successfully", natInstanceId, id);
    }

    private void configureMapping(@Nonnull final InstanceIdentifier<MappingEntry> id,
                                  @Nonnull final MappingEntry entry,
                                  @Nonnull final Long natInstanceId,
                                  final boolean isAdd) throws WriteFailedException {
        final IpAddress internalSrcAddress = entry.getInternalSrcAddress();
        final Ipv4Address internalV4SrcAddress = internalSrcAddress.getIpv4Address();
        final Ipv6Address internalV6SrcAddress = internalSrcAddress.getIpv6Address();
        if (internalV4SrcAddress != null) {
            final Nat44AddDelStaticMapping request = getNat44Request(id, entry, natInstanceId, isAdd);
            getReplyForWrite(jvppNat.nat44AddDelStaticMapping(request).toCompletableFuture(), id);
        } else {
            checkState(internalV6SrcAddress != null,
                    "internalSrcAddress.getIpv6Address() should not return null if v4 address is not given");
            final Nat64AddDelStaticBib request = getNat64Request(id, entry, natInstanceId, isAdd);
            getReplyForWrite(jvppNat.nat64AddDelStaticBib(request).toCompletableFuture(), id);
        }
    }

    /**
     * Check whether entry is already stored in context under the same index.
     *
     * @return true if it's not yet stored under same index, false otherwise.
     */
    private boolean shouldStoreContextMapping(final long natInstanceId, final long mappingEntryId,
                                              final MappingEntry dataAfter,
                                              final WriteContext writeCtx) {
        if (!mappingEntryContext.containsEntry(natInstanceId, dataAfter, writeCtx.getMappingContext())) {
            return true;
        }

        final Optional<Long> storedIndex =
                mappingEntryContext.getStoredIndex(natInstanceId, dataAfter, writeCtx.getMappingContext());
        if (!storedIndex.isPresent()) {
            return true;
        }

        if (storedIndex.get() != mappingEntryId) {
            return true;
        }

        return false;
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<MappingEntry> id,
                                        @Nonnull final MappingEntry dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final long natInstanceId = id.firstKeyOf(NatInstance.class).getId();
        final MappingEntryKey mappingEntryKey = id.firstKeyOf(MappingEntry.class);
        LOG.debug("Deleting mapping entry: {} for nat-instance(vrf): {}", natInstanceId, mappingEntryKey);

        configureMapping(id, dataBefore, natInstanceId, false);
        mappingEntryContext.removeEntry(natInstanceId, dataBefore, writeContext.getMappingContext());
        LOG.trace("Mapping entry: {} for nat-instance(vrf): {} deleted successfully", natInstanceId, id);
    }

    private Nat44AddDelStaticMapping getNat44Request(final InstanceIdentifier<MappingEntry> id,
                                                 final MappingEntry mappingEntry,
                                                 final Long natInstanceId,
                                                 final boolean isAdd)
            throws WriteFailedException.CreateFailedException {
        final Nat44AddDelStaticMapping request = new Nat44AddDelStaticMapping();
        request.isAdd = booleanToByte(isAdd);
        // VPP uses int, model long
        request.vrfId = natInstanceId.intValue();

        final Ipv4Address internalAddress = mappingEntry.getInternalSrcAddress().getIpv4Address();
        checkArgument(internalAddress != null, "No Ipv4 present in internal-src-address %s",
                mappingEntry.getInternalSrcAddress());

        request.addrOnly = 1;
        request.localIpAddress =
                ipv4AddressNoZoneToArray(mappingEntry.getInternalSrcAddress().getIpv4Address().getValue());
        request.externalIpAddress = ipv4AddressNoZoneToArray(mappingEntry.getExternalSrcAddress().getValue());
        request.externalSwIfIndex = -1; // external ip address is ignored if externalSwIfIndex is given
        request.protocol = -1;
        final Short protocol = mappingEntry.getTransportProtocol();
        if (protocol != null) {
            checkArgument(protocol == 1 || protocol == 6 || protocol == 17,
                    "Unsupported protocol %s only ICMP(1), TCP(6) and UDP(17) are currently supported for Nat44",
                    protocol);
            request.protocol = protocol.byteValue();
        }

        Optional<Short> internalPortNumber = getPortNumber(id, mappingEntry,
                (entry) -> Optional.fromNullable(entry.getInternalSrcPort()).transform(PortNumber::getPortType));
        Optional<Short> externalPortNumber = getPortNumber(id, mappingEntry,
                (entry) -> Optional.fromNullable(entry.getExternalSrcPort()).transform(PortNumber::getPortType));
        if (internalPortNumber.isPresent() && externalPortNumber.isPresent()) {
            request.addrOnly = 0;
            request.localPort = internalPortNumber.get();
            request.externalPort = externalPortNumber.get();
        }
        return request;
    }

    private Nat64AddDelStaticBib getNat64Request(final InstanceIdentifier<MappingEntry> id,
                                                 final MappingEntry mappingEntry,
                                                 final Long natInstanceId,
                                                 final boolean isAdd)
            throws WriteFailedException.CreateFailedException {
        final Nat64AddDelStaticBib request = new Nat64AddDelStaticBib();
        request.isAdd = booleanToByte(isAdd);
        // VPP uses int, model long
        request.vrfId = natInstanceId.intValue();

        final Ipv6Address internalAddress = mappingEntry.getInternalSrcAddress().getIpv6Address();
        checkArgument(internalAddress != null, "No Ipv6 present in internal-src-address %s",
                mappingEntry.getInternalSrcAddress());


        request.iAddr = ipv6AddressNoZoneToArray(internalAddress);
        request.oAddr = ipv4AddressNoZoneToArray(mappingEntry.getExternalSrcAddress().getValue());
        request.proto = -1;
        final Short protocol = mappingEntry.getTransportProtocol();
        if (protocol != null) {
            checkArgument(protocol == 1 || protocol == 6 || protocol == 17 || protocol == 58,
                    "Unsupported protocol %s only ICMP(1), IPv6-ICMP(58), TCP(6) and UDP(17) are currently supported for Nat64",
                    protocol);
            request.proto = protocol.byteValue();
        }

        Optional<Short> internalPortNumber = getPortNumber(id, mappingEntry,
                (entry) -> Optional.fromNullable(entry.getInternalSrcPort()).transform(PortNumber::getPortType));
        Optional<Short> externalPortNumber = getPortNumber(id, mappingEntry,
                (entry) -> Optional.fromNullable(entry.getExternalSrcPort()).transform(PortNumber::getPortType));
        if (internalPortNumber.isPresent() && externalPortNumber.isPresent()) {
            request.iPort = internalPortNumber.get();
            request.oPort = externalPortNumber.get();
        }
        return request;
    }


    private Optional<Short> getPortNumber(final InstanceIdentifier<MappingEntry> id, final MappingEntry dataAfter,
                                          final PortGetter portGetter) {
        return portGetter.getPortType(dataAfter).transform(port -> {
            if (port instanceof SinglePortNumber) {
                return ((SinglePortNumber) port).getSinglePortNumber().getValue().shortValue();
            } else {
                throw new IllegalArgumentException(
                        String.format("Only single port number supported. Submitted: %s for entry: %s",
                                dataAfter.getInternalSrcPort(), id));
            }
        });
    }

    interface PortGetter {
        Optional<PortType> getPortType(MappingEntry entry);
    }
}
