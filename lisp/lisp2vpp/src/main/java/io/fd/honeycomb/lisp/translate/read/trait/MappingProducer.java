package io.fd.honeycomb.lisp.translate.read.trait;

import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv6Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.LispAddressFamily;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.MacAfi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Trait that verifies data for mappings
 */
public interface MappingProducer {

    /**
     * Checks whether provided {@link LocalMapping} can be written under subtree idenfied by {@link InstanceIdentifier}
     */
    default void checkAllowedCombination(@Nonnull final InstanceIdentifier<LocalMapping> identifier,
                                         @Nonnull final LocalMapping data) throws WriteFailedException {
        final Class<? extends LispAddressFamily> eidAddressType = data.getEid().getAddressType();

        if (identifier.firstIdentifierOf(VrfSubtable.class) != null) {
            if (Ipv4Afi.class != eidAddressType && Ipv6Afi.class != eidAddressType) {
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
            if (Ipv4Afi.class != eidAddressType && Ipv6Afi.class != eidAddressType) {
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
