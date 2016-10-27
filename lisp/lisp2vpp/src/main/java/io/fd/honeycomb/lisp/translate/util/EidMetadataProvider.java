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
