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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.acl.ip.protocol.header.fields.ip.protocol.Icmp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.acl.ip.protocol.header.fields.ip.protocol.IcmpV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.acl.ip.protocol.header.fields.ip.protocol.Other;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.acl.ip.protocol.header.fields.ip.protocol.Tcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.acl.ip.protocol.header.fields.ip.protocol.Udp;

@RunWith(HoneycombTestRunner.class)
public class ProtocolParsingTest implements ProtoPreBindRuleProducer, IpProtocolReader, AclTestSchemaContext {
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
        assertEquals(0, icmpRule.tcpFlagsMask);
        assertEquals(0, icmpRule.tcpFlagsValue);

        final Icmp protocol = (Icmp)parseProtocol(icmpRule);
        assertEquals(5, protocol.getIcmpNodes().getIcmpTypeRange().getFirst().shortValue());
        assertEquals(8, protocol.getIcmpNodes().getIcmpTypeRange().getLast().shortValue());
        assertEquals(1, protocol.getIcmpNodes().getIcmpCodeRange().getFirst().shortValue());
        assertEquals(3, protocol.getIcmpNodes().getIcmpCodeRange().getLast().shortValue());
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

        final IcmpV6 protocol = (IcmpV6)parseProtocol(icmpv6Rule);
        assertEquals(5, protocol.getIcmpV6Nodes().getIcmpTypeRange().getFirst().shortValue());
        assertEquals(8, protocol.getIcmpV6Nodes().getIcmpTypeRange().getLast().shortValue());
        assertEquals(1, protocol.getIcmpV6Nodes().getIcmpCodeRange().getFirst().shortValue());
        assertEquals(3, protocol.getIcmpV6Nodes().getIcmpCodeRange().getLast().shortValue());
    }

    @Test
    public void testTcpRule(@InjectTestData(resourcePath = "/rules/tcp-rule.json") AccessLists acls) {
        final AclRule tcpRule = createPreBindRule(extractAce(acls));
        assertEquals(6, tcpRule.proto);
        assertEquals(1, tcpRule.tcpFlagsMask);
        assertEquals(7, tcpRule.tcpFlagsValue);

        final Tcp protocol = (Tcp)parseProtocol(tcpRule);
        assertEquals(1, protocol.getTcpNodes().getSourcePortRange().getLowerPort().getValue().intValue());
        assertEquals(5487, protocol.getTcpNodes().getSourcePortRange().getUpperPort().getValue().intValue());
        assertEquals(87, protocol.getTcpNodes().getDestinationPortRange().getLowerPort().getValue().intValue());
        assertEquals(6745, protocol.getTcpNodes().getDestinationPortRange().getUpperPort().getValue().intValue());
    }

    @Test
    public void testTcpRuleNoFlags(@InjectTestData(resourcePath = "/rules/tcp-rule-no-flags.json") AccessLists acls) {
        final AclRule tcpRule = createPreBindRule(extractAce(acls));
        assertEquals(6, tcpRule.proto);
        assertEquals(123, tcpRule.srcportOrIcmptypeFirst);
        assertEquals(123, tcpRule.srcportOrIcmptypeLast);
        assertEquals((short)65000, tcpRule.dstportOrIcmpcodeFirst);
        assertEquals((short)65000, tcpRule.dstportOrIcmpcodeLast);
        assertEquals(0, tcpRule.tcpFlagsMask);
        assertEquals(0, tcpRule.tcpFlagsValue);

        final Tcp protocol = (Tcp)parseProtocol(tcpRule);
        assertEquals(123, protocol.getTcpNodes().getSourcePortRange().getLowerPort().getValue().intValue());
        assertEquals(123, protocol.getTcpNodes().getSourcePortRange().getUpperPort().getValue().intValue());
        assertEquals(65000, protocol.getTcpNodes().getDestinationPortRange().getLowerPort().getValue().intValue());
        assertEquals(65000, protocol.getTcpNodes().getDestinationPortRange().getUpperPort().getValue().intValue());
    }

    @Test
    public void testSourcePortRangeNotGiven() {
        AclRule rule = new AclRule();
        ProtoPreBindRuleProducer.bindSourcePortRange(rule, null);
        assertEquals(0, rule.srcportOrIcmptypeFirst);
        assertEquals(MAX_PORT_NUMBER, rule.srcportOrIcmptypeLast);
    }

    @Test
    public void testDestinationPortRangeNotGiven() {
        AclRule rule = new AclRule();
        ProtoPreBindRuleProducer.bindDestinationPortRange(rule, null);
        assertEquals(0, rule.dstportOrIcmpcodeFirst);
        assertEquals(MAX_PORT_NUMBER, rule.dstportOrIcmpcodeLast);
    }

    @Test
    public void testUdpRule(@InjectTestData(resourcePath = "/rules/udp-rule.json") AccessLists acls) {
        final AclRule udpRule = createPreBindRule(extractAce(acls));
        assertEquals(17, udpRule.proto);
        assertEquals(0, udpRule.tcpFlagsMask);
        assertEquals(0, udpRule.tcpFlagsValue);

        final Udp protocol = (Udp)parseProtocol(udpRule);
        assertEquals(1, protocol.getUdpNodes().getSourcePortRange().getLowerPort().getValue().intValue());
        assertEquals(5487, protocol.getUdpNodes().getSourcePortRange().getUpperPort().getValue().intValue());
        assertEquals(87, protocol.getUdpNodes().getDestinationPortRange().getLowerPort().getValue().intValue());
        assertEquals(6745, protocol.getUdpNodes().getDestinationPortRange().getUpperPort().getValue().intValue());
    }

    @Test
    public void testOtherRule(@InjectTestData(resourcePath = "/rules/other-rule.json") AccessLists acls) {
        final AclRule rule = createPreBindRule(extractAce(acls));
        final int protocolNumber = 64;
        assertEquals(protocolNumber, rule.proto);
        assertEquals(0, rule.srcportOrIcmptypeFirst);
        assertEquals((short) 65535, rule.srcportOrIcmptypeLast);
        assertEquals(0, rule.dstportOrIcmpcodeFirst);
        assertEquals((short) 65535, rule.dstportOrIcmpcodeLast);
        assertEquals(0, rule.tcpFlagsMask);
        assertEquals(0, rule.tcpFlagsValue);

        final Other protocol = (Other)parseProtocol(rule);
        assertEquals(protocolNumber, protocol.getOtherNodes().getProtocol().shortValue());
    }

    @Test
    public void tesProtocolNotSpecified(@InjectTestData(resourcePath = "/rules/no-protocol-rule.json") AccessLists acls) {
        final AclRule noProtocolRule = createPreBindRule(extractAce(acls));

        assertEquals(IGNORE_PROTOCOL, noProtocolRule.proto);
    }

}