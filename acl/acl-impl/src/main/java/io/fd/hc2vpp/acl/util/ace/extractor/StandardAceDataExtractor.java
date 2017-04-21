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

package io.fd.hc2vpp.acl.util.ace.extractor;

import com.google.common.collect.ImmutableMap;
import io.fd.hc2vpp.acl.util.protocol.IpProtocolReader;
import io.fd.hc2vpp.acl.util.protocol.ProtoPreBindRuleProducer;
import io.fd.vpp.jvpp.acl.types.AclRule;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Deny;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.DenyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Permit;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.PermitBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.AclIpv4HeaderFields;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.AclIpv6HeaderFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.AclIpProtocolHeaderFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.actions.packet.handling.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.actions.packet.handling.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.VppAceNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.acl.ip.protocol.header.fields.ip.protocol.IcmpV6;

public interface StandardAceDataExtractor extends AddressExtractor, ProtoPreBindRuleProducer, IpProtocolReader {

    /**
     * Allowed packet-processing actions for Acl's
     */
    Map<Class<? extends PacketHandling>, Integer> ACTION_VALUE_PAIRS = ImmutableMap.of(Deny.class, 0, Permit.class, 1,
            Stateful.class, 2);

    default VppAce fromStandardAce(@Nonnull final Ace ace) {
        return Optional.ofNullable(ace.getMatches())
                .map(Matches::getAceType)
                .map(VppAce.class::cast)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Unable to create VppAce from %s", ace)));
    }

    default boolean standardIsIpv6(@Nonnull final VppAce ace, @Nullable final Matches matches) {
        final Optional<AceIpVersion> aceIpVersion = Optional.ofNullable(matches)
                .map(Matches::getAceType)
                .map(VppAce.class::cast)
                .map(VppAce::getVppAceNodes)
                .map(VppAceNodes::getAceIpVersion);

        // tries to detect version by ace-ip-version
        if(aceIpVersion.isPresent()){
            return aceIpVersion
                    .map(version -> version instanceof AceIpv6)
                    .orElse(false);
        }

        // otherwise goes by ip-protocol
        return Optional.ofNullable(ace.getVppAceNodes())
                .map(AclIpProtocolHeaderFields::getIpProtocol)
                .map(ipProtocol -> ipProtocol instanceof IcmpV6)
                .orElse(false);
    }

    default byte[] ipv4SourceAddress(@Nonnull final VppAce ace) {
        return extractIp4Address(
                extractV4SourceAddressOrNull(ace));
    }

    default byte ipv4SourceAddressPrefix(@Nonnull final VppAce ace) {
        return extractIp4AddressPrefix(
                extractV4SourceAddressOrNull(ace));
    }

    static Ipv4Prefix extractV4SourceAddressOrNull(@Nonnull final VppAce ace) {
        return Optional.ofNullable(ace.getVppAceNodes())
                .map(VppAceNodes::getAceIpVersion)
                .map(AclIpv4HeaderFields.class::cast)
                .map(AclIpv4HeaderFields::getSourceIpv4Network)
                .orElse(null);
    }


    default byte[] ipv4DestinationAddress(@Nonnull final VppAce ace) {
        return extractIp4Address(extractV4DestinationAddressOrNull(ace));
    }

    default byte ipv4DestinationAddressPrefix(@Nonnull final VppAce ace) {
        return extractIp4AddressPrefix(extractV4DestinationAddressOrNull(ace));
    }

    static Ipv4Prefix extractV4DestinationAddressOrNull(@Nonnull final VppAce ace) {
        return Optional.ofNullable(ace.getVppAceNodes())
                .map(VppAceNodes::getAceIpVersion)
                .map(AclIpv4HeaderFields.class::cast)
                .map(AclIpv4HeaderFields::getDestinationIpv4Network)
                .orElse(null);
    }

    default byte[] ipv6SourceAddress(@Nonnull final VppAce ace) {
        return extractIp6Address(extractV6SourceAddressOrNull(ace));
    }

