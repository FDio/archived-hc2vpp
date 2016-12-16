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

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppMacipAce;

/**
 * Extracts data from Acls.
 * Expects data validated by {@link AclValidator}
 */
public interface AclDataExtractor {

    /**
     * Checks if provided {@link Acl} has aces of type {@link VppAce}
     */
    default boolean isStandardAcl(@Nonnull final Acl acl) {
        return acl.getAccessListEntries().getAce().stream()
                .map(Ace::getMatches)
                .map(Matches::getAceType)
                .filter(aceType -> aceType instanceof VppAce)
                .findAny()
                .isPresent();
    }

    /**
     * Checks if provided {@link Acl} has aces of type {@link VppMacipAce}
     */
    default boolean isMacIpAcl(@Nonnull final Acl acl) {
        return acl.getAccessListEntries().getAce().stream()
                .map(Ace::getMatches)
                .map(Matches::getAceType)
                .filter(aceType -> aceType instanceof VppMacipAce)
                .findAny()
                .isPresent();
    }

    default List<Ace> getAces(@Nonnull final Acl acl) {
        return acl.getAccessListEntries().getAce();
    }

    /**
     * Convert {@link Acl} name to byte array as UTF_8
     */
    default byte[] getAclNameAsBytes(@Nonnull final Acl acl) {
        return acl.getAclName().getBytes(StandardCharsets.UTF_8);
    }
}
