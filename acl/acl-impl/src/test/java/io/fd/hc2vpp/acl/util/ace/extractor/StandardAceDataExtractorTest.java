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
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.AcceptAndReflect;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.Accept;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.Drop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.L3;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.destination.network.DestinationIpv4Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.destination.network.DestinationIpv4NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.source.network.SourceIpv4Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv4.header.fields.source.network.SourceIpv4NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.destination.network.DestinationIpv6Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.destination.network.DestinationIpv6NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.source.network.SourceIpv6Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev181001.acl.ipv6.header.fields.source.network.SourceIpv6NetworkBuilder;


public class StandardAceDataExtractorTest extends AceDataExtractorTestCase implements StandardAceDataExtractor,
        CommonTests {

    @Test
    public void testStandardIsIpv6WithoutMatch() {
        assertFalse(standardIsIpv6(null));
        assertFalse(standardIsIpv6(new MatchesBuilder().build()));
        assertFalse(standardIsIpv6(new MatchesBuilder().setL3(new Ipv6Builder().build()).build()));
        assertTrue(standardIsIpv6(new MatchesBuilder().setL3(new Ipv6Builder().setIpv6(
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder()
                        .build()).build()).build()));
    }

    @Test
    public void testIpv4SourceAddress() {
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4SourceAddress(new MatchesBuilder().build())));
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES,
                ipv4SourceAddress(new MatchesBuilder().setL3(new Ipv4Builder().build()).build())));
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4SourceAddress(new MatchesBuilder()
                .setL3(new Ipv4Builder().setIpv4(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder()
                                .build()).build()).build())));
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4SourceAddress(new MatchesBuilder()
                .setL3(new Ipv4Builder().setIpv4(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder()
                                .setSourceNetwork(new SourceIpv4NetworkBuilder().build()).build()).build()).build())));
        assertTrue(Arrays.equals(IPV4_PREFIX_BYTES, ipv4SourceAddress(new MatchesBuilder()
                .setL3(new Ipv4Builder().setIpv4(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder()
                                .setSourceNetwork(
                                        new SourceIpv4NetworkBuilder().setSourceIpv4Network(IPV4_PREFIX).build())
                                .build()).build()).build())));
    }

    @Test
    public void testIpv4SourceAddressPrefix() {
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4SourceAddressPrefix(new MatchesBuilder().build()));
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE,
                ipv4SourceAddressPrefix(new MatchesBuilder().setL3(new Ipv4Builder().build()).build()));
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4SourceAddressPrefix(new MatchesBuilder()
                .setL3(new Ipv4Builder().setIpv4(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder()
                                .build()).build()).build()));
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4SourceAddressPrefix(new MatchesBuilder()
                .setL3(new Ipv4Builder().setIpv4(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder()
                                .setSourceNetwork(new SourceIpv4NetworkBuilder().build()).build()).build()).build()));
        assertEquals(IPV4_PREFIX_VALUE, ipv4SourceAddressPrefix(new MatchesBuilder()
                .setL3(new Ipv4Builder().setIpv4(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder()
                                .setSourceNetwork(
                                        new SourceIpv4NetworkBuilder().setSourceIpv4Network(IPV4_PREFIX).build())
                                .build()).build()).build()));
    }

    @Test
    public void testIpv4DestinationAddress() {
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4DestinationAddress(new MatchesBuilder().build())));
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4DestinationAddress(
                new MatchesBuilder().setL3(new Ipv4Builder().build()).build())));
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4DestinationAddress(
                new MatchesBuilder().setL3(new Ipv4Builder().setIpv4(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder()
                                .build()).build()).build())));
        assertTrue(Arrays.equals(DEFAULT_IPV4_ADDRESS_BYTES, ipv4DestinationAddress(
                new MatchesBuilder().setL3(new Ipv4Builder().setIpv4(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder()
                                .setDestinationNetwork(new DestinationIpv4NetworkBuilder()
                                        .build()).build()).build()).build())));
        assertTrue(Arrays.equals(IPV4_PREFIX_BYTES, ipv4DestinationAddress(
                new MatchesBuilder().setL3(new Ipv4Builder().setIpv4(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder()
                                .setDestinationNetwork(
                                        new DestinationIpv4NetworkBuilder().setDestinationIpv4Network(IPV4_PREFIX)
                                                .build()).build()).build()).build())));
    }

    @Test
    public void testIpv4DestinationAddressPrefix() {
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4DestinationAddressPrefix(new MatchesBuilder().build()));
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4DestinationAddressPrefix(
                new MatchesBuilder().setL3(new Ipv4Builder().build()).build()));
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4DestinationAddressPrefix(
                new MatchesBuilder().setL3(new Ipv4Builder().setIpv4(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder()
                                .build()).build()).build()));
        assertEquals(DEFAULT_IPV4_PREFIX_VALUE, ipv4DestinationAddressPrefix(
                new MatchesBuilder().setL3(new Ipv4Builder().setIpv4(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder()
                                .setDestinationNetwork(new DestinationIpv4NetworkBuilder()
                                        .build()).build()).build()).build()));
        assertEquals(IPV4_PREFIX_VALUE, ipv4DestinationAddressPrefix(
                new MatchesBuilder().setL3(new Ipv4Builder().setIpv4(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4Builder()
                                .setDestinationNetwork(
                                        new DestinationIpv4NetworkBuilder().setDestinationIpv4Network(IPV4_PREFIX)
                                                .build()).build()).build()).build()));
    }

    @Test
    public void testIpv6SourceAddress() {
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6SourceAddress(new MatchesBuilder().build())));
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES,
                ipv6SourceAddress(new MatchesBuilder().setL3(new Ipv6Builder().build()).build())));
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6SourceAddress(new MatchesBuilder()
                .setL3(new Ipv6Builder().setIpv6(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder()
                                .build()).build()).build())));
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6SourceAddress(new MatchesBuilder()
                .setL3(new Ipv6Builder().setIpv6(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder()
                                .setSourceNetwork(new SourceIpv6NetworkBuilder().build()).build()).build()).build())));
        assertTrue(Arrays.equals(IPV6_PREFIX_BYTES, ipv6SourceAddress(new MatchesBuilder().setL3(new Ipv6Builder()
                .setIpv6(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder()
                                .setSourceNetwork(
                                        new SourceIpv6NetworkBuilder().setSourceIpv6Network(IPV6_PREFIX).build())
                                .build()).build()).build())));
    }

    @Test
    public void ipv6SourceAddressPrefix() {
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6SourceAddressPrefix(new MatchesBuilder().build()));
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE,
                ipv6SourceAddressPrefix(new MatchesBuilder().setL3(new Ipv6Builder().build()).build()));
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6SourceAddressPrefix(new MatchesBuilder()
                .setL3(new Ipv6Builder().setIpv6(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder()
                                .build()).build()).build()));
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6SourceAddressPrefix(new MatchesBuilder()
                .setL3(new Ipv6Builder().setIpv6(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder()
                                .setSourceNetwork(new SourceIpv6NetworkBuilder().build()).build()).build()).build()));
        assertEquals(IPV6_PREFIX_VALUE, ipv6SourceAddressPrefix(new MatchesBuilder().setL3(new Ipv6Builder()
                .setIpv6(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder()
                                .setSourceNetwork(
                                        new SourceIpv6NetworkBuilder().setSourceIpv6Network(IPV6_PREFIX).build())
                                .build()).build()).build()));
    }

    @Test
    public void ipv6DestinationAddress() {
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6DestinationAddress(new MatchesBuilder().build())));
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES,
                ipv6DestinationAddress(new MatchesBuilder().setL3(new Ipv6Builder().build()).build())));
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6DestinationAddress(new MatchesBuilder()
                .setL3(new Ipv6Builder().setIpv6(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder()
                                .build()).build()).build())));
        assertTrue(Arrays.equals(DEFAULT_IPV6_ADDRESS_BYTES, ipv6DestinationAddress(new MatchesBuilder()
                .setL3(new Ipv6Builder().setIpv6(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder()
                                .setDestinationNetwork(new DestinationIpv6NetworkBuilder().build()).build()).build())
                .build())));
        assertTrue(Arrays.equals(IPV6_PREFIX_BYTES, ipv6DestinationAddress(new MatchesBuilder().setL3(new Ipv6Builder()
                .setIpv6(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder()
                                .setDestinationNetwork(
                                        new DestinationIpv6NetworkBuilder().setDestinationIpv6Network(IPV6_PREFIX)
                                                .build())
                                .build()).build()).build())));
    }

    @Test
    public void ipv6DestinationAddressPrefix() {
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6DestinationAddressPrefix(new MatchesBuilder().build()));
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE,
                ipv6DestinationAddressPrefix(new MatchesBuilder().setL3(new Ipv6Builder().build()).build()));
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6DestinationAddressPrefix(new MatchesBuilder()
                .setL3(new Ipv6Builder().setIpv6(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder()
                                .build()).build()).build()));
        assertEquals(DEFAULT_IPV6_PREFIX_VALUE, ipv6DestinationAddressPrefix(new MatchesBuilder()
                .setL3(new Ipv6Builder().setIpv6(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder()
                                .setDestinationNetwork(new DestinationIpv6NetworkBuilder().build()).build()).build())
                .build()));
        assertEquals(IPV6_PREFIX_VALUE, ipv6DestinationAddressPrefix(new MatchesBuilder().setL3(new Ipv6Builder()
                .setIpv6(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6Builder()
                                .setDestinationNetwork(
                                        new DestinationIpv6NetworkBuilder().setDestinationIpv6Network(IPV6_PREFIX)
                                                .build())
                                .build()).build()).build()));
    }

    @Test
    public void testStandardAction() {
        verifyExceptionalCase(() -> standardAction(new AceBuilder().build()), IllegalArgumentException.class);
        verifyExceptionalCase(() -> standardAction(new AceBuilder().setActions(new ActionsBuilder().build()).build()),
                IllegalArgumentException.class);

        // this one should pass because of default value
        assertEquals(0, standardAction(
                new AceBuilder().setActions(new ActionsBuilder().setForwarding(Drop.class).build()).build()));

        assertEquals(2, standardAction(new AceBuilder().setActions(
                new ActionsBuilder().setForwarding(AcceptAndReflect.class).build()).build()));
    }

    @Test
    public void testIpVersionV4Defined() {
        AclRule rule = new AclRule();

        rule.isIpv6 = 0;
        rule.srcIpAddr = IPV4_PREFIX_BYTES;
        rule.srcIpPrefixLen = IPV4_PREFIX_VALUE;
        rule.dstIpAddr = IPV4_2_PREFIX_BYTES;
        rule.dstIpPrefixLen = IPV4_2_PREFIX_VALUE;

        final L3 result = parseStandardAceL3(rule);
        assertEquals(result.getImplementedInterface(), Ipv4.class);
        assertEquals(IPV4_PREFIX,
                ((SourceIpv4Network) ((Ipv4) result).getIpv4().getSourceNetwork()).getSourceIpv4Network());
        assertEquals(IPV4_2_PREFIX,
                ((DestinationIpv4Network) ((Ipv4) result).getIpv4().getDestinationNetwork())
                        .getDestinationIpv4Network());
    }

    @Test
    public void testIpVersionV4Undefined() {
        AclRule rule = new AclRule();

        rule.isIpv6 = 0;

        final L3 result = parseStandardAceL3(rule);
        assertEquals(result.getImplementedInterface(), Ipv4.class);
        assertNull(((Ipv4) result).getIpv4().getSourceNetwork());
        assertNull(((Ipv4) result).getIpv4().getDestinationNetwork());
    }

    @Test
    public void testIpVersionV6Defined() {
        AclRule rule = new AclRule();

        rule.isIpv6 = 1;
        rule.srcIpAddr = IPV6_PREFIX_BYTES;
        rule.srcIpPrefixLen = IPV6_PREFIX_VALUE;
        rule.dstIpAddr = IPV6_2_PREFIX_BYTES;
        rule.dstIpPrefixLen = IPV6_2_PREFIX_VALUE;

        final L3 result = parseStandardAceL3(rule);
        assertEquals(result.getImplementedInterface(), Ipv6.class);
        assertEquals(IPV6_PREFIX,
                ((SourceIpv6Network) ((Ipv6) result).getIpv6().getSourceNetwork()).getSourceIpv6Network());
        assertEquals(IPV6_2_PREFIX,
                ((DestinationIpv6Network) ((Ipv6) result).getIpv6().getDestinationNetwork())
                        .getDestinationIpv6Network());
    }

    @Test
    public void testIpVersionV6Undefined() {
        AclRule rule = new AclRule();

        rule.isIpv6 = 1;

        final L3 result = parseStandardAceL3(rule);
        assertEquals(result.getImplementedInterface(), Ipv6.class);
        assertNull((((Ipv6) result).getIpv6().getSourceNetwork()));
        assertNull((((Ipv6) result).getIpv6().getDestinationNetwork()));
    }


    @Test
    public void testActions() {
        verifyExceptionalCase(() -> actions((byte) -1), IllegalArgumentException.class);
        assertTrue(actions((byte) 0).getForwarding().equals(Drop.class));
        assertTrue(actions((byte) 1).getForwarding().equals(Accept.class));
    }
}