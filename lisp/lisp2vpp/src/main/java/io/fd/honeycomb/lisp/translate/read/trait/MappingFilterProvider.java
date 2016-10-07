package io.fd.honeycomb.lisp.translate.read.trait;

import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV4;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV6;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.MAC;

import java.util.function.Predicate;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.LispEidTableDetails;

/**
 * Trait providing predicates to filter mappings to respective subtables
 */
public interface MappingFilterProvider {

    Predicate<LispEidTableDetails> BRIDGE_DOMAIN_MAPPINGS_ONLY =
            (LispEidTableDetails detail) -> detail.eidType == MAC.getValue();

    Predicate<LispEidTableDetails> VRF_MAPPINGS_ONLY =
            (LispEidTableDetails detail) -> detail.eidType == IPV4.getValue() || detail.eidType == IPV6.getValue();

    default Predicate<LispEidTableDetails> subtableFilterForLocalMappings(
            @Nonnull final InstanceIdentifier<LocalMapping> identifier) {

        if (identifier.firstIdentifierOf(VrfSubtable.class) != null) {
            return VRF_MAPPINGS_ONLY;
        } else if (identifier.firstIdentifierOf(BridgeDomainSubtable.class) != null) {
            return BRIDGE_DOMAIN_MAPPINGS_ONLY;
        } else {
            throw new IllegalArgumentException("Cannot determine mappings predicate for " + identifier);
        }
    }

    default Predicate<LispEidTableDetails> subtableFilterForRemoteMappings(
            @Nonnull final InstanceIdentifier<RemoteMapping> identifier) {

        if (identifier.firstIdentifierOf(VrfSubtable.class) != null) {
            return VRF_MAPPINGS_ONLY;
        } else if (identifier.firstIdentifierOf(BridgeDomainSubtable.class) != null) {
            return BRIDGE_DOMAIN_MAPPINGS_ONLY;
        } else {
            throw new IllegalArgumentException("Cannot determine mappings predicate for " + identifier);
        }
    }
}
