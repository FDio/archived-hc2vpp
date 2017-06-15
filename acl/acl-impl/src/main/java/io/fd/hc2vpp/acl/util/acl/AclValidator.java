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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.AceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppMacipAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.VppMacipAce;

/**
 * Validate Acl data if processable by vpp
 */
public interface AclValidator {

    Set<Class<? extends AclBase>> SUPPORTED_ACL_TYPES = ImmutableSet.of(VppAcl.class, VppMacipAcl.class);

    Map<Class<? extends AclBase>, Class<? extends AceType>> ACL_ACE_PAIRS = ImmutableMap.of(
            VppAcl.class, VppAce.class,
            VppMacipAcl.class, VppMacipAce.class);

    static void isSupportedAclType(final Acl acl) {
        checkArgument(SUPPORTED_ACL_TYPES.contains(acl.getAclType()),
                "Unsupported Acl type %s detected for acl %s, allowed types are %s", acl.getAclType(),
                acl.getAclName(), SUPPORTED_ACL_TYPES);
    }

    static void hasConsistentAceTypeForAclType(final Acl acl) {
        checkTypesSame(acl.getAccessListEntries().getAce(), acl.getAclName(),
                checkNotNull(ACL_ACE_PAIRS.get(acl.getAclType()), "Unsupported ACL type %s for ACL %s",
                        acl.getAclType(), acl.getAclName()));
    }

    static void checkTypesSame(final List<Ace> aces, final String aclName, final Class<? extends AceType> aceType) {
        final Set<AceType> unsupportedAceTypes = aces.stream()
                .map(Ace::getMatches)
                .map(Matches::getAceType)
                .filter(aceType::equals)
                .collect(Collectors.toSet());
        checkArgument(unsupportedAceTypes.isEmpty(), "Detected unsupported ace types [%s] for ACL %s, expected %s",
                unsupportedAceTypes, aclName, aceType);
    }

    static void hasAceList(final Acl acl) {
        //checks if aces are defined
        checkArgument(!checkNotNull(checkNotNull(acl.getAccessListEntries(), "No access list entries defined")
                .getAce(), "No aces defined")
                .isEmpty(), "Empty ace list defined");
    }

    default void validateAcl(@Nonnull final Acl acl) {
        hasAceList(acl);
        isSupportedAclType(acl);
        hasConsistentAceTypeForAclType(acl);
    }
}
