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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class MappingEntryValidator implements Validator<MappingEntry>, Ipv4Translator, Ipv6Translator {
    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<MappingEntry> id,
                              @Nonnull final MappingEntry mappingEntry,
                              @Nonnull final WriteContext writeContext)
        throws CreateValidationFailedException {
        try {
            validateMappingEntry(id, mappingEntry);
        } catch (RuntimeException e) {
            throw new CreateValidationFailedException(id, mappingEntry, e);
        }
    }

    private void validateMappingEntry(final InstanceIdentifier<MappingEntry> id, final MappingEntry mappingEntry) {
        validateMappingEntryType(mappingEntry);
        validateInternalSrcAddress(mappingEntry);
        validateExternalSrcAddress(mappingEntry);
        validateTransportProtocol(mappingEntry);
        validatePortNumber(id, mappingEntry.getInternalSrcPort());
        validatePortNumber(id, mappingEntry.getExternalSrcPort());
    }

    @VisibleForTesting
    void validateMappingEntryType(final MappingEntry mappingEntry) {
        // Only static mapping are currently supported
        checkArgument(mappingEntry.getType()
                == org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.MappingEntry.Type.Static,
            "Only static NAT entries are supported currently. Trying to write: %s entry", mappingEntry.getType());
    }

    @VisibleForTesting
    void validateInternalSrcAddress(final MappingEntry mappingEntry) {
        final IpPrefix internalSrcPrefix = mappingEntry.getInternalSrcAddress();
        final Ipv4Prefix internalV4SrcPrefix = internalSrcPrefix.getIpv4Prefix();
        final Ipv6Prefix internalV6SrcPrefix = internalSrcPrefix.getIpv6Prefix();
        if (internalV4SrcPrefix != null) {
            checkArgument(extractPrefix(internalV4SrcPrefix) == 32,
                "Only /32 prefix in internal-src-address is supported, but was %s", internalV4SrcPrefix);
        } else {
            checkState(internalV6SrcPrefix != null,
                "internalSrcPrefix.getIpv6Prefix() should not return null if Ipv4 prefix is not given");
            checkArgument(extractPrefix(internalV6SrcPrefix) == (byte) 128,
                "Only /128 prefix in internal-src-address is supported, but was %s", internalV6SrcPrefix);
        }
    }

    @VisibleForTesting
    void validateExternalSrcAddress(final MappingEntry mappingEntry) {
        final IpPrefix externalSrcAddress = mappingEntry.getExternalSrcAddress();
        checkArgument(externalSrcAddress != null, "The external-src-address leaf is missing");
        final Ipv4Prefix ipv4Prefix = externalSrcAddress.getIpv4Prefix();
        checkArgument(ipv4Prefix != null, "No Ipv4 present in external-src-address %s", externalSrcAddress);
        checkArgument(extractPrefix(ipv4Prefix) == 32,
            "Only /32 prefix in external-src-address is supported, but was %s", ipv4Prefix);
    }

    @VisibleForTesting
    static void validateTransportProtocol(final MappingEntry mappingEntry) {
        final Short protocol = mappingEntry.getTransportProtocol();
        if (protocol == null) {
            return;
        }
        checkArgument(protocol == 1 || protocol == 6 || protocol == 17 || protocol == 58,
            "Unsupported protocol %s only ICMP(1), IPv6-ICMP(58), TCP(6) and UDP(17) are currently supported",
            protocol);
    }

    @VisibleForTesting
    static void validatePortNumber(final InstanceIdentifier<MappingEntry> id, final PortNumber portNumber) {
        if (portNumber == null) {
            return;
        }
        checkArgument(portNumber.getStartPortNumber() != null && portNumber.getEndPortNumber() == null,
            "Only single port number supported. Submitted: %s for entry: %s", portNumber, id);
    }
}
