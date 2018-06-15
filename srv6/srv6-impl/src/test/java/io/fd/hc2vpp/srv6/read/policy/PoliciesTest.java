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


package io.fd.hc2vpp.srv6.read.policy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.srv6.util.CandidatePathContextManager;
import io.fd.hc2vpp.srv6.util.PolicyContextManager;
import io.fd.hc2vpp.srv6.write.sid.request.LocalSidRequestTest;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.vpp.jvpp.core.dto.SrPoliciesDetails;
import io.fd.vpp.jvpp.core.dto.SrPoliciesDetailsReplyDump;
import io.fd.vpp.jvpp.core.types.Srv6Sid;
import io.fd.vpp.jvpp.core.types.Srv6SidList;
import java.util.Arrays;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.ProvisioningMethodConfig;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.ProvisioningMethodType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;

public abstract class PoliciesTest extends LocalSidRequestTest {
    Ipv6AddressNoZone BSID_ADR = new Ipv6AddressNoZone("a::e");
    Ipv6AddressNoZone BSID_2_ADR = new Ipv6AddressNoZone("e::a");
    IpAddress ENDPOINT_1 = new IpAddress(new Ipv6Address("e::1"));
    IpAddress ENDPOINT_2 = new IpAddress(new Ipv6Address("a::1"));
    Long COLOR_1 = 1L;
    Long COLOR_2 = 2L;
    long DISTINGUISHER_1 = 1L;
    long DISTINGUISHER_2 = 2L;
    long PREFERENCE_1 = 100L;
    long PREFERENCE_2 = 200L;
    Class<? extends ProvisioningMethodType> PROVISIONING_METHOD = ProvisioningMethodConfig.class;
    int WEIGHT = 0;
    SrPoliciesDetailsReplyDump replyDump = new SrPoliciesDetailsReplyDump();

    @Mock
    ReadContext readCtx;
    @Mock
    PolicyContextManager policyCtx;
    @Mock
    CandidatePathContextManager candidatePathCtx;
    Ipv6AddressNoZone A_ADDR = new Ipv6AddressNoZone("a::");
    Ipv6AddressNoZone B_ADDR = new Ipv6AddressNoZone("b::");
    Ipv6AddressNoZone C_ADDR = new Ipv6AddressNoZone("c::");

    @Mock
    private
    ModificationCache modificationCache;
    private Srv6Sid A = new Srv6Sid();
    private Srv6Sid B = new Srv6Sid();
    private Srv6Sid C = new Srv6Sid();
    private SrPoliciesDetails srPoliciesDetails1 = new SrPoliciesDetails();
    private SrPoliciesDetails srPoliciesDetails2 = new SrPoliciesDetails();

    @Override
    public void init() {
        Srv6Sid bsid = new Srv6Sid();
        bsid.addr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(BSID_ADR));
        srPoliciesDetails1.bsid = bsid;

        srPoliciesDetails1.numSidLists = 1;
        Srv6SidList[] srv6SidLists = new Srv6SidList[1];
        Srv6SidList srv6SidList = new Srv6SidList();
        srv6SidList.weight = WEIGHT;
        srv6SidList.numSids = 3;
        A.addr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(A_ADDR));
        B.addr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(B_ADDR));
        C.addr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(C_ADDR));

        srv6SidList.sids = ImmutableSet.of(B, C, A).toArray(new Srv6Sid[3]);

        srv6SidLists[0] = srv6SidList;
        srPoliciesDetails1.sidLists = srv6SidLists;
        // is_encap - behavior of SR policy.(0.SRH insert // 1.Encapsulation)
        srPoliciesDetails1.isEncap = ByteDataTranslator.BYTE_TRUE;
        // type is the type of the SR policy. (0.Default // 1.Spray)
        srPoliciesDetails1.type = 0;

        Srv6Sid bsid2 = new Srv6Sid();
        bsid2.addr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(BSID_2_ADR));
        srPoliciesDetails2.bsid = bsid2;
        srPoliciesDetails2.numSidLists = 1;
        Srv6SidList[] srv6SidLists2 = new Srv6SidList[1];
        Srv6SidList srv6SidList2 = new Srv6SidList();
        srv6SidList2.weight = WEIGHT;
        srv6SidList2.numSids = 3;

        srv6SidList2.sids = ImmutableSet.of(A, C, B).toArray(new Srv6Sid[3]);
        srv6SidLists2[0] = srv6SidList2;
        srPoliciesDetails2.sidLists = srv6SidLists2;
        srPoliciesDetails2.isEncap = ByteDataTranslator.BYTE_TRUE;
        srPoliciesDetails2.type = 0;

        replyDump.srPoliciesDetails = Arrays.asList(srPoliciesDetails1, srPoliciesDetails2);

        when(readCtx.getModificationCache()).thenReturn(modificationCache);
        when(readCtx.getMappingContext()).thenReturn(mappingContext);
        when(modificationCache.get(any())).thenReturn(replyDump);
    }
}
