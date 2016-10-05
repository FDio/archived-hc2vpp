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

package io.fd.honeycomb.nat.write;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import io.fd.honeycomb.nat.util.MappingEntryContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.Ipv4Translator;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.snat.dto.SnatAddStaticMapping;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import javax.annotation.Nonnull;
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
        JvppReplyConsumer, Ipv4Translator {

    private static final Logger LOG = LoggerFactory.getLogger(MappingEntryCustomizer.class);

    private final FutureJVppSnatFacade jvppSnat;
    private final MappingEntryContext mappingEntryContext;

    MappingEntryCustomizer(final FutureJVppSnatFacade jvppSnat, final MappingEntryContext mappingEntryContext) {
        this.jvppSnat = jvppSnat;
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

        final SnatAddStaticMapping request = getRequest(id, dataAfter, natInstanceId, true);
        getReplyForWrite(jvppSnat.snatAddStaticMapping(request).toCompletableFuture(), id);

        // Store context mapping only if not already present under the same exact mapping
        synchronized (mappingEntryContext) {
            if (shouldStoreContextMapping(natInstanceId, mappingEntryId, dataAfter, writeContext)) {
                mappingEntryContext
                        .addEntry(natInstanceId, mappingEntryId, dataAfter, writeContext.getMappingContext());
            }
        }
        LOG.trace("Mapping entry: {} for nat-instance(vrf): {} written successfully", request.vrfId, id);
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
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<MappingEntry> id,
                                        @Nonnull final MappingEntry dataBefore,
                                        @Nonnull final MappingEntry dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("Mapping entry update not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<MappingEntry> id,
                                        @Nonnull final MappingEntry dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final long natInstanceId = id.firstKeyOf(NatInstance.class).getId();
        final MappingEntryKey mappingEntryKey = id.firstKeyOf(MappingEntry.class);
        LOG.debug("Deleting mapping entry: {} for nat-instance(vrf): {}", natInstanceId, mappingEntryKey);

        final SnatAddStaticMapping request = getRequest(id, dataBefore, natInstanceId, false);
        getReplyForWrite(jvppSnat.snatAddStaticMapping(request).toCompletableFuture(), id);
        mappingEntryContext.removeEntry(natInstanceId, dataBefore, writeContext.getMappingContext());
        LOG.trace("Mapping entry: {} for nat-instance(vrf): {} deleted successfully", request.vrfId, id);
    }

    private SnatAddStaticMapping getRequest(final InstanceIdentifier<MappingEntry> id,
                                            final MappingEntry dataAfter,
                                            final Long natInstanceId,
                                            final boolean isAdd)
            throws WriteFailedException.CreateFailedException {
        final SnatAddStaticMapping request = new SnatAddStaticMapping();
        request.isAdd = isAdd
                ? (byte) 1
                : 0;
        request.isIp4 = 1;
        // VPP uses int, model long
        request.vrfId = natInstanceId.intValue();

        // Snat supports only ipv4 now
        if (dataAfter.getInternalSrcAddress().getIpv4Address() == null) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter,
                    new UnsupportedOperationException(
                            String.format("No Ipv4 present for in address %s. Ipv6 not supported",
                                    dataAfter.getInternalSrcAddress())));
        }

        request.addrOnly = 1;
        request.localIpAddress =
                ipv4AddressNoZoneToArray(dataAfter.getInternalSrcAddress().getIpv4Address().getValue());
        request.externalIpAddress = ipv4AddressNoZoneToArray(dataAfter.getExternalSrcAddress().getValue());

        Optional<Short> internalPortNumber = getPortNumber(id, dataAfter,
                (entry) -> Optional.fromNullable(entry.getInternalSrcPort()).transform(PortNumber::getPortType));
        Optional<Short> externalPortNumber = getPortNumber(id, dataAfter,
                (entry) -> Optional.fromNullable(entry.getExternalSrcPort()).transform(PortNumber::getPortType));
        if (internalPortNumber.isPresent() && externalPortNumber.isPresent()) {
            request.addrOnly = 0;
            request.localPort = internalPortNumber.get();
            request.externalPort = externalPortNumber.get();
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
