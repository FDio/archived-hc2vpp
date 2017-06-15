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


import io.fd.hc2vpp.common.translate.util.MacTranslator;
import io.fd.vpp.jvpp.acl.types.MacipAclRule;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Deny;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Permit;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppMacipAceEthHeaderFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppMacipAceIpv4HeaderFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppMacipAceIpv6HeaderFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.VppMacipAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.VppMacipAceNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.vpp.macip.ace.nodes.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.vpp.macip.ace.nodes.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.vpp.macip.ace.nodes.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.vpp.macip.ace.nodes.ace.ip.version.AceIpv6Builder;

public interface MacIpAceDataExtractor extends AddressExtractor, MacTranslator {

    String DEFAULT_MAC = "00:00:00:00:00:00";
    String DEFAULT_MAC_MASK = "00:00:00:00:00:00";
    byte[] DEFAULT_MAC_BYTES = {0, 0, 0, 0, 0, 0};
    byte[] DEFAULT_MAC_MASK_BYTES = {0, 0, 0, 0, 0, 0};

    default VppMacipAce fromMacIpAce(@Nonnull final Ace ace) {
        return Optional.ofNullable(ace.getMatches())
                .map(Matches::getAceType)
                .map(VppMacipAce.class::cast)
                .orElseThrow(
                        () -> new IllegalArgumentException(String.format("Unable to create VppMacipAce from %s", ace)));
    }

    default boolean macIpIsIpv6(@Nonnull final VppMacipAce ace) {
        return Optional.ofNullable(ace.getVppMacipAceNodes())
                .map(VppMacipAceNodes::getAceIpVersion)
                .map(aceIpVersion -> aceIpVersion instanceof AceIpv6)
                .orElse(false);
    }

    default byte[] sourceMacAsBytes(@Nonnull final VppMacipAce ace) {
        return macToByteArray(Optional.ofNullable(ace.getVppMacipAceNodes())
                .map(VppMacipAceEthHeaderFields::getSourceMacAddress)
                .map(MacAddress::getValue)
                .orElse(DEFAULT_MAC));
    }

    default byte[] sourceMacMaskAsBytes(@Nonnull final VppMacipAce ace) {
        return macToByteArray(Optional.ofNullable(ace.getVppMacipAceNodes())
                .map(VppMacipAceEthHeaderFields::getSourceMacAddressMask)
                .map(MacAddress::getValue)
                .orElse(DEFAULT_MAC_MASK));
    }

    default byte[] ipv4Address(@Nonnull final VppMacipAce ace) {
        return extractIp4Address(extractV4NetworkAddressOrNull(ace));
    }

    default byte ipv4AddressPrefix(@Nonnull final VppMacipAce ace) {
        return extractIp4AddressPrefix(extractV4NetworkAddressOrNull(ace));
    }

    static Ipv4Prefix extractV4NetworkAddressOrNull(final @Nonnull VppMacipAce ace) {
        return Optional.ofNullable(ace.getVppMacipAceNodes())
                .map(VppMacipAceNodes::getAceIpVersion)
                .map(VppMacipAceIpv4HeaderFields.class::cast)
                .map(VppMacipAceIpv4HeaderFields::getSourceIpv4Network)
                .orElse(null);
    }

    default byte[] ipv6Address(@Nonnull final VppMacipAce ace) {
        return extractIp6Address(extractV6NetworkAddressOrNull(ace));
    }

    default byte ipv6AddressPrefix(@Nonnull final VppMacipAce ace) {
        return extractIp6AddressPrefix(extractV6NetworkAddressOrNull(ace));
    }

    default Ipv6Prefix extractV6NetworkAddressOrNull(@Nonnull final VppMacipAce ace) {
        return Optional.ofNullable(ace.getVppMacipAceNodes())
                .map(VppMacipAceNodes::getAceIpVersion)
                .map(VppMacipAceIpv6HeaderFields.class::cast)
                .map(VppMacipAceIpv6HeaderFields::getSourceIpv6Network)
                .orElse(null);
    }

    /**
     * Only 0 and 1 are allowed for mac-ip
     */
    default byte macIpAction(@Nonnull final Ace ace) {
        // action choice itself has default, but nothing stops us from not defining actions container itself
        final PacketHandling action = Optional.ofNullable(ace.getActions()).orElseThrow(
                () -> new IllegalArgumentException(String.format("Unable to extract Action from %s", ace)))
                .getPacketHandling();
        if (action instanceof Permit) {
            return 1;
        } else if (action instanceof Deny) {
            return 0;
        } else {
            throw new IllegalArgumentException(
                    String.format("Unsupported packet-handling action %s for ACE %s", action, ace));
        }
    }

    default AceIpVersion ipVersion(@Nonnull final MacipAclRule rule) {
        if (rule.isIpv6 == 0) {
            return ip4Ace(rule);
        } else {
            return ip6Ace(rule);
        }
    }

    default AceIpVersion ip4Ace(@Nonnull final MacipAclRule rule) {
        final AceIpv4Builder ipVersion = new AceIpv4Builder();
        if (rule.srcIpAddr != null && rule.srcIpAddr.length != 0) {
            ipVersion.setSourceIpv4Network(toIpv4Prefix(truncateIp4Array(rule.srcIpAddr), rule.srcIpPrefixLen));
        }
        return ipVersion.build();
    }

    default AceIpVersion ip6Ace(@Nonnull final MacipAclRule rule) {
        final AceIpv6Builder ipVersion = new AceIpv6Builder();
        if (rule.srcIpAddr != null && rule.srcIpAddr.length != 0) {
            ipVersion.setSourceIpv6Network(toIpv6Prefix(rule.srcIpAddr, rule.srcIpPrefixLen));
        }
        return ipVersion.build();
    }

    default MacAddress sourceMac(@Nonnull final MacipAclRule rule) {
        return new MacAddress(byteArrayToMacSeparated(rule.srcMac != null
                ? rule.srcMac
                : DEFAULT_MAC_BYTES));
    }

    default MacAddress sourceMacMask(@Nonnull final MacipAclRule rule) {
        return new MacAddress(byteArrayToMacSeparated(rule.srcMacMask != null
                ? rule.srcMacMask
                : DEFAULT_MAC_MASK_BYTES));
    }
}
