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

package io.fd.hc2vpp.acl.util.ace;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.acl.util.ace.extractor.MacIpAceDataExtractor;
import io.fd.hc2vpp.acl.util.ace.extractor.StandardAceDataExtractor;
import io.fd.hc2vpp.acl.util.protocol.ProtoPreBindRuleProducer;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.jvpp.acl.types.AclRule;
import io.fd.jvpp.acl.types.MacipAclRule;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.AceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.L3;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l2.EthBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l2.eth.Eth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.SourceNetwork;

/**
 * Convert between Ace's and vpp rules.
 */
public interface AceConverter extends MacIpAceDataExtractor, StandardAceDataExtractor, ProtoPreBindRuleProducer {

    default MacipAclRule[] toMacIpAclRules(@Nonnull final List<Ace> aces) {
        return aces.stream()
                .filter(ace -> ace.getMatches() != null && ace.getMatches().getL2() != null)
                .filter(ace -> ace.getMatches().getL2().getImplementedInterface()
                        .equals(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l2.Eth.class))
                .map(ace -> {
                    MacipAclRule rule = new MacipAclRule();
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l2.Eth
                            l2 =
                            (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l2.Eth) ace
                                    .getMatches().getL2();

                    Eth eth = Preconditions
                            .checkNotNull(l2.getEth(), "Cannot parse eth for MacIpAcl ACE rule: {}", ace);

                    rule.srcMac = sourceMacAsBytes(eth.getSourceMacAddress());
                    rule.srcMacMask = sourceMacMaskAsBytes(eth.getSourceMacAddressMask());
                    rule.isPermit = macIpAction(ace);

                    L3 l3 = ace.getMatches().getL3();

                    if (l3 != null && l3.getImplementedInterface()
                            .equals(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv4.class)) {
                        Ipv4 ipv4 =
                                ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv4) l3)
                                        .getIpv4();
                        if (ipv4 != null && ipv4.getSourceNetwork() != null) {
                            // IPv4 is set for MacIpAcl
                            SourceNetwork sourceNetwork = ipv4.getSourceNetwork();
                            rule.isIpv6 = 0;
                            rule.srcIpAddr = ipv4Address(sourceNetwork);
                            rule.srcIpPrefixLen = ipv4AddressPrefix(sourceNetwork);
                        }
                    } else if (l3 != null && l3.getImplementedInterface()
                            .equals(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv6.class)) {
                        Ipv6 ipv6 =
                                ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv6) l3)
                                        .getIpv6();
                        if (ipv6 != null && ipv6.getSourceNetwork() != null) {
                            // IPv6 is set for MacIpAcl
                            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.SourceNetwork
                                    sourceNetwork = ipv6.getSourceNetwork();
                            rule.isIpv6 = 1;
                            rule.srcIpAddr = ipv6Address(sourceNetwork);
                            rule.srcIpPrefixLen = ipv6AddressPrefix(sourceNetwork);
                        }
                    } else {
                        // No IP is set for MacIpAcl
                        rule.isIpv6 = 0;
                        rule.srcIpAddr = new byte[4];
                        rule.srcIpPrefixLen = 0;
                    }

                    return rule;
                })
                .collect(Collectors.toList())
                .toArray(new MacipAclRule[aces.size()]);
    }

    default AclRule[] toStandardAclRules(@Nonnull final List<Ace> aces) {
        return aces.stream()
                .filter(ace -> ace.getMatches() != null)
                .map(ace -> {
                    // pre-bind rule with protocol based attributes (if present)
                    AclRule rule = createPreBindRule(ace);

                    rule.isPermit = standardAction(ace);

                    if (standardIsIpv6(ace.getMatches())) {
                        rule.isIpv6 = 1;
                        rule.srcIpAddr = ipv6SourceAddress(ace.getMatches());
                        rule.srcIpPrefixLen = ipv6SourceAddressPrefix(ace.getMatches());
                        rule.dstIpAddr = ipv6DestinationAddress(ace.getMatches());
                        rule.dstIpPrefixLen = ipv6DestinationAddressPrefix(ace.getMatches());
                    } else {
                        rule.isIpv6 = 0;
                        rule.srcIpAddr = ipv4SourceAddress(ace.getMatches());
                        rule.srcIpPrefixLen = ipv4SourceAddressPrefix(ace.getMatches());
                        rule.dstIpAddr = ipv4DestinationAddress(ace.getMatches());
                        rule.dstIpPrefixLen = ipv4DestinationAddressPrefix(ace.getMatches());
                    }

                    return rule;
                })
                .collect(Collectors.toList())
                .toArray(new AclRule[aces.size()]);
    }

    default List<Ace> toMacIpAces(final String aclName, @Nonnull MacipAclRule[] rules,
                                  @Nonnull final AclContextManager macipAclContext,
                                  @Nonnull final MappingContext mappingContext) {
        final List<Ace> aces = new ArrayList<>(rules.length);
        int i = 0;
        for (final MacipAclRule rule : rules) {
            final AceBuilder ace = new AceBuilder();
            ace.setMatches(
                    new MatchesBuilder()
                            .setL3(parseMacIpAceL3(rule))
                            .setL2(new EthBuilder()
                                    .setEth(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l2.eth.EthBuilder()
                                            .setSourceMacAddress(sourceMac(rule))
                                            .setSourceMacAddressMask(sourceMacMask(rule))
                                            .build())
                                    .build())
                            .build());
            ace.setActions(actions(rule.isPermit));

            final String aceName = macipAclContext.getAceName(aclName, i++, mappingContext);
            ace.setName(aceName);
            ace.withKey(new AceKey(aceName));

            aces.add(ace.build());
        }
        return aces;
    }

    default List<Ace> toStandardAces(final String aclName, @Nonnull AclRule[] rules,
                                     @Nonnull final AclContextManager standardAclContext,
                                     @Nonnull final MappingContext mappingContext) {
        final List<Ace> aces = new ArrayList<>(rules.length);
        int i = 0;
        for (final AclRule rule : rules) {
            final AceBuilder ace = new AceBuilder();
            ace.setMatches(new MatchesBuilder()
                    .setL3(parseStandardAceL3(rule))
                    .setL4(parseProtocol(rule))
                    .build());
            ace.setActions(actions(rule.isPermit));

            final String aceName = standardAclContext.getAceName(aclName, i++, mappingContext);
            ace.setName(aceName);
            ace.withKey(new AceKey(aceName));
            aces.add(ace.build());
        }
        return aces;
    }
}
