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

package io.fd.hc2vpp.acl.util.acl;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.acl.write.AclValidator;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppAcl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppAclAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppMacipAcl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.Ipv4AclType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.Ace;

/**
 * Extracts data from Acls.
 * Expects data validated by {@link AclValidator}
 */
public interface AclDataExtractor {

    /**
     * Checks if provided {@link Acl} has aces of type {@link Ipv4AclType} or {@link VppAcl}
     */
    default boolean isStandardAcl(@Nonnull final Acl acl) {
        Class<? extends AclBase> type =
                Preconditions.checkNotNull(acl.getType(), "Type is not set for ACL:{}", acl);
        return type.equals(VppAcl.class);
    }

    /**
     * Checks if provided {@link Acl} has aces of type {@link VppMacipAcl}
     */
    default boolean isMacIpAcl(@Nonnull final Acl acl) {
        Class<? extends AclBase> type =
                Preconditions.checkNotNull(acl.getType(), "Type is not set for ACL:{}", acl);
        return type.equals(VppMacipAcl.class);
    }

    default List<Ace> getAces(@Nonnull final Acl acl) {
        return Optional.ofNullable(acl.getAces()).orElseThrow(() ->
                new IllegalArgumentException(String.format("Unable to extract aces from %s", acl))).getAce();
    }

    /**
     * Convert {@link Acl} tag to byte array in US_ASCII
     */
    default byte[] getAclTag(@Nonnull final Acl acl) {
        final VppAclAugmentation augmentation = acl.augmentation(VppAclAugmentation.class);
        if (augmentation != null && augmentation.getTag() != null) {
            return augmentation.getTag().getBytes(StandardCharsets.US_ASCII);
        }
        return new byte[0];
    }
}
