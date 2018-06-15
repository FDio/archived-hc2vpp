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

package io.fd.hc2vpp.srv6.read.steering;

import io.fd.hc2vpp.srv6.Srv6PolicyIIds;
import io.fd.honeycomb.translate.read.ReadFailedException;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.autoroute.include.AutorouteInclude;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.Policy;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.Prefixes;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.PrefixesBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.State;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.StateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PrefixesStateCustomizerTest extends SteeringTest {

    @Test
    public void readCurrentAttributesTest() throws ReadFailedException {
        InstanceIdentifier<State> iId =
                Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, new PolicyKey(0L, new IpAddress(new Ipv6Address("e::1"))))
                        .child(AutorouteInclude.class).child(Prefixes.class).child(State.class);

        PrefixesStateCustomizer customizer = new PrefixesStateCustomizer(api);
        StateBuilder builder = customizer.getBuilder(Srv6PolicyIIds.SR_TE_PLS_POL_AI_PFS_STATE);

        customizer.readCurrentAttributes(iId, builder, readCtx);
        Assert.assertEquals(false, builder.isPrefixesAll());

        PrefixesBuilder parentBuilder = new PrefixesBuilder();
        customizer.merge(parentBuilder, builder.build());

        Assert.assertNotNull(parentBuilder.getState());
        Assert.assertEquals(false, parentBuilder.getState().isPrefixesAll());
    }
}
