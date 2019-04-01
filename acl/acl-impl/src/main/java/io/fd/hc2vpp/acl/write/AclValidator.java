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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.acl.AclIIds;
import io.fd.hc2vpp.acl.util.acl.AclDataExtractor;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.UpdateValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppAcl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppMacipAcl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.AttachmentPoints;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.Aces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.L3;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l2.Eth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.AclSets;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.source.network.SourceIpv4Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.source.network.SourceIpv6Network;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class AclValidator implements Validator<Acl>, AclDataExtractor {

    private static final Set<Class<? extends AclBase>> SUPPORTED_ACL_TYPES =
            ImmutableSet.of(VppAcl.class, VppMacipAcl.class);

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
            final List<String> references = checkAclReferenced(ctx, dataBefore);
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
        final Aces accessListEntries = acl.getAces();
        checkArgument(accessListEntries != null, "The access-list-entries container is not defined.");
        final List<Ace> ace = accessListEntries.getAce();
        checkArgument(ace != null, "The ace list is not defined.");
        checkArgument(!ace.isEmpty(), "The ace list is empty.");
    }

    private static void isSupportedAclType(final Acl acl) {
        checkArgument(SUPPORTED_ACL_TYPES.contains(acl.getType()),
                "Unsupported Acl type %s detected for acl %s, allowed types are %s", acl.getType(),
                acl.getName(), SUPPORTED_ACL_TYPES);
    }

    private static void hasConsistentAceTypeForAclType(final Acl acl) {
        Class<? extends AclBase> type = acl.getType();
        Preconditions.checkNotNull(type, "Cannot resolve Acl type for validation of Acl: {}", acl);
        Preconditions.checkNotNull(acl.getAces(), "ACEs are missing for validation of Acl: {}", acl);
        Preconditions.checkNotNull(acl.getAces().getAce(), "List of ACEs is null for validation of Acl: {}", acl);
        if (type.equals(VppAcl.class)) {
            Set<Ace> unsupportedVppAcls =
                    acl.getAces().getAce().stream().filter(ace -> !isVppAce(ace)).collect(Collectors.toSet());
            checkArgument(unsupportedVppAcls.isEmpty(), "Detected unsupported ace types [%s] for ACL %s",
                    unsupportedVppAcls, acl.getName());
        }

        if (type.equals(VppMacipAcl.class)) {
            Set<Ace> unsupportedVppMacipAclAcls =
                    acl.getAces().getAce().stream().filter(ace -> !isVppMacipAclAce(ace)).collect(Collectors.toSet());
            checkArgument(unsupportedVppMacipAclAcls.isEmpty(), "Detected unsupported ace types [%s] for ACL %s",
                    unsupportedVppMacipAclAcls, acl.getName());
        }
    }

    private static boolean isVppMacipAclAce(final Ace ace) {
        Matches matches = Preconditions
                .checkNotNull(ace.getMatches(), "Cannot validate VppMacipAcl type for Ace: {}, matches are not defined",
                        ace);
        if (matches.getL2() == null || !(matches.getL2() instanceof Eth)) {
            return false;
        }

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l2.eth.Eth
                eth = ((Eth) matches.getL2()).getEth();
        if (eth == null) {
            return false;
        }

        return true;
    }

    private static boolean isVppAce(final Ace ace) {
        Matches matches = Preconditions
                .checkNotNull(ace.getMatches(), "Cannot validate VppMacipAcl type for Ace: {}, matches are not defined",
                        ace);
        L3 l3 = matches.getL3();
        if (l3 == null || (!(l3 instanceof Ipv4)) && (!(l3 instanceof Ipv6))) {
            return false;
        }

        if (l3 instanceof Ipv4) {
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4
                    ipv4 = ((Ipv4) l3).getIpv4();
            if (ipv4 == null || ipv4.getSourceNetwork() == null ||
                    !(ipv4.getSourceNetwork() instanceof SourceIpv4Network)) {
                return false;
            }
        }

        if (l3 instanceof Ipv6) {
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6
                    ipv6 = ((Ipv6) l3).getIpv6();
            if (ipv6 == null || ipv6.getSourceNetwork() == null ||
                    !(ipv6.getSourceNetwork() instanceof SourceIpv6Network)) {
                return false;
            }
        }

        return true;
    }

    @VisibleForTesting
    static List<String> checkAclReferenced(@Nonnull final WriteContext writeContext, @Nonnull final Acl acl) {
        Preconditions.checkNotNull(acl.getType(), "Cannot validate acl: {}, type is not set.", acl);
        if (!acl.getType().equals(VppAcl.class) && !acl.getType().equals(VppMacipAcl.class)) {
            throw new IllegalArgumentException(String.format("Acl type %s not supported", acl.getType()));
        }

        Optional<AttachmentPoints> attachmentPointsOpt = writeContext.readAfter(AclIIds.ACLS_AP);
        if (!attachmentPointsOpt.isPresent() || attachmentPointsOpt.get().getInterface() == null) {
            return Collections.emptyList();
        }

        final List<Interface> interfaces = attachmentPointsOpt.get().getInterface();
        if (interfaces == null) {
            return Collections.emptyList();
        }
        final String aclName = acl.getName();

        HashMap<String, AclSets> sets = getIngressAclSets(interfaces);
        sets.putAll(getEgressAclSets(interfaces));
        List<String> referencedIfcs = new ArrayList<>();
        sets.forEach((ifc, aclSets) -> {
            if (aclSets.getAclSet() != null) {
                if (aclSets.getAclSet().stream()
                        .map(AclSet::getName)
                        .filter(Objects::nonNull)
                        .anyMatch(name -> name.equalsIgnoreCase(aclName))) {
                    referencedIfcs.add(ifc);
                }
            }
        });
        return referencedIfcs.stream().distinct().collect(Collectors.toList());
    }

    private static HashMap<String, AclSets> getEgressAclSets(final List<Interface> interfaces) {
        HashMap<String, AclSets> map = new HashMap<>();
        interfaces.stream().filter(anInterface -> anInterface.getEgress() != null)
                .forEach(anInterface -> map.put(anInterface.getInterfaceId(), anInterface.getEgress().getAclSets()));
        return map;
    }

    private static HashMap<String, AclSets> getIngressAclSets(final List<Interface> interfaces) {
        HashMap<String, AclSets> map = new HashMap<>();
        interfaces.stream().filter(anInterface -> anInterface.getIngress() != null)
                .forEach(anInterface -> map.put(anInterface.getInterfaceId(), anInterface.getIngress().getAclSets()));
        return map;
    }
}
