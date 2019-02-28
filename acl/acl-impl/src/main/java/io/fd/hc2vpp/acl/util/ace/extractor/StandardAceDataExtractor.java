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
import io.fd.jvpp.acl.types.AclRule;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.AcceptAndReflect;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.Accept;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.Drop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.ForwardingAction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.Reject;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.L3;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.AclIpv4HeaderFields;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.AclIpv6HeaderFields;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.destination.network.DestinationIpv4Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.destination.network.DestinationIpv4NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.source.network.SourceIpv4Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.source.network.SourceIpv4NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.destination.network.DestinationIpv6Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.destination.network.DestinationIpv6NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.source.network.SourceIpv6Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.source.network.SourceIpv6NetworkBuilder;

public interface StandardAceDataExtractor extends AddressExtractor, ProtoPreBindRuleProducer, IpProtocolReader {

    /**
     * Allowed packet-processing actions for Acl's
     */
    Map<Class<? extends ForwardingAction>, Integer> ACTION_VALUE_PAIRS =
            ImmutableMap.of(Drop.class, 0, Reject.class, 0, Accept.class, 1, AcceptAndReflect.class, 2);

    default boolean standardIsIpv6(@Nullable final Matches matches) {
        return Optional.ofNullable(matches)
                .map(Matches::getL3)
                .filter(l3 -> l3.getImplementedInterface().equals(Ipv6.class))
                .map(Ipv6.class::cast)
                .map(Ipv6::getIpv6)
                .isPresent();
    }

    default byte[] ipv4SourceAddress(@Nonnull final Matches matches) {
        return extractIp4Address(extractV4SourceAddressOrNull(matches));
    }

    default byte ipv4SourceAddressPrefix(@Nonnull final Matches matches) {
        return extractIp4AddressPrefix(extractV4SourceAddressOrNull(matches));
    }

    static Ipv4Prefix extractV4SourceAddressOrNull(@Nonnull final Matches matches) {
        return getIpv4Optional(matches)
                .map(AclIpv4HeaderFields::getSourceNetwork)
                .filter(network -> network instanceof SourceIpv4Network)
                .map(SourceIpv4Network.class::cast)
                .map(SourceIpv4Network::getSourceIpv4Network)
                .orElse(null);
    }


    default byte[] ipv4DestinationAddress(@Nonnull final Matches matches) {
        return extractIp4Address(extractV4DestinationAddressOrNull(matches));
    }

    default byte ipv4DestinationAddressPrefix(@Nonnull final Matches matches) {
        return extractIp4AddressPrefix(extractV4DestinationAddressOrNull(matches));
    }

    static Ipv4Prefix extractV4DestinationAddressOrNull(@Nonnull final Matches matches) {
        return getIpv4Optional(matches)
                .map(AclIpv4HeaderFields::getDestinationNetwork)
                .filter(destinationNetwork -> destinationNetwork instanceof DestinationIpv4Network)
                .map(DestinationIpv4Network.class::cast)
                .map(DestinationIpv4Network::getDestinationIpv4Network)
                .orElse(null);
    }

    static Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4> getIpv4Optional(
            @Nonnull final Matches matches) {
        return Optional.ofNullable(matches.getL3())
                .filter(l3 -> l3.getImplementedInterface().equals(Ipv4.class))
                .map(Ipv4.class::cast)
                .map(Ipv4::getIpv4);
    }

    default byte[] ipv6SourceAddress(@Nonnull final Matches matches) {
        return extractIp6Address(extractV6SourceAddressOrNull(matches));
    }

    default byte ipv6SourceAddressPrefix(@Nonnull final Matches matches) {
        return extractIp6AddressPrefix(extractV6SourceAddressOrNull(matches));
    }

    static Ipv6Prefix extractV6SourceAddressOrNull(@Nonnull final Matches matches) {
        return getIpv6Optional(matches)
                .map(AclIpv6HeaderFields::getSourceNetwork)
                .filter(sourceNetwork -> sourceNetwork instanceof SourceIpv6Network)
                .map(SourceIpv6Network.class::cast)
                .map(SourceIpv6Network::getSourceIpv6Network)
                .orElse(null);
    }

