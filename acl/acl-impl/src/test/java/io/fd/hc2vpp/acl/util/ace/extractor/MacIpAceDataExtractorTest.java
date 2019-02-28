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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.fd.hc2vpp.common.test.util.CommonTests;
import io.fd.jvpp.acl.types.MacipAclRule;
import java.util.Arrays;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.Accept;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.Drop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.L3;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.source.network.SourceIpv4Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.source.network.SourceIpv4NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.source.network.SourceIpv6Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.source.network.SourceIpv6NetworkBuilder;

public class MacIpAceDataExtractorTest extends AceDataExtractorTestCase implements MacIpAceDataExtractor, CommonTests {

    @Test
    public void testSourceMacAsBytes() {
        assertTrue(Arrays.equals(MAC_ADDRESS_BYTES, sourceMacAsBytes(MAC_ADDRESS)));
    }

    @Test
    public void sourceMacMaskAsBytes() {
        assertTrue(Arrays.equals(MAC_ADDRESS_BYTES, sourceMacMaskAsBytes(MAC_ADDRESS)));
    }

    @Test
    public void testIpv4Address() {
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4Address(new SourceIpv4NetworkBuilder().build())));
        assertTrue(Arrays.equals(IPV4_PREFIX_BYTES,
                ipv4Address(new SourceIpv4NetworkBuilder().setSourceIpv4Network(IPV4_PREFIX).build())));
    }

    @Test
    public void testIpv4AddressPrefix() {
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4AddressPrefix(new SourceIpv4NetworkBuilder().build()));
        assertEquals(IPV4_PREFIX_VALUE,
                ipv4AddressPrefix(new SourceIpv4NetworkBuilder().setSourceIpv4Network(IPV4_PREFIX).build()));
    }

    @Test
    public void testIpv6Address() {
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6Address(new SourceIpv6NetworkBuilder().build())));
        assertTrue(Arrays.equals(IPV6_PREFIX_BYTES,
                ipv6Address(new SourceIpv6NetworkBuilder().setSourceIpv6Network(IPV6_PREFIX).build())));
    }

    @Test
    public void testIpv6AddressPrefix() {
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6AddressPrefix(new SourceIpv6NetworkBuilder().build()));
        assertEquals(IPV6_PREFIX_VALUE,
                ipv6AddressPrefix(new SourceIpv6NetworkBuilder().setSourceIpv6Network(IPV6_PREFIX).build()));
    }

    @Test
    public void testMacIpAction() {
        verifyExceptionalCase(() -> macIpAction(new AceBuilder().build()), IllegalArgumentException.class);
        verifyExceptionalCase(() -> macIpAction(new AceBuilder().setActions(new ActionsBuilder().build()).build()),
                IllegalArgumentException.class);
        // this one must pass even if deny is not fully set, because of default value definition
        assertEquals((byte) 0, macIpAction(
                new AceBuilder().setActions(new ActionsBuilder().setForwarding(Drop.class).build()).build()));
        assertEquals((byte) 1, macIpAction(
                new AceBuilder().setActions(new ActionsBuilder().setForwarding(Accept.class).build()).build()));
    }

    @Test
    public void testIpVersionV4Defined() {
        MacipAclRule rule = new MacipAclRule();

        rule.isIpv6 = 0;
        rule.srcIpAddr = IPV4_PREFIX_BYTES;
        rule.srcIpPrefixLen = IPV4_PREFIX_VALUE;

        final L3 result = parseMacIpAceL3(rule);
        assertEquals(Ipv4.class, result.getImplementedInterface());
        assertEquals(IPV4_PREFIX,
                ((SourceIpv4Network) ((Ipv4) result).getIpv4().getSourceNetwork()).getSourceIpv4Network());
    }

    @Test
    public void testIpVersionV4Undefined() {
        MacipAclRule rule = new MacipAclRule();

        rule.isIpv6 = 0;

        final L3 result = parseMacIpAceL3(rule);
        assertEquals(Ipv4.class, result.getImplementedInterface());
        assertNull(((Ipv4) result).getIpv4());
    }

    @Test
    public void testIpVersionV6Defined() {
        MacipAclRule rule = new MacipAclRule();

        rule.isIpv6 = 1;
        rule.srcIpAddr = IPV6_PREFIX_BYTES;
        rule.srcIpPrefixLen = IPV6_PREFIX_VALUE;

        final L3 result = parseMacIpAceL3(rule);
        assertEquals(Ipv6.class, result.getImplementedInterface());
        assertEquals(IPV6_PREFIX, ((SourceIpv6Network) ((Ipv6) result).getIpv6().getSourceNetwork())
                .getSourceIpv6Network());
    }

    @Test
    public void testIpVersionV6Undefined() {
        MacipAclRule rule = new MacipAclRule();

        rule.isIpv6 = 1;

        final L3 result = parseMacIpAceL3(rule);
        assertEquals(Ipv6.class, result.getImplementedInterface());
        assertNull(((Ipv6) result).getIpv6());
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