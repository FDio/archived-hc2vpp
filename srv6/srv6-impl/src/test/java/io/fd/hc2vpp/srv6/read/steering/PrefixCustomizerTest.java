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

import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.srv6.Srv6PolicyIIds;
import io.fd.honeycomb.translate.read.ReadFailedException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.autoroute.include.AutorouteInclude;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.Policy;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.Prefixes;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.PrefixesBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.Prefix;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.PrefixBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.PrefixKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class PrefixCustomizerTest extends SteeringTest {

    private static final IpPrefix IPV6_PREFIX = new IpPrefix(new Ipv6Prefix("a::/64"));
    private static final PrefixKey L3_KEY = new PrefixKey(IPV6_PREFIX);
    private static final IpPrefix IPV4_PREFIX = new IpPrefix(new Ipv4Prefix("10.0.0.1/24"));
    private static final PrefixKey L3_KEY_2 = new PrefixKey(IPV4_PREFIX);

    @Test
    public void readCurrentAttributesTest() throws ReadFailedException {
        KeyedInstanceIdentifier<Prefix, PrefixKey> key =
                Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, new PolicyKey(0L, new IpAddress(new Ipv6Address("e::1"))))
                        .child(AutorouteInclude.class).child(Prefixes.class).child(Prefix.class, L3_KEY);

        PrefixCustomizer customizer = new PrefixCustomizer(api);
        PrefixBuilder builder = customizer.getBuilder(key);

        customizer.readCurrentAttributes(key, builder, readCtx);

        Assert.assertEquals(L3_KEY, builder.key());
        Assert.assertEquals(IPV6_PREFIX, builder.getIpPrefix());

        PrefixesBuilder policyBuilder = new PrefixesBuilder();
        customizer.merge(policyBuilder, builder.build());

        Assert.assertNotNull(policyBuilder.getPrefix());
        Assert.assertEquals(1, policyBuilder.getPrefix().size());
    }

    @Test
    public void getAllIdsTest() throws ReadFailedException {
        PrefixCustomizer customizer = new PrefixCustomizer(api);
        List<PrefixKey> l3SteeringKeys = customizer.getAllIds(Srv6PolicyIIds.SR_TE_PLS_POL_AI_PFS_PF_IID, readCtx);

        Assert.assertNotNull(l3SteeringKeys);
        Assert.assertFalse(l3SteeringKeys.isEmpty());
        Assert.assertEquals(2, l3SteeringKeys.size());
        Assert.assertTrue(l3SteeringKeys.containsAll(ImmutableSet.of(L3_KEY, L3_KEY_2)));
    }
}
