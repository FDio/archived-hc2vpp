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

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.MacTranslator;
import io.fd.vpp.jvpp.acl.types.MacipAclRule;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Deny;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Permit;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.VppMacipAceIpv4HeaderFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.VppMacipAceIpv6HeaderFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppMacipAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.vpp.macip.ace.nodes.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.vpp.macip.ace.nodes.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.vpp.macip.ace.nodes.ace.ip.version.AceIpv6Builder;

public interface MacIpAceDataExtractor extends AddressTranslator, MacTranslator {

    default VppMacipAce fromMacIpAce(@Nonnull final Ace ace) {
        return VppMacipAce.class.cast(ace.getMatches().getAceType());
    }

    default boolean macIpIsIpv6(@Nonnull final VppMacipAce ace) {
        return ace.getVppMacipAceNodes().getAceIpVersion() instanceof AceIpv6;
    }

    default byte[] sourceMacAsBytes(@Nonnull final VppMacipAce ace) {
        return macToByteArray(ace.getVppMacipAceNodes().getSourceMacAddress().getValue());
    }

    default byte[] sourceMacMaskAsBytes(@Nonnull final VppMacipAce ace) {
        return macToByteArray(ace.getVppMacipAceNodes().getSourceMacAddressMask().getValue());
    }

    default byte[] ipv4Address(@Nonnull final VppMacipAce ace) {
        return ipv4AddressPrefixToArray(
            VppMacipAceIpv4HeaderFields.class.cast(ace.getVppMacipAceNodes().getAceIpVersion()).getSourceIpv4Network());
    }

    default byte ipv4AddressPrefix(@Nonnull final VppMacipAce ace) {
        return extractPrefix(
            VppMacipAceIpv4HeaderFields.class.cast(ace.getVppMacipAceNodes().getAceIpVersion()).getSourceIpv4Network());
    }

    default byte[] ipv6Address(@Nonnull final VppMacipAce ace) {
        return ipv6AddressPrefixToArray(
            VppMacipAceIpv6HeaderFields.class.cast(ace.getVppMacipAceNodes().getAceIpVersion()).getSourceIpv6Network());
    }

    default byte ipv6AddressPrefix(@Nonnull final VppMacipAce ace) {
        return extractPrefix(
            VppMacipAceIpv6HeaderFields.class.cast(ace.getVppMacipAceNodes().getAceIpVersion()).getSourceIpv6Network());
    }

    /**
     * Only 0 and 1 are allowed for mac-ip
     */
    default byte macIpAction(@Nonnull final Ace ace) {
        final PacketHandling action = ace.getActions().getPacketHandling();
        if (action instanceof Permit) {
            return 1;
        } else if (action instanceof Deny) {
            return 0;
        } else {
            throw new IllegalArgumentException(
                String.format("Unsupported packet-handling action %s for ACE %s", action, ace));
        }
    }

    default AceIpVersion ipVersion(final MacipAclRule rule) {
        if (rule.isIpv6 == 0) {
            return ip4Ace(rule);
        } else {
            return ip6Ace(rule);
        }
    }

    default AceIpVersion ip4Ace(MacipAclRule rule) {
        final AceIpv4Builder ipVersion = new AceIpv4Builder();
        if (rule.srcIpAddr != null && rule.srcIpAddr.length != 0) {
            ipVersion.setSourceIpv4Network(toIpv4Prefix(truncateIp4Array(rule.srcIpAddr), rule.srcIpPrefixLen));
        }
        return ipVersion.build();
    }

    default AceIpVersion ip6Ace(MacipAclRule rule) {
        final AceIpv6Builder ipVersion = new AceIpv6Builder();
        if (rule.srcIpAddr != null && rule.srcIpAddr.length != 0) {
            ipVersion.setSourceIpv6Network(toIpv6Prefix(rule.srcIpAddr, rule.srcIpPrefixLen));
        }
        return ipVersion.build();
    }

    default MacAddress sourceMac(final MacipAclRule rule) {
        return new MacAddress(byteArrayToMacSeparated(rule.srcMac));
    }

    default MacAddress sourceMacMask(final MacipAclRule rule) {
        return new MacAddress(byteArrayToMacSeparated(rule.srcMacMask));
    }


}