    default byte[] ipv6DestinationAddress(@Nonnull final Matches matches) {
        return extractIp6Address(extractV6DestinationAddressOrNull(matches));
    }

    default byte ipv6DestinationAddressPrefix(@Nonnull final Matches matches) {
        return extractIp6AddressPrefix(extractV6DestinationAddressOrNull(matches));
    }

    static Ipv6Prefix extractV6DestinationAddressOrNull(@Nonnull final Matches matches) {
        return getIpv6Optional(matches)
                .map(AclIpv6HeaderFields::getDestinationNetwork)
                .filter(destinationNetwork -> destinationNetwork instanceof DestinationIpv6Network)
                .map(DestinationIpv6Network.class::cast)
                .map(DestinationIpv6Network::getDestinationIpv6Network)
                .orElse(null);
    }

    static Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6> getIpv6Optional(
            @Nonnull final Matches matches) {
        return Optional.ofNullable(matches.getL3())
                .filter(l3 -> l3.getImplementedInterface().equals(Ipv6.class))
                .map(Ipv6.class::cast)
                .map(Ipv6::getIpv6);
    }

    default byte standardAction(@Nonnull final Ace ace) {
        Class<? extends ForwardingAction> forwarding = Optional.ofNullable(ace.getActions())
                .orElseThrow(() -> new IllegalArgumentException(String.format("Unable to extract Action from %s", ace)))
                .getForwarding();
        return ACTION_VALUE_PAIRS.get(ACTION_VALUE_PAIRS.keySet().stream()
                .filter(aClass -> aClass.equals(forwarding))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Unsupported packet-handling action %s for ACE %s", forwarding,
                                ace.getName())))).byteValue();
    }

    default L3 parseStandardAceL3(@Nonnull final AclRule rule) {
        if (rule.isIpv6 == 0) {
            return ip4Ace(rule);
        } else {
            return ip6Ace(rule);
        }
    }

    default org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv4 ip4Ace(
            @Nonnull final AclRule rule) {
        Ipv4Builder ipv4Builder = new Ipv4Builder();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder
                ip4Builder =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder();
        if (rule.srcIpAddr != null && rule.srcIpAddr.length != 0) {
            ip4Builder.setSourceNetwork(new SourceIpv4NetworkBuilder()
                    .setSourceIpv4Network(toIpv4Prefix(truncateIp4Array(rule.srcIpAddr), rule.srcIpPrefixLen))
                    .build());
        }
        if (rule.dstIpAddr != null && rule.dstIpAddr.length != 0) {
            ip4Builder.setDestinationNetwork(new DestinationIpv4NetworkBuilder()
                    .setDestinationIpv4Network(toIpv4Prefix(truncateIp4Array(rule.dstIpAddr), rule.dstIpPrefixLen))
                    .build());
        }
        return ipv4Builder.setIpv4(ip4Builder.build()).build();
    }

    default org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv6 ip6Ace(
            @Nonnull final AclRule rule) {
        Ipv6Builder ipv6Builder = new Ipv6Builder();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder
                ip6Builder =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder();

        if (rule.srcIpAddr != null && rule.srcIpAddr.length != 0) {
            ip6Builder.setSourceNetwork(new SourceIpv6NetworkBuilder()
                    .setSourceIpv6Network(toIpv6Prefix(rule.srcIpAddr, rule.srcIpPrefixLen))
                    .build());
        }
        if (rule.dstIpAddr != null && rule.dstIpAddr.length != 0) {
            ip6Builder.setDestinationNetwork(new DestinationIpv6NetworkBuilder()
                    .setDestinationIpv6Network(toIpv6Prefix(rule.dstIpAddr, rule.dstIpPrefixLen))
                    .build());
        }
        return ipv6Builder.setIpv6(ip6Builder.build()).build();
    }

    default Actions actions(final byte isPermit) {
        final ActionsBuilder actions = new ActionsBuilder();
        switch (isPermit) {
            case 0:
                actions.setForwarding(Drop.class);
                break;
            case 1:
                actions.setForwarding(Accept.class);
                break;

            default:
                throw new IllegalArgumentException("Unsupported action: " + isPermit);
        }
        return actions.build();
    }
}
