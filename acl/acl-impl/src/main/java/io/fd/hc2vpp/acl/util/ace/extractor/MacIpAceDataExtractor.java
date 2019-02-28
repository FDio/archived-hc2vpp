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
import io.fd.jvpp.acl.types.MacipAclRule;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.Accept;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.L3;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.SourceNetwork;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.source.network.SourceIpv4Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.source.network.SourceIpv4NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.source.network.SourceIpv6Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.source.network.SourceIpv6NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;

public interface MacIpAceDataExtractor extends AddressExtractor, MacTranslator {

    default byte[] sourceMacAsBytes(final MacAddress mac) {
        return macToByteArray(Optional.ofNullable(mac)
                .map(MacAddress::getValue)
                .orElse(Impl.DEFAULT_MAC));
    }

    default byte[] sourceMacMaskAsBytes(final MacAddress mac) {
        return macToByteArray(Optional.ofNullable(mac)
                .map(MacAddress::getValue)
                .orElse(Impl.DEFAULT_MAC_MASK));
    }

    default byte[] ipv4Address(@Nonnull final SourceNetwork network) {
        return extractIp4Address(extractV4NetworkAddressOrNull(network));
    }

    default byte ipv4AddressPrefix(@Nonnull final SourceNetwork network) {
        return extractIp4AddressPrefix(extractV4NetworkAddressOrNull(network));
    }

    static Ipv4Prefix extractV4NetworkAddressOrNull(final @Nonnull SourceNetwork network) {
        return Optional.of(network).filter(net -> net instanceof SourceIpv4Network)
                .map(SourceIpv4Network.class::cast)
                .map(SourceIpv4Network::getSourceIpv4Network)
                .orElse(null);
    }

    default byte[] ipv6Address(
            @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.SourceNetwork network) {
        return extractIp6Address(extractV6NetworkAddressOrNull(network));
    }

    default byte ipv6AddressPrefix(
            @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.SourceNetwork network) {
        return extractIp6AddressPrefix(extractV6NetworkAddressOrNull(network));
    }

    default Ipv6Prefix extractV6NetworkAddressOrNull(
            @Nonnull final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.SourceNetwork network) {
        return Optional.of(network).filter(net -> net instanceof SourceIpv6Network)
                .map(SourceIpv6Network.class::cast)
                .map(SourceIpv6Network::getSourceIpv6Network)
                .orElse(null);
    }

    /**
     * Only 0 and 1 are allowed for mac-ip
     */
    default byte macIpAction(@Nonnull final Ace ace) {
        // action choice itself has default, but nothing stops us from not defining actions container itself
        Actions actions = Optional.ofNullable(ace.getActions()).orElseThrow(
                () -> new IllegalArgumentException(String.format("Unable to extract Action from %s", ace)));
        if (actions.getForwarding() != null) {
            if (ace.getActions().getForwarding().equals(Accept.class)) {
                return 1;
            } else {
                return 0;
            }
        } else {
            throw new IllegalArgumentException(
                    String.format("Unsupported packet-handling action %s for ACE %s", actions, ace));
        }
    }

    default L3 parseMacIpAceL3(@Nonnull final MacipAclRule rule) {
        if (rule.isIpv6 == 0) {
            return ip4L3(rule);
        } else {
            return ip6L3(rule);
        }
    }

    default Ipv4 ip4L3(@Nonnull final MacipAclRule rule) {
        final Ipv4Builder ipv4Builder = new Ipv4Builder();

        if (rule.srcIpAddr != null && rule.srcIpAddr.length != 0) {
            ipv4Builder.setIpv4(
                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder()
                            .setSourceNetwork(new SourceIpv4NetworkBuilder()
                                    .setSourceIpv4Network(
                                            toIpv4Prefix(truncateIp4Array(rule.srcIpAddr), rule.srcIpPrefixLen))
                                    .build())
                            .build());
        }
        return ipv4Builder.build();
    }

    default Ipv6 ip6L3(@Nonnull final MacipAclRule rule) {
        final Ipv6Builder ipv6Builder = new Ipv6Builder();
        if (rule.srcIpAddr != null && rule.srcIpAddr.length != 0) {
            ipv6Builder.setIpv6(
                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder()
                            .setSourceNetwork(new SourceIpv6NetworkBuilder()
                                    .setSourceIpv6Network(toIpv6Prefix(rule.srcIpAddr, rule.srcIpPrefixLen))
                                    .build())
                            .build());
        }
        return ipv6Builder.build();
    }

    default MacAddress sourceMac(@Nonnull final MacipAclRule rule) {
        return new MacAddress(byteArrayToMacSeparated(rule.srcMac != null
                ? rule.srcMac
                : Impl.DEFAULT_MAC_BYTES));
    }

    default MacAddress sourceMacMask(@Nonnull final MacipAclRule rule) {
        return new MacAddress(byteArrayToMacSeparated(rule.srcMacMask != null
                ? rule.srcMacMask
                : Impl.DEFAULT_MAC_MASK_BYTES));
    }

    final class Impl {
        private static final String DEFAULT_MAC = "00:00:00:00:00:00";
        private static final String DEFAULT_MAC_MASK = "00:00:00:00:00:00";
        private static final byte[] DEFAULT_MAC_BYTES = {0, 0, 0, 0, 0, 0};
        private static final byte[] DEFAULT_MAC_MASK_BYTES = {0, 0, 0, 0, 0, 0};
    }
}
