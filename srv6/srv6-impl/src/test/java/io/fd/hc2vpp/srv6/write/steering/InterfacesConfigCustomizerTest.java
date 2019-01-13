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

package io.fd.hc2vpp.srv6.write.steering;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.srv6.Srv6PolicyIIds;
import io.fd.hc2vpp.srv6.util.JvppRequestTest;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SrSteeringAddDel;
import io.fd.vpp.jvpp.core.dto.SrSteeringAddDelReply;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.autoroute.include.AutorouteInclude;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.Policies;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.Policy;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.VppL2AutorouteIncludeAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.segment.routing.traffic.engineering.policies.policy.autoroute.include.Interfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.Interface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.sr.interfaces._interface.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfacesConfigCustomizerTest extends JvppRequestTest {

    private static final PolicyKey POLICY_KEY = new PolicyKey(1L, new IpAddress(new Ipv6Address("e::1")));
    private static final Ipv6Address BSID_ADR = new Ipv6Address("a::e");
    private static final String LOCAL_0 = "local0";
    private static final InterfaceKey L2_KEY = new InterfaceKey(LOCAL_0);
    private static final Interface L2_STEERING = new InterfaceBuilder().setInputInterface(LOCAL_0)
            .setConfig(new ConfigBuilder().setInputInterface(LOCAL_0).build()).build();

    private InstanceIdentifier<Interface> L2_STEER_IID =
            Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, POLICY_KEY)
                    .child(AutorouteInclude.class)
                    .augmentation(VppL2AutorouteIncludeAugmentation.class).child(Interfaces.class)
                    .child(Interface.class, L2_KEY);

    private NamingContext interfaceContext;

    @InjectTestData(resourcePath = "/policy.json", id = POLICIES_LISTS_PATH)
    private Policies policies;

    @Captor
    private ArgumentCaptor<SrSteeringAddDel> requestcaptor;

    @Override
    protected void init() {
        when(api.srSteeringAddDel(any())).thenReturn(future(new SrSteeringAddDelReply()));
        defineMapping(mappingContext, LOCAL_0, 1, "interface-context");
        defineMapping(mappingContext, "vlan0", 2, "interface-context");
        when(ctx.getMappingContext()).thenReturn(mappingContext);
        interfaceContext = new NamingContext("iface", "interface-context");
        when(ctx.readAfter(Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, POLICY_KEY)))
                .thenReturn(Optional.of(policies.getPolicy().get(0)));
        when(ctx.readBefore(Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, POLICY_KEY)))
                .thenReturn(Optional.of(policies.getPolicy().get(0)));
    }

    @Test
    public void writeCurrentAttributesTest() throws WriteFailedException {
        InterfacesConfigCustomizer customizer = new InterfacesConfigCustomizer(api, interfaceContext);
        customizer.writeCurrentAttributes(L2_STEER_IID, L2_STEERING, ctx);

        verify(api, times(1)).srSteeringAddDel(requestcaptor.capture());
        SrSteeringAddDel srSteering = requestcaptor.getValue();

        testRequestValidity(srSteering, ByteDataTranslator.BYTE_FALSE);
    }

    @Test
    public void deleteCurrentAttributesV6Test() throws WriteFailedException {
        InterfacesConfigCustomizer customizer = new InterfacesConfigCustomizer(api, interfaceContext);
        customizer.deleteCurrentAttributes(L2_STEER_IID, L2_STEERING, ctx);

        verify(api, times(1)).srSteeringAddDel(requestcaptor.capture());
        SrSteeringAddDel srSteering = requestcaptor.getValue();

        testRequestValidity(srSteering, ByteDataTranslator.BYTE_TRUE);
    }

    private void testRequestValidity(SrSteeringAddDel srSteering, byte byteTrue) {
        Assert.assertArrayEquals(AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(BSID_ADR)),
                                 srSteering.bsidAddr);
        Assert.assertEquals(byteTrue, srSteering.isDel);
        Assert.assertEquals((byte) 2, srSteering.trafficType);
        Assert.assertEquals(1, srSteering.swIfIndex);
    }
}
