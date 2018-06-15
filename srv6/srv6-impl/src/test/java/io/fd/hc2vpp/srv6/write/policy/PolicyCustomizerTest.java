/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6.write.policy;

import static io.fd.hc2vpp.srv6.util.Srv6Util.getCandidatePathName;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.hc2vpp.srv6.Srv6PolicyIIds;
import io.fd.hc2vpp.srv6.util.CandidatePathContextManager;
import io.fd.hc2vpp.srv6.util.JvppRequestTest;
import io.fd.hc2vpp.srv6.util.PolicyContextManager;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SrPolicyAdd;
import io.fd.vpp.jvpp.core.dto.SrPolicyAddReply;
import io.fd.vpp.jvpp.core.dto.SrPolicyDel;
import io.fd.vpp.jvpp.core.dto.SrPolicyDelReply;
import io.fd.vpp.jvpp.core.dto.SrPolicyMod;
import io.fd.vpp.jvpp.core.dto.SrPolicyModReply;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.NamedSegmentLists;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentList;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentListKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.Policies;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.Policy;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class PolicyCustomizerTest extends JvppRequestTest {

    private static final VniReference TABLE_ID = new VniReference(0L);
    private static final Ipv6Address BSID_ADR = new Ipv6Address("a::e");
    private static final String PATH_NAME = getCandidatePathName(BSID_ADR, 0L);
    private static final String PATH_NAME_2 = getCandidatePathName(BSID_ADR, 1L);
    private static final Ipv6AddressNoZone A_ADDR = new Ipv6AddressNoZone("a::");
    private static final Ipv6AddressNoZone B_ADDR = new Ipv6AddressNoZone("b::");
    private static final Ipv6AddressNoZone C_ADDR = new Ipv6AddressNoZone("c::");
    private static final PolicyKey POLICY_KEY = new PolicyKey(1L, new IpAddress(new Ipv6Address("e::1")));
    private static final KeyedInstanceIdentifier<Table, TableKey> VRF_IID =
            FibManagementIIds.FM_FIB_TABLES.child(Table.class, new TableKey(Ipv6.class, TABLE_ID));
    private static final Table VRF_TABLE = new TableBuilder().setKey(VRF_IID.getKey()).setAddressFamily(Ipv6.class)
            .setTableId(TABLE_ID).setName("VRF-0").build();

    @InjectTestData(resourcePath = "/named-segments.json", id = NAMED_SEG_LISTS_PATH)
    private NamedSegmentLists namedSegmentLists;
    @InjectTestData(resourcePath = "/policy.json", id = POLICIES_LISTS_PATH)
    private Policies policies;

    @Mock
    private PolicyContextManager policyCtx;
    @Mock
    private CandidatePathContextManager candidatePathCtx;

    @Captor
    private ArgumentCaptor<SrPolicyAdd> requestcaptorAdd;
    @Captor
    private ArgumentCaptor<SrPolicyDel> requestcaptorDel;
    @Captor
    private ArgumentCaptor<SrPolicyMod> requestcaptorMod;

    @Override
    protected void init() {
        when(api.srPolicyAdd(any())).thenReturn(future(new SrPolicyAddReply()));
        when(api.srPolicyDel(any())).thenReturn(future(new SrPolicyDelReply()));
        when(api.srPolicyMod(any())).thenReturn(future(new SrPolicyModReply()));
        when(ctx.readAfter(
                Srv6PolicyIIds.SR_TE_NSLS.child(NamedSegmentList.class, new NamedSegmentListKey(PATH_NAME))))
                .thenReturn(Optional.of(namedSegmentLists.getNamedSegmentList().get(0)));
        when(ctx.readAfter(
                Srv6PolicyIIds.SR_TE_NSLS.child(NamedSegmentList.class, new NamedSegmentListKey(PATH_NAME_2))))
                .thenReturn(Optional.of(namedSegmentLists.getNamedSegmentList().get(1)));
        when(ctx.readAfter(Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, POLICY_KEY)))
                .thenReturn(Optional.of(policies.getPolicy().get(0)));
        when(ctx.readBefore(Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, POLICY_KEY)))
                .thenReturn(Optional.of(policies.getPolicy().get(0)));
        when(ctx.readAfter(VRF_IID)).thenReturn(Optional.of(VRF_TABLE));
    }

    @Test
    public void writeCurrentAttributesTest() throws WriteFailedException {
        PolicyCustomizer customizer = new PolicyCustomizer(api, policyCtx, candidatePathCtx);
        customizer.writeCurrentAttributes(Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, POLICY_KEY),
                policies.getPolicy().get(0), ctx);

        verify(api, times(1)).srPolicyAdd(requestcaptorAdd.capture());
        SrPolicyAdd srPolicy = requestcaptorAdd.getValue();

        verify(api, times(1)).srPolicyMod(requestcaptorMod.capture());
        SrPolicyMod srPolicyMod = requestcaptorMod.getValue();

        Assert.assertEquals(0L, srPolicy.fibTable);
        Assert.assertArrayEquals(AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(BSID_ADR)),
                srPolicy.bsidAddr);
        Assert.assertEquals(ByteDataTranslator.BYTE_TRUE, srPolicy.isEncap);
        Assert.assertEquals(0, srPolicy.type);
        Assert.assertNotNull(srPolicy.sids);
        Assert.assertEquals(3, srPolicy.sids.numSids);
        Assert.assertEquals(0, srPolicy.sids.weight);
        Assert.assertArrayEquals(AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(B_ADDR)),
                srPolicy.sids.sids[0].addr);
        Assert.assertArrayEquals(AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(C_ADDR)),
                srPolicy.sids.sids[1].addr);
        Assert.assertArrayEquals(AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(A_ADDR)),
                srPolicy.sids.sids[2].addr);

        Assert.assertEquals(0L, srPolicyMod.fibTable);
        Assert.assertArrayEquals(AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(BSID_ADR)),
                srPolicyMod.bsidAddr);
        Assert.assertNotNull(srPolicy.sids);
        Assert.assertEquals(3, srPolicyMod.sids.numSids);
        Assert.assertEquals(1, srPolicyMod.sids.weight);
        Assert.assertArrayEquals(AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(C_ADDR)),
                srPolicyMod.sids.sids[0].addr);
        Assert.assertArrayEquals(AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(B_ADDR)),
                srPolicyMod.sids.sids[1].addr);
        Assert.assertArrayEquals(AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(A_ADDR)),
                srPolicyMod.sids.sids[2].addr);
    }

    @Test
    public void deleteCurrentAttributesTest() throws WriteFailedException {
        PolicyCustomizer customizer = new PolicyCustomizer(api, policyCtx, candidatePathCtx);
        customizer.deleteCurrentAttributes(Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, POLICY_KEY),
                policies.getPolicy().get(0), ctx);

        verify(api, times(1)).srPolicyDel(requestcaptorDel.capture());
        SrPolicyDel srPolicy = requestcaptorDel.getValue();

        Assert.assertArrayEquals(AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(BSID_ADR)),
                srPolicy.bsidAddr.addr);
    }

}
