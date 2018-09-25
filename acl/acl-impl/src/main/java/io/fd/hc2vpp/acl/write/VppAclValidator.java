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

package io.fd.hc2vpp.acl.write;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.acl.util.acl.AclDataExtractor;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.UpdateValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.AceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.InterfaceAclAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclsBaseAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppMacipAclsBaseAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppMacipAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.VppMacipAce;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class VppAclValidator implements Validator<Acl>, AclDataExtractor {

    private static final Set<Class<? extends AclBase>> SUPPORTED_ACL_TYPES =
        ImmutableSet.of(VppAcl.class, VppMacipAcl.class);
    private static final Map<Class<? extends AclBase>, Class<? extends AceType>> ACL_ACE_PAIRS =
        ImmutableMap.of(VppAcl.class, VppAce.class, VppMacipAcl.class, VppMacipAce.class);

    @Override
    public void validateWrite(final InstanceIdentifier<Acl> id, final Acl dataAfter, final WriteContext ctx)
        throws CreateValidationFailedException {
        try {
            validateAcl(dataAfter);
        } catch (RuntimeException e) {
            throw new CreateValidationFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void validateUpdate(final InstanceIdentifier<Acl> id, final Acl dataBefore, final Acl dataAfter,
                               final WriteContext ctx) throws UpdateValidationFailedException {
        try {
            validateAcl(dataAfter);
        } catch (RuntimeException e) {
            throw new UpdateValidationFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void validateDelete(final InstanceIdentifier<Acl> id, final Acl dataBefore, final WriteContext ctx)
        throws DeleteValidationFailedException {
        try {
            validateAcl(dataBefore);
            final List<Interface> references = checkAclReferenced(ctx, dataBefore);
            // references must be check, to not leave dead references in configuration
            checkState(references.isEmpty(),
                "%s cannot be removed, it is referenced in following interfaces %s", dataBefore, references);
        } catch (RuntimeException e) {
            throw new DeleteValidationFailedException(id, e);
        }
    }

    private static void validateAcl(@Nonnull final Acl acl) {
        hasAceList(acl);
        isSupportedAclType(acl);
        hasConsistentAceTypeForAclType(acl);
    }

    private static void hasAceList(final Acl acl) {
        final AccessListEntries accessListEntries = acl.getAccessListEntries();
        checkArgument(accessListEntries != null, "The access-list-entries container is not defined.");
        final List<Ace> ace = accessListEntries.getAce();
        checkArgument(ace != null, "The ace list is not defined.");
        checkArgument(!ace.isEmpty(), "The ace list is empty.");
    }

    private static void isSupportedAclType(final Acl acl) {
        checkArgument(SUPPORTED_ACL_TYPES.contains(acl.getAclType()),
            "Unsupported Acl type %s detected for acl %s, allowed types are %s", acl.getAclType(),
            acl.getAclName(), SUPPORTED_ACL_TYPES);
    }

    private static void hasConsistentAceTypeForAclType(final Acl acl) {
        checkTypesSame(acl.getAccessListEntries().getAce(), acl.getAclName(),
            checkNotNull(ACL_ACE_PAIRS.get(acl.getAclType()), "Unsupported ACL type %s for ACL %s",
                acl.getAclType(), acl.getAclName()));
    }

    private static void checkTypesSame(final List<Ace> aces, final String aclName,
                                       final Class<? extends AceType> aceType) {
        final Set<AceType> unsupportedAceTypes = aces.stream()
            .map(Ace::getMatches)
            .map(Matches::getAceType)
            .filter(aceType::equals)
            .collect(Collectors.toSet());
        checkArgument(unsupportedAceTypes.isEmpty(), "Detected unsupported ace types [%s] for ACL %s, expected %s",
            unsupportedAceTypes, aclName, aceType);
    }

    @VisibleForTesting
    static List<Interface> checkAclReferenced(@Nonnull final WriteContext writeContext,
                                              @Nonnull final Acl acl) {
        final Optional<Interfaces> readAfter = writeContext.readAfter(InstanceIdentifier.create(Interfaces.class));
        if (!readAfter.isPresent() || readAfter.get().getInterface() == null) {
            return Collections.emptyList();
        }

        final List<Interface> interfaces = readAfter.get().getInterface();
        final Class<? extends AclBase> aclType = acl.getAclType();
        final String aclName = acl.getAclName();

        if (aclType.equals(VppAcl.class)) {
            return interfaces.stream()
                .filter(iface -> ofNullable(iface.augmentation(VppAclInterfaceAugmentation.class))
                    .map(InterfaceAclAttributes::getAcl)
                    .filter(references ->
                        checkVppAcls(references.getIngress(), aclName) ||
                            checkVppAcls(references.getEgress(), aclName)).isPresent()
                ).collect(Collectors.toList());
        } else if (aclType.equals(VppMacipAcl.class)) {
            return interfaces.stream()
                .filter(iface -> ofNullable(iface.augmentation(VppAclInterfaceAugmentation.class))
                    .map(InterfaceAclAttributes::getAcl)
                    .map(aclAttr -> aclAttr.getIngress())
                    .map(VppMacipAclsBaseAttributes::getVppMacipAcl)
                    .filter(vppMacipAcl -> vppMacipAcl.getName().equals(aclName))
                    .isPresent())
                .collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException(format("Acl type %s not supported", aclType));
        }
    }

    private static boolean checkVppAcls(@Nullable final VppAclsBaseAttributes attrs, @Nonnull final String name) {
        return ofNullable(attrs).map(VppAclsBaseAttributes::getVppAcls)
            .orElse(emptyList())
            .stream().anyMatch(acl -> acl.getName().equals(name));
    }
}
