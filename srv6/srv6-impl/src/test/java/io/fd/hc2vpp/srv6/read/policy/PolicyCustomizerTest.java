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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.srv6.Srv6PolicyIIds;
import io.fd.hc2vpp.srv6.util.Srv6Util;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.DataplaneType;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.PathExplicitlyDefined;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.ProvisioningMethodConfig;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.SegmentListOperState;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.candidate.paths.candidate.paths.CandidatePath;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.path.segment.list.properties.segment.lists.SegmentList;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.PoliciesBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.Policy;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.policy.context.rev180607.srv6.candidate.path.context.attributes.srv6.candidate.path.mappings.Srv6CandidatePathMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.policy.context.rev180607.srv6.policy.context.attributes.srv6.policy.mappings.Srv6PolicyMappingBuilder;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class PolicyCustomizerTest extends PoliciesTest {

    private PolicyKey POLICY_KEY = new PolicyKey(COLOR_1, ENDPOINT_1);
    private PolicyKey POLICY_KEY_2 = new PolicyKey(COLOR_2, ENDPOINT_2);

    @Override
    public void init() {
        super.init();
        when(policyCtx.getPolicy(eq(BSID_ADR), any(MappingContext.class))).thenReturn(
                new Srv6PolicyMappingBuilder().setBsid(BSID_ADR).setColor(COLOR_1)
                        .setEndpoint(ENDPOINT_1.getIpv6Address()).setName(BSID_ADR.getValue()).build());
        when(policyCtx.getPolicy(eq(BSID_2_ADR), any(MappingContext.class))).thenReturn(
                new Srv6PolicyMappingBuilder().setBsid(BSID_2_ADR).setColor(COLOR_2)
                        .setEndpoint(ENDPOINT_2.getIpv6Address()).setName(BSID_2_ADR.getValue()).build());
        when(policyCtx.getPolicyBsid(COLOR_1, ENDPOINT_1.getIpv6Address(), mappingContext)).thenReturn(BSID_ADR);
        when(policyCtx.getPolicyBsid(COLOR_2, ENDPOINT_2.getIpv6Address(), mappingContext)).thenReturn(BSID_2_ADR);

        when(candidatePathCtx.getCandidatePath(BSID_ADR, mappingContext)).thenReturn(
                new Srv6CandidatePathMappingBuilder().setBsid(BSID_ADR).setDistinguisher(DISTINGUISHER_1)
                        .setPreference(PREFERENCE_1).setName(BSID_ADR.getValue())
                        .setProvisioningMethod(PROVISIONING_METHOD).build());
        when(candidatePathCtx.getCandidatePath(BSID_2_ADR, mappingContext)).thenReturn(
                new Srv6CandidatePathMappingBuilder().setBsid(BSID_2_ADR).setDistinguisher(DISTINGUISHER_2)
                        .setPreference(PREFERENCE_2).setName(BSID_2_ADR.getValue())
                        .setProvisioningMethod(PROVISIONING_METHOD).build());
    }

    @Test
    public void getAllIdsTest() throws ReadFailedException {
        PolicyCustomizer customizer = new PolicyCustomizer(api, policyCtx, candidatePathCtx);
        List<PolicyKey> policyKeys = customizer.getAllIds(Srv6PolicyIIds.SR_TE_PLS_POL, readCtx);

        Assert.assertNotNull(policyKeys);
        Assert.assertFalse(policyKeys.isEmpty());
        Assert.assertEquals(replyDump.srPoliciesDetails.size(), policyKeys.size());
        Assert.assertTrue(policyKeys.containsAll(ImmutableSet.of(POLICY_KEY, POLICY_KEY_2)));
    }

    @Test
    public void readCurrentAttributesTest() throws ReadFailedException {
        PolicyCustomizer customizer = new PolicyCustomizer(api, policyCtx, candidatePathCtx);
        PolicyBuilder policyBuilder = new PolicyBuilder();
        KeyedInstanceIdentifier<Policy, PolicyKey> key = Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, POLICY_KEY);
        customizer.readCurrentAttributes(key, policyBuilder, readCtx);

        Assert.assertEquals(POLICY_KEY, policyBuilder.getKey());
        Assert.assertEquals(BSID_ADR.getValue(), policyBuilder.getName());
        Assert.assertNotNull(customizer.getBuilder(key));

        //verify BSID container
        Assert.assertNotNull(policyBuilder.getBindingSid());
        Assert.assertNotNull(policyBuilder.getBindingSid().getState());
        Assert.assertEquals(DataplaneType.Srv6, policyBuilder.getBindingSid().getState().getType());
        Assert.assertEquals(BSID_ADR,
                policyBuilder.getBindingSid().getState().getValue().getIpAddress().getIpv6Address());

        //verify state container
        Assert.assertNotNull(policyBuilder.getState());
        Assert.assertEquals(COLOR_1, policyBuilder.getState().getColor());
        Assert.assertEquals(ENDPOINT_1, policyBuilder.getState().getEndpoint());
        Assert.assertEquals(BSID_ADR.getValue(), policyBuilder.getState().getName());

        //verify candidate-paths container
        Assert.assertNotNull(policyBuilder.getCandidatePaths());
        Assert.assertNotNull(policyBuilder.getCandidatePaths().getCandidatePath());
        Assert.assertEquals(1, policyBuilder.getCandidatePaths().getCandidatePath().size());
        CandidatePath candidatePath = policyBuilder.getCandidatePaths().getCandidatePath().get(0);
        Assert.assertEquals(DISTINGUISHER_1, candidatePath.getDistinguisher().longValue());
        Assert.assertEquals(PREFERENCE_1, candidatePath.getPreference().longValue());
        Assert.assertEquals(ProvisioningMethodConfig.class, candidatePath.getProvisioningMethod());

        //verify candidate-paths/state container
        Assert.assertNotNull(candidatePath.getState());
        Assert.assertEquals(DISTINGUISHER_1, candidatePath.getState().getDistinguisher().longValue());
        Assert.assertEquals(PREFERENCE_1, candidatePath.getState().getPreference().longValue());
        Assert.assertEquals(ProvisioningMethodConfig.class, candidatePath.getState().getProvisioningMethod());
        Assert.assertEquals(PathExplicitlyDefined.class, candidatePath.getState().getComputationMethod());
        Assert.assertNotNull(candidatePath.getBindingSid());
        Assert.assertNotNull(candidatePath.getBindingSid().getState());
        Assert.assertEquals(BSID_ADR.getValue(),
                candidatePath.getBindingSid().getState().getValue().getIpAddress().getIpv6Address().getValue());

        //verify candidate-paths/segment-lists container
        Assert.assertNotNull(candidatePath.getSegmentLists());
        Assert.assertNotNull(candidatePath.getSegmentLists().getSegmentList());
        Assert.assertEquals(1, candidatePath.getSegmentLists().getSegmentList().size());
        SegmentList segmentList = candidatePath.getSegmentLists().getSegmentList().get(0);
        Assert.assertEquals(Srv6Util.getCandidatePathName(BSID_ADR, WEIGHT), segmentList.getName());
        Assert.assertNotNull(segmentList.getState());
        Assert.assertEquals(WEIGHT, segmentList.getState().getWeight().intValue());
        Assert.assertEquals(SegmentListOperState.ACTIVE, segmentList.getState().getOperState());

        //verify merge
        PoliciesBuilder policiesBuilder = new PoliciesBuilder();
        Policy policy = policyBuilder.build();
        customizer.merge(policiesBuilder, policy);
        Assert.assertEquals(policy, policiesBuilder.getPolicy().get(0));
    }
}
