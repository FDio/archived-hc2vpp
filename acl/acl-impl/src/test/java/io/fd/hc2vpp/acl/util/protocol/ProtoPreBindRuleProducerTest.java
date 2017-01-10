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

package io.fd.hc2vpp.acl.util.protocol;

import static org.junit.Assert.assertEquals;

import io.fd.hc2vpp.acl.AclTestSchemaContext;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.vpp.jvpp.acl.types.AclRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessLists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAce;

@RunWith(HoneycombTestRunner.class)
public class ProtoPreBindRuleProducerTest implements ProtoPreBindRuleProducer, AclTestSchemaContext {
    private static final byte IGNORE_PROTOCOL = 0;

    //TODO - remove after resolving how to address identity from different model in textual yang instance identifier
    private VppAce extractAce(AccessLists accessLists) {
        return VppAce.class
                .cast(accessLists.getAcl().get(0).getAccessListEntries().getAce().get(0).getMatches().getAceType());
    }

    @Test
    public void testIcmpRule(@InjectTestData(resourcePath = "/rules/icmp-rule.json") AccessLists acls) {
        final AclRule icmpRule = createPreBindRule(extractAce(acls));

        assertEquals(1, icmpRule.proto);
        assertEquals(5, icmpRule.srcportOrIcmptypeFirst);
        assertEquals(8, icmpRule.srcportOrIcmptypeLast);
        assertEquals(1, icmpRule.dstportOrIcmpcodeFirst);
        assertEquals(3, icmpRule.dstportOrIcmpcodeLast);
        assertEquals(0, icmpRule.tcpFlagsMask);
        assertEquals(0, icmpRule.tcpFlagsValue);
    }


    @Test
    public void testIcmpv6Rule(@InjectTestData(resourcePath = "/rules/icmp-v6-rule.json") AccessLists acls) {
        final AclRule icmpv6Rule = createPreBindRule(extractAce(acls));

        assertEquals(58, icmpv6Rule.proto);
        assertEquals(5, icmpv6Rule.srcportOrIcmptypeFirst);
        assertEquals(8, icmpv6Rule.srcportOrIcmptypeLast);
        assertEquals(1, icmpv6Rule.dstportOrIcmpcodeFirst);
        assertEquals(3, icmpv6Rule.dstportOrIcmpcodeLast);
        assertEquals(0, icmpv6Rule.tcpFlagsMask);
        assertEquals(0, icmpv6Rule.tcpFlagsValue);
    }

    @Test
    public void testTcpRule(@InjectTestData(resourcePath = "/rules/tcp-rule.json") AccessLists acls) {
        final AclRule tcpRule = createPreBindRule(extractAce(acls));
        assertEquals(6, tcpRule.proto);
        assertEquals(1, tcpRule.srcportOrIcmptypeFirst);
        assertEquals(5487, tcpRule.srcportOrIcmptypeLast);
        assertEquals(87, tcpRule.dstportOrIcmpcodeFirst);
        assertEquals(6745, tcpRule.dstportOrIcmpcodeLast);
        assertEquals(1, tcpRule.tcpFlagsMask);
        assertEquals(7, tcpRule.tcpFlagsValue);
    }

    @Test
    public void testUdpRule(@InjectTestData(resourcePath = "/rules/udp-rule.json") AccessLists acls) {
        final AclRule udpRule = createPreBindRule(extractAce(acls));
        assertEquals(17, udpRule.proto);
        assertEquals(1, udpRule.srcportOrIcmptypeFirst);
        assertEquals(5487, udpRule.srcportOrIcmptypeLast);
        assertEquals(87, udpRule.dstportOrIcmpcodeFirst);
        assertEquals(6745, udpRule.dstportOrIcmpcodeLast);
        assertEquals(0, udpRule.tcpFlagsMask);
        assertEquals(0, udpRule.tcpFlagsValue);
    }

    @Test
    public void testOtherRule(@InjectTestData(resourcePath = "/rules/other-rule.json") AccessLists acls) {
        final AclRule icmpRule = createPreBindRule(extractAce(acls));
        assertEquals(64, icmpRule.proto);
        assertEquals(0, icmpRule.srcportOrIcmptypeFirst);
        assertEquals((short) 65535, icmpRule.srcportOrIcmptypeLast);
        assertEquals(0, icmpRule.dstportOrIcmpcodeFirst);
        assertEquals((short) 65535, icmpRule.dstportOrIcmpcodeLast);
        assertEquals(0, icmpRule.tcpFlagsMask);
        assertEquals(0, icmpRule.tcpFlagsValue);
    }

    @Test
    public void tesProtocolNotSpecified(@InjectTestData(resourcePath = "/rules/no-protocol-rule.json") AccessLists acls) {
        final AclRule noProtocolRule = createPreBindRule(extractAce(acls));

        assertEquals(IGNORE_PROTOCOL, noProtocolRule.proto);
    }

}