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

package io.fd.hc2vpp.acl.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.acl.AclTestSchemaContext;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.acl.dto.AclAddReplace;
import io.fd.vpp.jvpp.acl.dto.AclAddReplaceReply;
import io.fd.vpp.jvpp.acl.dto.AclDel;
import io.fd.vpp.jvpp.acl.dto.AclDelReply;
import io.fd.vpp.jvpp.acl.dto.MacipAclAdd;
import io.fd.vpp.jvpp.acl.dto.MacipAclAddReply;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import io.fd.vpp.jvpp.acl.types.AclRule;
import io.fd.vpp.jvpp.acl.types.MacipAclRule;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.VppAcl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class VppAclCustomizerTest extends WriterCustomizerTest implements AclTestSchemaContext {

    @Mock
    private FutureJVppAclFacade aclApi;

    @Captor
    private ArgumentCaptor<AclAddReplace> aclAddReplaceRequestCaptor;

    @Captor
    private ArgumentCaptor<MacipAclAdd> macipAclAddReplaceRequestCaptor;

    @Captor
    private ArgumentCaptor<AclDel> aclDelRequestCaptor;

    private InstanceIdentifier<Acl> validId;
    private InstanceIdentifier<Acl> validMacipId;
    private VppAclCustomizer aclCustomizer;

    @Mock
    private AclContextManager standardAclContext;
    @Mock
    private AclContextManager macIpAclContext;


    @Override
    protected void setUpTest() throws Exception {
        validId =
                InstanceIdentifier.create(AccessLists.class).child(Acl.class, new AclKey("standard-acl", VppAcl.class));
        validMacipId =
                InstanceIdentifier.create(AccessLists.class).child(Acl.class, new AclKey("macip-acl", VppAcl.class));
        aclCustomizer = new VppAclCustomizer(aclApi, standardAclContext, macIpAclContext);

        when(aclApi.aclAddReplace(any())).thenReturn(future(new AclAddReplaceReply()));
        when(aclApi.aclDel(any())).thenReturn(future(new AclDelReply()));
        when(aclApi.macipAclAdd(any())).thenReturn(future(new MacipAclAddReply()));
    }

    @Test
    public void writeCurrentAttributesMacip(@InjectTestData(resourcePath = "/acl/macip/macip-acl.json")
                                                    AccessLists macipAcl) throws WriteFailedException {

        aclCustomizer.writeCurrentAttributes(validMacipId, macipAcl.getAcl().get(0), writeContext);

        verify(aclApi, times(1)).macipAclAdd(macipAclAddReplaceRequestCaptor.capture());

        final MacipAclAdd request = macipAclAddReplaceRequestCaptor.getValue();

        assertEquals(1, request.count);
        assertEquals("macip-tag-value", new String(request.tag, StandardCharsets.US_ASCII));

        final MacipAclRule rule = request.r[0];

        assertEquals(0, rule.isIpv6);
        assertEquals(1, rule.isPermit);
        assertTrue(Arrays.equals(new byte[]{-64, -88, 2, 2}, rule.srcIpAddr));
        assertEquals(32, rule.srcIpPrefixLen);
        assertTrue(Arrays.equals(new byte[]{(byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xaa}, rule.srcMac));
        assertTrue(Arrays.equals(new byte[]{(byte)0xff, 0, 0, 0, 0, 0}, rule.srcMacMask));
    }

    @Test
    public void writeCurrentAttributesIcmpIpv4(@InjectTestData(resourcePath = "/acl/standard/standard-acl-icmp.json")
                                                       AccessLists standardAcls) throws Exception {
        aclCustomizer.writeCurrentAttributes(validId, standardAcls.getAcl().get(0), writeContext);

        verify(aclApi, times(1)).aclAddReplace(aclAddReplaceRequestCaptor.capture());
        verifyIcmpIpv4Request(-1);
    }

    @Test
    public void updateCurrentAttributesIcmpIpv4(@InjectTestData(resourcePath = "/acl/standard/standard-acl-icmp.json")
                                                        AccessLists standardAcls) throws Exception {
        final int aclIndex = 4;
        when(standardAclContext.getAclIndex("standard-acl", mappingContext)).thenReturn(aclIndex);
        final Acl data = standardAcls.getAcl().get(0);

        aclCustomizer.updateCurrentAttributes(validId, data, data, writeContext);

        verify(aclApi, times(1)).aclAddReplace(aclAddReplaceRequestCaptor.capture());
        verifyIcmpIpv4Request(aclIndex);
    }


    @Test
    public void writeCurrentAttributesIcmpIpv6(@InjectTestData(resourcePath = "/acl/standard/standard-acl-icmp-v6.json")
                                                       AccessLists standardAcls) throws Exception {
        aclCustomizer.writeCurrentAttributes(validId, standardAcls.getAcl().get(0), writeContext);

        verify(aclApi, times(1)).aclAddReplace(aclAddReplaceRequestCaptor.capture());
        verifyIcmpv6Request(-1);
    }

    @Test
    public void updateCurrentAttributesIcmpIpv6(
            @InjectTestData(resourcePath = "/acl/standard/standard-acl-icmp-v6.json")
                    AccessLists standardAcls) throws Exception {
        final int aclIndex = 4;
        when(standardAclContext.getAclIndex("standard-acl", mappingContext)).thenReturn(aclIndex);
        final Acl data = standardAcls.getAcl().get(0);
        aclCustomizer.updateCurrentAttributes(validId, data, data, writeContext);

        verify(aclApi, times(1)).aclAddReplace(aclAddReplaceRequestCaptor.capture());
        verifyIcmpv6Request(aclIndex);
    }


    @Test
    public void writeCurrentAttributesTcp(@InjectTestData(resourcePath = "/acl/standard/standard-acl-tcp.json")
                                                  AccessLists standardAcls) throws Exception {
        aclCustomizer.writeCurrentAttributes(validId, standardAcls.getAcl().get(0), writeContext);

        verify(aclApi, times(1)).aclAddReplace(aclAddReplaceRequestCaptor.capture());
        verifyTcpRequest(-1);
    }

    @Test
    public void updateCurrentAttributesTcp(@InjectTestData(resourcePath = "/acl/standard/standard-acl-tcp.json")
                                           AccessLists standardAcls) throws Exception {
        final int aclIndex = 4;
        when(standardAclContext.getAclIndex("standard-acl", mappingContext)).thenReturn(aclIndex);
        final Acl data = standardAcls.getAcl().get(0);
        aclCustomizer.updateCurrentAttributes(validId, data, data, writeContext);

        verify(aclApi, times(1)).aclAddReplace(aclAddReplaceRequestCaptor.capture());
        verifyTcpRequest(aclIndex);
    }

    @Test
    public void updateCurrentAttributesTcpSrcOnly(@InjectTestData(resourcePath = "/acl/standard/standard-acl-tcp-src-only.json")
                                           AccessLists standardAcls) throws Exception {
        final int aclIndex = 4;
        when(standardAclContext.getAclIndex("standard-acl", mappingContext)).thenReturn(aclIndex);
        final Acl data = standardAcls.getAcl().get(0);
        aclCustomizer.updateCurrentAttributes(validId, data, data, writeContext);

        verify(aclApi, times(1)).aclAddReplace(aclAddReplaceRequestCaptor.capture());
        final AclAddReplace request = aclAddReplaceRequestCaptor.getValue();
        final AclRule tcpRule = request.r[0];
        assertTrue(Arrays.equals(new byte[]{-64, -88, 2, 2}, tcpRule.srcIpAddr));
        assertEquals(32, tcpRule.srcIpPrefixLen);
        assertTrue(Arrays.equals(new byte[]{0, 0, 0, 0}, tcpRule.dstIpAddr));
        assertEquals(0, tcpRule.dstIpPrefixLen);
    }


    @Test
    public void writeCurrentAttributesUdp(@InjectTestData(resourcePath = "/acl/standard/standard-acl-udp.json")
                                                  AccessLists standardAcls) throws Exception {
        aclCustomizer.writeCurrentAttributes(validId, standardAcls.getAcl().get(0), writeContext);

        verify(aclApi, times(1)).aclAddReplace(aclAddReplaceRequestCaptor.capture());

        verifyUdpRequest(-1);
    }

    @Test
    public void updateCurrentAttributesUdp(@InjectTestData(resourcePath = "/acl/standard/standard-acl-udp.json")
                                                   AccessLists standardAcls) throws Exception {
        final int aclIndex = 4;
        when(standardAclContext.getAclIndex("standard-acl", mappingContext)).thenReturn(aclIndex);
        final Acl data = standardAcls.getAcl().get(0);
        aclCustomizer.updateCurrentAttributes(validId, data, data, writeContext);

        verify(aclApi, times(1)).aclAddReplace(aclAddReplaceRequestCaptor.capture());
        verifyUdpRequest(aclIndex);
    }


    @Test
    public void deleteCurrentAttributesIcmpIpv4(@InjectTestData(resourcePath = "/acl/standard/standard-acl-icmp.json")
                                                        AccessLists standardAcls) throws Exception {
        final int aclIndex = 4;
        when(standardAclContext.getAclIndex("standard-acl", mappingContext)).thenReturn(aclIndex);
        aclCustomizer.deleteCurrentAttributes(validId, standardAcls.getAcl().get(0), writeContext);

        verify(aclApi, times(1)).aclDel(aclDelRequestCaptor.capture());
        assertEquals(aclIndex, aclDelRequestCaptor.getValue().aclIndex);
    }

    @Test
    public void deleteCurrentAttributesIcmpIpv6(
            @InjectTestData(resourcePath = "/acl/standard/standard-acl-icmp-v6.json")
                    AccessLists standardAcls) throws Exception {
        final int aclIndex = 4;
        when(standardAclContext.getAclIndex("standard-acl", mappingContext)).thenReturn(aclIndex);
        aclCustomizer.deleteCurrentAttributes(validId, standardAcls.getAcl().get(0), writeContext);

        verify(aclApi, times(1)).aclDel(aclDelRequestCaptor.capture());
        assertEquals(aclIndex, aclDelRequestCaptor.getValue().aclIndex);
    }

    @Test
    public void deleteCurrentAttributesTcp(@InjectTestData(resourcePath = "/acl/standard/standard-acl-tcp.json")
                                                   AccessLists standardAcls) throws Exception {
        final int aclIndex = 4;
        when(standardAclContext.getAclIndex("standard-acl", mappingContext)).thenReturn(aclIndex);
        aclCustomizer.deleteCurrentAttributes(validId, standardAcls.getAcl().get(0), writeContext);

        verify(aclApi, times(1)).aclDel(aclDelRequestCaptor.capture());
        assertEquals(aclIndex, aclDelRequestCaptor.getValue().aclIndex);
    }

    @Test
    public void deleteCurrentAttributesUdp(@InjectTestData(resourcePath = "/acl/standard/standard-acl-udp.json")
                                                   AccessLists standardAcls) throws Exception {
        final int aclIndex = 4;
        when(standardAclContext.getAclIndex("standard-acl", mappingContext)).thenReturn(aclIndex);
        aclCustomizer.deleteCurrentAttributes(validId, standardAcls.getAcl().get(0), writeContext);

        verify(aclApi, times(1)).aclDel(aclDelRequestCaptor.capture());
        assertEquals(aclIndex, aclDelRequestCaptor.getValue().aclIndex);
    }

    private void verifyUdpRequest(final int aclIndex) {
        final AclAddReplace request = aclAddReplaceRequestCaptor.getValue();
        assertEquals(aclIndex, request.aclIndex);
        assertEquals(1, request.count);
        assertEquals("udp-tag-value", new String(request.tag, StandardCharsets.US_ASCII));

        final AclRule udpRule = request.r[0];

        assertEquals(0, udpRule.isIpv6);
        assertEquals(1, udpRule.isPermit);
        assertTrue(Arrays.equals(new byte[]{-64, -88, 2, 2}, udpRule.srcIpAddr));
        assertEquals(32, udpRule.srcIpPrefixLen);
        assertTrue(Arrays.equals(new byte[]{-64, -88, 2, 1}, udpRule.dstIpAddr));
        assertEquals(24, udpRule.dstIpPrefixLen);

        assertEquals(17, udpRule.proto);
        assertEquals(1, udpRule.srcportOrIcmptypeFirst);
        assertEquals(5487, udpRule.srcportOrIcmptypeLast);
        assertEquals(87, udpRule.dstportOrIcmpcodeFirst);
        assertEquals(6745, udpRule.dstportOrIcmpcodeLast);
        assertEquals(0, udpRule.tcpFlagsMask);
        assertEquals(0, udpRule.tcpFlagsValue);
    }

    private void verifyTcpRequest(final int aclIndex) {
        final AclAddReplace request = aclAddReplaceRequestCaptor.getValue();
        assertEquals(aclIndex, request.aclIndex);
        assertEquals(1, request.count);
        assertEquals("tcp-tag-value", new String(request.tag, StandardCharsets.US_ASCII));

        final AclRule tcpRule = request.r[0];

        assertEquals(0, tcpRule.isIpv6);
        assertEquals(1, tcpRule.isPermit);
        assertTrue(Arrays.equals(new byte[]{-64, -88, 2, 2}, tcpRule.srcIpAddr));
        assertEquals(32, tcpRule.srcIpPrefixLen);
        assertTrue(Arrays.equals(new byte[]{-64, -88, 2, 1}, tcpRule.dstIpAddr));
        assertEquals(24, tcpRule.dstIpPrefixLen);

        assertEquals(6, tcpRule.proto);
        assertEquals(1, tcpRule.srcportOrIcmptypeFirst);
        assertEquals(5487, tcpRule.srcportOrIcmptypeLast);
        assertEquals(87, tcpRule.dstportOrIcmpcodeFirst);
        assertEquals(6745, tcpRule.dstportOrIcmpcodeLast);
        assertEquals(1, tcpRule.tcpFlagsMask);
        assertEquals(7, tcpRule.tcpFlagsValue);
    }

    private void verifyIcmpv6Request(final int aclIndex) {
        final AclAddReplace request = aclAddReplaceRequestCaptor.getValue();
        assertEquals(aclIndex, request.aclIndex);
        assertEquals(1, request.count);
        assertEquals("icmp-v6-tag-value", new String(request.tag, StandardCharsets.US_ASCII));


        final AclRule icmpv6Rule = request.r[0];

        assertEquals(1, icmpv6Rule.isIpv6);
        assertEquals(1, icmpv6Rule.isPermit);
        assertTrue(
                Arrays.equals(new byte[]{32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 0, 2},
                        icmpv6Rule.srcIpAddr));
        assertEquals(48, icmpv6Rule.srcIpPrefixLen);
        assertTrue(
                Arrays.equals(new byte[]{32, 1, 13, -72, 10, 11, 18, -16, 0, 0, 0, 0, 0, 0, 0, 1},
                        icmpv6Rule.dstIpAddr));
        assertEquals(64, icmpv6Rule.dstIpPrefixLen);

        assertEquals(58, icmpv6Rule.proto);
        assertEquals(5, icmpv6Rule.srcportOrIcmptypeFirst);
        assertEquals(8, icmpv6Rule.srcportOrIcmptypeLast);
        assertEquals(1, icmpv6Rule.dstportOrIcmpcodeFirst);
        assertEquals(3, icmpv6Rule.dstportOrIcmpcodeLast);
        assertEquals(0, icmpv6Rule.tcpFlagsMask);
        assertEquals(0, icmpv6Rule.tcpFlagsValue);
    }

    private void verifyIcmpIpv4Request(final int aclIndex) {
        final AclAddReplace request = aclAddReplaceRequestCaptor.getValue();
        assertEquals(aclIndex, request.aclIndex);
        assertEquals(1, request.count);
        assertEquals("icmp-v4-tag-value", new String(request.tag, StandardCharsets.US_ASCII));

        final AclRule icmpRule = request.r[0];

        assertEquals(0, icmpRule.isIpv6);
        assertEquals(1, icmpRule.isPermit);
        assertTrue(Arrays.equals(new byte[]{-64, -88, 2, 2}, icmpRule.srcIpAddr));
        assertEquals(32, icmpRule.srcIpPrefixLen);
        assertTrue(Arrays.equals(new byte[]{-64, -88, 2, 1}, icmpRule.dstIpAddr));
        assertEquals(24, icmpRule.dstIpPrefixLen);

        assertEquals(1, icmpRule.proto);
        assertEquals(5, icmpRule.srcportOrIcmptypeFirst);
        assertEquals(8, icmpRule.srcportOrIcmptypeLast);
        assertEquals(1, icmpRule.dstportOrIcmpcodeFirst);
        assertEquals(3, icmpRule.dstportOrIcmpcodeLast);
        assertEquals(0, icmpRule.tcpFlagsMask);
        assertEquals(0, icmpRule.tcpFlagsValue);
    }
}