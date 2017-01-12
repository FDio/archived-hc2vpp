/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.fd.hc2vpp.common.test.util.CommonTests;
import io.fd.vpp.jvpp.acl.types.AclRule;
import java.util.Arrays;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Deny;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.DenyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Permit;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.actions.packet.handling.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.actions.packet.handling.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.VppAceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.VppAceNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.vpp.ace.nodes.ace.ip.version.AceIpv6Builder;


public class StandardAceDataExtractorTest extends AceDataExtractorTestCase implements StandardAceDataExtractor,
        CommonTests {

    @Test
    public void testFromStandardAce() {
        verifyExceptionalCase(() -> fromStandardAce(new AceBuilder().build()), IllegalArgumentException.class);
        verifyExceptionalCase(() -> fromStandardAce(new AceBuilder().setMatches(new MatchesBuilder().build()).build()),
                IllegalArgumentException.class);

        final VppAce ace = new VppAceBuilder().build();
        assertEquals(ace, fromStandardAce(new AceBuilder().setMatches(new MatchesBuilder()
                .setAceType(ace).build()).build()));
    }

    @Test
    public void testStandardIsIpv6() {
        assertFalse(standardIsIpv6(new AceBuilder().build()));
        assertFalse(standardIsIpv6(new AceBuilder().setMatches(new MatchesBuilder().build()).build()));
        assertFalse(standardIsIpv6(
                new AceBuilder().setMatches(new MatchesBuilder().setAceType(new VppAceBuilder().build()).build())
                        .build()));
        assertFalse(standardIsIpv6(new AceBuilder().setMatches(new MatchesBuilder()
                .setAceType(new VppAceBuilder().setVppAceNodes(new VppAceNodesBuilder().build()).build()).build())
                .build()));
        assertTrue(standardIsIpv6(new AceBuilder().setMatches(new MatchesBuilder().setAceType(new VppAceBuilder()
                .setVppAceNodes(new VppAceNodesBuilder().setAceIpVersion(new AceIpv6Builder().build()).build()).build())
                .build()).build()));
    }

    @Test
    public void testIpv4SourceAddress() {
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4SourceAddress(new VppAceBuilder().build())));
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4SourceAddress(
                new VppAceBuilder().setVppAceNodes(new VppAceNodesBuilder().build()).build())));
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4SourceAddress(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv4Builder().build()).build()).build())));
        assertTrue(Arrays.equals(IPV4_PREFIX_BYTES, ipv4SourceAddress(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv4Builder()
                        .setSourceIpv4Network(IPV4_PREFIX).build()).build()).build())));
    }

    @Test
    public void testIpv4SourceAddressPrefix() {
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4SourceAddressPrefix(new VppAceBuilder().build()));
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4SourceAddressPrefix(
                new VppAceBuilder().setVppAceNodes(new VppAceNodesBuilder().build()).build()));
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4SourceAddressPrefix(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv4Builder().build()).build()).build()));
        assertEquals(IPV4_PREFIX_VALUE, ipv4SourceAddressPrefix(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv4Builder()
                        .setSourceIpv4Network(IPV4_PREFIX).build()).build()).build()));
    }

    @Test
    public void testIpv4DestinationAddress() {
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4DestinationAddress(new VppAceBuilder().build())));
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4DestinationAddress(
                new VppAceBuilder().setVppAceNodes(new VppAceNodesBuilder().build()).build())));
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4DestinationAddress(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv4Builder().build()).build()).build())));
        assertTrue(Arrays.equals(IPV4_PREFIX_BYTES, ipv4DestinationAddress(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv4Builder()
                        .setDestinationIpv4Network(IPV4_PREFIX).build()).build()).build())));
    }

    @Test
    public void testIpv4DestinationAddressPrefix() {
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4DestinationAddressPrefix(new VppAceBuilder().build()));
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4DestinationAddressPrefix(
                new VppAceBuilder().setVppAceNodes(new VppAceNodesBuilder().build()).build()));
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4DestinationAddressPrefix(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv4Builder().build()).build()).build()));
        assertEquals(IPV4_PREFIX_VALUE, ipv4DestinationAddressPrefix(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv4Builder()
                        .setDestinationIpv4Network(IPV4_PREFIX).build()).build()).build()));
    }

    @Test
    public void testIpv6SourceAddress() {
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6SourceAddress(new VppAceBuilder().build())));
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6SourceAddress(
                new VppAceBuilder().setVppAceNodes(new VppAceNodesBuilder().build()).build())));
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6SourceAddress(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv6Builder().build()).build()).build())));
        assertTrue(Arrays.equals(IPV6_PREFIX_BYTES, ipv6SourceAddress(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv6Builder()
                        .setSourceIpv6Network(IPV6_PREFIX).build()).build()).build())));
    }

    @Test
    public void ipv6SourceAddressPrefix() {
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6SourceAddressPrefix(new VppAceBuilder().build()));
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6SourceAddressPrefix(
                new VppAceBuilder().setVppAceNodes(new VppAceNodesBuilder().build()).build()));
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6SourceAddressPrefix(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv6Builder().build()).build()).build()));
        assertEquals(IPV6_PREFIX_VALUE, ipv6SourceAddressPrefix(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv6Builder()
                        .setSourceIpv6Network(IPV6_PREFIX).build()).build()).build()));
    }

    @Test
    public void ipv6DestinationAddress() {
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6DestinationAddress(new VppAceBuilder().build())));
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6DestinationAddress(
                new VppAceBuilder().setVppAceNodes(new VppAceNodesBuilder().build()).build())));
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6DestinationAddress(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv6Builder().build()).build()).build())));
        assertTrue(Arrays.equals(IPV6_PREFIX_BYTES, ipv6DestinationAddress(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv6Builder()
                        .setDestinationIpv6Network(IPV6_PREFIX).build()).build()).build())));
    }

    @Test
    public void ipv6DestinationAddressPrefix() {
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6DestinationAddressPrefix(new VppAceBuilder().build()));
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6DestinationAddressPrefix(
                new VppAceBuilder().setVppAceNodes(new VppAceNodesBuilder().build()).build()));
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6DestinationAddressPrefix(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv6Builder().build()).build()).build()));
        assertEquals(IPV6_PREFIX_VALUE, ipv6DestinationAddressPrefix(new VppAceBuilder().setVppAceNodes(
                new VppAceNodesBuilder().setAceIpVersion(new AceIpv6Builder()
                        .setDestinationIpv6Network(IPV6_PREFIX).build()).build()).build()));
    }

    @Test
    public void testStandardAction() {
        verifyExceptionalCase(() -> standardAction(new AceBuilder().build()), IllegalArgumentException.class);
        verifyExceptionalCase(() -> standardAction(new AceBuilder().setActions(new ActionsBuilder().build()).build()),
                IllegalArgumentException.class);

        // this one should pass because of default value
        assertEquals(0, standardAction(
                new AceBuilder().setActions(new ActionsBuilder().setPacketHandling(new DenyBuilder().build()).build())
                        .build()));

        assertEquals(2, standardAction(new AceBuilder().setActions(
                new ActionsBuilder().setPacketHandling(new StatefulBuilder().setPermit(true).build()).build())
                .build()));
    }

    @Test
    public void testIpVersionV4Defined() {
        AclRule rule = new AclRule();

        rule.isIpv6 = 0;
        rule.srcIpAddr = IPV4_PREFIX_BYTES;
        rule.srcIpPrefixLen = IPV4_PREFIX_VALUE;
        rule.dstIpAddr = IPV4_2_PREFIX_BYTES;
        rule.dstIpPrefixLen = IPV4_2_PREFIX_VALUE;

        final AceIpVersion result = ipVersion(rule);
        assertTrue(result instanceof AceIpv4);
        assertEquals(IPV4_PREFIX, AceIpv4.class.cast(result).getSourceIpv4Network());
        assertEquals(IPV4_2_PREFIX, AceIpv4.class.cast(result).getDestinationIpv4Network());
    }

    @Test
    public void testIpVersionV4Undefined() {
        AclRule rule = new AclRule();

        rule.isIpv6 = 0;

        final AceIpVersion result = ipVersion(rule);
        assertTrue(result instanceof AceIpv4);
        assertNull(AceIpv4.class.cast(result).getSourceIpv4Network());
        assertNull(AceIpv4.class.cast(result).getDestinationIpv4Network());
    }

    @Test
    public void testIpVersionV6Defined() {
        AclRule rule = new AclRule();

        rule.isIpv6 = 1;
        rule.srcIpAddr = IPV6_PREFIX_BYTES;
        rule.srcIpPrefixLen = IPV6_PREFIX_VALUE;
        rule.dstIpAddr = IPV6_2_PREFIX_BYTES;
        rule.dstIpPrefixLen = IPV6_2_PREFIX_VALUE;

        final AceIpVersion result = ipVersion(rule);
        assertTrue(result instanceof AceIpv6);
        assertEquals(IPV6_PREFIX, AceIpv6.class.cast(result).getSourceIpv6Network());
        assertEquals(IPV6_2_PREFIX, AceIpv6.class.cast(result).getDestinationIpv6Network());
    }

    @Test
    public void testIpVersionV6Undefined() {
        AclRule rule = new AclRule();

        rule.isIpv6 = 1;

        final AceIpVersion result = ipVersion(rule);
        assertTrue(result instanceof AceIpv6);
        assertNull(AceIpv6.class.cast(result).getSourceIpv6Network());
        assertNull(AceIpv6.class.cast(result).getDestinationIpv6Network());
    }


    @Test
    public void testActions() {
        verifyExceptionalCase(() -> actions((byte) -1), IllegalArgumentException.class);
        assertTrue(actions((byte) 0).getPacketHandling() instanceof Deny);
        assertTrue(actions((byte) 1).getPacketHandling() instanceof Permit);
        assertTrue(actions((byte) 2).getPacketHandling() instanceof Stateful);
    }

}