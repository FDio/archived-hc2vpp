/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

import java.util.Optional;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.nat.util.MappingEntryContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.nat.dto.Nat44AddDelStaticMapping;
import io.fd.jvpp.nat.dto.Nat64AddDelStaticBib;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.Instance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntryKey;
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
        final Long natInstanceId = id.firstKeyOf(Instance.class).getId();
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
        final IpPrefix internalSrcPrefix = entry.getInternalSrcAddress();
        final Ipv4Prefix internalV4SrcPrefix = internalSrcPrefix.getIpv4Prefix();
        if (internalV4SrcPrefix != null) {
            final Nat44AddDelStaticMapping request = getNat44Request(id, entry, natInstanceId, isAdd);
            getReplyForWrite(jvppNat.nat44AddDelStaticMapping(request).toCompletableFuture(), id);
        } else {
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
        final long natInstanceId = id.firstKeyOf(Instance.class).getId();
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

        final Ipv4Prefix internalAddress = mappingEntry.getInternalSrcAddress().getIpv4Prefix();
        request.addrOnly = 1;
        request.localIpAddress = ipv4AddressPrefixToArray(internalAddress);
        request.externalIpAddress = ipv4AddressPrefixToArray(mappingEntry.getExternalSrcAddress().getIpv4Prefix());
        request.externalSwIfIndex = -1; // external ip address is ignored if externalSwIfIndex is given
        request.protocol = -1;
        final Short protocol = mappingEntry.getTransportProtocol();
        if (protocol != null) {
            request.protocol = protocol.byteValue();
        }

        final Integer internalPortNumber = getPortNumber(mappingEntry.getInternalSrcPort());
        final Integer externalPortNumber = getPortNumber(mappingEntry.getExternalSrcPort());
        if (internalPortNumber != null && externalPortNumber != null) {
            request.addrOnly = 0;
            request.localPort = internalPortNumber.shortValue();
            request.externalPort = externalPortNumber.shortValue();
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

        final Ipv6Prefix internalAddress = mappingEntry.getInternalSrcAddress().getIpv6Prefix();
        request.iAddr = ipv6AddressPrefixToArray(internalAddress);
        request.oAddr = ipv4AddressPrefixToArray(mappingEntry.getExternalSrcAddress().getIpv4Prefix());
        request.proto = -1;
        final Short protocol = mappingEntry.getTransportProtocol();
        if (protocol != null) {
            request.proto = protocol.byteValue();
        }

        final Integer internalPortNumber = getPortNumber(mappingEntry.getInternalSrcPort());
        final Integer externalPortNumber = getPortNumber(mappingEntry.getExternalSrcPort());
        if (internalPortNumber != null && externalPortNumber != null) {
            request.iPort = internalPortNumber.shortValue();
            request.oPort = externalPortNumber.shortValue();
        }
        return request;
    }

    private Integer getPortNumber(final PortNumber portNumber) {
        if (portNumber != null) {
            return portNumber.getStartPortNumber().getValue();
        }
        return null;
    }
}
