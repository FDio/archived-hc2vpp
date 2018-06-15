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

import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.srv6.Srv6PolicyIIds;
import io.fd.honeycomb.translate.read.ReadFailedException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.autoroute.include.AutorouteInclude;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.Policy;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyKey;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.VppL2AutorouteIncludeAugmentation;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.segment.routing.traffic.engineering.policies.policy.autoroute.include.Interfaces;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.segment.routing.traffic.engineering.policies.policy.autoroute.include.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceCustomizerTest extends SteeringTest {

    private static final String LOCAL_0 = "local0";
    private static final InterfaceKey L2_KEY = new InterfaceKey(LOCAL_0);

    private NamingContext interfaceContext;

    @Override
    protected void init() {
        super.init();
        defineMapping(mappingContext, LOCAL_0, 1, "interface-context");
        defineMapping(mappingContext, "vlan0", 2, "interface-context");
        when(ctx.getMappingContext()).thenReturn(mappingContext);
        when(readCtx.getMappingContext()).thenReturn(mappingContext);
        interfaceContext = new NamingContext("iface", "interface-context");
    }

    @Test
    public void readCurrentAttributesTest() throws ReadFailedException {
        InstanceIdentifier<Interface> key =
                Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, new PolicyKey(0L, new IpAddress(new Ipv6Address("e::1"))))
                        .child(AutorouteInclude.class)
                        .augmentation(VppL2AutorouteIncludeAugmentation.class).child(Interfaces.class)
                        .child(Interface.class, L2_KEY);

        InterfaceCustomizer customizer = new InterfaceCustomizer(api, interfaceContext);
        InterfaceBuilder builder = customizer.getBuilder(key);
        customizer.readCurrentAttributes(key, builder, readCtx);

        Assert.assertEquals(L2_KEY, builder.getKey());
        Assert.assertEquals(LOCAL_0, builder.getInputInterface());

        InterfacesBuilder policyBuilder = new InterfacesBuilder();
        customizer.merge(policyBuilder, builder.build());

        Assert.assertNotNull(policyBuilder.getInterface());
        Assert.assertEquals(1, policyBuilder.getInterface().size());
    }

    @Test
    public void getAllIdsTest() throws ReadFailedException {
        InterfaceCustomizer customizer = new InterfaceCustomizer(api, interfaceContext);
        List<InterfaceKey> l2SteeringKeys = customizer.getAllIds(Srv6PolicyIIds.SR_TE_PLS_POL_AI_IFCS_IFC, readCtx);

        Assert.assertNotNull(l2SteeringKeys);
        Assert.assertFalse(l2SteeringKeys.isEmpty());
        Assert.assertEquals(1, l2SteeringKeys.size());
        Assert.assertTrue(l2SteeringKeys.containsAll(ImmutableSet.of(L2_KEY)));
    }
}
