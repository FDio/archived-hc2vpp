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


package io.fd.hc2vpp.srv6.util;

import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.hc2vpp.srv6.Srv6PolicyIIds;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.Policies;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.Policy;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Srv6UtilTest extends JvppRequestTest{
    private static final Ipv6Address BSID = new Ipv6Address("a::e");
    private static final String CANDIDATE_PATH_NAME = BSID.getValue() + "-0";
    private static final PolicyKey POLICY_KEY = new PolicyKey(1L, new IpAddress(new Ipv6Address("e::1")));
    private static final InstanceIdentifier<Policy> POLICY_IID =
            Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, POLICY_KEY);

    @InjectTestData(resourcePath = "/policy.json", id = POLICIES_LISTS_PATH)
    private Policies policies;

    @Override
    protected void init() {
        when(ctx.readAfter(Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, POLICY_KEY)))
                .thenReturn(Optional.of(policies.getPolicy().get(0)));
    }

    @Test
    public void getCandidatePathNameTest() {
        Assert.assertEquals(CANDIDATE_PATH_NAME, Srv6Util.getCandidatePathName(BSID, 0L));
    }

    @Test
    public void extractBsidTest() {
        Assert.assertEquals(BSID.getValue(), Srv6Util.extractBsid(POLICY_IID, ctx, true).getValue());
    }

    @Test
    public void extractVrfFibTest() {
        Assert.assertEquals(0, Srv6Util.extractVrfFib(POLICY_IID, ctx, true));
    }
}