    default byte ipv6SourceAddressPrefix(@Nonnull final VppAce ace) {
        return extractIp6AddressPrefix(extractV6SourceAddressOrNull(ace));
    }

    static Ipv6Prefix extractV6SourceAddressOrNull(@Nonnull final VppAce ace) {
        return Optional.ofNullable(ace.getVppAceNodes())
                .map(VppAceNodes::getAceIpVersion)
                .map(AclIpv6HeaderFields.class::cast)
                .map(AclIpv6HeaderFields::getSourceIpv6Network)
                .orElse(null);
    }

    default byte[] ipv6DestinationAddress(@Nonnull final VppAce ace) {
        return extractIp6Address(extractV6DestinationAddressOrNull(ace));
    }

    default byte ipv6DestinationAddressPrefix(@Nonnull final VppAce ace) {
        return extractIp6AddressPrefix(extractV6DestinationAddressOrNull(ace));
    }

    static Ipv6Prefix extractV6DestinationAddressOrNull(@Nonnull final VppAce ace) {
        return Optional.ofNullable(ace.getVppAceNodes())
                .map(VppAceNodes::getAceIpVersion)
                .map(AclIpv6HeaderFields.class::cast)
                .map(AclIpv6HeaderFields::getDestinationIpv6Network)
                .orElse(null);
    }

    default byte standardAction(@Nonnull final Ace ace) {
        // default == deny
        final PacketHandling action = Optional.ofNullable(ace.getActions())
                .orElseThrow(() -> new IllegalArgumentException(String.format("Unable to extract Action from %s", ace)))
                .getPacketHandling();
        return ACTION_VALUE_PAIRS.get(ACTION_VALUE_PAIRS.keySet().stream()
                .filter(aClass -> aClass.isInstance(action))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Unsupported packet-handling action %s for ACE %s", action,
                                ace.getRuleName())))).byteValue();
    }

    default AceIpVersion ipVersion(@Nonnull final AclRule rule) {
        if (rule.isIpv6 == 0) {
            return ip4Ace(rule);
        } else {
            return ip6Ace(rule);
        }
    }

    default AceIpVersion ip4Ace(@Nonnull final AclRule rule) {
        final AceIpv4Builder ipVersion = new AceIpv4Builder();
        if (rule.srcIpAddr != null && rule.srcIpAddr.length != 0) {
            ipVersion.setSourceIpv4Network(toIpv4Prefix(truncateIp4Array(rule.srcIpAddr), rule.srcIpPrefixLen));
        }
        if (rule.dstIpAddr != null && rule.dstIpAddr.length != 0) {
            ipVersion.setDestinationIpv4Network(toIpv4Prefix(truncateIp4Array(rule.dstIpAddr), rule.dstIpPrefixLen));
        }
        return ipVersion.build();
    }

    default AceIpVersion ip6Ace(@Nonnull final AclRule rule) {
        final AceIpv6Builder ipVersion = new AceIpv6Builder();
        if (rule.srcIpAddr != null && rule.srcIpAddr.length != 0) {
            ipVersion.setSourceIpv6Network(toIpv6Prefix(rule.srcIpAddr, rule.srcIpPrefixLen));
        }
        if (rule.dstIpAddr != null && rule.dstIpAddr.length != 0) {
            ipVersion.setDestinationIpv6Network(toIpv6Prefix(rule.dstIpAddr, rule.dstIpPrefixLen));
        }
        return ipVersion.build();
    }

    default Actions actions(final byte isPermit) {
        final ActionsBuilder actions = new ActionsBuilder();
        switch (isPermit) {
            case 0:
                actions.setPacketHandling(new DenyBuilder().setDeny(true).build());
                break;
            case 1:
                actions.setPacketHandling(new PermitBuilder().setPermit(true).build());
                break;
            case 2:
                actions.setPacketHandling(new StatefulBuilder().setPermit(true).build());
                break;
            default:
                throw new IllegalArgumentException("Unsupported action: " + isPermit);
        }
        return actions.build();
    }
}
