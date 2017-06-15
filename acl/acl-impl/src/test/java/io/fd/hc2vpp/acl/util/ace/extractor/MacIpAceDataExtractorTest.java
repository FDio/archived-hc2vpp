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
import io.fd.vpp.jvpp.acl.types.MacipAclRule;
import java.util.Arrays;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.DenyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.PermitBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.VppMacipAce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.VppMacipAceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.VppMacipAceNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.vpp.macip.ace.nodes.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.vpp.macip.ace.nodes.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.vpp.macip.ace.nodes.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.vpp.macip.ace.nodes.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.vpp.macip.ace.nodes.ace.ip.version.AceIpv6Builder;

public class MacIpAceDataExtractorTest extends AceDataExtractorTestCase implements MacIpAceDataExtractor, CommonTests {

    @Test
    public void testFromMacIpAce() {
        verifyExceptionalCase(() -> fromMacIpAce(new AceBuilder().build()), IllegalArgumentException.class);
        verifyExceptionalCase(() -> fromMacIpAce(new AceBuilder().setMatches(new MatchesBuilder().build()).build()),
                IllegalArgumentException.class);

        final VppMacipAce macipAce = new VppMacipAceBuilder().build();
        assertEquals(macipAce, fromMacIpAce(new AceBuilder().setMatches(new MatchesBuilder()
                .setAceType(macipAce).build()).build()));
    }

    @Test
    public void testMacIpIsIpv6() {
        assertFalse(macIpIsIpv6(new VppMacipAceBuilder().build()));
        assertFalse(macIpIsIpv6(
                new VppMacipAceBuilder().setVppMacipAceNodes(new VppMacipAceNodesBuilder().build()).build()));
        assertFalse(macIpIsIpv6(new VppMacipAceBuilder().setVppMacipAceNodes(
                new VppMacipAceNodesBuilder().setAceIpVersion(new AceIpv4Builder().build()).build()).build()));
        assertTrue(macIpIsIpv6(new VppMacipAceBuilder().setVppMacipAceNodes(
                new VppMacipAceNodesBuilder().setAceIpVersion(new AceIpv6Builder().build()).build()).build()));
    }

    @Test
    public void testSourceMacAsBytes() {
        assertTrue(Arrays.equals(DEFAULT_MAC_ADDRESS_BYTES, sourceMacAsBytes(new VppMacipAceBuilder().build())));
        assertTrue(
                Arrays.equals(DEFAULT_MAC_ADDRESS_BYTES, sourceMacAsBytes(new VppMacipAceBuilder().setVppMacipAceNodes(
                        new VppMacipAceNodesBuilder().build()).build())));
        assertTrue(Arrays.equals(MAC_ADDRESS_BYTES,
                sourceMacAsBytes(new VppMacipAceBuilder().setVppMacipAceNodes(
                        new VppMacipAceNodesBuilder().setSourceMacAddress(MAC_ADDRESS).build())
                        .build())));
    }

    @Test
    public void sourceMacMaskAsBytes() {
        assertTrue(Arrays.equals(DEFAULT_MAC_ADDRESS_BYTES, sourceMacMaskAsBytes(new VppMacipAceBuilder().build())));
        assertTrue(Arrays.equals(DEFAULT_MAC_ADDRESS_BYTES,
                sourceMacMaskAsBytes(new VppMacipAceBuilder().setVppMacipAceNodes(
                        new VppMacipAceNodesBuilder().build()).build())));
        assertTrue(Arrays.equals(MAC_ADDRESS_BYTES,
                sourceMacMaskAsBytes(new VppMacipAceBuilder().setVppMacipAceNodes(
                        new VppMacipAceNodesBuilder().setSourceMacAddressMask(MAC_ADDRESS).build())
                        .build())));
    }

