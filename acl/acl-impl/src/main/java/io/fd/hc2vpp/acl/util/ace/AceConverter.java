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

import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.acl.util.ace.extractor.MacIpAceDataExtractor;
import io.fd.hc2vpp.acl.util.ace.extractor.StandardAceDataExtractor;
import io.fd.hc2vpp.acl.util.protocol.ProtoPreBindRuleProducer;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.acl.types.AclRule;
import io.fd.vpp.jvpp.acl.types.MacipAclRule;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.AceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppMacipAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppMacipAceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.VppAceNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.VppMacipAceNodesBuilder;

/**
 * Convert between Ace's and vpp rules.
 */
public interface AceConverter extends MacIpAceDataExtractor, StandardAceDataExtractor, ProtoPreBindRuleProducer {

    default MacipAclRule[] toMacIpAclRules(@Nonnull final List<Ace> aces) {
        return aces.stream()
                .map(ace -> {
                    final VppMacipAce macIpAce = fromMacIpAce(ace);

                    MacipAclRule rule = new MacipAclRule();

                    rule.srcMac = sourceMacAsBytes(macIpAce);
                    rule.srcMacMask = sourceMacMaskAsBytes(macIpAce);
                    rule.isPermit = macIpAction(ace);

                    if (macIpIsIpv6(macIpAce)) {
                        rule.isIpv6 = 1;
                        rule.srcIpAddr = ipv6Address(macIpAce);
                        rule.srcIpPrefixLen = ipv6AddressPrefix(macIpAce);
                    } else {
                        rule.isIpv6 = 0;
                        rule.srcIpAddr = ipv4Address(macIpAce);
                        rule.srcIpPrefixLen = ipv4AddressPrefix(macIpAce);
                    }

                    return rule;
                })
                .collect(Collectors.toList())
                .toArray(new MacipAclRule[aces.size()]);
    }

    default AclRule[] toStandardAclRules(@Nonnull final List<Ace> aces) {
        return aces.stream()
                .map(ace -> {
                    final VppAce standardAce = fromStandardAce(ace);

                    // pre-bind rule with protocol based attributes
                    AclRule rule = createPreBindRule(standardAce);

                    rule.isPermit = standardAction(ace);

                    if (standardIsIpv6(ace)) {
                        rule.isIpv6 = 1;
                        rule.srcIpAddr = ipv6SourceAddress(standardAce);
                        rule.srcIpPrefixLen = ipv6SourceAddressPrefix(standardAce);
                        rule.dstIpAddr = ipv6DestinationAddress(standardAce);
                        rule.dstIpPrefixLen = ipv6DestinationAddressPrefix(standardAce);
                    } else {
                        rule.isIpv6 = 0;
                        rule.srcIpAddr = ipv4SourceAddress(standardAce);
                        rule.srcIpPrefixLen = ipv4SourceAddressPrefix(standardAce);
                        rule.dstIpAddr = ipv4DestinationAddress(standardAce);
                        rule.dstIpPrefixLen = ipv4DestinationAddressPrefix(standardAce);
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
            final VppMacipAceBuilder aceType = new VppMacipAceBuilder();
            final VppMacipAceNodesBuilder nodes = new VppMacipAceNodesBuilder();
            nodes.setAceIpVersion(ipVersion(rule));
            nodes.setSourceMacAddress(sourceMac(rule));
            nodes.setSourceMacAddressMask(sourceMacMask(rule));
            aceType.setVppMacipAceNodes(nodes.build());

            ace.setMatches(new MatchesBuilder().setAceType(aceType.build()).build());
            ace.setActions(actions(rule.isPermit));

            final String aceName = macipAclContext.getAceName(aclName, i++, mappingContext);
            ace.setRuleName(aceName);
            ace.setKey(new AceKey(aceName));

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
            final VppAceBuilder aceType = new VppAceBuilder();
            final VppAceNodesBuilder nodes = new VppAceNodesBuilder();
            nodes.setAceIpVersion(ipVersion(rule));
            nodes.setIpProtocol(parseProtocol(rule));
            aceType.setVppAceNodes(nodes.build());

            ace.setMatches(new MatchesBuilder().setAceType(aceType.build()).build());
            ace.setActions(actions(rule.isPermit));

            final String aceName = standardAclContext.getAceName(aclName, i++, mappingContext);
            ace.setRuleName(aceName);
            ace.setKey(new AceKey(aceName));
            aces.add(ace.build());
        }
        return aces;
    }
}
