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

import io.fd.hc2vpp.acl.util.ace.extractor.MacIpAceDataExtractor;
import io.fd.hc2vpp.acl.util.ace.extractor.StandardAceDataExtractor;
import io.fd.hc2vpp.acl.util.protocol.ProtoPreBindRuleProducer;
import io.fd.vpp.jvpp.acl.types.AclRule;
import io.fd.vpp.jvpp.acl.types.MacipAclRule;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppMacipAce;

/**
 * Convert Ace's to vpp rules
 */
public interface AceConverter extends MacIpAceDataExtractor, StandardAceDataExtractor, ProtoPreBindRuleProducer {


    default MacipAclRule[] convertToMacIpAclRules(@Nonnull final List<Ace> aces) {
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

    default AclRule[] convertToStandardAclRules(@Nonnull final List<Ace> aces) {
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
}