    @Test
    public void testIpv4Address() {
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4Address(new VppMacipAceBuilder().build())));
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4Address(
                new VppMacipAceBuilder().setVppMacipAceNodes(new VppMacipAceNodesBuilder().build()).build())));
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4Address(new VppMacipAceBuilder().setVppMacipAceNodes(
                new VppMacipAceNodesBuilder().setAceIpVersion(new AceIpv4Builder().build()).build()).build())));
        assertTrue(Arrays.equals(IPV4_PREFIX_BYTES, ipv4Address(new VppMacipAceBuilder().setVppMacipAceNodes(
                new VppMacipAceNodesBuilder().setAceIpVersion(new AceIpv4Builder()
                        .setSourceIpv4Network(IPV4_PREFIX).build()).build()).build())));
    }

    @Test
    public void testIpv4AddressPrefix() {
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4AddressPrefix(new VppMacipAceBuilder().build()));
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4AddressPrefix(
                new VppMacipAceBuilder().setVppMacipAceNodes(new VppMacipAceNodesBuilder().build()).build()));
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4AddressPrefix(new VppMacipAceBuilder().setVppMacipAceNodes(
                new VppMacipAceNodesBuilder().setAceIpVersion(new AceIpv4Builder().build()).build()).build()));
        assertEquals(IPV4_PREFIX_VALUE, ipv4AddressPrefix(new VppMacipAceBuilder().setVppMacipAceNodes(
                new VppMacipAceNodesBuilder().setAceIpVersion(new AceIpv4Builder()
                        .setSourceIpv4Network(IPV4_PREFIX).build()).build()).build()));
    }

    @Test
    public void testIpv6Address() {
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6Address(new VppMacipAceBuilder().build())));
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6Address(
                new VppMacipAceBuilder().setVppMacipAceNodes(new VppMacipAceNodesBuilder().build()).build())));
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6Address(new VppMacipAceBuilder().setVppMacipAceNodes(
                new VppMacipAceNodesBuilder().setAceIpVersion(new AceIpv6Builder().build()).build()).build())));
        assertTrue(Arrays.equals(IPV6_PREFIX_BYTES, ipv6Address(new VppMacipAceBuilder().setVppMacipAceNodes(
                new VppMacipAceNodesBuilder().setAceIpVersion(new AceIpv6Builder()
                        .setSourceIpv6Network(IPV6_PREFIX).build()).build()).build())));
    }

    @Test
    public void testIpv6AddressPrefix() {
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6AddressPrefix(new VppMacipAceBuilder().build()));
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6AddressPrefix(
                new VppMacipAceBuilder().setVppMacipAceNodes(new VppMacipAceNodesBuilder().build()).build()));
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6AddressPrefix(new VppMacipAceBuilder().setVppMacipAceNodes(
                new VppMacipAceNodesBuilder().setAceIpVersion(new AceIpv6Builder().build()).build()).build()));
        assertEquals(IPV6_PREFIX_VALUE, ipv6AddressPrefix(new VppMacipAceBuilder().setVppMacipAceNodes(
                new VppMacipAceNodesBuilder().setAceIpVersion(new AceIpv6Builder()
                        .setSourceIpv6Network(IPV6_PREFIX).build()).build()).build()));
    }

    @Test
    public void testMacIpAction() {
        verifyExceptionalCase(() -> macIpAction(new AceBuilder().build()), IllegalArgumentException.class);
        verifyExceptionalCase(() -> macIpAction(new AceBuilder().setActions(new ActionsBuilder().build()).build()),
                IllegalArgumentException.class);
        // this one must pass even if deny is not fully set, because of default value definition
        assertEquals((byte) 0, macIpAction(new AceBuilder().setActions(new ActionsBuilder().setPacketHandling(
                new DenyBuilder().build()).build()).build()));

        assertEquals((byte) 1, macIpAction(new AceBuilder().setActions(new ActionsBuilder().setPacketHandling(
                new PermitBuilder().setPermit(true).build()).build()).build()));
    }

    @Test
    public void testIpVersionV4Defined() {
        MacipAclRule rule = new MacipAclRule();

        rule.isIpv6 = 0;
        rule.srcIpAddr = IPV4_PREFIX_BYTES;
        rule.srcIpPrefixLen = IPV4_PREFIX_VALUE;

        final AceIpVersion result = ipVersion(rule);
        assertTrue(result instanceof AceIpv4);
        assertEquals(IPV4_PREFIX, AceIpv4.class.cast(result).getSourceIpv4Network());
    }

    @Test
    public void testIpVersionV4Undefined() {
        MacipAclRule rule = new MacipAclRule();

        rule.isIpv6 = 0;

        final AceIpVersion result = ipVersion(rule);
        assertTrue(result instanceof AceIpv4);
        assertNull(AceIpv4.class.cast(result).getSourceIpv4Network());
    }

    @Test
    public void testIpVersionV6Defined() {
        MacipAclRule rule = new MacipAclRule();

        rule.isIpv6 = 1;
        rule.srcIpAddr = IPV6_PREFIX_BYTES;
        rule.srcIpPrefixLen = IPV6_PREFIX_VALUE;

        final AceIpVersion result = ipVersion(rule);
        assertTrue(result instanceof AceIpv6);
        assertEquals(IPV6_PREFIX, AceIpv6.class.cast(result).getSourceIpv6Network());
    }

    @Test
    public void testIpVersionV6Undefined() {
        MacipAclRule rule = new MacipAclRule();

        rule.isIpv6 = 1;

        final AceIpVersion result = ipVersion(rule);
        assertTrue(result instanceof AceIpv6);
        assertNull(AceIpv6.class.cast(result).getSourceIpv6Network());
    }

    @Test
    public void testSourceMac() {
        assertEquals(DEFAULT_MAC_ADDRESS, sourceMac(new MacipAclRule()));


        MacipAclRule rule = new MacipAclRule();
        rule.srcMac = MAC_ADDRESS_BYTES;
        assertEquals(MAC_ADDRESS, sourceMac(rule));
    }

    @Test
    public void testSourceMacMask() {
        assertEquals(DEFAULT_MAC_MASK_ADDRESS, sourceMacMask(new MacipAclRule()));


        MacipAclRule rule = new MacipAclRule();
        rule.srcMac = MAC_ADDRESS_MASK_BYTES;
        assertEquals(MAC_ADDRESS_MASK, sourceMac(rule));
    }
}