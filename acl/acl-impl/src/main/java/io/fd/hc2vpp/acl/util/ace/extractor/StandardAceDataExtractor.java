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
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.vpp.jvpp.acl.types.AclRule;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Deny;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.DenyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Permit;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.PermitBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.AclIpv4HeaderFields;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.AclIpv6HeaderFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.actions.packet.handling.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.actions.packet.handling.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv6Builder;

public interface StandardAceDataExtractor extends AddressTranslator, ProtoPreBindRuleProducer, IpProtocolReader {

    /**
     * Allowed packet-processing actions for Acl's
     */
    Map<Class<? extends PacketHandling>, Integer> ACTION_VALUE_PAIRS = ImmutableMap.of(Deny.class, 0, Permit.class, 1,
            Stateful.class, 2);

    default VppAce fromStandardAce(@Nonnull final Ace ace) {
        return VppAce.class.cast(ace.getMatches().getAceType());
    }

    default boolean standardIsIpv6(@Nonnull final Ace ace) {
        return VppAce.class.cast(ace.getMatches().getAceType()).getVppAceNodes().getAceIpVersion() instanceof AceIpv6;
    }

    default byte[] ipv4SourceAddress(@Nonnull final VppAce ace) {
        return ipv4AddressPrefixToArray(
            AclIpv4HeaderFields.class.cast(ace.getVppAceNodes().getAceIpVersion()).getSourceIpv4Network());
    }

    default byte ipv4SourceAddressPrefix(@Nonnull final VppAce ace) {
        return extractPrefix(AclIpv4HeaderFields.class.cast(ace.getVppAceNodes().getAceIpVersion()).getSourceIpv4Network());
    }

    default byte[] ipv4DestinationAddress(@Nonnull final VppAce ace) {
        return ipv4AddressPrefixToArray(
            AclIpv4HeaderFields.class.cast(ace.getVppAceNodes().getAceIpVersion()).getDestinationIpv4Network());
    }

    default byte ipv4DestinationAddressPrefix(@Nonnull final VppAce ace) {
        return extractPrefix(AceIpv4.class.cast(ace.getVppAceNodes().getAceIpVersion()).getDestinationIpv4Network());
    }

    default byte[] ipv6SourceAddress(@Nonnull final VppAce ace) {
        return ipv6AddressPrefixToArray(
            AclIpv6HeaderFields.class.cast(ace.getVppAceNodes().getAceIpVersion()).getSourceIpv6Network());
    }

    default byte ipv6SourceAddressPrefix(@Nonnull final VppAce ace) {
        return extractPrefix(AclIpv6HeaderFields.class.cast(ace.getVppAceNodes().getAceIpVersion()).getSourceIpv6Network());
    }

    default byte[] ipv6DestinationAddress(@Nonnull final VppAce ace) {
        return ipv6AddressPrefixToArray(
            AclIpv6HeaderFields.class.cast(ace.getVppAceNodes().getAceIpVersion()).getDestinationIpv6Network());
    }

    default byte ipv6DestinationAddressPrefix(@Nonnull final VppAce ace) {
        return extractPrefix(AclIpv6HeaderFields.class.cast(ace.getVppAceNodes().getAceIpVersion()).getDestinationIpv6Network());
    }

    default byte standardAction(@Nonnull final Ace ace) {
        final PacketHandling action = ace.getActions().getPacketHandling();
        return ACTION_VALUE_PAIRS.get(ACTION_VALUE_PAIRS.keySet().stream()
                .filter(aClass -> aClass.isInstance(action))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Unsupported packet-handling action %s for ACE %s", action,
                                ace.getRuleName())))).byteValue();
    }

    default AceIpVersion ipVersion(final AclRule rule) {
        if (rule.isIpv6 == 0) {
            return ip4Ace(rule);
        } else {
            return ip6Ace(rule);
        }
    }

    default AceIpVersion ip4Ace(final AclRule rule) {
        final AceIpv4Builder ipVersion = new AceIpv4Builder();
        if (rule.srcIpAddr != null && rule.srcIpAddr.length != 0) {
            ipVersion.setSourceIpv4Network(toIpv4Prefix(truncateIp4Array(rule.srcIpAddr), rule.srcIpPrefixLen));
        }
        if (rule.dstIpAddr != null && rule.dstIpAddr.length != 0) {
            ipVersion.setDestinationIpv4Network(toIpv4Prefix(truncateIp4Array(rule.dstIpAddr), rule.dstIpPrefixLen));
        }
        return ipVersion.build();
    }

    default AceIpVersion ip6Ace(final AclRule rule) {
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
