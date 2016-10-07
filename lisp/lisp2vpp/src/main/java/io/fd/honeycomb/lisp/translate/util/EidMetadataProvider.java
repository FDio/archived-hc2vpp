package io.fd.honeycomb.lisp.translate.util;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.InstanceIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.LispAddressFamily;

/**
 * Trait providing metadata for eid's
 */
public interface EidMetadataProvider {

    /**
     * Returns new {@link org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder}
     * binded with metadata
     */
    default org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder newRemoteEidBuilder(
            @Nonnull final Class<? extends LispAddressFamily> eidAddressType,
            final int vni) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                .setAddressType(eidAddressType)
                .setVirtualNetworkId(new InstanceIdType(Long.valueOf(vni)));
    }

    /**
     * Returns new {@link org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder}
     * binded with metadata
     */
    default org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder newLocalEidBuilder(
            @Nonnull final Class<? extends LispAddressFamily> eidAddressType,
            final int vni) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder()
                .setAddressType(eidAddressType)
                .setVirtualNetworkId(new InstanceIdType(Long.valueOf(vni)));
    }

}
