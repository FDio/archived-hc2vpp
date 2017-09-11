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

package io.fd.hc2vpp.lisp.translate.read.trait;

import com.google.common.collect.ImmutableSet;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4PrefixAfi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv6Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv6PrefixAfi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.LispAddressFamily;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.MacAfi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Trait that verifies data for mappings
 */
public interface MappingProducer {

    Set<Class<? extends LispAddressFamily>> VRF_ALLOWED_ADDRESS_TYPES = ImmutableSet.of(
            Ipv4Afi.class,
            Ipv6Afi.class,
            Ipv4PrefixAfi.class,
            Ipv6PrefixAfi.class);

    /**
     * Checks whether provided {@link LocalMapping} can be written under subtree identified by {@link InstanceIdentifier}
     */
    default void checkAllowedCombination(@Nonnull final InstanceIdentifier<LocalMapping> identifier,
                                         @Nonnull final LocalMapping data) throws WriteFailedException {
        final Class<? extends LispAddressFamily> eidAddressType = data.getEid().getAddressType();

        if (identifier.firstIdentifierOf(VrfSubtable.class) != null) {
            if (!VRF_ALLOWED_ADDRESS_TYPES.contains(eidAddressType)) {
                throw new WriteFailedException.CreateFailedException(identifier, data,
                        new IllegalArgumentException("Only Ipv4/Ipv6 eid's are allowed for Vrf Subtable"));
            }
        } else if (identifier.firstIdentifierOf(BridgeDomainSubtable.class) != null) {
            if (MacAfi.class != eidAddressType) {
                throw new WriteFailedException.CreateFailedException(identifier, data,
                        new IllegalArgumentException("Only Mac eid's are allowed for Bridge Domain Subtable"));
            }
        }
    }

    default void checkAllowedCombination(@Nonnull final InstanceIdentifier<RemoteMapping> identifier,
                                         @Nonnull final RemoteMapping data) throws WriteFailedException {
        final Class<? extends LispAddressFamily> eidAddressType = data.getEid().getAddressType();

        if (identifier.firstIdentifierOf(VrfSubtable.class) != null) {
            if (!VRF_ALLOWED_ADDRESS_TYPES.contains(eidAddressType)) {
                throw new WriteFailedException.CreateFailedException(identifier, data,
                        new IllegalArgumentException("Only Ipv4/Ipv6 eid's are allowed for Vrf Subtable"));
            }
        } else if (identifier.firstIdentifierOf(BridgeDomainSubtable.class) != null) {
            if (MacAfi.class != eidAddressType) {
                throw new WriteFailedException.CreateFailedException(identifier, data,
                        new IllegalArgumentException("Only Mac eid's are allowed for Bridge Domain Subtable"));
            }
        }
    }
}
